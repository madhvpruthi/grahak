package com.princesanitary.grahak.ui.consumer.cart

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.navigationBars
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.LocalOffer
import androidx.compose.material.icons.rounded.LocalShipping
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Payment
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.princesanitary.grahak.data.model.CartItem
import com.princesanitary.grahak.data.model.CartSummary
import com.princesanitary.grahak.data.model.Coupon
import com.princesanitary.grahak.data.model.StoreSettings
import com.princesanitary.grahak.di.appViewModel
import com.princesanitary.grahak.ui.components.BackButton
import com.princesanitary.grahak.ui.components.CircleIconButton
import com.princesanitary.grahak.ui.components.EmptyState
import com.princesanitary.grahak.ui.components.ErrorBanner
import com.princesanitary.grahak.ui.components.LoadingState
import com.princesanitary.grahak.ui.components.formatRupees
import com.princesanitary.grahak.ui.theme.EmeraldSignal
import com.princesanitary.grahak.ui.theme.EmeraldSignalBorder
import com.princesanitary.grahak.ui.theme.EmeraldSignalSurface
import com.princesanitary.grahak.ui.theme.Ink
import com.princesanitary.grahak.ui.theme.Line
import com.princesanitary.grahak.ui.theme.LuxeGold
import com.princesanitary.grahak.ui.theme.LuxeGoldBorder
import com.princesanitary.grahak.ui.theme.LuxeGoldLight
import com.princesanitary.grahak.ui.theme.LuxeGoldSurface
import com.princesanitary.grahak.ui.theme.Paper
import com.princesanitary.grahak.ui.theme.PaperDim
import com.princesanitary.grahak.ui.theme.StatusError
import com.princesanitary.grahak.ui.theme.SurfaceWhite
import com.princesanitary.grahak.ui.theme.TextSecondary
import com.princesanitary.grahak.ui.theme.TextTertiary
import com.princesanitary.grahak.ui.theme.isDarkThemeActive

/**
 * Ultra-Aesthetic Cart Screen (Inspired by Pinterest luxury cart UI reference)
 */
@Composable
fun CartScreen(
    viewModel: CartViewModel = appViewModel(),
    onCheckout: () -> Unit,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showClearDialog by remember { mutableStateOf(false) }

    val success = uiState as? CartUiState.Success
    val items = success?.cart?.items ?: emptyList()
    val settings = success?.storeSettings ?: StoreSettings()
    val coupons = success?.availableCoupons ?: emptyList()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Paper)
    ) {
        Column(Modifier.fillMaxSize()) {
            // ---- Minimalist Top Header ----
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BackButton(onClick = onBack)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "Cart",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = 24.sp
                        ),
                        color = Ink
                    )
                    if (items.isNotEmpty()) {
                        Text(
                            text = "${items.sumOf { it.quantity }} items in your bag",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                            color = TextSecondary
                        )
                    }
                }
                if (items.isNotEmpty()) {
                    CircleIconButton(
                        icon = Icons.Rounded.DeleteOutline,
                        onClick = { showClearDialog = true },
                        contentDescription = "Clear cart",
                        tint = StatusError
                    )
                }
            }

            when {
                uiState is CartUiState.Loading -> LoadingState()

                uiState is CartUiState.Error -> Column(Modifier.padding(20.dp)) {
                    ErrorBanner((uiState as CartUiState.Error).message)
                }

                items.isEmpty() -> EmptyState(
                    icon = Icons.Rounded.ShoppingBag,
                    title = "Your cart is empty",
                    message = "Browse the catalog to add sanitaryware, pipes, fittings or taps."
                )

                else -> LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(
                        start = 18.dp,
                        end = 18.dp,
                        top = 4.dp,
                        bottom = 150.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // 1. Delivery Location Address Pill (Inspired by Image 3: Nikolskaya 24)
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = if (isDarkThemeActive) Color(0xFF161622) else Color(0xFFF1F5F9),
                            border = BorderStroke(1.dp, Line)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
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
                                        Icons.Rounded.Place,
                                        contentDescription = null,
                                        tint = LuxeGold,
                                        modifier = Modifier.size(17.dp)
                                    )
                                }
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        text = "Delivering to Saharanpur",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = Ink
                                    )
                                    Text(
                                        text = "Prince Sanitary Store • Express Dispatch",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = TextSecondary
                                    )
                                }
                            }
                        }
                    }

                    // 2. Free Delivery Tracker Banner
                    item {
                        FreeDeliveryBanner(
                            subtotal = success?.summary?.subtotal ?: 0.0,
                            threshold = settings.freeDeliveryThreshold
                        )
                    }

                    // 3. Cart Items List (Matching Image 3 Pinterest reference!)
                    items(items, key = { it.productId }) { item ->
                        AestheticCartRow(
                            item = item,
                            onQuantityChange = { qty ->
                                viewModel.updateQuantity(item.productId, qty)
                            },
                            onRemove = { viewModel.removeItem(item.productId) }
                        )
                    }

                    // 4. Coupons & Offers Showcase
                    if (coupons.isNotEmpty()) {
                        item {
                            CartCouponsCard(
                                subtotal = success?.summary?.subtotal ?: 0.0,
                                coupons = coupons
                            )
                        }
                    }

                    // 5. Bill Summary Card
                    item {
                        BillSummaryCard(
                            summary = success?.summary ?: CartSummary(),
                            settings = settings
                        )
                    }
                }
            }
        }

        // ---- Pinned Totals & Checkout Panel (Matching Image 3 Checkout Bar) ----
        if (items.isNotEmpty() && success != null) {
            AestheticCheckoutPanel(
                summary = success.summary,
                onCheckout = onCheckout,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Empty your cart?", style = MaterialTheme.typography.titleLarge) },
            text = {
                Text(
                    "This removes every item from your cart. You will need to add them again.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showClearDialog = false
                    viewModel.clearCart()
                }) {
                    Text("Empty cart", color = StatusError)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Keep items", color = TextSecondary)
                }
            },
            containerColor = SurfaceWhite
        )
    }
}

