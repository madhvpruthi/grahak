package com.princesanitary.grahak.ui.plumber.jobs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.princesanitary.grahak.data.model.ServiceJob
import com.princesanitary.grahak.data.repository.PlumberRepository
import com.princesanitary.grahak.data.repository.ServiceJobRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

class JobRequestsViewModel @Inject constructor(
    private val serviceJobRepository: ServiceJobRepository,
    private val plumberRepository: PlumberRepository
) : ViewModel() {

    private val _availableJobs = MutableStateFlow<List<ServiceJob>>(emptyList())
    val availableJobs: StateFlow<List<ServiceJob>> = _availableJobs.asStateFlow()

    private val _acceptingJobId = MutableStateFlow<String?>(null)
    val acceptingJobId: StateFlow<String?> = _acceptingJobId.asStateFlow()

    private val _decliningJobId = MutableStateFlow<String?>(null)
    val decliningJobId: StateFlow<String?> = _decliningJobId.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var previousJobIds: Set<String>? = null

    init {
        loadAvailableJobs()
    }

    /**
     * Streams the open queue, filtered to jobs this plumber should be offered:
     * nothing they have declined, nothing addressed to a different plumber, and
     * work matching their specialities floated to the top.
     *
     * The specialities come from their live profile, so editing their services
     * re-sorts the queue without a restart.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun loadAvailableJobs() {
        viewModelScope.launch {
            plumberRepository.getCurrentPlumberProfile()
                .map { result -> result.getOrNull()?.specialities ?: emptyList() }
                .distinctUntilChanged()
                .flatMapLatest { specialities ->
                    serviceJobRepository.getJobsForPlumber(specialities)
                }
                .collect { result ->
                    result.fold(
                        onSuccess = { jobs ->
                            val newIds = jobs.map { it.id }.toSet()
                            if (previousJobIds != null) {
                                val freshJobs = jobs.filter { !previousJobIds!!.contains(it.id) }
                                for (j in freshJobs) {
                                    try {
                                        com.princesanitary.grahak.data.service.NotificationHelper.showPlumberJobNotification(
                                            context = com.princesanitary.grahak.di.ServiceLocator.application,
                                            title = "New job request available",
                                            body = "${j.serviceCategory.ifBlank { "Plumbing Job" }} • ${j.siteAddress.fullAddress.take(45)} • ₹${"%.0f".format(j.agreedVisitRate)} visit",
                                            jobId = j.id
                                        )
                                    } catch (_: Exception) {}
                                }
                            }
                            previousJobIds = newIds
                            _availableJobs.value = jobs
                            _errorMessage.value = null
                        },
                        onFailure = { error ->
                            _errorMessage.value = error.message ?: "Failed to load jobs"
                        }
                    )
                }
        }
    }

    fun acceptJob(jobId: String) {
        viewModelScope.launch {
            _acceptingJobId.value = jobId
            _errorMessage.value = null

            serviceJobRepository.acceptJob(jobId).fold(
                onSuccess = {
                    // Left set: the caller navigates to the active job.
                },
                onFailure = { error ->
                    _acceptingJobId.value = null
                    _errorMessage.value = error.message ?: "Failed to accept job"
                }
            )
        }
    }

    /**
     * Passing on a job is an explicit choice rather than just ignoring the row.
     * It drops the job out of this plumber's queue, and a job that was addressed
     * to them directly is released back to the open pool for everyone else.
     */
    fun declineJob(jobId: String) {
        viewModelScope.launch {
            _decliningJobId.value = jobId
            _errorMessage.value = null

            serviceJobRepository.declineJob(jobId).fold(
                onSuccess = { _decliningJobId.value = null },
                onFailure = { error ->
                    _decliningJobId.value = null
                    _errorMessage.value = error.message ?: "Could not decline this job"
                }
            )
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
