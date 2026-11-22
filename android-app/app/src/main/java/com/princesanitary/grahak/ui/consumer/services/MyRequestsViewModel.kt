package com.princesanitary.grahak.ui.consumer.services

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.princesanitary.grahak.data.model.JobStatus
import com.princesanitary.grahak.data.model.Plumber
import com.princesanitary.grahak.data.model.ServiceJob
import com.princesanitary.grahak.data.repository.PlumberRepository
import com.princesanitary.grahak.data.repository.ReviewRepository
import com.princesanitary.grahak.data.repository.ServiceJobRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface RequestsState {
    data object Loading : RequestsState
    data class Ready(val jobs: List<ServiceJob>) : RequestsState
    data class Error(val message: String) : RequestsState
}

data class TrackedRequest(
    val job: ServiceJob,
    val plumber: Plumber? = null,
    val alreadyRated: Boolean = false
)

/**
 * The customer's own service requests, live.
 *
 * `ServiceJobRepository.getConsumerJobs()` already existed but nothing in the UI
 * ever called it, so a customer could book a plumber and then never see the
 * request again. This backs both the list and the tracking screen.
 */
class MyRequestsViewModel @Inject constructor(
    private val serviceJobRepository: ServiceJobRepository,
    private val plumberRepository: PlumberRepository,
    private val reviewRepository: ReviewRepository
) : ViewModel() {

    private val _state = MutableStateFlow<RequestsState>(RequestsState.Loading)
    val state: StateFlow<RequestsState> = _state.asStateFlow()

    private val _tracked = MutableStateFlow<TrackedRequest?>(null)
    val tracked: StateFlow<TrackedRequest?> = _tracked.asStateFlow()

    private val _cancelError = MutableStateFlow<String?>(null)
    val cancelError: StateFlow<String?> = _cancelError.asStateFlow()

    init {
        viewModelScope.launch {
            serviceJobRepository.getConsumerJobs().collect { result ->
                result.fold(
                    onSuccess = { jobs ->
                        _state.value = RequestsState.Ready(jobs)
                        // Keep an open tracking screen in step with the feed.
                        _tracked.value?.let { current ->
                            jobs.firstOrNull { it.id == current.job.id }
                                ?.let { fresh -> refreshTracked(fresh) }
                        }
                    },
                    onFailure = {
                        _state.value = RequestsState.Error(
                            it.message ?: "Could not load your requests"
                        )
                    }
                )
            }
        }
    }

    /** Opens the tracking view and pulls in who accepted it. */
    fun track(job: ServiceJob) {
        _tracked.value = TrackedRequest(job)
        refreshTracked(job)
    }

    fun stopTracking() {
        _tracked.value = null
    }

    private fun refreshTracked(job: ServiceJob) {
        viewModelScope.launch {
            val plumber = job.plumberId
                ?.let { plumberRepository.getPlumberById(it).getOrNull() }

            val rated = if (job.status == JobStatus.COMPLETED) {
                reviewRepository.hasReview(job.id)
            } else {
                false
            }

            _tracked.value = TrackedRequest(job, plumber, rated)
        }
    }

    fun cancel(jobId: String) {
        viewModelScope.launch {
            serviceJobRepository.cancelJob(jobId).onFailure {
                _cancelError.value = it.message ?: "Could not cancel this request"
            }
        }
    }

    fun clearCancelError() {
        _cancelError.value = null
    }
}
