package com.princesanitary.grahak.ui.plumber.estimate

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.princesanitary.grahak.data.model.EstimateItem
import com.princesanitary.grahak.data.model.Product
import com.princesanitary.grahak.data.repository.EstimateRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class EstimateUiState {
    object Idle : EstimateUiState()
    object Loading : EstimateUiState()
    data class Success(val estimateId: String) : EstimateUiState()
    data class Error(val message: String) : EstimateUiState()
}

class CreateEstimateViewModel @Inject constructor(
    private val estimateRepository: EstimateRepository
) : ViewModel() {

    private val _estimateItems = MutableStateFlow<List<EstimateItem>>(emptyList())
    val estimateItems: StateFlow<List<EstimateItem>> = _estimateItems.asStateFlow()

    private val _labourCharge = MutableStateFlow(0.0)
    val labourCharge: StateFlow<Double> = _labourCharge.asStateFlow()

    private val _notes = MutableStateFlow("")
    val notes: StateFlow<String> = _notes.asStateFlow()

    private val _totals = MutableStateFlow(EstimateTotals())
    val totals: StateFlow<EstimateTotals> = _totals.asStateFlow()

    private val _uiState = MutableStateFlow<EstimateUiState>(EstimateUiState.Idle)
    val uiState: StateFlow<EstimateUiState> = _uiState.asStateFlow()

    fun addItem(product: Product, quantity: Int) {
        val total = product.unitPrice * quantity
        val newItem = EstimateItem(
            productId = product.id,
            productName = product.name,
            quantity = quantity,
            unitPrice = product.unitPrice,
            total = total,
            gstRate = product.tax.gstRate
        )

        _estimateItems.value = _estimateItems.value + newItem
        calculateTotals()
    }

    fun updateItemQuantity(productId: String, newQuantity: Int) {
        _estimateItems.value = _estimateItems.value.map { item ->
            if (item.productId == productId) {
                val newTotal = item.unitPrice * newQuantity
                item.copy(quantity = newQuantity, total = newTotal)
            } else {
                item
            }
        }
        calculateTotals()
    }

    fun removeItem(productId: String) {
        _estimateItems.value = _estimateItems.value.filter { it.productId != productId }
        calculateTotals()
    }

    fun setLabourCharge(amount: Double) {
        _labourCharge.value = amount
        calculateTotals()
    }

    fun setNotes(text: String) {
        _notes.value = text
    }

    private fun calculateTotals() {
        val subtotal = _estimateItems.value.sumOf { it.total }
        val taxTotal = _estimateItems.value.sumOf { it.total * it.gstRate / 100 }
        val grandTotal = subtotal + taxTotal + _labourCharge.value

        _totals.value = EstimateTotals(
            subtotal = subtotal,
            taxTotal = taxTotal,
            labourCharge = _labourCharge.value,
            grandTotal = grandTotal
        )
    }

    fun submitEstimate(jobId: String, customerId: String) {
        viewModelScope.launch {
            if (_estimateItems.value.isEmpty()) {
                _uiState.value = EstimateUiState.Error("Please add at least one item")
                return@launch
            }

            _uiState.value = EstimateUiState.Loading

            estimateRepository.createEstimate(
                jobId = jobId,
                customerId = customerId,
                items = _estimateItems.value,
                labourCharge = _labourCharge.value,
                notes = _notes.value
            ).fold(
                onSuccess = { estimateId ->
                    // Submit the estimate
                    estimateRepository.submitEstimate(estimateId).fold(
                        onSuccess = {
                            _uiState.value = EstimateUiState.Success(estimateId)
                        },
                        onFailure = { error ->
                            _uiState.value = EstimateUiState.Error(
                                error.message ?: "Failed to submit estimate"
                            )
                        }
                    )
                },
                onFailure = { error ->
                    _uiState.value = EstimateUiState.Error(
                        error.message ?: "Failed to create estimate"
                    )
                }
            )
        }
    }
}
