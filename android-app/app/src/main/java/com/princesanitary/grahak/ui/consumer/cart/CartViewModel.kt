package com.princesanitary.grahak.ui.consumer.cart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.princesanitary.grahak.data.model.Cart
import com.princesanitary.grahak.data.model.CartSummary
import com.princesanitary.grahak.data.model.Coupon
import com.princesanitary.grahak.data.model.Product
import com.princesanitary.grahak.data.model.StoreSettings
import com.princesanitary.grahak.data.repository.CartRepository
import com.princesanitary.grahak.data.repository.OrderRepository
import com.princesanitary.grahak.di.ServiceLocator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class CartUiState {
    object Loading : CartUiState()
    data class Success(
        val cart: Cart,
        val summary: CartSummary,
        val storeSettings: StoreSettings = StoreSettings(),
        val availableCoupons: List<Coupon> = emptyList()
    ) : CartUiState()
    data class Error(val message: String) : CartUiState()
}

class CartViewModel @Inject constructor(
    private val cartRepository: CartRepository,
    private val orderRepository: OrderRepository = ServiceLocator.orderRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<CartUiState>(CartUiState.Loading)
    val uiState: StateFlow<CartUiState> = _uiState.asStateFlow()

    private val _actionState = MutableStateFlow<ActionState>(ActionState.Idle)
    val actionState: StateFlow<ActionState> = _actionState.asStateFlow()

    init {
        loadCart()
    }

    private fun loadCart() {
        viewModelScope.launch {
            _uiState.value = CartUiState.Loading
            
            val settings = orderRepository.getStoreSettings()
            val coupons = orderRepository.getActiveCoupons()

            cartRepository.getCart().collect { result ->
                result.fold(
                    onSuccess = { cart ->
                        val summary = cartRepository.calculateSummary(cart)
                        _uiState.value = CartUiState.Success(cart, summary, settings, coupons)
                    },
                    onFailure = { error ->
                        _uiState.value = CartUiState.Error(
                            error.message ?: "Failed to load cart"
                        )
                    }
                )
            }
        }
    }

    /**
     * Add a product to the cart with instant optimistic UI update.
     */
    fun addToCart(product: Product, quantity: Int = 1) {
        // Instant optimistic update
        val current = _uiState.value
        if (current is CartUiState.Success) {
            val existingIdx = current.cart.items.indexOfFirst { it.productId == product.id }
            val updatedItems = if (existingIdx >= 0) {
                current.cart.items.toMutableList().apply {
                    this[existingIdx] = this[existingIdx].copy(quantity = this[existingIdx].quantity + quantity)
                }
            } else {
                current.cart.items + com.princesanitary.grahak.data.model.CartItem(
                    productId = product.id,
                    productName = product.name,
                    quantity = quantity,
                    unitPrice = product.unitPrice,
                    gstRate = product.tax.gstRate,
                    imageUrl = product.mediaUrls.firstOrNull() ?: "",
                    addedAt = java.util.Date()
                )
            }
            val newCart = current.cart.copy(items = updatedItems)
            val newSummary = cartRepository.calculateSummary(newCart)
            _uiState.value = current.copy(cart = newCart, summary = newSummary)
        }

        viewModelScope.launch {
            _actionState.value = ActionState.Loading

            cartRepository.addToCart(product, quantity).fold(
                onSuccess = {
                    _actionState.value = ActionState.Success("${product.name} added to cart")
                },
                onFailure = { error ->
                    _actionState.value = ActionState.Error(
                        error.message ?: "Could not add this item to your cart"
                    )
                }
            )
        }
    }

    fun updateQuantity(productId: String, newQuantity: Int) {
        // Instant optimistic update
        val current = _uiState.value
        if (current is CartUiState.Success) {
            val updatedItems = if (newQuantity <= 0) {
                current.cart.items.filter { it.productId != productId }
            } else {
                current.cart.items.map {
                    if (it.productId == productId) it.copy(quantity = newQuantity) else it
                }
            }
            val newCart = current.cart.copy(items = updatedItems)
            val newSummary = cartRepository.calculateSummary(newCart)
            _uiState.value = current.copy(cart = newCart, summary = newSummary)
        }

        viewModelScope.launch {
            _actionState.value = ActionState.Loading
            
            cartRepository.updateQuantity(productId, newQuantity).fold(
                onSuccess = {
                    _actionState.value = ActionState.Success("Quantity updated")
                },
                onFailure = { error ->
                    _actionState.value = ActionState.Error(
                        error.message ?: "Failed to update quantity"
                    )
                }
            )
        }
    }

    fun removeItem(productId: String) {
        // Instant optimistic update
        val current = _uiState.value
        if (current is CartUiState.Success) {
            val updatedItems = current.cart.items.filter { it.productId != productId }
            val newCart = current.cart.copy(items = updatedItems)
            val newSummary = cartRepository.calculateSummary(newCart)
            _uiState.value = current.copy(cart = newCart, summary = newSummary)
        }

        viewModelScope.launch {
            _actionState.value = ActionState.Loading
            
            cartRepository.removeFromCart(productId).fold(
                onSuccess = {
                    _actionState.value = ActionState.Success("Item removed from cart")
                },
                onFailure = { error ->
                    _actionState.value = ActionState.Error(
                        error.message ?: "Failed to remove item"
                    )
                }
            )
        }
    }

    fun clearCart() {
        viewModelScope.launch {
            _actionState.value = ActionState.Loading
            
            cartRepository.clearCart().fold(
                onSuccess = {
                    _actionState.value = ActionState.Success("Cart cleared")
                },
                onFailure = { error ->
                    _actionState.value = ActionState.Error(
                        error.message ?: "Failed to clear cart"
                    )
                }
            )
        }
    }

    fun resetActionState() {
        _actionState.value = ActionState.Idle
    }
}

sealed class ActionState {
    object Idle : ActionState()
    object Loading : ActionState()
    data class Success(val message: String) : ActionState()
    data class Error(val message: String) : ActionState()
}
