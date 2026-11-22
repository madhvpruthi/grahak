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

sealed class OrderHistoryUiState {
    object Loading : OrderHistoryUiState()
    data class Success(val orders: List<Order>) : OrderHistoryUiState()
    data class Error(val message: String) : OrderHistoryUiState()
}

class OrderHistoryViewModel @Inject constructor(
    private val orderRepository: OrderRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<OrderHistoryUiState>(OrderHistoryUiState.Loading)
    val uiState: StateFlow<OrderHistoryUiState> = _uiState.asStateFlow()

    init {
        loadOrders()
    }

    private fun loadOrders() {
        viewModelScope.launch {
            _uiState.value = OrderHistoryUiState.Loading

            orderRepository.getCustomerOrders().collect { result ->
                result.fold(
                    onSuccess = { orders ->
                        _uiState.value = OrderHistoryUiState.Success(orders)
                    },
                    onFailure = { error ->
                        _uiState.value = OrderHistoryUiState.Error(
                            error.message ?: "Failed to load orders"
                        )
                    }
                )
            }
        }
    }

    fun refresh() {
        loadOrders()
    }
}
