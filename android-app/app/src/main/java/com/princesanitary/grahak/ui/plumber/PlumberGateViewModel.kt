package com.princesanitary.grahak.ui.plumber

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.princesanitary.grahak.data.model.OperationalStatus
import com.princesanitary.grahak.data.model.Plumber
import com.princesanitary.grahak.data.model.VerificationStatus
import com.princesanitary.grahak.data.repository.PlumberRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Which of the three plumber-side experiences to show.
 */
sealed interface PlumberGate {
    data object Loading : PlumberGate

    /** No `plumbers/{uid}` document yet - run onboarding. */
    data object NeedsOnboarding : PlumberGate

    /** Applied, waiting on the store. Can look around but not accept work. */
    data class AwaitingApproval(val plumber: Plumber) : PlumberGate

    data class Rejected(val plumber: Plumber) : PlumberGate

    /** Approved - the full app. */
    data class Active(val plumber: Plumber) : PlumberGate
}

/**
 * Decides what a signed-in plumber sees.
 *
 * This is the piece that was missing entirely. Nothing routed to the
 * registration screen, so a plumber who signed up landed on empty tabs and no
 * `plumbers/{uid}` document was ever written - which is also why the customer's
 * booking screen always showed zero plumbers.
 */
class PlumberGateViewModel @Inject constructor(
    private val plumberRepository: PlumberRepository
) : ViewModel() {

    private val _gate = MutableStateFlow<PlumberGate>(PlumberGate.Loading)
    val gate: StateFlow<PlumberGate> = _gate.asStateFlow()

    init {
        observeProfile()
    }

    private fun observeProfile() {
        viewModelScope.launch {
            plumberRepository.getCurrentPlumberProfile().collect { result ->
                result.fold(
                    onSuccess = { plumber ->
                        _gate.value = when {
                            plumber == null -> {
                                if (_gate.value is PlumberGate.Active || _gate.value is PlumberGate.AwaitingApproval) {
                                    _gate.value
                                } else {
                                    PlumberGate.NeedsOnboarding
                                }
                            }
                            plumber.kyc.verified -> PlumberGate.Active(plumber)
                            VerificationStatus.fromStored(plumber.kyc.reviewStatus) ==
                                VerificationStatus.REJECTED -> PlumberGate.Rejected(plumber)
                            else -> PlumberGate.AwaitingApproval(plumber)
                        }
                    },
                    onFailure = {
                        // Don't downgrade an active or awaiting plumber to onboarding on transient failure
                        if (_gate.value !is PlumberGate.Active && _gate.value !is PlumberGate.AwaitingApproval) {
                            _gate.value = PlumberGate.NeedsOnboarding
                        }
                    }
                )
            }
        }
    }

    fun refresh() {
        observeProfile()
    }

    /** Availability toggle shown on the dashboard. */
    fun setOnline(online: Boolean) {
        viewModelScope.launch {
            plumberRepository.updateOperationalStatus(
                if (online) OperationalStatus.ONLINE else OperationalStatus.OFFLINE
            )
        }
    }
}
