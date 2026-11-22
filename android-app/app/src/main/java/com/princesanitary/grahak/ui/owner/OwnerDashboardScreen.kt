package com.princesanitary.grahak.ui.owner

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.princesanitary.grahak.data.model.*
import com.princesanitary.grahak.di.appViewModel
import com.princesanitary.grahak.ui.components.*
import com.princesanitary.grahak.ui.theme.*

private enum class OwnerTab(val label: String, val icon: ImageVector) {
    PRODUCTS("Products", Icons.Rounded.Inventory2),
    ORDERS("Orders", Icons.Rounded.LocalShipping),
    PLUMBERS("Plumbers", Icons.Rounded.Engineering),
    PAYOUTS("Payouts", Icons.Rounded.AccountBalanceWallet)
}

@Composable
fun OwnerDashboardScreen(
    onBack: () -> Unit,
    viewModel: OwnerViewModel = appViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isAuthorized by viewModel.isAuthorized.collectAsStateWithLifecycle()

    var activeTab by remember { mutableStateOf(OwnerTab.PRODUCTS) }
    var showAddProductDialog by remember { mutableStateOf(false) }
    var editingProduct by remember { mutableStateOf<Product?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Paper)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BackButton(onClick = onBack)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Store Management",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = Ink
                            )
                            Spacer(Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(BentoYellow.copy(alpha = 0.2f))
                                    .border(1.dp, BentoYellow.copy(alpha = 0.5f), CircleShape)
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "OWNER",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 9.sp
                                    ),
                                    color = BentoYellow
                                )
                            }
                        }
                        Text(
                            text = "Prince Sanitary Store Admin Panel",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }

                if (activeTab == OwnerTab.PRODUCTS) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(BentoGreen)
                            .clickable {
                                editingProduct = null
                                showAddProductDialog = true
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Rounded.Add,
                            contentDescription = "Add Product",
                            tint = BentoOnVibrant,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            // Tab Switcher Pills
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OwnerTab.values().forEach { tab ->
                    val selected = activeTab == tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .clip(CircleShape)
                            .background(if (selected) BentoDark else SurfaceWhite)
                            .border(1.dp, if (selected) BentoDark else Line, CircleShape)
                            .clickable { activeTab = tab },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                tab.icon,
                                contentDescription = null,
                                tint = if (selected) BentoOnDark else TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = tab.label,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                ),
                                color = if (selected) BentoOnDark else TextSecondary
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // Body based on state
            when {
                isAuthorized == false -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                            Icon(Icons.Rounded.Lock, null, Modifier.size(56.dp), tint = StatusError)
                            Spacer(Modifier.height(14.dp))
                            Text("Access Restricted", style = MaterialTheme.typography.titleLarge, color = Ink)
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "This panel is restricted exclusively to authorized store owners.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }

                uiState is OwnerUiState.Loading -> {
                    LoadingState()
                }

                uiState is OwnerUiState.Error -> {
                    Column(Modifier.padding(20.dp)) {
                        ErrorBanner((uiState as OwnerUiState.Error).message)
                    }
                }

                uiState is OwnerUiState.Success -> {
                    val data = uiState as OwnerUiState.Success
                    when (activeTab) {
                        OwnerTab.PRODUCTS -> OwnerProductsTab(
                            products = data.products,
                            onEdit = {
                                editingProduct = it
                                showAddProductDialog = true
                            },
                            onTogglePublish = { id, pub -> viewModel.toggleProductPublish(id, pub) },
                            onDelete = { id -> viewModel.deleteProduct(id) }
                        )
                        OwnerTab.ORDERS -> OwnerOrdersTab(
                            orders = data.orders,
                            onUpdateStatus = { id, status -> viewModel.updateOrderStatus(id, status) },
                            onUpdatePayment = { id, status -> viewModel.updatePaymentStatus(id, status) }
                        )
                        OwnerTab.PLUMBERS -> OwnerPlumbersTab(
                            plumbers = data.plumbers,
                            onVerify = { id, approved -> viewModel.verifyPlumber(id, approved) }
                        )
                        OwnerTab.PAYOUTS -> OwnerPayoutsTab(
                            payouts = data.pendingPayouts,
                            onApprove = { id, ref -> viewModel.approvePayout(id, ref) }
                        )
                    }
                }
            }
        }
    }

    // Add / Edit Product Dialog
    if (showAddProductDialog) {
        ProductEditDialog(
            existing = editingProduct,
            onDismiss = { showAddProductDialog = false },
            onSave = { product ->
                viewModel.saveProduct(product) { success, _ ->
                    if (success) showAddProductDialog = false
                }
            }
        )
    }
}

// =========================================================================
// PRODUCTS TAB
// =========================================================================