/**
 * Free Delivery progress tracker showing how much more to add for free delivery.
 */
@Composable
private fun FreeDeliveryBanner(
    subtotal: Double,
    threshold: Double
) {
    val effectiveThreshold = if (threshold > 0) threshold else 500.0
    val isFree = subtotal >= effectiveThreshold
    val diff = (effectiveThreshold - subtotal).coerceAtLeast(0.0)
    val progress = (subtotal / effectiveThreshold).toFloat().coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(targetValue = progress, label = "delivery_progress")

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isFree) EmeraldSignalSurface else LuxeGoldSurface
        ),
        border = BorderStroke(
            1.dp,
            if (isFree) EmeraldSignalBorder else LuxeGoldBorder
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(if (isFree) EmeraldSignal else LuxeGold),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isFree) Icons.Rounded.CheckCircle else Icons.Rounded.LocalShipping,
                        contentDescription = null,
                        tint = Color(0xFF09090B),
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = if (isFree) "FREE Delivery Unlocked!" else "Add ₹${diff.toInt()} more for FREE Delivery",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (isFree) EmeraldSignal else LuxeGold
                    )
                    Text(
                        text = if (isFree)
                            "Orders above ₹${effectiveThreshold.toInt()} get free site delivery in Saharanpur"
                        else
                            "Free delivery on orders above ₹${effectiveThreshold.toInt()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            if (!isFree) {
                Spacer(Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape),
                    color = LuxeGold,
                    trackColor = Line,
                    strokeCap = StrokeCap.Round
                )
            }
        }
    }
}

/**
 * Ultra-Aesthetic Product Row in Cart (Inspired by Image 3 Pinterest reference):
 * - Rounded 20dp Card with Line border
 * - Square product thumbnail on the left (80x80 dp) in soft container
 * - Center: Product name, size/price line, tactile compact stepper [- 1 +]
 * - Right: Item total price, pale-red delete icon button
 */
@Composable
private fun AestheticCartRow(
    item: CartItem,
    onQuantityChange: (Int) -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        border = BorderStroke(1.dp, Line),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Product Thumbnail in soft square box
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isDarkThemeActive) Color(0xFF1B1B24) else Color(0xFFF6F7F9)),
                contentAlignment = Alignment.Center
            ) {
                if (item.imageUrl.isNotBlank()) {
                    AsyncImage(
                        model = item.imageUrl,
                        contentDescription = item.productName,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(6.dp),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Text(
                        text = item.productName.take(2).uppercase(),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextTertiary
                    )
                }
            }

            Spacer(Modifier.width(14.dp))

            // Center: Details + Compact Stepper
            Column(Modifier.weight(1f)) {
                Text(
                    text = item.productName,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp,
                        lineHeight = 17.5.sp
                    ),
                    color = Ink,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = "${formatRupees(item.unitPrice)} / pc",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                    color = TextSecondary
                )
                Spacer(Modifier.height(8.dp))

                // Compact Stepper (Image 3 Style)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .height(32.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isDarkThemeActive) Color(0xFF262632) else Color(0xFFF1F3F6))
                        .padding(horizontal = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .clickable {
                                if (item.quantity <= 1) onRemove() else onQuantityChange(item.quantity - 1)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Rounded.Remove,
                            contentDescription = "Decrease",
                            tint = Ink,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = item.quantity.toString(),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.5.sp
                        ),
                        color = Ink
                    )
                    Spacer(Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { onQuantityChange(item.quantity + 1) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Rounded.Add,
                            contentDescription = "Increase",
                            tint = Ink,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.width(10.dp))

            // Right Column: Price & Soft Delete Button (Matching Image 3 pink delete icon)
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.height(76.dp)
            ) {
                Text(
                    text = formatRupees(item.total),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp
                    ),
                    color = Ink
                )

                // Soft Pink/Red Trash Pill Button (Image 3 Inspiration)
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEF4444).copy(alpha = 0.12f))
                        .clickable { onRemove() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.DeleteOutline,
                        contentDescription = "Remove item",
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(17.dp)
                    )
                }
            }
        }
    }
}

