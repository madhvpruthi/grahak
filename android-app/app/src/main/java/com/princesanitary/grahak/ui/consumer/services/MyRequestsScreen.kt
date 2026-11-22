package com.princesanitary.grahak.ui.consumer.services

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Plumbing
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.princesanitary.grahak.data.model.JobStatus
import com.princesanitary.grahak.data.model.ServiceSpeciality
import com.princesanitary.grahak.di.appViewModel
import com.princesanitary.grahak.ui.components.BackButton
import com.princesanitary.grahak.ui.components.EmptyState
import com.princesanitary.grahak.ui.components.ErrorBanner
import com.princesanitary.grahak.ui.components.GrahakCard
import com.princesanitary.grahak.ui.components.JobSummaryCard
import com.princesanitary.grahak.ui.components.JobTimeline
import com.princesanitary.grahak.ui.components.ListCardSkeleton
import com.princesanitary.grahak.ui.components.SecondaryButton
import com.princesanitary.grahak.ui.components.StatusPill
import com.princesanitary.grahak.ui.components.formatRupees
import com.princesanitary.grahak.ui.components.style
import com.princesanitary.grahak.ui.theme.BrandTeal
import com.princesanitary.grahak.ui.theme.Ink
import com.princesanitary.grahak.ui.theme.Paper
import com.princesanitary.grahak.ui.theme.StatusError
import com.princesanitary.grahak.ui.theme.SurfaceWhite
import com.princesanitary.grahak.ui.theme.TextSecondary
import com.princesanitary.grahak.ui.theme.TextTertiary
import com.princesanitary.grahak.ui.theme.TintMint

/**
 * The customer's service requests: live, past and cancelled.
 *
 * Nothing in the app used to show these at all - a customer could book a plumber
 * and then have no way to see what happened next.
 */
