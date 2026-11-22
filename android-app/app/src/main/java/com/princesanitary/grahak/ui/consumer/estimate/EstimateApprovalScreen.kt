@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.princesanitary.grahak.ui.consumer.estimate

import com.princesanitary.grahak.ui.theme.Paper
import com.princesanitary.grahak.ui.theme.Ink

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.princesanitary.grahak.di.appViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.princesanitary.grahak.data.model.Estimate
import com.princesanitary.grahak.data.model.EstimateStatus
import com.princesanitary.grahak.data.model.SavedAddress
import com.princesanitary.grahak.ui.theme.ConsumerAccent
import com.princesanitary.grahak.ui.theme.StatusSuccess
import com.princesanitary.grahak.ui.theme.StatusWarning

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EstimateApprovalScreen(
    estimateId: String,
    viewModel: EstimateApprovalViewModel = appViewModel(),
    onOrderCreated: (String) -> Unit,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val actionState by viewModel.actionState.collectAsStateWithLifecycle()

    var showAddressDialog by remember { mutableStateOf(false) }
    var selectedAddress by remember { mutableStateOf<SavedAddress?>(null) }
    var showRejectDialog by remember { mutableStateOf(false) }
    var rejectReason by remember { mutableStateOf("") }

    LaunchedEffect(estimateId) {
        viewModel.loadEstimate(estimateId)
    }

    // Handle approval success
    LaunchedEffect(actionState) {
        when (val state = actionState) {
            is ApprovalAction.Approved -> {
                onOrderCreated(state.orderId)
            }
            is ApprovalAction.Rejected -> {
                onBack()
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Material Estimate") },
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
        when (val state = uiState) {
            is EstimateApprovalUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = ConsumerAccent)
                }
            }
            is EstimateApprovalUiState.Success -> {
                EstimateContent(
                    estimate = state.estimate,
                    onApprove = {
                        if (selectedAddress == null && viewModel.getAvailableAddresses().isNotEmpty()) {
                            selectedAddress = viewModel.getAvailableAddresses().first()
                        }
                        showAddressDialog = true
                    },
                    onReject = { showRejectDialog = true },
                    isProcessing = actionState is ApprovalAction.Processing,
                    modifier = Modifier.padding(padding)
                )

                // Address selection dialog
                if (showAddressDialog) {
                    AddressSelectionDialog(
                        addresses = viewModel.getAvailableAddresses(),
                        selectedAddress = selectedAddress,
                        onAddressSelected = { address ->
                            selectedAddress = address
                        },
                        onConfirm = {
                            if (selectedAddress != null) {
                                viewModel.approveEstimate(state.estimate.id, selectedAddress!!)
                                showAddressDialog = false
                            }
                        },
                        onDismiss = { showAddressDialog = false }
                    )
                }

                // Reject dialog
                if (showRejectDialog) {
                    AlertDialog(
                        onDismissRequest = { showRejectDialog = false },
                        icon = { Icon(Icons.Default.Cancel, contentDescription = null) },
                        title = { Text("Reject Estimate?") },
                        text = {
                            Column {
                                Text("Please provide a reason for rejection:")
                                Spacer(modifier = Modifier.height(12.dp))
                                OutlinedTextField(
                                    value = rejectReason,
                                    onValueChange = { rejectReason = it },
                                    label = { Text("Reason") },
                                    modifier = Modifier.fillMaxWidth(),
                                    minLines = 2
                                )
                            }
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    viewModel.rejectEstimate(state.estimate.id, rejectReason)
                                    showRejectDialog = false
                                },
                                enabled = rejectReason.isNotBlank()
                            ) {
                                Text("Reject", color = MaterialTheme.colorScheme.error)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showRejectDialog = false }) {
                                Text("Cancel")
                            }
                        }
                    )
                }
            }
            is EstimateApprovalUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onBack) {
                            Text("Go Back")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EstimateContent(
    estimate: Estimate,
    onApprove: () -> Unit,
    onReject: () -> Unit,
    isProcessing: Boolean,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Paper.copy(alpha = 0.1f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Plumber's Estimate",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Estimate #${estimate.id.take(8).uppercase()}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        EstimateStatusChip(status = estimate.status)
                    }
                }
            }
        }

        // Material Items
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.ShoppingCart,
                            contentDescription = null,
                            tint = ConsumerAccent
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Materials Required (${estimate.items.size} items)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Divider()

                    estimate.items.forEach { item ->
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.productName,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "₹${item.unitPrice} × ${item.quantity}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (item.gstRate > 0) {
                                    Text(
                                        text = "+ ${item.gstRate}% GST",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Text(
                                text = "₹${item.total + (item.total * item.gstRate / 100)}",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        // Cost Summary
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Cost Breakdown",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Materials Subtotal:", style = MaterialTheme.typography.bodyMedium)
                        Text("₹${estimate.subtotal}", style = MaterialTheme.typography.bodyMedium)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("GST:", style = MaterialTheme.typography.bodyMedium)
                        Text("₹${estimate.taxTotal}", style = MaterialTheme.typography.bodyMedium)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Labour Charge:", style = MaterialTheme.typography.bodyMedium)
                        Text("₹${estimate.labourCharge}", style = MaterialTheme.typography.bodyMedium)
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Divider()
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Total Amount:",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "₹${estimate.grandTotal}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = ConsumerAccent
                        )
                    }
                }
            }
        }

        // Notes (if any)
        if (estimate.notes.isNotEmpty()) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Note,
                                contentDescription = null,
                                tint = ConsumerAccent
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Plumber's Notes",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = estimate.notes,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }

        // Action Buttons (only for SUBMITTED status)
        if (estimate.status == EstimateStatus.SUBMITTED) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onReject,
                        modifier = Modifier.weight(1f).height(56.dp),
                        enabled = !isProcessing,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(Icons.Default.Cancel, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Reject")
                    }

                    Button(
                        onClick = onApprove,
                        modifier = Modifier.weight(1f).height(56.dp),
                        enabled = !isProcessing,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = StatusSuccess
                        )
                    ) {
                        if (isProcessing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Icon(Icons.Default.CheckCircle, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Approve & Order")
                        }
                    }
                }
            }
        }

        // Bottom spacing
        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun EstimateStatusChip(status: EstimateStatus) {
    val (color, text) = when (status) {
        EstimateStatus.DRAFT -> MaterialTheme.colorScheme.surfaceVariant to "Draft"
        EstimateStatus.SUBMITTED -> StatusWarning to "Pending Approval"
        EstimateStatus.APPROVED -> StatusSuccess to "Approved"
        EstimateStatus.REJECTED -> MaterialTheme.colorScheme.error to "Rejected"
        EstimateStatus.CONVERTED_TO_ORDER -> StatusSuccess to "Order Placed"
    }

    Surface(
        color = color.copy(alpha = 0.15f),
        shape = MaterialTheme.shapes.small
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = color,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun AddressSelectionDialog(
    addresses: List<SavedAddress>,
    selectedAddress: SavedAddress?,
    onAddressSelected: (SavedAddress) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocationOn, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Select Delivery Address")
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Materials will be delivered to:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                addresses.forEach { address ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onAddressSelected(address) },
                        border = if (address == selectedAddress)
                            BorderStroke(2.dp, ConsumerAccent)
                        else
                            null,
                        colors = if (address == selectedAddress)
                            CardDefaults.cardColors(
                                containerColor = Paper.copy(alpha = 0.1f)
                            )
                        else
                            CardDefaults.cardColors()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = address.tag,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (address == selectedAddress) ConsumerAccent else MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = address.fullAddress,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = selectedAddress != null
            ) {
                Text("Approve & Place Order")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
