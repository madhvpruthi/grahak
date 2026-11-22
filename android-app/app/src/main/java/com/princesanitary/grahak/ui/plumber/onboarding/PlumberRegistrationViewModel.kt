package com.princesanitary.grahak.ui.plumber.onboarding

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.princesanitary.grahak.data.model.ServiceSpeciality
import com.princesanitary.grahak.data.repository.PlumberRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Everything collected across the onboarding steps. */
data class PlumberApplication(
    // Step 1 - identity
    val name: String = "",
    val phone: String = "",
    val photoUri: Uri? = null,
    val experienceYears: String = "",
    val about: String = "",

    // Step 2 - work + pricing
    val specialities: Set<ServiceSpeciality> = emptySet(),
    /** Speciality id -> the plumber's own rate, as typed. */
    val rates: Map<String, String> = emptyMap(),
    val visitCharge: String = "",

    // Step 3 - area + documents
    val servicePincode: String = "247001",
    val baseAddress: String = "",
    val aadhaarNo: String = "",
    val documents: List<Uri> = emptyList()
)

sealed interface RegistrationState {
    data object Editing : RegistrationState
    data object Submitting : RegistrationState
    data object Submitted : RegistrationState
    data class Error(val message: String) : RegistrationState
}

class PlumberRegistrationViewModel @Inject constructor(
    private val plumberRepository: PlumberRepository
) : ViewModel() {

    private val _application = MutableStateFlow(PlumberApplication())
    val application: StateFlow<PlumberApplication> = _application.asStateFlow()

    private val _state = MutableStateFlow<RegistrationState>(RegistrationState.Editing)
    val state: StateFlow<RegistrationState> = _state.asStateFlow()

    /** False when Firebase Storage is unavailable, so the UI can soften its copy. */
    private val _uploadsAvailable = MutableStateFlow(true)
    val uploadsAvailable: StateFlow<Boolean> = _uploadsAvailable.asStateFlow()

    init {
        viewModelScope.launch {
            _uploadsAvailable.value = plumberRepository.isStorageAvailable()
        }
    }

    /* ------------------------------- edits ------------------------------- */

    fun update(transform: (PlumberApplication) -> PlumberApplication) {
        _application.value = transform(_application.value)
    }

    /**
     * Toggling a speciality on seeds its rate with the suggested figure, so the
     * plumber always has a sensible number to adjust rather than an empty box.
     */
    fun toggleSpeciality(speciality: ServiceSpeciality) {
        val current = _application.value
        val selected = current.specialities.toMutableSet()
        val rates = current.rates.toMutableMap()

        if (speciality in selected) {
            selected -= speciality
            rates -= speciality.id
        } else {
            selected += speciality
            rates[speciality.id] = speciality.suggestedRate.toInt().toString()
        }

        _application.value = current.copy(specialities = selected, rates = rates)
    }

    fun setRate(speciality: ServiceSpeciality, value: String) {
        val digits = value.filter(Char::isDigit).take(6)
        _application.value = _application.value.copy(
            rates = _application.value.rates + (speciality.id to digits)
        )
    }

    /* ---------------------------- validation ----------------------------- */

    /** Per-step gate so "Continue" can be disabled with a reason. */
    fun problemWith(step: Int): String? {
        val a = _application.value
        return when (step) {
            0 -> when {
                a.name.trim().length < 3 -> "Enter your full name"
                a.phone.filter(Char::isDigit).length != 10 -> "Enter a 10-digit mobile number"
                a.experienceYears.isBlank() -> "Add your years of experience"
                else -> null
            }
            1 -> when {
                a.specialities.isEmpty() -> "Pick at least one type of work"
                a.specialities.any { (a.rates[it.id]?.toIntOrNull() ?: 0) <= 0 } ->
                    "Set a rate for every selected service"
                (a.visitCharge.toIntOrNull() ?: 0) <= 0 -> "Set your visit charge"
                else -> null
            }
            2 -> when {
                a.servicePincode.filter(Char::isDigit).length != 6 -> "Enter a 6-digit pincode"
                a.baseAddress.trim().length < 6 -> "Add the area you work from"
                a.aadhaarNo.filter(Char::isDigit).length != 12 -> "Aadhaar must be 12 digits"
                else -> null
            }
            else -> null
        }
    }

    /* ----------------------------- submission ---------------------------- */

    fun submit() {
        val a = _application.value

        (0..2).forEach { step ->
            problemWith(step)?.let {
                _state.value = RegistrationState.Error(it)
                return
            }
        }

        viewModelScope.launch {
            _state.value = RegistrationState.Submitting

            plumberRepository.registerPlumber(
                name = a.name.trim(),
                phone = a.phone.filter(Char::isDigit),
                photoUri = a.photoUri,
                aadhaarNo = a.aadhaarNo.filter(Char::isDigit),
                kycDocuments = a.documents,
                visitCharge = a.visitCharge.toDoubleOrNull() ?: 0.0,
                customRates = a.specialities.associate { speciality ->
                    speciality.id to (a.rates[speciality.id]?.toDoubleOrNull() ?: 0.0)
                },
                specialities = a.specialities.map { it.id },
                experienceYears = a.experienceYears.toIntOrNull() ?: 0,
                about = a.about.trim(),
                servicePincode = a.servicePincode.filter(Char::isDigit),
                baseAddress = a.baseAddress.trim()
            ).fold(
                onSuccess = {
                    com.princesanitary.grahak.di.ServiceLocator.auth.currentUser?.uid?.let { uid ->
                        com.princesanitary.grahak.GrahakApplication.cacheUserRole(
                            com.princesanitary.grahak.di.ServiceLocator.application,
                            uid,
                            com.princesanitary.grahak.ui.UserRole.PLUMBER.name
                        )
                    }
                    _state.value = RegistrationState.Submitted
                },
                onFailure = {
                    _state.value = RegistrationState.Error(
                        it.message ?: "Could not submit your application"
                    )
                }
            )
        }
    }

    fun clearError() {
        if (_state.value is RegistrationState.Error) _state.value = RegistrationState.Editing
    }
}
