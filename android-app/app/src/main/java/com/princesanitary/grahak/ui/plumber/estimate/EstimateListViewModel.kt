package com.princesanitary.grahak.ui.plumber.estimate

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.princesanitary.grahak.data.model.Estimate
import com.princesanitary.grahak.data.repository.EstimateRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class EstimateListUiState {
    object Loading : EstimateListUiState()
    data class Success(val estimates: List<Estimate>) : EstimateListUiState()
    data class Error(val message: String) : EstimateListUiState()
}

class EstimateListViewModel @Inject constructor(
    private val estimateRepository: EstimateRepository
) : ViewModel() {

    private val _estimatesState = MutableStateFlow<EstimateListUiState>(EstimateListUiState.Loading)
    val estimatesState: StateFlow<EstimateListUiState> = _estimatesState.asStateFlow()

    init {
        loadEstimates()
    }

    fun loadEstimates() {
        viewModelScope.launch {
            _estimatesState.value = EstimateListUiState.Loading
            
            estimateRepository.getPlumberEstimates().collect { result ->
                result.fold(
                    onSuccess = { estimates ->
                        _estimatesState.value = EstimateListUiState.Success(estimates)
                    },
                    onFailure = { error ->
                        _estimatesState.value = EstimateListUiState.Error(
                            error.message ?: "Failed to load estimates"
                        )
                    }
                )
            }
        }
    }
}
