package com.princesanitary.grahak.ui.consumer.checkout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.princesanitary.grahak.data.model.*
import com.princesanitary.grahak.data.repository.CartRepository
import com.princesanitary.grahak.data.repository.OrderRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class CheckoutUiState {
    object Loading : CheckoutUiState()
    data class Success(
        val cart: Cart,
        val summary: CartSummary,
        val selectedAddress: SavedAddress?,
        val selectedDeliveryType: DeliveryType,
        val selectedPaymentMethod: PaymentMethod,
        val customerName: String = "",
        val customerPhone: String = "",
        val deliveryFee: Double = 0.0,
        val discountAmount: Double = 0.0,
        val appliedCoupon: Coupon? = null,
        val couponError: String? = null,
        val storeSettings: StoreSettings = StoreSettings(),
        val availableCoupons: List<Coupon> = emptyList()
    ) : CheckoutUiState() {
        val grandTotal: Double
            get() = (summary.subtotal + deliveryFee - discountAmount).coerceAtLeast(0.0)
    }
    data class Error(val message: String) : CheckoutUiState()
}

sealed class CheckoutActionState {
    object Idle : CheckoutActionState()
    object PlacingOrder : CheckoutActionState()
    data class OrderPlaced(val orderId: String) : CheckoutActionState()
    data class Error(val message: String) : CheckoutActionState()
}

