package com.princesanitary.grahak.ui.plumber.jobs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Plumbing
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.princesanitary.grahak.data.model.ServiceJob
import com.princesanitary.grahak.di.appViewModel
import com.princesanitary.grahak.ui.components.PrimaryButton
import com.princesanitary.grahak.ui.components.SecondaryButton
import com.princesanitary.grahak.ui.theme.BentoBlue
import com.princesanitary.grahak.ui.theme.BentoGreen
import com.princesanitary.grahak.ui.theme.BentoOnVibrant
import com.princesanitary.grahak.ui.theme.BentoPink
import com.princesanitary.grahak.ui.theme.ElectricLime
import com.princesanitary.grahak.ui.theme.Ink
import com.princesanitary.grahak.ui.theme.InkDark
import com.princesanitary.grahak.ui.theme.Line
import com.princesanitary.grahak.ui.theme.PaperDim
import com.princesanitary.grahak.ui.theme.SurfaceWhite
import com.princesanitary.grahak.ui.theme.TextSecondary
import com.princesanitary.grahak.ui.theme.TextTertiary

@Composable
fun JobRequestsScreen(
    onJobAccepted: (String) -> Unit = {}
) {
    val viewModel: JobRequestsViewModel = appViewModel()
    val availableJobs by viewModel.availableJobs.collectAsStateWithLifecycle()
    val acceptingJobId by viewModel.acceptingJobId.collectAsStateWithLifecycle()
    val decliningJobId by viewModel.decliningJobId.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    LaunchedEffect(acceptingJobId) {
        acceptingJobId?.let { jobId ->
            onJobAccepted(jobId)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (availableJobs.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(PaperDim),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.Bolt,
                        contentDescription = null,
                        tint = BentoGreen,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "You're all caught up!",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = Ink
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "New customer booking requests will ping here live.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(availableJobs) { job ->
                    JobRequestCard(
                        job = job,
                        isAccepting = acceptingJobId == job.id,
                        isDeclining = decliningJobId == job.id,
                        onAccept = { viewModel.acceptJob(job.id) },
                        onDecline = { viewModel.declineJob(job.id) }
                    )
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun JobRequestCard(
    job: ServiceJob,
    isAccepting: Boolean,
    isDeclining: Boolean,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        border = BorderStroke(1.dp, Line)
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            // Category & Badge Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(BentoPink.copy(alpha = 0.2f))
                        .padding(horizontal = 12.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = job.serviceCategory.uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            letterSpacing = 0.5.sp
                        ),
                        color = BentoPink
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(BentoGreen.copy(alpha = 0.15f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "NEW REQUEST",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp
                        ),
                        color = BentoGreen
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            Text(
                text = job.serviceCategory,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = Ink
            )

            // Description
            if (job.description.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = job.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Ink.copy(alpha = 0.85f)
                )
            }

            Spacer(Modifier.height(14.dp))

            // Site Address
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(PaperDim)
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = ElectricLime
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = job.siteAddress.fullAddress.ifBlank { "Site address provided on booking" },
                    style = MaterialTheme.typography.bodySmall,
                    color = Ink,
                    maxLines = 2
                )
            }

            // Customer Name
            if (job.customerName.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = TextSecondary
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = job.customerName,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    onClick = onDecline,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    enabled = !isAccepting && !isDeclining,
                    shape = CircleShape,
                    color = PaperDim,
                    border = BorderStroke(1.dp, Line)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (isDeclining) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = TextSecondary
                            )
                        } else {
                            Text(
                                text = "Pass",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = TextSecondary
                            )
                        }
                    }
                }

                Surface(
                    onClick = onAccept,
                    modifier = Modifier
                        .weight(1.3f)
                        .height(48.dp),
                    enabled = !isAccepting && !isDeclining,
                    shape = CircleShape,
                    color = BentoGreen
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (isAccepting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = BentoOnVibrant
                            )
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Rounded.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = BentoOnVibrant
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "Accept Job",
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = BentoOnVibrant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
