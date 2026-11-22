package com.princesanitary.grahak.ui.consumer.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.princesanitary.grahak.data.model.Order
import com.princesanitary.grahak.data.repository.OrderRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class OrderDetailUiState {
    object Loading : OrderDetailUiState()
    data class Success(val order: Order) : OrderDetailUiState()
    data class Error(val message: String) : OrderDetailUiState()
}

sealed class OrderAction {
    object Idle : OrderAction()
    object Cancelling : OrderAction()
    object Cancelled : OrderAction()
    data class Error(val message: String) : OrderAction()
}

class OrderDetailViewModel @Inject constructor(
    private val orderRepository: OrderRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<OrderDetailUiState>(OrderDetailUiState.Loading)
    val uiState: StateFlow<OrderDetailUiState> = _uiState.asStateFlow()

    private val _actionState = MutableStateFlow<OrderAction>(OrderAction.Idle)
    val actionState: StateFlow<OrderAction> = _actionState.asStateFlow()

    fun loadOrder(orderId: String) {
        viewModelScope.launch {
            _uiState.value = OrderDetailUiState.Loading

            orderRepository.getOrder(orderId).collect { result ->
                result.fold(
                    onSuccess = { order ->
                        if (order != null) {
                            _uiState.value = OrderDetailUiState.Success(order)
                        } else {
                            _uiState.value = OrderDetailUiState.Error("Order not found")
                        }
                    },
                    onFailure = { error ->
                        _uiState.value = OrderDetailUiState.Error(
                            error.message ?: "Failed to load order"
                        )
                    }
                )
            }
        }
    }

    fun cancelOrder(orderId: String, reason: String) {
        viewModelScope.launch {
            _actionState.value = OrderAction.Cancelling

            orderRepository.cancelOrder(orderId, reason).fold(
                onSuccess = {
                    _actionState.value = OrderAction.Cancelled
                },
                onFailure = { error ->
                    _actionState.value = OrderAction.Error(
                        error.message ?: "Failed to cancel order"
                    )
                }
            )
        }
    }

    fun resetActionState() {
        _actionState.value = OrderAction.Idle
    }
}
