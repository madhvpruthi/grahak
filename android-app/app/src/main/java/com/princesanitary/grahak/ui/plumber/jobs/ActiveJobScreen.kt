package com.princesanitary.grahak.ui.plumber.jobs

import com.princesanitary.grahak.ui.theme.Paper
import com.princesanitary.grahak.ui.theme.Ink

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.princesanitary.grahak.di.appViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.princesanitary.grahak.data.model.JobStatus
import com.princesanitary.grahak.data.model.Order
import com.princesanitary.grahak.data.model.OrderStatus
import com.princesanitary.grahak.data.model.ServiceJob
import com.princesanitary.grahak.ui.theme.PlumberAccent
import com.princesanitary.grahak.ui.theme.StatusSuccess
import com.princesanitary.grahak.ui.theme.StatusWarning

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveJobScreen(
    jobId: String,
    viewModel: ActiveJobViewModel = appViewModel(),
    onBack: () -> Unit,
    onCreateEstimate: (String) -> Unit
) {
    val jobState by viewModel.jobState.collectAsStateWithLifecycle()

    LaunchedEffect(jobId) {
        viewModel.loadJob(jobId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Active Job") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Paper,
                    titleContentColor = Ink,
                    navigationIconContentColor = Ink
                )
            )
        }
    ) { padding ->
        when (val state = jobState) {
            is JobUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = PlumberAccent)
                }
            }
            is JobUiState.Success -> {
                JobContent(
                    job = state.job,
                    linkedOrders = state.linkedOrders,
                    onUpdateStatus = { status -> viewModel.updateStatus(status) },
                    onCreateEstimate = { onCreateEstimate(state.job.customerId) },
                    onBack = onBack,
                    modifier = Modifier.padding(padding)
                )
            }
            is JobUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = state.message,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun JobContent(
    job: ServiceJob,
    linkedOrders: List<Order>,
    onUpdateStatus: (JobStatus) -> Unit,
    onCreateEstimate: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showCompleteConfirmation by remember { mutableStateOf(false) }

    if (showCompleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showCompleteConfirmation = false },
            title = { Text("Complete & Close Job") },
            text = {
                Text(
                    "Are you sure you want to mark this job as completed? This will conclude the service visit and record it in your work history."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showCompleteConfirmation = false
                        onUpdateStatus(JobStatus.COMPLETED)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PlumberAccent)
                ) {
                    Text("Yes, Complete Job")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCompleteConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Status Timeline
        item {
            JobStatusTimeline(currentStatus = job.status)
        }

        // Completion Banner if COMPLETED
        if (job.status == JobStatus.COMPLETED) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = StatusSuccess.copy(alpha = 0.12f)
                    ),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(StatusSuccess)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = StatusSuccess,
                            modifier = Modifier.size(52.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Job Completed & Closed",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = StatusSuccess
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Great job! This service request has been finished.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (job.agreedVisitRate > 0) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Visit Fee: ${com.princesanitary.grahak.ui.components.formatRupees(job.agreedVisitRate)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        com.princesanitary.grahak.ui.components.PrimaryButton(
                            text = "Back to Jobs",
                            icon = Icons.Default.ArrowBack,
                            onClick = onBack
                        )
                    }
                }
            }
        }

        /*
         * Site card: map + address + Call / Directions.
         *
         * Replaces a plain two-line text block. Once a job is accepted the only
         * things that matter are getting there and reaching the customer, so both
         * are primary actions instead of being buried further down the page.
         */
        item {
            JobSiteCard(job = job)
        }

        // What the customer actually asked for.
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "The job",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    DetailRow(
                        icon = Icons.Default.Build,
                        label = "Service",
                        value = com.princesanitary.grahak.data.model.ServiceSpeciality
                            .fromId(job.serviceCategory)?.displayName
                            ?: job.serviceCategory
                    )

                    if (job.description.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        DetailRow(
                            icon = Icons.Default.Info,
                            label = "Details from customer",
                            value = job.description
                        )
                    }

                    if (job.agreedVisitRate > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        DetailRow(
                            icon = Icons.Default.CheckCircle,
                            label = "Agreed visit rate",
                            value = com.princesanitary.grahak.ui.components
                                .formatRupees(job.agreedVisitRate)
                        )
                    }
                }
            }
        }

        // Materials & Parchi Section
        item {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Materials & Parchi (${linkedOrders.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (job.status != JobStatus.COMPLETED && job.status != JobStatus.CANCELLED) {
                            TextButton(onClick = onCreateEstimate) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = PlumberAccent
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (linkedOrders.isEmpty()) "Make Parchi" else "Add Parchi",
                                    color = PlumberAccent,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    if (linkedOrders.isEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No material estimates / parchis linked yet. If you need supplies from Prince Sanitary, create a parchi for the customer.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Spacer(modifier = Modifier.height(12.dp))
                        linkedOrders.forEach { order ->
                            OrderSummaryCard(order = order)
                            Spacer(modifier = Modifier.height(10.dp))
                        }
                    }
                }
            }
        }

        // Quick Actions based on status
        if (job.status != JobStatus.COMPLETED && job.status != JobStatus.CANCELLED) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Paper.copy(alpha = 0.1f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Next Steps",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        when (job.status) {
                            JobStatus.ACCEPTED -> {
                                Text(
                                    text = "Navigate to the site and update status when you're on your way",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                                com.princesanitary.grahak.ui.components.PrimaryButton(
                                    text = "I'm On My Way",
                                    icon = Icons.Default.DirectionsCar,
                                    onClick = { onUpdateStatus(JobStatus.EN_ROUTE) }
                                )
                            }
                            JobStatus.EN_ROUTE -> {
                                Text(
                                    text = "Update status when you arrive at the site",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                                com.princesanitary.grahak.ui.components.PrimaryButton(
                                    text = "I've Arrived (Start Inspection)",
                                    icon = Icons.Default.CheckCircle,
                                    onClick = { onUpdateStatus(JobStatus.INSPECTING) }
                                )
                            }
                            JobStatus.INSPECTING -> {
                                Text(
                                    text = "You are currently inspecting the site. Create material parchi, start plumbing work, or complete the job when finished.",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Spacer(modifier = Modifier.height(14.dp))

                                // Button 1: Start Work (In Progress)
                                com.princesanitary.grahak.ui.components.PrimaryButton(
                                    text = "Start Work (In Progress)",
                                    icon = Icons.Default.PlayArrow,
                                    onClick = { onUpdateStatus(JobStatus.IN_PROGRESS) }
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                // Button 2: Create Material Estimate (Parchi)
                                OutlinedButton(
                                    onClick = onCreateEstimate,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = PlumberAccent
                                    )
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (linkedOrders.isEmpty()) "Create Material Estimate (Parchi)" else "Add Another Parchi",
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Button 3: Complete & Close Job
                                Button(
                                    onClick = { showCompleteConfirmation = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = StatusSuccess
                                    )
                                ) {
                                    Icon(Icons.Default.Done, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Complete & Close Job", fontWeight = FontWeight.SemiBold)
                                }
                            }
                            JobStatus.IN_PROGRESS -> {
                                Text(
                                    text = "Plumbing work is in progress. Once you finish or parts are installed, mark the job as completed.",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Spacer(modifier = Modifier.height(14.dp))

                                // Primary Action: Complete & Close Job
                                com.princesanitary.grahak.ui.components.PrimaryButton(
                                    text = "Complete & Close Job",
                                    icon = Icons.Default.Done,
                                    onClick = { showCompleteConfirmation = true }
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                // Secondary Action: Add more materials
                                OutlinedButton(
                                    onClick = onCreateEstimate,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = PlumberAccent
                                    )
                                ) {
                                    Icon(Icons.Default.AddShoppingCart, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Add More Materials (Parchi)", fontWeight = FontWeight.SemiBold)
                                }
                            }
                            else -> {}
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OrderSummaryCard(order: Order) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Inventory2,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = PlumberAccent
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = order.getDisplayOrderNumber(),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                val (statusColor, statusBg, statusText) = when (order.status) {
                    OrderStatus.DELIVERED -> Triple(StatusSuccess, StatusSuccess.copy(alpha = 0.15f), "Delivered")
                    OrderStatus.SHIPPED -> Triple(PlumberAccent, PlumberAccent.copy(alpha = 0.15f), "Out for Delivery")
                    OrderStatus.PACKED -> Triple(StatusWarning, StatusWarning.copy(alpha = 0.15f), "Packed")
                    OrderStatus.CONFIRMED -> Triple(StatusWarning, StatusWarning.copy(alpha = 0.15f), "Confirmed")
                    OrderStatus.PLACED -> Triple(Ink, Ink.copy(alpha = 0.1f), "Placed")
                    OrderStatus.CANCELLED -> Triple(MaterialTheme.colorScheme.error, MaterialTheme.colorScheme.error.copy(alpha = 0.15f), "Cancelled")
                }

                Surface(
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(6.dp),
                    color = statusBg
                ) {
                    Text(
                        text = statusText,
                        color = statusColor,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val itemCount = order.items.sumOf { it.qty }
                Text(
                    text = if (itemCount > 0) "$itemCount item${if (itemCount > 1) "s" else ""}" else "Material request",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = com.princesanitary.grahak.ui.components.formatRupees(order.billing.grandTotal),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (order.plumberCommission > 0) {
                Spacer(modifier = Modifier.height(6.dp))
                val isCredited = order.commissionReleased || order.status == OrderStatus.DELIVERED
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isCredited) Icons.Default.CheckCircle else Icons.Default.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = if (isCredited) StatusSuccess else StatusWarning
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isCredited)
                            "Commission: ${com.princesanitary.grahak.ui.components.formatRupees(order.plumberCommission)} (Credited)"
                        else
                            "Commission: ${com.princesanitary.grahak.ui.components.formatRupees(order.plumberCommission)} (On Delivery)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isCredited) StatusSuccess else StatusWarning
                    )
                }
            }
        }
    }
}

@Composable
fun JobStatusTimeline(currentStatus: JobStatus) {
    val steps = listOf(
        JobStatus.ACCEPTED to "Job Accepted",
        JobStatus.EN_ROUTE to "On the Way",
        JobStatus.INSPECTING to "Inspecting Site",
        JobStatus.IN_PROGRESS to "Work in Progress",
        JobStatus.COMPLETED to "Completed"
    )

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Job Progress",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            steps.forEachIndexed { index, (status, label) ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Step Indicator
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(
                                if (steps.indexOf(status to label) <= steps.indexOfFirst { it.first == currentStatus })
                                    PlumberAccent
                                else
                                    MaterialTheme.colorScheme.surfaceVariant
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (status == currentStatus) {
                            Icon(
                                imageVector = Icons.Default.RadioButtonChecked,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        } else if (steps.indexOf(status to label) < steps.indexOfFirst { it.first == currentStatus }) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (status == currentStatus) FontWeight.Bold else FontWeight.Normal,
                        color = if (steps.indexOf(status to label) <= steps.indexOfFirst { it.first == currentStatus })
                            MaterialTheme.colorScheme.onSurface
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (index < steps.size - 1) {
                    Box(
                        modifier = Modifier
                            .padding(start = 15.dp, top = 4.dp, bottom = 4.dp)
                            .width(2.dp)
                            .height(24.dp)
                            .background(
                                if (steps.indexOf(status to label) < steps.indexOfFirst { it.first == currentStatus })
                                    PlumberAccent
                                else
                                    MaterialTheme.colorScheme.surfaceVariant
                            )
                    )
                }
            }
        }
    }
}

@Composable
fun DetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = PlumberAccent
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
