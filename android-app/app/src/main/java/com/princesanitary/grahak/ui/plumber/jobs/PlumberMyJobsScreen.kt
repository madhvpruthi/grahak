package com.princesanitary.grahak.ui.plumber.jobs

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.LocalShipping
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.WorkOutline
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.princesanitary.grahak.data.model.Order
import com.princesanitary.grahak.data.model.OrderStatus
import com.princesanitary.grahak.di.appViewModel
import com.princesanitary.grahak.ui.components.EmptyState
import com.princesanitary.grahak.ui.components.ErrorBanner
import com.princesanitary.grahak.ui.components.JobSummaryCard
import com.princesanitary.grahak.ui.components.ListCardSkeleton
import com.princesanitary.grahak.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * The plumber's own work: active jobs first, then material orders, then finished ones.
 */
@Composable
fun PlumberMyJobsScreen(
    onOpenJob: (String) -> Unit,
    viewModel: PlumberJobsHistoryViewModel = appViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    when {
        state.loading -> ListCardSkeleton(count = 3)

        state.error != null -> Column(Modifier.padding(20.dp)) {
            ErrorBanner(state.error!!)
        }

        state.active.isEmpty() && state.finished.isEmpty() && state.orders.isEmpty() -> EmptyState(
            icon = Icons.Rounded.WorkOutline,
            title = "No jobs or orders yet",
            message = "Accept a request from the Requests tab or submit material estimates and they will appear here."
        )

        else -> LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 20.dp, end = 20.dp, top = 4.dp, bottom = 24.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (state.active.isNotEmpty()) {
                item {
                    SectionLabel("Active Jobs", "${state.active.size}")
                }
                items(state.active, key = { it.id }) { job ->
                    JobSummaryCard(
                        job = job,
                        asPlumber = true,
                        onClick = { onOpenJob(job.id) }
                    )
                }
            }

            if (state.orders.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(8.dp))
                    SectionLabel("Store & Material Orders", "${state.orders.size}")
                }
                items(state.orders, key = { it.id }) { order ->
                    PlumberOrderSummaryCard(order = order)
                }
            }

            if (state.finished.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(8.dp))
                    SectionLabel("Completed Work History", "${state.finished.size}")
                }
                items(state.finished, key = { it.id }) { job ->
                    JobSummaryCard(
                        job = job,
                        asPlumber = true,
                        onClick = { onOpenJob(job.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PlumberOrderSummaryCard(order: Order) {
    val context = LocalContext.current
    val dateStr = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(order.createdAt)

    val (statusLabel, statusColor, statusBg) = when (order.status) {
        OrderStatus.PLACED -> Triple("Placed", Color(0xFF3B82F6), Color(0xFF3B82F6).copy(alpha = 0.12f))
        OrderStatus.CONFIRMED -> Triple("Confirmed", LuxeGold, LuxeGold.copy(alpha = 0.15f))
        OrderStatus.PACKED -> Triple("Packed", LuxeGold, LuxeGold.copy(alpha = 0.15f))
        OrderStatus.SHIPPED -> Triple("Dispatched", Color(0xFF8B5CF6), Color(0xFF8B5CF6).copy(alpha = 0.12f))
        OrderStatus.DELIVERED -> Triple("Delivered", BentoGreen, BentoGreen.copy(alpha = 0.15f))
        OrderStatus.CANCELLED -> Triple("Cancelled", Color(0xFFEF4444), Color(0xFFEF4444).copy(alpha = 0.12f))
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(SurfaceWhite)
            .border(1.dp, Line, RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        // Top row: Order Number badge & Status Pill
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(PaperDim)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "#${order.getDisplayOrderNumber()}",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = Ink
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    text = dateStr,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextTertiary
                )
            }

            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(statusBg)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = statusLabel,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = statusColor
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        // Items and Customer row
        Text(
            text = "${order.items.size} item(s): " + order.items.take(3).joinToString(", ") { "${it.qty}x ${it.name}" } + if (order.items.size > 3) "..." else "",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = Ink
        )

        if (order.customerName.isNotBlank() || order.customerPhone.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Customer: ${order.customerName.ifBlank { "Client" }} (${order.customerPhone})",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }

        Spacer(Modifier.height(12.dp))

        // Bottom row: Grand Total and Commission Earned
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Order Amount", style = MaterialTheme.typography.labelSmall, color = TextTertiary)
                Text(
                    text = "₹${"%.2f".format(order.billing.grandTotal)}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Ink
                )
            }

            if (order.plumberCommission > 0) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(BentoGreen.copy(alpha = 0.12f))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "+₹${"%.2f".format(order.plumberCommission)} Commission",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = BentoGreen
                    )
                }
            } else if (order.customerPhone.isNotBlank()) {
                FilledTonalButton(
                    onClick = {
                        try {
                            context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${order.customerPhone}")))
                        } catch (_: Exception) {}
                    },
                    shape = CircleShape,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = PaperDim,
                        contentColor = Ink
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Rounded.Call, null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Call Customer", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(title: String, count: String) {
    Box(Modifier.padding(vertical = 4.dp)) {
        Text(
            text = "$title · $count",
            style = MaterialTheme.typography.labelMedium,
            color = TextSecondary
        )
    }
}
