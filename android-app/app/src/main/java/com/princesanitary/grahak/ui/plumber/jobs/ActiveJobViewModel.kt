package com.princesanitary.grahak.ui.plumber.jobs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.princesanitary.grahak.data.model.JobStatus
import com.princesanitary.grahak.data.model.Order
import com.princesanitary.grahak.data.model.ServiceJob
import com.princesanitary.grahak.data.repository.OrderRepository
import com.princesanitary.grahak.data.repository.ServiceJobRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class JobUiState {
    object Loading : JobUiState()
    data class Success(val job: ServiceJob, val linkedOrders: List<Order> = emptyList()) : JobUiState()
    data class Error(val message: String) : JobUiState()
}

class ActiveJobViewModel @Inject constructor(
    private val serviceJobRepository: ServiceJobRepository,
    private val orderRepository: OrderRepository
) : ViewModel() {

    private val _jobState = MutableStateFlow<JobUiState>(JobUiState.Loading)
    val jobState: StateFlow<JobUiState> = _jobState.asStateFlow()

    fun loadJob(jobId: String) {
        viewModelScope.launch {
            combine(
                serviceJobRepository.getJobDetails(jobId),
                orderRepository.getOrdersForJob(jobId)
            ) { jobResult, ordersResult ->
                val job = jobResult.getOrNull()
                val orders = ordersResult.getOrDefault(emptyList())

                if (job != null) {
                    JobUiState.Success(job, orders)
                } else if (jobResult.isFailure) {
                    JobUiState.Error(jobResult.exceptionOrNull()?.message ?: "Failed to load job")
                } else {
                    JobUiState.Error("Job not found")
                }
            }.collect { state ->
                _jobState.value = state
            }
        }
    }

    fun updateStatus(newStatus: JobStatus) {
        viewModelScope.launch {
            val currentJob = (_jobState.value as? JobUiState.Success)?.job ?: return@launch

            serviceJobRepository.updateJobStatus(currentJob.id, newStatus).fold(
                onSuccess = {
                    // Job will update via Flow automatically
                },
                onFailure = { error ->
                    _jobState.value = JobUiState.Error(
                        error.message ?: "Failed to update status"
                    )
                }
            )
        }
    }
}
