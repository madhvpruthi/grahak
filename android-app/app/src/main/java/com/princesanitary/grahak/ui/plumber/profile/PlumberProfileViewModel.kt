package com.princesanitary.grahak.ui.plumber.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.princesanitary.grahak.data.model.OperationalStatus
import com.princesanitary.grahak.data.model.Plumber
import com.princesanitary.grahak.data.repository.PlumberRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class ProfileUiState {
    object Loading : ProfileUiState()
    data class Success(val plumber: Plumber) : ProfileUiState()
    data class Error(val message: String) : ProfileUiState()
}

class PlumberProfileViewModel @Inject constructor(
    private val plumberRepository: PlumberRepository
) : ViewModel() {

    private val _profileState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val profileState: StateFlow<ProfileUiState> = _profileState.asStateFlow()

    init {
        loadProfile()
    }

    private fun loadProfile() {
        viewModelScope.launch {
            plumberRepository.getCurrentPlumberProfile().collect { result ->
                result.fold(
                    onSuccess = { plumber ->
                        if (plumber != null) {
                            _profileState.value = ProfileUiState.Success(plumber)
                        } else {
                            _profileState.value = ProfileUiState.Error("Profile not found")
                        }
                    },
                    onFailure = { error ->
                        _profileState.value = ProfileUiState.Error(
                            error.message ?: "Failed to load profile"
                        )
                    }
                )
            }
        }
    }

    fun updateOperationalStatus(status: OperationalStatus) {
        viewModelScope.launch {
            plumberRepository.updateOperationalStatus(status).fold(
                onSuccess = {
                    // Profile will update automatically via Flow
                },
                onFailure = { error ->
                    _profileState.value = ProfileUiState.Error(
                        error.message ?: "Failed to update status"
                    )
                }
            )
        }
    }
}
