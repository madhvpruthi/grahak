package com.princesanitary.grahak.ui.consumer.estimate

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.princesanitary.grahak.data.model.Estimate
import com.princesanitary.grahak.data.model.SavedAddress
import com.princesanitary.grahak.data.repository.EstimateRepository
import com.princesanitary.grahak.data.repository.OrderRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class EstimateApprovalUiState {
    object Loading : EstimateApprovalUiState()
    data class Success(val estimate: Estimate) : EstimateApprovalUiState()
    data class Error(val message: String) : EstimateApprovalUiState()
}

sealed class ApprovalAction {
    object Idle : ApprovalAction()
    object Processing : ApprovalAction()
    data class Approved(val orderId: String) : ApprovalAction()
    object Rejected : ApprovalAction()
    data class Error(val message: String) : ApprovalAction()
}

class EstimateApprovalViewModel @Inject constructor(
    private val estimateRepository: EstimateRepository,
    private val orderRepository: OrderRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<EstimateApprovalUiState>(EstimateApprovalUiState.Loading)
    val uiState: StateFlow<EstimateApprovalUiState> = _uiState.asStateFlow()

    private val _actionState = MutableStateFlow<ApprovalAction>(ApprovalAction.Idle)
    val actionState: StateFlow<ApprovalAction> = _actionState.asStateFlow()

    fun loadEstimate(estimateId: String) {
        viewModelScope.launch {
            _uiState.value = EstimateApprovalUiState.Loading

            estimateRepository.getEstimate(estimateId).collect { result ->
                result.fold(
                    onSuccess = { estimate ->
                        if (estimate != null) {
                            _uiState.value = EstimateApprovalUiState.Success(estimate)
                        } else {
                            _uiState.value = EstimateApprovalUiState.Error("Estimate not found")
                        }
                    },
                    onFailure = { error ->
                        _uiState.value = EstimateApprovalUiState.Error(
                            error.message ?: "Failed to load estimate"
                        )
                    }
                )
            }
        }
    }

    fun approveEstimate(estimateId: String, deliveryAddress: SavedAddress) {
        viewModelScope.launch {
            _actionState.value = ApprovalAction.Processing

            // First approve the estimate
            estimateRepository.approveEstimate(estimateId).fold(
                onSuccess = {
                    // Get the estimate to create order
                    val currentState = _uiState.value
                    if (currentState is EstimateApprovalUiState.Success) {
                        val estimate = currentState.estimate
                        
                        // Create order from estimate
                        orderRepository.createOrderFromEstimate(
                            estimate = estimate,
                            jobId = estimate.jobId,
                            deliveryAddress = deliveryAddress
                        ).fold(
                            onSuccess = { orderId ->
                                _actionState.value = ApprovalAction.Approved(orderId)
                            },
                            onFailure = { error ->
                                _actionState.value = ApprovalAction.Error(
                                    "Estimate approved but order creation failed: ${error.message}"
                                )
                            }
                        )
                    } else {
                        _actionState.value = ApprovalAction.Error("Invalid estimate state")
                    }
                },
                onFailure = { error ->
                    _actionState.value = ApprovalAction.Error(
                        error.message ?: "Failed to approve estimate"
                    )
                }
            )
        }
    }

    fun rejectEstimate(estimateId: String, reason: String) {
        viewModelScope.launch {
            _actionState.value = ApprovalAction.Processing

            estimateRepository.rejectEstimate(estimateId, reason).fold(
                onSuccess = {
                    _actionState.value = ApprovalAction.Rejected
                },
                onFailure = { error ->
                    _actionState.value = ApprovalAction.Error(
                        error.message ?: "Failed to reject estimate"
                    )
                }
            )
        }
    }

    fun resetActionState() {
        _actionState.value = ApprovalAction.Idle
    }

    // Mock addresses - in real app, fetch from user profile
    fun getAvailableAddresses(): List<SavedAddress> {
        return listOf(
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
    }
}
