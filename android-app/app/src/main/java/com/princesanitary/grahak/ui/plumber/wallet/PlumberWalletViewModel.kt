package com.princesanitary.grahak.ui.plumber.wallet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.princesanitary.grahak.data.model.PlumberWallet
import com.princesanitary.grahak.data.model.Transaction
import com.princesanitary.grahak.data.model.WithdrawalRequest
import com.princesanitary.grahak.data.repository.WalletRepository
import com.princesanitary.grahak.data.repository.WalletStats
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class WalletUiState {
    object Loading : WalletUiState()
    data class Success(
        val wallet: PlumberWallet,
        val stats: WalletStats,
        val transactions: List<Transaction>,
        val withdrawalRequests: List<WithdrawalRequest>
    ) : WalletUiState()
    data class Error(val message: String) : WalletUiState()
}

sealed class WithdrawalAction {
    object Idle : WithdrawalAction()
    object Processing : WithdrawalAction()
    data class Success(val requestId: String) : WithdrawalAction()
    data class Error(val message: String) : WithdrawalAction()
}

class PlumberWalletViewModel @Inject constructor(
    private val walletRepository: WalletRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<WalletUiState>(WalletUiState.Loading)
    val uiState: StateFlow<WalletUiState> = _uiState.asStateFlow()

    private val _withdrawalAction = MutableStateFlow<WithdrawalAction>(WithdrawalAction.Idle)
    val withdrawalAction: StateFlow<WithdrawalAction> = _withdrawalAction.asStateFlow()

    init {
        loadWalletData()
    }

    private fun loadWalletData() {
        viewModelScope.launch {
            _uiState.value = WalletUiState.Loading

            // Combine all wallet data streams
            walletRepository.getWallet().collect { walletResult ->
                walletResult.fold(
                    onSuccess = { wallet ->
                        // Load stats
                        walletRepository.getWalletStats().fold(
                            onSuccess = { stats ->
                                // Load transactions
                                walletRepository.getTransactions().collect { transactionsResult ->
                                    transactionsResult.fold(
                                        onSuccess = { transactions ->
                                            // Load withdrawal requests
                                            walletRepository.getWithdrawalRequests().collect { withdrawalsResult ->
                                                withdrawalsResult.fold(
                                                    onSuccess = { withdrawals ->
                                                        _uiState.value = WalletUiState.Success(
                                                            wallet = wallet,
                                                            stats = stats,
                                                            transactions = transactions,
                                                            withdrawalRequests = withdrawals
                                                        )
                                                    },
                                                    onFailure = { /* Use empty list */ }
                                                )
                                            }
                                        },
                                        onFailure = { /* Use empty list */ }
                                    )
                                }
                            },
                            onFailure = { error ->
                                _uiState.value = WalletUiState.Error(
                                    error.message ?: "Failed to load wallet stats"
                                )
                            }
                        )
                    },
                    onFailure = { error ->
                        _uiState.value = WalletUiState.Error(
                            error.message ?: "Failed to load wallet"
                        )
                    }
                )
            }
        }
    }

    fun requestWithdrawal(amount: Double, upiId: String, notes: String = "") {
        viewModelScope.launch {
            _withdrawalAction.value = WithdrawalAction.Processing

            walletRepository.requestWithdrawal(amount, upiId, notes).fold(
                onSuccess = { requestId ->
                    _withdrawalAction.value = WithdrawalAction.Success(requestId)
                },
                onFailure = { error ->
                    _withdrawalAction.value = WithdrawalAction.Error(
                        error.message ?: "Failed to request withdrawal"
                    )
                }
            )
        }
    }

    fun updateUpiId(upiId: String) {
        viewModelScope.launch {
            walletRepository.updateUpiId(upiId).fold(
                onSuccess = {
                    // Wallet will update via real-time listener
                },
                onFailure = { error ->
                    _withdrawalAction.value = WithdrawalAction.Error(
                        error.message ?: "Failed to update UPI ID"
                    )
                }
            )
        }
    }

    fun resetWithdrawalAction() {
        _withdrawalAction.value = WithdrawalAction.Idle
    }

    fun refresh() {
        loadWalletData()
    }
}
