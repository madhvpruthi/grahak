package com.princesanitary.grahak.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.princesanitary.grahak.data.model.JobStatus
import com.princesanitary.grahak.data.model.ServiceJob
import com.princesanitary.grahak.data.model.ServiceSpeciality
import com.princesanitary.grahak.ui.theme.Ink
import com.princesanitary.grahak.ui.theme.Line
import com.princesanitary.grahak.ui.theme.SurfaceWhite
import com.princesanitary.grahak.ui.theme.TextSecondary
import com.princesanitary.grahak.ui.theme.TextTertiary
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Row used in both the customer's request list and the plumber's job history.
 *
 * Sharing one card is what keeps the two sides consistent: the same job reads
 * identically to whoever is looking at it, apart from the caption, which is
 * written from each side's point of view.
 */
@Composable
fun JobSummaryCard(
    job: ServiceJob,
    asPlumber: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val style = job.status.style()
    val serviceName = ServiceSpeciality.fromId(job.serviceCategory)?.displayName
        ?: job.serviceCategory.ifBlank { "Service" }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(SurfaceWhite)
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            StatusPill(style.label, style.container, style.content)
            Spacer(Modifier.weight(1f))
            Text(
                text = remember_dateFormat.format(job.updatedAt),
                style = MaterialTheme.typography.bodySmall,
                color = TextTertiary
            )
        }

        Spacer(Modifier.height(12.dp))

        Text(
            text = serviceName,
            style = MaterialTheme.typography.titleMedium,
            color = Ink
        )

        if (job.description.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = job.description,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(Modifier.height(10.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Place, null, Modifier.size(15.dp), tint = TextTertiary)
            Spacer(Modifier.width(6.dp))
            Text(
                text = job.siteAddress.fullAddress.ifBlank { "Address not set" },
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Icon(Icons.Rounded.ChevronRight, null, Modifier.size(18.dp), tint = TextTertiary)
        }

        Spacer(Modifier.height(10.dp))

        Text(
            text = if (asPlumber) style.plumberCaption else style.customerCaption,
            style = MaterialTheme.typography.bodySmall,
            color = style.content
        )
    }
}

private val remember_dateFormat = SimpleDateFormat("d MMM, h:mm a", Locale.getDefault())

/**
 * Vertical progress timeline for a job.
 *
 * Completed stages collapse to a tick, the current stage is filled, and future
 * stages are hollow - so the customer can see at a glance both where things are
 * and what is still to come.
 */
@Composable
fun JobTimeline(
    current: JobStatus,
    asPlumber: Boolean,
    modifier: Modifier = Modifier
) {
    if (current == JobStatus.CANCELLED) {
        val style = JobStatus.CANCELLED.style()
        Row(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(style.container)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (asPlumber) style.plumberCaption else style.customerCaption,
                style = MaterialTheme.typography.bodyMedium,
                color = style.content
            )
        }
        return
    }

    val currentIndex = JOB_TIMELINE.indexOf(current).coerceAtLeast(0)

    Column(modifier = modifier.fillMaxWidth()) {
        JOB_TIMELINE.forEachIndexed { index, stage ->
            val done = index < currentIndex
            val active = index == currentIndex
            val style = stage.style()

            Row(verticalAlignment = Alignment.Top) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    done -> style.content
                                    active -> style.content
                                    else -> Line
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (done) {
                            Icon(
                                Icons.Rounded.Check,
                                null,
                                Modifier.size(14.dp),
                                tint = SurfaceWhite
                            )
                        } else if (active) {
                            Box(
                                Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(SurfaceWhite)
                            )
                        }
                    }

                    if (index != JOB_TIMELINE.lastIndex) {
                        Box(
                            Modifier
                                .width(2.dp)
                                .height(30.dp)
                                .background(if (done) style.content else Line)
                        )
                    }
                }

                Spacer(Modifier.width(14.dp))

                Column(Modifier.padding(bottom = 14.dp)) {
                    Text(
                        text = stage.style().label.lowercase()
                            .replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.titleSmall,
                        color = if (done || active) Ink else TextTertiary
                    )
                    if (active) {
                        Text(
                            text = if (asPlumber) {
                                style.plumberCaption
                            } else {
                                style.customerCaption
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }
        }
    }
}
