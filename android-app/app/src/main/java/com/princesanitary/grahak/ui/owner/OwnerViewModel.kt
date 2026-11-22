package com.princesanitary.grahak.ui.owner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.princesanitary.grahak.data.model.*
import com.princesanitary.grahak.data.repository.admin.AdminRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface OwnerUiState {
    data object Loading : OwnerUiState
    data class Success(
        val products: List<Product>,
        val orders: List<Order>,
        val plumbers: List<Plumber>,
        val pendingPayouts: List<WithdrawalRequest>,
        val estimates: List<Estimate> = emptyList()
    ) : OwnerUiState
    data class Error(val message: String) : OwnerUiState
}

class OwnerViewModel @Inject constructor(
    private val adminRepository: AdminRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<OwnerUiState>(OwnerUiState.Loading)
    val uiState: StateFlow<OwnerUiState> = _uiState.asStateFlow()

    private val _isAuthorized = MutableStateFlow<Boolean?>(null)
    val isAuthorized: StateFlow<Boolean?> = _isAuthorized.asStateFlow()

    private val _storeSettings = MutableStateFlow(StoreSettings())
    val storeSettings: StateFlow<StoreSettings> = _storeSettings.asStateFlow()

    private val _coupons = MutableStateFlow<List<Coupon>>(emptyList())
    val coupons: StateFlow<List<Coupon>> = _coupons.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _isPinUnlocked = MutableStateFlow(
        com.princesanitary.grahak.data.local.SettingsStorage.isOwnerPinSessionUnlocked()
    )
    val isPinUnlocked: StateFlow<Boolean> = _isPinUnlocked.asStateFlow()

    private val _pinError = MutableStateFlow<String?>(null)
    val pinError: StateFlow<String?> = _pinError.asStateFlow()

    private var currentProducts: List<Product> = emptyList()
    private var currentOrders: List<Order> = emptyList()
    private var currentPlumbers: List<Plumber> = emptyList()
    private var currentPayouts: List<WithdrawalRequest> = emptyList()
    private var currentEstimates: List<com.princesanitary.grahak.data.model.Estimate> = emptyList()
    private var previousOrderIds: Set<String>? = null

    init {
        checkAuthorization()
    }

    fun checkAuthorization() {
        viewModelScope.launch {
            val isAdmin = adminRepository.isCurrentUserAdmin()
            _isAuthorized.value = isAdmin
            if (isAdmin) {
                loadAllData()
            } else {
                _uiState.value = OwnerUiState.Error("You are not authorized as a store owner.")
            }
        }
    }

    private fun loadAllData() {
        viewModelScope.launch {
            adminRepository.getAllProducts().collect { result ->
                result.onSuccess {
                    currentProducts = it
                    publishState()
                }
            }
        }
        viewModelScope.launch {
            adminRepository.getAllOrders().collect { result ->
                result.onSuccess { orders ->
                    val newIds = orders.map { it.id }.toSet()
                    if (previousOrderIds != null) {
                        val freshOrders = orders.filter { !previousOrderIds!!.contains(it.id) }
                        for (o in freshOrders) {
                            try {
                                com.princesanitary.grahak.data.service.NotificationHelper.showOrderNotification(
                                    context = com.princesanitary.grahak.di.ServiceLocator.application,
                                    title = "New Store Order Received!",
                                    body = "${o.customerName.ifBlank { "Customer" }} • ₹${"%.2f".format(o.billing.grandTotal)} • Order #${o.id.take(8).uppercase()}"
                                )
                            } catch (_: Exception) {}
                        }
                    }
                    previousOrderIds = newIds
                    currentOrders = orders
                    publishState()
                }
            }
        }
        viewModelScope.launch {
            adminRepository.getAllPlumbers().collect { result ->
                result.onSuccess {
                    currentPlumbers = it
                    publishState()
                }
            }
        }
        viewModelScope.launch {
            adminRepository.getPendingWithdrawals().collect { result ->
                result.onSuccess {
                    currentPayouts = it
                    publishState()
                }
            }
        }
        viewModelScope.launch {
            adminRepository.getStoreSettings().collect { result ->
                result.onSuccess {
                    _storeSettings.value = it
                }
            }
        }
        viewModelScope.launch {
            adminRepository.getAllCoupons().collect { result ->
                result.onSuccess {
                    _coupons.value = it
                }
            }
        }
        viewModelScope.launch {
            adminRepository.getAllEstimates().collect { result ->
                result.onSuccess {
                    currentEstimates = it
                    publishState()
                }
            }
        }
    }

    fun refreshAllData() {
        viewModelScope.launch {
            _isRefreshing.value = true
            loadAllData()
            delay(800)
            _isRefreshing.value = false
        }
    }

    fun updateStoreSettings(settings: StoreSettings, onComplete: (Boolean, String?) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            _storeSettings.value = settings
            val res = adminRepository.updateStoreSettings(settings)
            if (res.isSuccess) {
                onComplete(true, null)
            } else {
                onComplete(false, res.exceptionOrNull()?.message ?: "Failed to update store settings")
            }
        }
    }

    fun saveCoupon(coupon: Coupon, onComplete: (Boolean, String?) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            val res = adminRepository.saveCoupon(coupon)
            if (res.isSuccess) {
                // Instantly update current coupon list in ViewModel state
                val existing = _coupons.value.filterNot { it.id == coupon.id || it.code.equals(coupon.code, ignoreCase = true) }
                _coupons.value = existing + coupon
                onComplete(true, null)
            } else {
                onComplete(false, res.exceptionOrNull()?.message ?: "Failed to save coupon")
            }
        }
    }

    fun deleteCoupon(couponId: String, onComplete: (Boolean, String?) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            val res = adminRepository.deleteCoupon(couponId)
            if (res.isSuccess) {
                _coupons.value = _coupons.value.filterNot { it.id == couponId }
                onComplete(true, null)
            } else {
                onComplete(false, res.exceptionOrNull()?.message ?: "Failed to delete coupon")
            }
        }
    }

    private fun publishState() {
        _uiState.value = OwnerUiState.Success(
            products = currentProducts,
            orders = currentOrders,
            plumbers = currentPlumbers,
            pendingPayouts = currentPayouts,
            estimates = currentEstimates
        )
    }

    fun acceptEstimateAsOrder(
        estimate: com.princesanitary.grahak.data.model.Estimate,
        onComplete: (Boolean, String?) -> Unit = { _, _ -> }
    ) {
        viewModelScope.launch {
            val res = adminRepository.acceptEstimateAsOrder(estimate)
            onComplete(res.isSuccess, res.exceptionOrNull()?.message)
        }
    }

    // Product actions with support for up to 4 multiple pictures
    fun saveProductWithMultipleImages(
        product: Product,
        existingUrls: List<String>,
        newImagesBytes: List<ByteArray>,
        useCloudinaryAiCutout: Boolean = false,
        onComplete: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val uploadedUrls = mutableListOf<String>()
                for (bytes in newImagesBytes) {
                    if (bytes.isNotEmpty()) {
                        val uploadRes = adminRepository.uploadProductImage(bytes, useCloudinaryAiCutout)
                        if (uploadRes.isSuccess) {
                            uploadedUrls.add(uploadRes.getOrThrow())
                        } else {
                            val err = uploadRes.exceptionOrNull()?.message ?: "Failed to upload image"
                            onComplete(false, err)
                            return@launch
                        }
                    }
                }
                val combinedUrls = (existingUrls + uploadedUrls).distinct().take(4)
                val finalProduct = product.copy(mediaUrls = combinedUrls)
                val res = adminRepository.saveProduct(finalProduct)
                if (res.isSuccess) {
                    onComplete(true, null)
                } else {
                    val err = res.exceptionOrNull()?.message ?: "Failed to save product"
                    onComplete(false, err)
                }
            } catch (e: Exception) {
                onComplete(false, e.message ?: "An unexpected error occurred")
            }
        }
    }

    fun saveProduct(
        product: Product,
        imageBytes: ByteArray? = null,
        useCloudinaryAiCutout: Boolean = false,
        onComplete: (Boolean, String?) -> Unit
    ) {
        val bytesList = if (imageBytes != null && imageBytes.isNotEmpty()) listOf(imageBytes) else emptyList()
        val existingUrls = if (bytesList.isNotEmpty()) emptyList() else product.mediaUrls
        saveProductWithMultipleImages(product, existingUrls, bytesList, useCloudinaryAiCutout, onComplete)
    }

    fun toggleProductPublish(productId: String, isPublished: Boolean) {
        viewModelScope.launch {
            // Optimistically update product in local state
            currentProducts = currentProducts.map {
                if (it.id == productId) it.copy(isPublished = isPublished) else it
            }
            publishState()
            val res = adminRepository.toggleProductPublish(productId, isPublished)
            if (res.isFailure) {
                // If it failed, log error
                android.util.Log.e("OwnerViewModel", "Failed to update isPublished: ${res.exceptionOrNull()?.message}")
            }
        }
    }

    fun toggleProductFeatured(productId: String, isFeatured: Boolean) {
        viewModelScope.launch {
            currentProducts = currentProducts.map {
                if (it.id == productId) it.copy(isFeatured = isFeatured) else it
            }
            publishState()
            val res = adminRepository.toggleProductFeatured(productId, isFeatured)
            if (res.isFailure) {
                android.util.Log.e("OwnerViewModel", "Failed to update isFeatured: ${res.exceptionOrNull()?.message}")
            }
        }
    }

    fun updateProductStock(productId: String, newStock: Int) {
        viewModelScope.launch {
            adminRepository.updateProductStock(productId, newStock)
        }
    }

    fun deleteProduct(productId: String) {
        viewModelScope.launch {
            adminRepository.deleteProduct(productId)
        }
    }

    fun verifyOwnerPin(pin: String) {
        viewModelScope.launch {
            val valid = adminRepository.verifyOwnerSecurityPin(pin)
            if (valid) {
                com.princesanitary.grahak.data.local.SettingsStorage.setOwnerPinSessionUnlocked(true)
                _isPinUnlocked.value = true
                _pinError.value = null
            } else {
                _pinError.value = "Incorrect Master Security Passcode."
            }
        }
    }

    fun resetPinState() {
        com.princesanitary.grahak.data.local.SettingsStorage.setOwnerPinSessionUnlocked(false)
        _isPinUnlocked.value = false
        _pinError.value = null
    }

    fun updateOwnerPin(newPin: String, onComplete: (Boolean, String?) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            val res = adminRepository.updateOwnerSecurityPin(newPin)
            if (res.isSuccess) {
                com.princesanitary.grahak.data.local.SettingsStorage.setOwnerSecurityPin(newPin)
                onComplete(true, null)
            } else {
                onComplete(false, res.exceptionOrNull()?.message ?: "Failed to update security PIN")
            }
        }
    }

    // Order actions
    fun updateOrderStatus(orderId: String, status: OrderStatus) {
        val existing = currentOrders.find { it.id == orderId }
        // Lock: delivered and cancelled orders are finalized and cannot be modified
        if (existing != null && (existing.status == OrderStatus.DELIVERED || existing.status == OrderStatus.CANCELLED)) {
            return
        }
        viewModelScope.launch {
            adminRepository.updateOrderStatus(orderId, status)
        }
    }

    fun updatePaymentStatus(orderId: String, status: PaymentStatus) {
        val existing = currentOrders.find { it.id == orderId }
        if (existing != null && (existing.status == OrderStatus.DELIVERED || existing.status == OrderStatus.CANCELLED)) {
            return
        }
        viewModelScope.launch {
            adminRepository.updatePaymentStatus(orderId, status)
        }
    }

    // Plumber actions
    fun verifyPlumber(plumberId: String, approved: Boolean) {
        viewModelScope.launch {
            adminRepository.verifyPlumberKYC(plumberId, approved)
        }
    }

    fun setPlumberVisibility(plumberId: String, isVisibleToOrders: Boolean, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val res = adminRepository.setPlumberVisibility(plumberId, isVisibleToOrders)
            onComplete(res.isSuccess)
        }
    }

    fun deletePlumber(plumberId: String, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val res = adminRepository.deletePlumber(plumberId)
            onComplete(res.isSuccess)
        }
    }

    fun deleteUser(userId: String, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val res = adminRepository.deleteUser(userId)
            onComplete(res.isSuccess)
        }
    }

    // Payout actions
    fun approvePayout(requestId: String, txnId: String) {
        viewModelScope.launch {
            adminRepository.approveWithdrawal(requestId, txnId)
        }
    }
}