@Composable
private fun OwnerProductsTab(
    products: List<Product>,
    onEdit: (Product) -> Unit,
    onTogglePublish: (String, Boolean) -> Unit,
    onDelete: (String) -> Unit
) {
    if (products.isEmpty()) {
        EmptyState(
            icon = Icons.Rounded.Inventory2,
            title = "No products found",
            message = "Tap the + button above to add your first sanitaryware or hardware item."
        )
    } else {
        LazyColumn(
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 30.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Text(
                    text = "${products.size} Items in Catalog",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = TextSecondary
                )
            }
            items(products, key = { it.id }) { product ->
                GrahakCard(cornerRadius = 20.dp, contentPadding = 16.dp) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = product.name,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Ink
                                )
                                Spacer(Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(if (product.isPublished) BentoGreen.copy(alpha = 0.15f) else StatusError.copy(alpha = 0.15f))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = if (product.isPublished) "ACTIVE" else "HIDDEN",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 9.sp
                                        ),
                                        color = if (product.isPublished) BentoGreen else StatusError
                                    )
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "${product.brand} • ${ProductCategory.fromId(product.categoryId)?.displayName ?: product.categoryId}",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                            Spacer(Modifier.height(10.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = formatRupees(product.unitPrice),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = BentoGreen
                                )
                                Spacer(Modifier.width(16.dp))
                                Text(
                                    text = "Stock: ${product.stockLevel}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (product.stockLevel > 0) TextSecondary else StatusError
                                )
                            }
                        }

                        // Actions
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { onEdit(product) }) {
                                Icon(Icons.Rounded.Edit, contentDescription = "Edit", tint = Ink)
                            }
                            IconButton(onClick = { onTogglePublish(product.id, !product.isPublished) }) {
                                Icon(
                                    if (product.isPublished) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                    contentDescription = "Toggle Visibility",
                                    tint = TextSecondary
                                )
                            }
                            IconButton(onClick = { onDelete(product.id) }) {
                                Icon(Icons.Rounded.Delete, contentDescription = "Delete", tint = StatusError)
                            }
                        }
                    }
                }
            }
        }
    }
}

// =========================================================================
// ORDERS TAB
// =========================================================================

