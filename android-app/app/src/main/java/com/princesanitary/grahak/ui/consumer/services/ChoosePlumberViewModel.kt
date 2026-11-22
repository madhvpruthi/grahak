package com.princesanitary.grahak.ui.consumer.services

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.princesanitary.grahak.data.model.OperationalStatus
import com.princesanitary.grahak.data.model.Plumber
import com.princesanitary.grahak.data.model.SavedAddress
import com.princesanitary.grahak.data.model.ServiceSpeciality
import com.princesanitary.grahak.data.repository.PlumberRepository
import com.princesanitary.grahak.data.repository.ServiceJobRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** A plumber paired with what they charge for the job being booked. */
data class PlumberOffer(
    val plumber: Plumber,
    /** Their rate for the chosen speciality, falling back to the visit charge. */
    val rate: Double,
    val isAvailableNow: Boolean
)

sealed interface ChoosePlumberState {
    data object Loading : ChoosePlumberState
    data class Ready(val offers: List<PlumberOffer>) : ChoosePlumberState
    data class Error(val message: String) : ChoosePlumberState
}

sealed interface RequestState {
    data object Idle : RequestState
    data object Sending : RequestState
    data class Sent(val jobId: String, val broadcast: Boolean) : RequestState
    data class Failed(val message: String) : RequestState
}

/**
 * Backs the "choose your plumber" step.
 *
 * The customer picks the work first, and the list then shows each approved
 * plumber *with their own rate for that work* - which is the point of letting
 * plumbers set per-speciality pricing. They can either request one plumber
 * directly or broadcast to everyone and take the first to accept.
 */
class ChoosePlumberViewModel @Inject constructor(
    private val plumberRepository: PlumberRepository,
    private val serviceJobRepository: ServiceJobRepository
) : ViewModel() {

    private val _speciality = MutableStateFlow(ServiceSpeciality.LEAK_REPAIR)
    val speciality: StateFlow<ServiceSpeciality> = _speciality.asStateFlow()

    private val _state = MutableStateFlow<ChoosePlumberState>(ChoosePlumberState.Loading)
    val state: StateFlow<ChoosePlumberState> = _state.asStateFlow()

    private val _request = MutableStateFlow<RequestState>(RequestState.Idle)
    val request: StateFlow<RequestState> = _request.asStateFlow()

    private var roster: List<Plumber> = emptyList()

    init {
        viewModelScope.launch {
            plumberRepository.observeApprovedPlumbers().collect { result ->
                result.fold(
                    onSuccess = {
                        roster = it
                        recompute()
                    },
                    onFailure = {
                        _state.value = ChoosePlumberState.Error(
                            it.message ?: "Could not load plumbers"
                        )
                    }
                )
            }
        }
    }

    fun selectSpeciality(value: ServiceSpeciality) {
        _speciality.value = value
        recompute()
    }

    private fun recompute() {
        val chosen = _speciality.value

        val offers = roster
            // Only plumbers who actually do this work.
            .filter { it.specialities.isEmpty() || chosen.id in it.specialities }
            .map { plumber ->
                PlumberOffer(
                    plumber = plumber,
                    rate = plumber.rates.customRates[chosen.id]
                        ?: plumber.rates.visitCharge,
                    isAvailableNow = plumber.operationalStatus == OperationalStatus.ONLINE
                )
            }
            // Cheapest available first; offline plumbers sink below.
            .sortedWith(
                compareByDescending<PlumberOffer> { it.isAvailableNow }
                    .thenBy { it.rate }
            )

        _state.value = ChoosePlumberState.Ready(offers)
    }

    /**
     * @param plumberId null broadcasts the job to every eligible plumber.
     */
    fun requestJob(
        plumberId: String?,
        rate: Double,
        address: SavedAddress,
        description: String
    ) {
        viewModelScope.launch {
            _request.value = RequestState.Sending

            serviceJobRepository.createServiceJob(
                serviceCategory = _speciality.value.id,
                siteAddress = address,
                description = description,
                requestedPlumberId = plumberId,
                agreedVisitRate = rate
            ).fold(
                onSuccess = {
                    _request.value = RequestState.Sent(it, broadcast = plumberId == null)
                },
                onFailure = {
                    _request.value = RequestState.Failed(
                        it.message ?: "Could not send your request"
                    )
                }
            )
        }
    }

    fun resetRequest() {
        _request.value = RequestState.Idle
    }
}