class CheckoutViewModel @Inject constructor(
    private val cartRepository: CartRepository,
    private val orderRepository: OrderRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<CheckoutUiState>(CheckoutUiState.Loading)
    val uiState: StateFlow<CheckoutUiState> = _uiState.asStateFlow()

    private val _actionState = MutableStateFlow<CheckoutActionState>(CheckoutActionState.Idle)
    val actionState: StateFlow<CheckoutActionState> = _actionState.asStateFlow()

    // Saharanpur (UP - 247001) service area addresses
    private val defaultAddresses = mutableListOf(
        SavedAddress(
            tag = "Home",
            fullAddress = "Prince Sanitary Store, Numaish Camp, Saharanpur, UP - 247001",
            lat = 29.9680,
            lng = 77.5552
        ),
        SavedAddress(
            tag = "Site",
            fullAddress = "Plot 24, Delhi Road, Near ITI, Saharanpur, UP - 247001",
            lat = 29.9560,
            lng = 77.5410
        ),
        SavedAddress(
            tag = "Work",
            fullAddress = "Mission Compound, Chilkana Road, Saharanpur, UP - 247001",
            lat = 29.9750,
            lng = 77.5620
        )
    )

    private val _availableAddresses = MutableStateFlow<List<SavedAddress>>(defaultAddresses)
    val availableAddresses: StateFlow<List<SavedAddress>> = _availableAddresses.asStateFlow()

    private val _isPlumber = MutableStateFlow(false)
    val isPlumber: StateFlow<Boolean> = _isPlumber.asStateFlow()

    init {
        checkPlumberStatus()
        loadCheckoutData()
    }

    private fun checkPlumberStatus() {
        viewModelScope.launch {
            _isPlumber.value = orderRepository.isCurrentUserPlumber()
        }
    }

    private fun loadCheckoutData() {
        viewModelScope.launch {
            _uiState.value = CheckoutUiState.Loading

            val settings = orderRepository.getStoreSettings()
            val coupons = orderRepository.getActiveCoupons()

            cartRepository.getCart().collect { result ->
                result.fold(
                    onSuccess = { cart ->
                        if (cart.items.isEmpty()) {
                            _uiState.value = CheckoutUiState.Error("Cart is empty")
                        } else {
                            val summary = cartRepository.calculateSummary(cart)
                            val currentState = _uiState.value

                            val initialDeliveryType = if (currentState is CheckoutUiState.Success)
                                currentState.selectedDeliveryType
                            else
                                DeliveryType.SITE_DROP

                            val fee = if (initialDeliveryType == DeliveryType.SITE_DROP) {
                                if (summary.subtotal >= settings.freeDeliveryThreshold) 0.0 else settings.deliveryFee
                            } else 0.0

                            _uiState.value = CheckoutUiState.Success(
                                cart = cart,
                                summary = summary,
                                selectedAddress = if (currentState is CheckoutUiState.Success && currentState.selectedAddress != null)
                                    currentState.selectedAddress
                                else
                                    _availableAddresses.value.firstOrNull(),
                                selectedDeliveryType = initialDeliveryType,
                                selectedPaymentMethod = if (currentState is CheckoutUiState.Success)
                                    currentState.selectedPaymentMethod
                                else
                                    PaymentMethod.COD,
                                customerName = if (currentState is CheckoutUiState.Success) currentState.customerName else "",
                                customerPhone = if (currentState is CheckoutUiState.Success) currentState.customerPhone else "",
                                deliveryFee = fee,
                                discountAmount = if (currentState is CheckoutUiState.Success) currentState.discountAmount else 0.0,
                                appliedCoupon = if (currentState is CheckoutUiState.Success) currentState.appliedCoupon else null,
                                storeSettings = settings,
                                availableCoupons = coupons
                            )
                        }
                    },
                    onFailure = { error ->
                        _uiState.value = CheckoutUiState.Error(
                            error.message ?: "Failed to load checkout data"
                        )
                    }
                )
            }
        }
    }

    fun updateCustomerDetails(name: String, phone: String) {
        val currentState = _uiState.value
        if (currentState is CheckoutUiState.Success) {
            _uiState.value = currentState.copy(
                customerName = name,
                customerPhone = phone
            )
        }
    }

    fun applyCoupon(code: String) {
        viewModelScope.launch {
            val currentState = _uiState.value as? CheckoutUiState.Success ?: return@launch
            orderRepository.validateCoupon(code, currentState.summary.subtotal).fold(
                onSuccess = { coupon ->
                    val isPercentage = coupon.discountType.equals(DiscountType.PERCENTAGE, ignoreCase = true) ||
                        coupon.discountType.equals("PERCENTAGE", ignoreCase = true)
                    val rawDiscount = if (isPercentage) {
                        val calc = currentState.summary.subtotal * (coupon.discountValue / 100.0)
                        if (coupon.maxDiscountAmount > 0.0) {
                            calc.coerceAtMost(coupon.maxDiscountAmount)
                        } else {
                            calc
                        }
                    } else {
                        coupon.discountValue
                    }
                    val discount = rawDiscount.coerceAtMost(currentState.summary.subtotal).coerceAtLeast(0.0)

                    _uiState.value = currentState.copy(
                        appliedCoupon = coupon,
                        discountAmount = Math.round(discount * 100.0) / 100.0,
                        couponError = null
                    )
                },
                onFailure = { error ->
                    _uiState.value = currentState.copy(
                        couponError = error.message ?: "Invalid coupon"
                    )
                }
            )
        }
    }

    fun removeCoupon() {
        val currentState = _uiState.value
        if (currentState is CheckoutUiState.Success) {
            _uiState.value = currentState.copy(
                appliedCoupon = null,
                discountAmount = 0.0,
                couponError = null
            )
        }
    }

    fun selectAddress(address: SavedAddress) {
        val currentState = _uiState.value
        if (currentState is CheckoutUiState.Success) {
            _uiState.value = currentState.copy(selectedAddress = address)
        }
    }

    fun addAndSelectAddress(address: SavedAddress) {
        val updated = mutableListOf<SavedAddress>()
        updated.add(address)
        updated.addAll(_availableAddresses.value.filter { it.fullAddress != address.fullAddress })
        _availableAddresses.value = updated
        selectAddress(address)
    }

    fun selectDeliveryType(type: DeliveryType) {
        val currentState = _uiState.value
        if (currentState is CheckoutUiState.Success) {
            val fee = if (type == DeliveryType.SITE_DROP) {
                if (currentState.summary.subtotal >= currentState.storeSettings.freeDeliveryThreshold) 0.0
                else currentState.storeSettings.deliveryFee
            } else {
                0.0
            }
            _uiState.value = currentState.copy(
                selectedDeliveryType = type,
                deliveryFee = fee
            )
        }
    }

    fun selectPaymentMethod(method: PaymentMethod) {
        val currentState = _uiState.value
        if (currentState is CheckoutUiState.Success) {
            _uiState.value = currentState.copy(selectedPaymentMethod = method)
        }
    }

    fun placeOrder(notes: String = "") {
        viewModelScope.launch {
            val currentState = _uiState.value
            if (currentState !is CheckoutUiState.Success) {
                _actionState.value = CheckoutActionState.Error("Invalid checkout state")
                return@launch
            }

            if (currentState.customerName.trim().length < 2) {
                _actionState.value = CheckoutActionState.Error("Please enter your name")
                return@launch
            }

            val cleanPhone = currentState.customerPhone.filter { it.isDigit() }
            if (cleanPhone.length < 10) {
                _actionState.value = CheckoutActionState.Error("Please enter a valid 10-digit mobile number")
                return@launch
            }

            if (currentState.selectedAddress == null) {
                _actionState.value = CheckoutActionState.Error("Please select a delivery address")
                return@launch
            }

            _actionState.value = CheckoutActionState.PlacingOrder

            // Convert cart items to order items
            val orderItems = currentState.cart.items.map { cartItem ->
                OrderItem(
                    productId = cartItem.productId,
                    name = cartItem.productName,
                    itemNumber = cartItem.itemNumber.ifBlank { "PS-${1000 + (kotlin.math.abs(cartItem.productId.hashCode()) % 9000)}" },
                    size = cartItem.size,
                    brand = cartItem.brand,
                    qty = cartItem.quantity,
                    unitPrice = cartItem.unitPrice,
                    gstRate = cartItem.gstRate,
                    total = cartItem.total
                )
            }

            // Create order
            orderRepository.createOrder(
                items = orderItems,
                deliveryAddress = currentState.selectedAddress,
                deliveryType = currentState.selectedDeliveryType,
                paymentMethod = currentState.selectedPaymentMethod,
                customerName = currentState.customerName.trim(),
                customerPhone = cleanPhone,
                deliveryFee = currentState.deliveryFee,
                discountAmount = currentState.discountAmount,
                couponCode = currentState.appliedCoupon?.code.orEmpty(),
                notes = notes
            ).fold(
                onSuccess = { orderId ->
                    // Clear cart after successful order
                    cartRepository.clearCart()
                    _actionState.value = CheckoutActionState.OrderPlaced(orderId)
                },
                onFailure = { error ->
                    _actionState.value = CheckoutActionState.Error(
                        error.message ?: "Failed to place order"
                    )
                }
            )
        }
    }

    fun getAvailableAddresses(): List<SavedAddress> {
        return _availableAddresses.value
    }

    fun resetActionState() {
        _actionState.value = CheckoutActionState.Idle
    }
}