@Composable
private fun OwnerOrdersTab(
    orders: List<Order>,
    onUpdateStatus: (String, OrderStatus) -> Unit,
    onUpdatePayment: (String, PaymentStatus) -> Unit
) {
    if (orders.isEmpty()) {
        EmptyState(
            icon = Icons.Rounded.LocalShipping,
            title = "No orders yet",
            message = "Orders placed by consumers or plumbers will appear here for fulfillment."
        )
    } else {
        LazyColumn(
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 30.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(orders, key = { it.id }) { order ->
                GrahakCard(cornerRadius = 20.dp, contentPadding = 16.dp) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Order #${order.id.take(8).uppercase()}",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Ink
                                )
                                Text(
                                    text = "Type: ${order.delivery.type.name}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                            Text(
                                text = formatRupees(order.billing.grandTotal),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = BentoGreen
                            )
                        }

                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = order.items.joinToString { "${it.name} (x${it.qty})" },
                            style = MaterialTheme.typography.bodySmall,
                            color = Ink,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(Modifier.height(14.dp))

                        // Status Progression Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            when (order.status) {
                                OrderStatus.PLACED -> {
                                    Button(
                                        onClick = { onUpdateStatus(order.id, OrderStatus.CONFIRMED) },
                                        modifier = Modifier.weight(1f),
                                        shape = CircleShape,
                                        colors = ButtonDefaults.buttonColors(containerColor = BentoBlue)
                                    ) {
                                        Text("Accept & Confirm", color = BentoOnVibrant)
                                    }
                                }
                                OrderStatus.CONFIRMED -> {
                                    Button(
                                        onClick = { onUpdateStatus(order.id, OrderStatus.PACKED) },
                                        modifier = Modifier.weight(1f),
                                        shape = CircleShape,
                                        colors = ButtonDefaults.buttonColors(containerColor = BentoYellow)
                                    ) {
                                        Text("Mark Packed", color = BentoOnVibrant)
                                    }
                                }
                                OrderStatus.PACKED -> {
                                    Button(
                                        onClick = { onUpdateStatus(order.id, OrderStatus.SHIPPED) },
                                        modifier = Modifier.weight(1f),
                                        shape = CircleShape,
                                        colors = ButtonDefaults.buttonColors(containerColor = BentoPink)
                                    ) {
                                        Text("Dispatch to Site", color = BentoOnVibrant)
                                    }
                                }
                                OrderStatus.SHIPPED -> {
                                    Button(
                                        onClick = {
                                            onUpdateStatus(order.id, OrderStatus.DELIVERED)
                                            onUpdatePayment(order.id, PaymentStatus.COMPLETED)
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = CircleShape,
                                        colors = ButtonDefaults.buttonColors(containerColor = BentoGreen)
                                    ) {
                                        Text("Mark Delivered & Paid", color = BentoOnVibrant)
                                    }
                                }
                                OrderStatus.DELIVERED -> {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(CircleShape)
                                            .background(BentoGreen.copy(alpha = 0.15f))
                                            .padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("Delivered & Completed", color = BentoGreen, fontWeight = FontWeight.Bold)
                                    }
                                }
                                OrderStatus.CANCELLED -> {
                                    Text("Cancelled", color = StatusError, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// =========================================================================
// PLUMBERS TAB (KYC Approvals)
// =========================================================================

@Composable
private fun OwnerPlumbersTab(
    plumbers: List<Plumber>,
    onVerify: (String, Boolean) -> Unit
) {
    if (plumbers.isEmpty()) {
        EmptyState(
            icon = Icons.Rounded.Engineering,
            title = "No registered plumbers",
            message = "Plumbers registering via the app will show up here for KYC verification."
        )
    } else {
        LazyColumn(
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 30.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(plumbers, key = { it.id }) { plumber ->
                GrahakCard(cornerRadius = 20.dp, contentPadding = 16.dp) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = plumber.name,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Ink
                                )
                                Spacer(Modifier.width(8.dp))
                                if (plumber.kyc.verified) {
                                    Icon(Icons.Rounded.Verified, null, Modifier.size(16.dp), tint = BentoGreen)
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(text = "Phone: ${plumber.phone}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            Text(text = "Experience: ${plumber.experienceYears} yrs • Base: ${plumber.baseAddress}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = "Aadhaar: ${plumber.kyc.aadhaarNo.ifBlank { "Not provided" }}",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                color = Ink
                            )
                        }

                        // Approval switch
                        Button(
                            onClick = { onVerify(plumber.id, !plumber.kyc.verified) },
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (plumber.kyc.verified) StatusError.copy(alpha = 0.15f) else BentoGreen
                            )
                        ) {
                            Text(
                                text = if (plumber.kyc.verified) "Revoke" else "Approve KYC",
                                color = if (plumber.kyc.verified) StatusError else BentoOnVibrant,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

// =========================================================================
// PAYOUTS TAB
// =========================================================================

@Composable
private fun OwnerPayoutsTab(
    payouts: List<WithdrawalRequest>,
    onApprove: (String, String) -> Unit
) {
    if (payouts.isEmpty()) {
        EmptyState(
            icon = Icons.Rounded.CheckCircle,
            title = "No pending payouts",
            message = "All plumber commission and job balance withdrawal requests are settled."
        )
    } else {
        LazyColumn(
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 30.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(payouts, key = { it.id }) { req ->
                GrahakCard(cornerRadius = 20.dp, contentPadding = 16.dp) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Payout Request",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Ink
                                )
                                Text(
                                    text = "UPI: ${req.upiId}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                            Text(
                                text = formatRupees(req.amount),
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = BentoGreen
                            )
                        }

                        Spacer(Modifier.height(14.dp))

                        PrimaryButton(
                            text = "Mark Paid via UPI",
                            onClick = { onApprove(req.id, "UPI-${System.currentTimeMillis()}") }
                        )
                    }
                }
            }
        }
    }
}

// =========================================================================
// PRODUCT EDIT DIALOG
// =========================================================================

@Composable
private fun ProductEditDialog(
    existing: Product?,
    onDismiss: () -> Unit,
    onSave: (Product) -> Unit
) {
    var name by remember { mutableStateOf(existing?.name.orEmpty()) }
    var brand by remember { mutableStateOf(existing?.brand.orEmpty()) }
    var categoryId by remember { mutableStateOf(existing?.categoryId?.ifEmpty { "pipes" } ?: "pipes") }
    var priceText by remember { mutableStateOf(if (existing != null) existing.unitPrice.toString() else "0.0") }
    var stockText by remember { mutableStateOf(if (existing != null) existing.stockLevel.toString() else "50") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Add Store Product" else "Edit Product") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Product Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = brand,
                    onValueChange = { brand = it },
                    label = { Text("Brand (e.g. Supreme, Astral)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = priceText,
                    onValueChange = { priceText = it },
                    label = { Text("Price (₹)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = stockText,
                    onValueChange = { stockText = it },
                    label = { Text("Stock Level") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val product = (existing ?: Product()).copy(
                        name = name,
                        brand = brand,
                        categoryId = categoryId,
                        unitPrice = priceText.toDoubleOrNull() ?: 0.0,
                        stockLevel = stockText.toIntOrNull() ?: 0,
                        isPublished = true
                    )
                    onSave(product)
                },
                colors = ButtonDefaults.buttonColors(containerColor = BentoGreen)
            ) {
                Text("Save", color = BentoOnVibrant)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}
