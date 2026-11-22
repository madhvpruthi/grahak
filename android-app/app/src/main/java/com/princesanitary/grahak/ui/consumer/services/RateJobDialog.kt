package com.princesanitary.grahak.ui.consumer.services

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.princesanitary.grahak.di.ServiceLocator
import kotlinx.coroutines.launch

/**
 * Hosts [RateJobSheet] and performs the write.
 *
 * Kept separate from the sheet so the sheet stays a pure piece of UI that can be
 * previewed and reused, while the transactional work (review + plumber average)
 * lives here.
 */
@Composable
fun RateJobDialog(
    jobId: String,
    plumberId: String,
    plumberName: String,
    serviceCategory: String,
    customerName: String,
    onDone: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var submitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = { if (!submitting) onDone() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        RateJobSheet(
            plumberName = plumberName,
            submitting = submitting,
            error = error,
            onDismiss = onDone,
            onSubmit = { rating, comment ->
                submitting = true
                error = null
                scope.launch {
                    ServiceLocator.reviewRepository.submitReview(
                        jobId = jobId,
                        plumberId = plumberId,
                        rating = rating,
                        comment = comment,
                        customerName = customerName,
                        serviceCategory = serviceCategory
                    ).fold(
                        onSuccess = {
                            submitting = false
                            onDone()
                        },
                        onFailure = {
                            submitting = false
                            error = it.message ?: "Could not submit your rating"
                        }
                    )
                }
            }
        )
    }
}
