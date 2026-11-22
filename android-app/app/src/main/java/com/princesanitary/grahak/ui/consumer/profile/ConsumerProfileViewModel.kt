package com.princesanitary.grahak.ui.consumer.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.princesanitary.grahak.data.model.User
import com.princesanitary.grahak.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val profile: User? = null,
    val loading: Boolean = true,
    val saving: Boolean = false,
    val error: String? = null,
    val savedMessage: String? = null
)

/**
 * Lets a customer read and edit their own profile.
 *
 * This exists because the phone number had no way in for anyone who signed up
 * before it became part of the form. Their jobs were reaching plumbers with a
 * blank `customerPhone`, so the Call button had nothing to dial.
 */
class ConsumerProfileViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true)
            userRepository.getProfile().fold(
                onSuccess = { profile ->
                    _uiState.value = _uiState.value.copy(profile = profile, loading = false)
                },
                onFailure = {
                    _uiState.value = _uiState.value.copy(
                        loading = false,
                        error = it.message ?: "Could not load your profile"
                    )
                }
            )
        }
    }

    fun save(name: String, phone: String) {
        val digits = phone.filter(Char::isDigit)
        val trimmedName = name.trim()

        if (trimmedName.length < 2) {
            _uiState.value = _uiState.value.copy(error = "Enter your name")
            return
        }
        if (digits.length != 10) {
            _uiState.value = _uiState.value.copy(error = "Enter a 10-digit mobile number")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(saving = true, error = null)

            val nameResult = userRepository.updateName(trimmedName)
            val phoneResult = userRepository.updatePhone(digits)

            val failure = nameResult.exceptionOrNull() ?: phoneResult.exceptionOrNull()
            if (failure != null) {
                _uiState.value = _uiState.value.copy(
                    saving = false,
                    error = failure.message ?: "Could not save your details"
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    saving = false,
                    savedMessage = "Details saved",
                    profile = _uiState.value.profile?.copy(name = trimmedName, phone = digits)
                        ?: User(name = trimmedName, phone = digits)
                )
                load()
            }
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(error = null, savedMessage = null)
    }
}
