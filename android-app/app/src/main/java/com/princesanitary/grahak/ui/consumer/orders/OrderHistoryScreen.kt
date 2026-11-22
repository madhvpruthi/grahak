package com.princesanitary.grahak.ui.consumer.orders

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.princesanitary.grahak.data.model.Order
import com.princesanitary.grahak.data.model.OrderStatus
import com.princesanitary.grahak.di.appViewModel
import com.princesanitary.grahak.ui.components.BackButton
import com.princesanitary.grahak.ui.components.formatRupees
import com.princesanitary.grahak.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderHistoryScreen(
    viewModel: OrderHistoryViewModel = appViewModel(),
    onOrderClick: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Paper)
    ) {
        Column(Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "My Orders",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = 24.sp
                        ),
                        color = Ink
                    )
                    Text(
                        text = "Prince Sanitary Store • Saharanpur",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                        color = TextSecondary
                    )
                }
            }

            when (val state = uiState) {
                is OrderHistoryUiState.Loading -> {
                    com.princesanitary.grahak.ui.components.ListCardSkeleton(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        count = 4
                    )
                }
                is OrderHistoryUiState.Success -> {
                    if (state.orders.isEmpty()) {
                        EmptyOrdersView(modifier = Modifier.fillMaxSize())
                    } else {
                        OrderList(
                            orders = state.orders,
                            onOrderClick = onOrderClick,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                is OrderHistoryUiState.Error -> {
                    ErrorView(
                        message = state.message,
                        onRetry = { viewModel.refresh() },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

@Composable
fun OrderList(
    orders: List<Order>,
    onOrderClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        items(orders, key = { it.id }) { order ->
            OrderCard(
                order = order,
                onClick = { onOrderClick(order.id) }
            )
        }
    }
}

@Composable
fun OrderCard(
    order: Order,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = SurfaceWhite,
        border = BorderStroke(1.dp, Line),
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Order ID & Status Chip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "ORDER #${order.id.take(8).uppercase()}",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = 13.5.sp,
                            letterSpacing = 0.5.sp
                        ),
                        color = Ink
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = formatDate(order.createdAt),
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = TextSecondary
                    )
                }
                OrderStatusChip(status = order.status)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Items Preview Container
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = PaperDim
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(LuxeGoldSurface),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ShoppingBag,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = LuxeGold
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "${order.items.size} item${if (order.items.size != 1) "s" else ""}",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.5.sp
                                ),
                                color = Ink
                            )
                            val itemsSummary = order.items.take(2).joinToString(", ") { it.name }
                            if (itemsSummary.isNotBlank()) {
                                Text(
                                    text = itemsSummary,
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = TextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    Spacer(Modifier.width(8.dp))

                    Text(
                        text = formatRupees(order.billing.grandTotal),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp
                        ),
                        color = Ink
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Footer Row: Delivery info & Track CTA
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (order.delivery.type == com.princesanitary.grahak.data.model.DeliveryType.SITE_DROP)
                            Icons.Rounded.Place
                        else
                            Icons.Rounded.Storefront,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                        tint = TextSecondary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (order.delivery.type == com.princesanitary.grahak.data.model.DeliveryType.SITE_DROP)
                            "Direct Site Drop • Saharanpur"
                        else
                            "Store Pickup",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                        color = TextSecondary
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "Track Order",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.5.sp
                        ),
                        color = LuxeGold
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Icon(
                        Icons.AutoMirrored.Rounded.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(13.dp),
                        tint = LuxeGold
                    )
                }
            }
        }
    }
}

@Composable
fun OrderStatusChip(status: OrderStatus) {
    val (color, text) = when (status) {
        OrderStatus.PLACED -> Pair(LuxeGold, "Placed")
        OrderStatus.CONFIRMED -> Pair(LuxeGold, "Confirmed")
        OrderStatus.PACKED -> Pair(LuxeGold, "Packed")
        OrderStatus.SHIPPED -> Pair(LuxeGold, "Dispatched")
        OrderStatus.DELIVERED -> Pair(EmeraldSignal, "Delivered")
        OrderStatus.CANCELLED -> Pair(MaterialTheme.colorScheme.error, "Cancelled")
    }

    Surface(
        shape = CircleShape,
        color = color.copy(alpha = 0.12f),
        border = BorderStroke(0.7.dp, color.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = text.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 9.sp,
                    letterSpacing = 0.5.sp
                ),
                color = color
            )
        }
    }
}

@Composable
fun EmptyOrdersView(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(PaperDim),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.ShoppingBag,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = TextSecondary
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No Orders Yet",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                ),
                color = Ink
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Browse the catalog to place wholesale sanitary & plumbing orders.",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = TextSecondary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
fun ErrorView(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.ErrorOutline,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Error Loading Orders",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Ink
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = LuxeGold,
                    contentColor = Color(0xFF09090B)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Retry", color = Color(0xFF09090B), fontWeight = FontWeight.Bold)
            }
        }
    }
}

private fun formatDate(date: Date): String {
    val formatter = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
    return formatter.format(date)
}

/**
 * The order list without its own app bar.
 */
@Composable
fun OrderHistoryList(
    viewModel: OrderHistoryViewModel,
    onOrderClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    when (val state = uiState) {
        is OrderHistoryUiState.Loading ->
            com.princesanitary.grahak.ui.components.ListCardSkeleton(
                modifier = modifier.padding(16.dp),
                count = 3
            )

        is OrderHistoryUiState.Success ->
            if (state.orders.isEmpty()) {
                EmptyOrdersView(modifier = modifier)
            } else {
                OrderList(
                    orders = state.orders,
                    onOrderClick = onOrderClick,
                    modifier = modifier
                )
            }

        is OrderHistoryUiState.Error ->
            ErrorView(
                message = state.message,
                onRetry = { viewModel.refresh() },
                modifier = modifier
            )
    }
}