/**
 * Bill Summary Card with Clear Breakdown
 */
@Composable
private fun BillSummaryCard(
    summary: CartSummary,
    settings: StoreSettings
) {
    val isFreeDelivery = summary.subtotal >= settings.freeDeliveryThreshold || settings.deliveryFee == 0.0
    val deliveryFee = if (isFreeDelivery) 0.0 else settings.deliveryFee

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        border = BorderStroke(1.dp, Line)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Bill Details",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = Ink
            )
            Spacer(Modifier.height(12.dp))

            // Items Total
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Items Total", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                Text(formatRupees(summary.subtotal), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = Ink)
            }

            Spacer(Modifier.height(8.dp))

            // Delivery Fee
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Site Delivery Fee", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                if (isFreeDelivery) {
                    Text("FREE", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.ExtraBold), color = EmeraldSignal)
                } else {
                    Text(formatRupees(deliveryFee), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = Ink)
                }
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = Line)
            Spacer(Modifier.height(12.dp))

            // Grand Total
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Grand Total", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold), color = Ink)
                    Text("Inclusive of all taxes", style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.5.sp), color = TextSecondary)
                }
                Text(formatRupees(summary.grandTotal), style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black, fontSize = 20.sp), color = Ink)
            }
        }
    }
}

/**
 * Coupons and promo offers showcase in Cart.
 */
@Composable
private fun CartCouponsCard(
    subtotal: Double,
    coupons: List<Coupon>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        border = BorderStroke(1.dp, Line)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Rounded.LocalOffer,
                    contentDescription = null,
                    tint = LuxeGold,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Store Coupons Available",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = Ink
                )
            }
            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                coupons.forEach { coupon ->
                    val isEligible = subtotal >= coupon.minOrderAmount
                    val diff = (coupon.minOrderAmount - subtotal).toInt()

                    Box(
                        modifier = Modifier
                            .width(220.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isEligible) LuxeGoldSurface else PaperDim)
                            .border(
                                1.dp,
                                if (isEligible) LuxeGoldBorder else Line,
                                RoundedCornerShape(14.dp)
                            )
                            .padding(12.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = coupon.code,
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp
                                    ),
                                    color = if (isEligible) LuxeGold else Ink
                                )
                                if (isEligible) {
                                    Box(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(LuxeGold)
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "APPLY AT CHECKOUT",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 8.sp
                                            ),
                                            color = Color(0xFF09090B)
                                        )
                                    }
                                } else {
                                    Icon(
                                        Icons.Rounded.Lock,
                                        contentDescription = null,
                                        tint = TextSecondary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = coupon.description,
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (!isEligible) {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "Add ₹$diff more to avail this!",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = LuxeGoldLight
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Pinned checkout dock at the bottom of the Cart (Matching Image 3 Checkout Bar):
 * - Payment preview row: "Payment: Cash on Delivery / UPI"
 * - High-contrast full width button: "Proceed to Checkout  ₹... →"
 */
@Composable
private fun AestheticCheckoutPanel(
    summary: CartSummary,
    onCheckout: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
        color = SurfaceWhite,
        shadowElevation = 20.dp,
        border = BorderStroke(1.dp, Line)
    ) {
        Column(
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 18.dp, vertical = 14.dp)
        ) {
            // Payment method selector preview (Image 3 style: Payment by Visa ...7844 >)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(PaperDim)
                    .clickable { onCheckout() }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.Payment,
                        contentDescription = null,
                        tint = LuxeGold,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Payment: Pay on Delivery (Cash / UPI)",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        ),
                        color = Ink
                    )
                }
                Icon(
                    Icons.AutoMirrored.Rounded.ArrowForward,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(14.dp)
                )
            }

            Spacer(Modifier.height(12.dp))

            // Large tactile Checkout Pill
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = LuxeGold,
                shadowElevation = 4.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .clickable { onCheckout() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Proceed to Checkout",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp
                        ),
                        color = Color(0xFF09090B)
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = formatRupees(summary.grandTotal),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp
                            ),
                            color = Color(0xFF09090B)
                        )
                        Spacer(Modifier.width(6.dp))
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowForward,
                            contentDescription = null,
                            tint = Color(0xFF09090B),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
