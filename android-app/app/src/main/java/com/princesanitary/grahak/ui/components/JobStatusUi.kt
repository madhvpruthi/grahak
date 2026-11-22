package com.princesanitary.grahak.ui.components

import androidx.compose.ui.graphics.Color
import com.princesanitary.grahak.data.model.JobStatus
import com.princesanitary.grahak.ui.theme.PaperDim
import com.princesanitary.grahak.ui.theme.StatusError
import com.princesanitary.grahak.ui.theme.StatusErrorSoft
import com.princesanitary.grahak.ui.theme.StatusInfo
import com.princesanitary.grahak.ui.theme.StatusInfoSoft
import com.princesanitary.grahak.ui.theme.StatusSuccess
import com.princesanitary.grahak.ui.theme.StatusSuccessSoft
import com.princesanitary.grahak.ui.theme.StatusWarning
import com.princesanitary.grahak.ui.theme.StatusWarningSoft
import com.princesanitary.grahak.ui.theme.TextSecondary

/**
 * One place that decides how a job status looks and reads.
 *
 * Customer and plumber both display the same job, so the wording and colour must
 * come from the same source - otherwise the two sides drift and a job looks
 * different depending on who is looking at it.
 */
data class JobStatusStyle(
    val label: String,
    val container: Color,
    val content: Color,
    /** Written from the customer's point of view. */
    val customerCaption: String,
    /** Written from the plumber's point of view. */
    val plumberCaption: String
)

fun JobStatus.style(): JobStatusStyle = when (this) {
    JobStatus.SEARCHING -> JobStatusStyle(
        label = "FINDING PLUMBER",
        container = StatusWarningSoft,
        content = StatusWarning,
        customerCaption = "Waiting for a plumber to accept",
        plumberCaption = "Open request - yours to accept"
    )

    JobStatus.ACCEPTED -> JobStatusStyle(
        label = "ACCEPTED",
        container = StatusInfoSoft,
        content = StatusInfo,
        customerCaption = "A plumber has taken your job",
        plumberCaption = "You accepted this job"
    )

    JobStatus.EN_ROUTE -> JobStatusStyle(
        label = "ON THE WAY",
        container = StatusInfoSoft,
        content = StatusInfo,
        customerCaption = "Your plumber is on the way",
        plumberCaption = "You're travelling to the site"
    )

    JobStatus.INSPECTING -> JobStatusStyle(
        label = "INSPECTING",
        container = StatusInfoSoft,
        content = StatusInfo,
        customerCaption = "Your plumber is checking the problem",
        plumberCaption = "Inspecting the site"
    )

    JobStatus.IN_PROGRESS -> JobStatusStyle(
        label = "WORK ON",
        container = StatusInfoSoft,
        content = StatusInfo,
        customerCaption = "Work is underway",
        plumberCaption = "Work in progress"
    )

    JobStatus.COMPLETED -> JobStatusStyle(
        label = "COMPLETED",
        container = StatusSuccessSoft,
        content = StatusSuccess,
        customerCaption = "Job finished",
        plumberCaption = "You completed this job"
    )

    JobStatus.CANCELLED -> JobStatusStyle(
        label = "CANCELLED",
        container = StatusErrorSoft,
        content = StatusError,
        customerCaption = "This request was cancelled",
        plumberCaption = "This job was cancelled"
    )
}

/** Ordered stages used by the tracking timeline. Cancelled is not a stage. */
val JOB_TIMELINE: List<JobStatus> = listOf(
    JobStatus.SEARCHING,
    JobStatus.ACCEPTED,
    JobStatus.EN_ROUTE,
    JobStatus.INSPECTING,
    JobStatus.IN_PROGRESS,
    JobStatus.COMPLETED
)

/** Fallback colours for anything outside the timeline. */
val IdleStatusContainer: Color = PaperDim
val IdleStatusContent: Color = TextSecondary
