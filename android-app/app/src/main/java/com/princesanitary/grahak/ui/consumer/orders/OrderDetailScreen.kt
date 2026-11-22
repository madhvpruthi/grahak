package com.princesanitary.grahak.ui.consumer.orders

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.Chat
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.LocalShipping
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Money
import androidx.compose.material.icons.rounded.Payment
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.princesanitary.grahak.data.model.Order
import com.princesanitary.grahak.data.model.OrderStatus
import com.princesanitary.grahak.data.model.PaymentMethod
import com.princesanitary.grahak.data.model.PaymentStatus
import com.princesanitary.grahak.data.model.StoreSettings
import com.princesanitary.grahak.di.ServiceLocator
import com.princesanitary.grahak.di.appViewModel
import com.princesanitary.grahak.ui.components.formatRupees
import com.princesanitary.grahak.ui.theme.EmeraldSignal
import com.princesanitary.grahak.ui.theme.Ink
import com.princesanitary.grahak.ui.theme.Line
import com.princesanitary.grahak.ui.theme.LuxeGold
import com.princesanitary.grahak.ui.theme.LuxeGoldSurface
import com.princesanitary.grahak.ui.theme.Paper
import com.princesanitary.grahak.ui.theme.PaperDim
import com.princesanitary.grahak.ui.theme.StatusError
import com.princesanitary.grahak.ui.theme.StatusSuccess
import com.princesanitary.grahak.ui.theme.SurfaceWhite
import com.princesanitary.grahak.ui.theme.TextSecondary
import com.princesanitary.grahak.ui.theme.TextTertiary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun OrderDetailScreen(
    orderId: String,
    viewModel: OrderDetailViewModel = appViewModel(),
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val actionState by viewModel.actionState.collectAsStateWithLifecycle()
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    val storeSettings by ServiceLocator.orderRepository.observeStoreSettings()
        .collectAsStateWithLifecycle(initialValue = com.princesanitary.grahak.data.local.SettingsStorage.getCachedStoreSettings())
    val ownerPhone = storeSettings.ownerPhone

    var showCancelDialog by remember { mutableStateOf(false) }
    var cancelReason by remember { mutableStateOf("") }

    LaunchedEffect(orderId) {
        viewModel.loadOrder(orderId)
    }

    LaunchedEffect(actionState) {
        if (actionState is OrderAction.Cancelled) {
            viewModel.resetActionState()
        }
    }

    Scaffold(
        containerColor = Paper,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(PaperDim)
                            .border(1.dp, Line, CircleShape)
                            .clickable(onClick = onBack),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = Ink,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Order Details",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Ink
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable {
                                clipboardManager.setText(AnnotatedString(orderId))
                                Toast.makeText(context, "Order ID copied", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Text(
                                text = "#${orderId.take(10).uppercase()}",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                color = TextSecondary
                            )
                            Spacer(Modifier.width(4.dp))
                            Icon(Icons.Rounded.ContentCopy, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(12.dp))
                        }
                    }
                }

                val currentStatus = (uiState as? OrderDetailUiState.Success)?.order?.status
                if (currentStatus != null) {
                    StatusBadge(status = currentStatus)
                }
            }
        }
    ) { padding ->
        when (val state = uiState) {
            is OrderDetailUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    com.princesanitary.grahak.ui.components.FloatingBallLoader(
                        ballColor = LuxeGold,
                        label = "Loading order..."
                    )
                }
            }
            is OrderDetailUiState.Success -> {
                CleanOrderDetailContent(
                    order = state.order,
                    ownerPhone = ownerPhone,
                    onCancelOrder = { showCancelDialog = true },
                    modifier = Modifier.padding(padding)
                )

                if (showCancelDialog) {
                    AlertDialog(
                        onDismissRequest = { showCancelDialog = false },
                        icon = {
                            Icon(Icons.Rounded.Cancel, contentDescription = null, tint = StatusError, modifier = Modifier.size(28.dp))
                        },
                        title = {
                            Text(
                                "Cancel this Order?",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = Ink
                            )
                        },
                        text = {
                            Column {
                                Text(
                                    "Are you sure you want to cancel this order? Dispatched site vehicles may be recalled.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                                OutlinedTextField(
                                    value = cancelReason,
                                    onValueChange = { cancelReason = it },
                                    label = { Text("Reason for cancellation (optional)") },
                                    modifier = Modifier.fillMaxWidth(),
                                    minLines = 2,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = LuxeGold,
                                        unfocusedBorderColor = Line
                                    )
                                )
                            }
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    viewModel.cancelOrder(state.order.id, cancelReason)
                                    showCancelDialog = false
                                },
                                enabled = actionState !is OrderAction.Cancelling
                            ) {
                                if (actionState is OrderAction.Cancelling) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = StatusError)
                                } else {
                                    Text("Yes, Cancel Order", color = StatusError, fontWeight = FontWeight.Bold)
                                }
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showCancelDialog = false }) {
                                Text("Keep Order", color = Ink, fontWeight = FontWeight.SemiBold)
                            }
                        },
                        containerColor = SurfaceWhite
                    )
                }
            }
            is OrderDetailUiState.Error -> {
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
                            imageVector = Icons.Rounded.ErrorOutline,
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            tint = StatusError
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Ink
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onBack,
                            colors = ButtonDefaults.buttonColors(containerColor = LuxeGold, contentColor = Color(0xFF09090B)),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("Go Back", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Clean, simplified, and ultra-premium single order layout.
 * Eliminates visual clutter while presenting all essential status, contact, item, and billing details.
 */
@Composable
fun CleanOrderDetailContent(
    order: Order,
    ownerPhone: String,
    onCancelOrder: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val cleanPhone = ownerPhone.filter(Char::isDigit)

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Sleek Status & Progress Card
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = SurfaceWhite,
                border = BorderStroke(1.dp, Line)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    val etaHeadline = when (order.status) {
                        OrderStatus.DELIVERED -> "Delivered to site"
                        OrderStatus.CANCELLED -> "Order Cancelled"
                        OrderStatus.SHIPPED -> "Out for site delivery"
                        OrderStatus.PACKED -> "Packed & vehicle assigned"
                        OrderStatus.CONFIRMED -> "Order confirmed"
                        OrderStatus.PLACED -> "Order placed"
                    }
                    Text(
                        text = etaHeadline,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 19.sp
                        ),
                        color = Ink
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "Placed on ${formatDateTime(order.createdAt)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )

                    Spacer(Modifier.height(18.dp))

                    // Minimalist Progress Nodes
                    SimpleStatusTracker(currentStatus = order.status)
                }
            }
        }

        // 2. Compact Store Contact & Dispatch Bar (Uses actual owner phone)
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = SurfaceWhite,
                border = BorderStroke(1.dp, Line)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Prince Sanitary Store",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = Ink
                            )
                            Spacer(Modifier.width(4.dp))
                            Icon(Icons.Rounded.Verified, null, tint = LuxeGold, modifier = Modifier.size(14.dp))
                        }
                        Text(
                            text = if (ownerPhone.isNotBlank()) "+91 $ownerPhone • Direct Dispatch" else "Direct Dispatch Helpline",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                            color = TextSecondary
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // WhatsApp
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(PaperDim)
                                .border(1.dp, Line, CircleShape)
                                .clickable {
                                    try {
                                        val waIntent = Intent(
                                            Intent.ACTION_VIEW,
                                            Uri.parse("https://wa.me/91$cleanPhone?text=${Uri.encode("Hi, I have an inquiry about my order #${order.id.take(8)}")}")
                                        )
                                        context.startActivity(waIntent)
                                    } catch (_: Exception) {
                                        Toast.makeText(context, "WhatsApp: +91 $cleanPhone", Toast.LENGTH_SHORT).show()
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.Chat, contentDescription = "Chat", tint = Color(0xFF25D366), modifier = Modifier.size(17.dp))
                        }

                        // Call
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(PaperDim)
                                .border(1.dp, Line, CircleShape)
                                .clickable {
                                    try {
                                        val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:+91$cleanPhone"))
                                        context.startActivity(dialIntent)
                                    } catch (_: Exception) {
                                        Toast.makeText(context, "Call: +91 $cleanPhone", Toast.LENGTH_SHORT).show()
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.Call, contentDescription = "Call", tint = EmeraldSignal, modifier = Modifier.size(17.dp))
                        }
                    }
                }
            }
        }

        // 3. Items Ordered
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = SurfaceWhite,
                border = BorderStroke(1.dp, Line)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Items in this order (${order.items.size})",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = Ink
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    order.items.forEachIndexed { index, item ->
                        if (index > 0) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(Line.copy(alpha = 0.5f))
                            )
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        color = LuxeGold.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = item.displayItemCode,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 10.sp,
                                                color = Color(0xFFB45309)
                                            ),
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                        )
                                    }
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = item.name,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = Ink,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                val subDetails = listOfNotNull(
                                    item.brand.takeIf { it.isNotBlank() },
                                    item.size.takeIf { it.isNotBlank() }
                                ).joinToString(" • ")
                                if (subDetails.isNotBlank()) {
                                    Text(
                                        text = subDetails,
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, color = TextTertiary)
                                    )
                                }
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = "${formatRupees(item.unitPrice)} × ${item.qty}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }

                            Text(
                                text = formatRupees(item.total),
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.5.sp
                                ),
                                color = Ink
                            )
                        }
                    }
                }
            }
        }

        // 4. Delivery Destination
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = SurfaceWhite,
                border = BorderStroke(1.dp, Line)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Delivery Location",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = Ink
                        )
                        Text(
                            text = order.delivery.address.tag.ifBlank { "SITE DROP" }.uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold, fontSize = 10.sp),
                            color = LuxeGold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = order.delivery.address.fullAddress.ifBlank { "Prince Sanitary Store Area, Saharanpur" },
                        style = MaterialTheme.typography.bodyMedium,
                        color = Ink
                    )
                }
            }
        }

        // 5. Bill Summary
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = SurfaceWhite,
                border = BorderStroke(1.dp, Line)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Bill Details",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = Ink
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    CleanBillRow(label = "Items Subtotal", value = formatRupees(order.billing.subtotal))

                    if (order.billing.taxTotal > 0.0) {
                        CleanBillRow(label = "GST / Taxes (Included)", value = formatRupees(order.billing.taxTotal))
                    }

                    CleanBillRow(
                        label = "Site Delivery",
                        value = if (order.billing.deliveryFee == 0.0) "FREE" else formatRupees(order.billing.deliveryFee),
                        isEmerald = order.billing.deliveryFee == 0.0
                    )

                    if (order.billing.discountAmount > 0.0) {
                        CleanBillRow(
                            label = "Coupon Discount",
                            value = "- ${formatRupees(order.billing.discountAmount)}",
                            isEmerald = true
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Line)
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Grand Total",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = Ink
                        )
                        Text(
                            text = formatRupees(order.billing.grandTotal),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 19.sp
                            ),
                            color = LuxeGold
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Payment verified pill
                    val isPaid = order.payment.status == PaymentStatus.COMPLETED
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isPaid) Icons.Rounded.CheckCircle else Icons.Rounded.Schedule,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp),
                            tint = if (isPaid) EmeraldSignal else LuxeGold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isPaid) "Payment Completed (Verified)" else "Payment Pending on Delivery",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold, fontSize = 11.5.sp),
                            color = if (isPaid) EmeraldSignal else Ink
                        )
                    }
                }
            }
        }

        // 6. Cancel Order or Contact Store Owner (Dispatched orders cannot be cancelled)
        if (order.status != OrderStatus.CANCELLED) {
            if (order.status == OrderStatus.SHIPPED || order.status == OrderStatus.DELIVERED) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFF141418),
                        border = BorderStroke(1.dp, Line)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = if (order.status == OrderStatus.SHIPPED) "Order Dispatched" else "Order Delivered",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Ink
                                )
                                Text(
                                    text = "Cannot be cancelled after dispatch. Need help with delivery?",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                                    color = TextSecondary
                                )
                            }
                            Spacer(Modifier.width(10.dp))
                            // Small call button type only as requested
                            Button(
                                onClick = {
                                    context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$cleanPhone")))
                                },
                                shape = CircleShape,
                                contentPadding = PaddingValues(horizontal = 13.dp, vertical = 6.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = LuxeGold,
                                    contentColor = Color(0xFF09090B)
                                )
                            ) {
                                Icon(Icons.Rounded.Call, contentDescription = null, modifier = Modifier.size(13.dp))
                                Spacer(Modifier.width(5.dp))
                                Text("Call Owner", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            } else {
                item {
                    OutlinedButton(
                        onClick = onCancelOrder,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusError),
                        border = BorderStroke(1.dp, StatusError.copy(alpha = 0.4f))
                    ) {
                        Icon(Icons.Rounded.Cancel, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Cancel Order", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun StatusBadge(status: OrderStatus) {
    val (color, text) = when (status) {
        OrderStatus.DELIVERED -> EmeraldSignal to "Delivered"
        OrderStatus.SHIPPED -> EmeraldSignal to "Dispatched"
        OrderStatus.PACKED -> LuxeGold to "Packed"
        OrderStatus.CONFIRMED -> LuxeGold to "Confirmed"
        OrderStatus.PLACED -> Ink to "Placed"
        OrderStatus.CANCELLED -> StatusError to "Cancelled"
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
            color = color,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun SimpleStatusTracker(currentStatus: OrderStatus) {
    val steps = listOf(
        OrderStatus.PLACED to "Placed",
        OrderStatus.CONFIRMED to "Confirmed",
        OrderStatus.PACKED to "Packed",
        OrderStatus.SHIPPED to "Shipped",
        OrderStatus.DELIVERED to "Delivered"
    )

    val currentIndex = steps.indexOfFirst { it.first == currentStatus }
    val isCancelled = currentStatus == OrderStatus.CANCELLED

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        steps.forEachIndexed { index, (status, label) ->
            val isPassed = !isCancelled && index < currentIndex
            val isCurrent = !isCancelled && index == currentIndex

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                isPassed -> EmeraldSignal
                                isCurrent && status == OrderStatus.DELIVERED -> EmeraldSignal
                                isCurrent -> LuxeGold
                                else -> PaperDim
                            }
                        )
                        .border(
                            1.dp,
                            when {
                                isPassed -> EmeraldSignal
                                isCurrent && status == OrderStatus.DELIVERED -> EmeraldSignal
                                isCurrent -> LuxeGold
                                else -> Line
                            },
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isPassed || (isCurrent && status == OrderStatus.DELIVERED)) {
                        Icon(
                            Icons.Rounded.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(13.dp)
                        )
                    } else if (isCurrent) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF09090B))
                        )
                    }
                }

                Spacer(Modifier.height(5.dp))

                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.sp,
                        fontWeight = if (isCurrent) FontWeight.ExtraBold else FontWeight.Medium
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Visible,
                    color = when {
                        isCurrent && status == OrderStatus.DELIVERED -> EmeraldSignal
                        isCurrent -> LuxeGold
                        isPassed -> Ink
                        else -> TextTertiary
                    }
                )
            }

            if (index < steps.size - 1) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(2.dp)
                        .padding(horizontal = 2.dp)
                        .background(
                            if (!isCancelled && index < currentIndex) EmeraldSignal else Line
                        )
                )
            }
        }
    }
}

@Composable
private fun CleanBillRow(
    label: String,
    value: String,
    isEmerald: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.5.sp),
            color = if (isEmerald) EmeraldSignal else TextSecondary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 13.5.sp,
                fontWeight = if (isEmerald) FontWeight.Bold else FontWeight.Medium
            ),
            color = if (isEmerald) EmeraldSignal else Ink
        )
    }
}

private fun formatDateTime(date: Date): String {
    val formatter = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
    return formatter.format(date)
}
