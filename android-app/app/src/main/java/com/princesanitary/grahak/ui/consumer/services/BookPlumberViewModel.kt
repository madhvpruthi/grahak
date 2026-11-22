package com.princesanitary.grahak.ui.consumer.services

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.princesanitary.grahak.data.model.Plumber
import com.princesanitary.grahak.data.model.SavedAddress
import com.princesanitary.grahak.data.repository.ServiceJobRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class BookingUiState {
    object Idle : BookingUiState()
    object Loading : BookingUiState()
    data class Success(val jobId: String) : BookingUiState()
    data class Error(val message: String) : BookingUiState()
}

class BookPlumberViewModel @Inject constructor(
    private val serviceJobRepository: ServiceJobRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<BookingUiState>(BookingUiState.Idle)
    val uiState: StateFlow<BookingUiState> = _uiState.asStateFlow()

    private val _availablePlumbers = MutableStateFlow<List<Plumber>>(emptyList())
    val availablePlumbers: StateFlow<List<Plumber>> = _availablePlumbers.asStateFlow()

    init {
        loadAvailablePlumbers()
    }

    private fun loadAvailablePlumbers() {
        viewModelScope.launch {
            serviceJobRepository.getAvailablePlumbers().fold(
                onSuccess = { plumbers ->
                    _availablePlumbers.value = plumbers
                },
                onFailure = { error ->
                    // Silently fail - not critical
                }
            )
        }
    }

    fun bookPlumber(
        serviceCategory: String,
        siteAddress: SavedAddress,
        description: String
    ) {
        viewModelScope.launch {
            _uiState.value = BookingUiState.Loading

            serviceJobRepository.createServiceJob(
                serviceCategory = serviceCategory,
                siteAddress = siteAddress,
                description = description
            ).fold(
                onSuccess = { jobId ->
                    _uiState.value = BookingUiState.Success(jobId)
                },
                onFailure = { error ->
                    _uiState.value = BookingUiState.Error(
                        error.message ?: "Failed to book service"
                    )
                }
            )
        }
    }
}
