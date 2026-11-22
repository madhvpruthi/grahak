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

/** Split so the plumber can tell live work from finished work at a glance. */
data class PlumberJobs(
    val active: List<ServiceJob> = emptyList(),
    val finished: List<ServiceJob> = emptyList(),
    val orders: List<Order> = emptyList(),
    val loading: Boolean = true,
    val error: String? = null
)

private val ACTIVE_STATUSES = setOf(
    JobStatus.ACCEPTED,
    JobStatus.EN_ROUTE,
    JobStatus.INSPECTING,
    JobStatus.IN_PROGRESS
)

/**
 * The plumber's own jobs and material orders - both in-flight and completed.
 */
class PlumberJobsHistoryViewModel @Inject constructor(
    private val serviceJobRepository: ServiceJobRepository,
    private val orderRepository: OrderRepository
) : ViewModel() {

    private val _state = MutableStateFlow(PlumberJobs())
    val state: StateFlow<PlumberJobs> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                serviceJobRepository.getPlumberJobHistory(),
                orderRepository.getPlumberOrders()
            ) { jobsResult, ordersResult ->
                val jobs = jobsResult.getOrDefault(emptyList())
                val orders = ordersResult.getOrDefault(emptyList())
                val err = jobsResult.exceptionOrNull()?.message ?: ordersResult.exceptionOrNull()?.message

                PlumberJobs(
                    active = jobs.filter { it.status in ACTIVE_STATUSES },
                    finished = jobs.filter {
                        it.status == JobStatus.COMPLETED || it.status == JobStatus.CANCELLED
                    },
                    orders = orders,
                    loading = false,
                    error = if (jobs.isEmpty() && orders.isEmpty()) err else null
                )
            }.collect { combinedState ->
                _state.value = combinedState
            }
        }
    }
}