@Composable
fun MyRequestsScreen(
    viewModel: MyRequestsViewModel = appViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val tracked by viewModel.tracked.collectAsStateWithLifecycle()

    // Tracking a single request takes over the screen.
    tracked?.let { request ->
        TrackRequestScreen(
            request = request,
            onBack = viewModel::stopTracking,
            onCancel = { viewModel.cancel(request.job.id) }
        )
        return
    }

    Box(Modifier.fillMaxSize()) {
        when (val s = state) {
            RequestsState.Loading -> ListCardSkeleton(count = 3)

            is RequestsState.Error -> Column(Modifier.padding(20.dp)) {
                ErrorBanner(s.message)
            }

            is RequestsState.Ready -> {
                if (s.jobs.isEmpty()) {
                    EmptyState(
                        icon = Icons.Rounded.Plumbing,
                        title = "No plumber bookings yet",
                        message = "Book a plumber from the Book tab and you'll be able " +
                            "to track them here."
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(
                            start = 20.dp, end = 20.dp, top = 4.dp, bottom = 20.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(s.jobs, key = { it.id }) { job ->
                            JobSummaryCard(
                                job = job,
                                asPlumber = false,
                                onClick = { viewModel.track(job) }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Live tracking for one request.
 *
 * Shows the stage timeline, who accepted it, a way to call them, and - once the
 * job is finished - the rating prompt.
 */
@Composable
private fun TrackRequestScreen(
    request: TrackedRequest,
    onBack: () -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val job = request.job
    val plumber = request.plumber
    val style = job.status.style()

    var confirmCancel by remember { mutableStateOf(false) }
    var showRating by remember { mutableStateOf(false) }

    val serviceName = ServiceSpeciality.fromId(job.serviceCategory)?.displayName
        ?: job.serviceCategory

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Paper)
    ) {
        Row(
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.statusBars)
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BackButton(onClick = onBack)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("Your request", style = MaterialTheme.typography.titleLarge, color = Ink)
                Text(
                    serviceName,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextTertiary
                )
            }
            StatusPill(style.label, style.container, style.content)
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            // ---- who's coming ----
            if (plumber != null) {
                GrahakCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(TintMint),
                            contentAlignment = Alignment.Center
                        ) {
                            if (plumber.photoUrl.isNotBlank()) {
                                AsyncImage(
                                    model = plumber.photoUrl,
                                    contentDescription = plumber.name,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Text(
                                    plumber.name.take(1).uppercase(),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = Ink
                                )
                            }
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                plumber.name,
                                style = MaterialTheme.typography.titleMedium,
                                color = Ink,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (plumber.stats.ratingCount > 0) {
                                    Icon(
                                        Icons.Rounded.Star,
                                        null,
                                        Modifier.size(13.dp),
                                        tint = Ink
                                    )
                                    Spacer(Modifier.width(3.dp))
                                    Text(
                                        "%.1f".format(plumber.stats.rating),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Ink
                                    )
                                    Spacer(Modifier.width(8.dp))
                                }
                                Text(
                                    "${plumber.stats.jobsCompleted} jobs done",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }

                        // Calling the plumber is the single most useful action
                        // here, so it stays a one-tap affordance.
                        if (plumber.phone.isNotBlank()) {
                            Surface(
                                onClick = {
                                    context.startActivity(
                                        Intent(
                                            Intent.ACTION_DIAL,
                                            Uri.parse("tel:${plumber.phone}")
                                        )
                                    )
                                },
                                shape = CircleShape,
                                color = BrandTeal,
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Rounded.Call,
                                        "Call plumber",
                                        Modifier.size(19.dp),
                                        tint = SurfaceWhite
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            // ---- progress ----
            GrahakCard {
                Text("Progress", style = MaterialTheme.typography.titleMedium, color = Ink)
                Spacer(Modifier.height(16.dp))
                JobTimeline(current = job.status, asPlumber = false)
            }

            Spacer(Modifier.height(16.dp))

            // ---- job facts ----
            GrahakCard {
                Text("Details", style = MaterialTheme.typography.titleMedium, color = Ink)
                Spacer(Modifier.height(12.dp))
                Fact("Service", serviceName)
                if (job.description.isNotBlank()) Fact("You said", job.description)
                Fact("Site", job.siteAddress.fullAddress.ifBlank { "Not set" })
                if (job.agreedVisitRate > 0) {
                    Fact("Agreed visit rate", formatRupees(job.agreedVisitRate))
                }
            }

            Spacer(Modifier.height(16.dp))

            // ---- actions ----
            if (job.status == JobStatus.SEARCHING || job.status == JobStatus.ACCEPTED) {
                SecondaryButton(
                    text = "Cancel this request",
                    onClick = { confirmCancel = true }
                )
            }

            if (job.status == JobStatus.COMPLETED && plumber != null) {
                if (request.alreadyRated) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(SurfaceWhite)
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.Star, null, Modifier.size(18.dp), tint = Ink)
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "Thanks - you've rated this job",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                } else {
                    SecondaryButton(
                        text = "Rate ${plumber.name.split(" ").first()}",
                        leadingIcon = Icons.Rounded.Star,
                        onClick = { showRating = true }
                    )
                }
            }

            Spacer(Modifier.height(28.dp))
        }
    }

    if (confirmCancel) {
        AlertDialog(
            onDismissRequest = { confirmCancel = false },
            title = { Text("Cancel this request?") },
            text = { Text("The plumber will be notified that the job is no longer needed.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmCancel = false
                    onCancel()
                }) { Text("Cancel request", color = StatusError) }
            },
            dismissButton = {
                TextButton(onClick = { confirmCancel = false }) {
                    Text("Keep it", color = TextSecondary)
                }
            },
            containerColor = SurfaceWhite
        )
    }

    if (showRating && plumber != null) {
        RateJobDialog(
            jobId = job.id,
            plumberId = plumber.id,
            plumberName = plumber.name,
            serviceCategory = job.serviceCategory,
            customerName = job.customerName,
            onDone = { showRating = false }
        )
    }
}

@Composable
private fun Fact(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            modifier = Modifier.width(120.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = Ink,
            modifier = Modifier.weight(1f)
        )
    }
}
