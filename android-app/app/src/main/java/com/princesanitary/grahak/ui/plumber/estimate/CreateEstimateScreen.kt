package com.princesanitary.grahak.ui.plumber.estimate

import com.princesanitary.grahak.ui.theme.Paper
import com.princesanitary.grahak.ui.theme.Ink

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
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.princesanitary.grahak.data.model.EstimateItem
import com.princesanitary.grahak.data.model.Product
import com.princesanitary.grahak.di.ServiceLocator
import com.princesanitary.grahak.di.appViewModel
import com.princesanitary.grahak.ui.components.formatRupees
import com.princesanitary.grahak.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEstimateScreen(
    jobId: String,
    customerId: String,
    viewModel: CreateEstimateViewModel = appViewModel(),
    onEstimateCreated: (String) -> Unit,
    onBack: () -> Unit
) {
    val estimateItems by viewModel.estimateItems.collectAsStateWithLifecycle()
    val labourCharge by viewModel.labourCharge.collectAsStateWithLifecycle()
    val notes by viewModel.notes.collectAsStateWithLifecycle()
    val totals by viewModel.totals.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    var showProductSearch by remember { mutableStateOf(false) }

    LaunchedEffect(uiState) {
        if (uiState is EstimateUiState.Success) {
            onEstimateCreated((uiState as EstimateUiState.Success).estimateId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Material Estimate") },
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
        },
        bottomBar = {
            EstimateBottomBar(
                totals = totals,
                isSubmitting = uiState is EstimateUiState.Loading,
                onSubmit = {
                    viewModel.submitEstimate(jobId, customerId)
                },
                onContinueJob = onBack
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Info
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Paper.copy(alpha = 0.1f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Receipt,
                            contentDescription = null,
                            tint = PlumberAccent,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Digital Parchi",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Add materials needed for this job",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Add Item Button
            item {
                com.princesanitary.grahak.ui.components.PrimaryButton(
                    text = "Add Material",
                    icon = Icons.Default.Add,
                    onClick = { showProductSearch = true }
                )
            }

            // Estimate Items
            if (estimateItems.isEmpty()) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.ShoppingCart,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "No items added yet",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            } else {
                items(estimateItems) { item ->
                    EstimateItemCard(
                        item = item,
                        onQuantityChange = { newQty ->
                            viewModel.updateItemQuantity(item.productId, newQty)
                        },
                        onRemove = {
                            viewModel.removeItem(item.productId)
                        }
                    )
                }
            }

            // Labour Charge
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Labour Charge",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = if (labourCharge > 0) labourCharge.toString() else "",
                            onValueChange = { 
                                viewModel.setLabourCharge(it.toDoubleOrNull() ?: 0.0)
                            },
                            label = { Text("Amount (₹)") },
                            modifier = Modifier.fillMaxWidth(),
                            prefix = { Text("₹") },
                            singleLine = true
                        )
                    }
                }
            }

            // Notes
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Notes (Optional)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = notes,
                            onValueChange = { viewModel.setNotes(it) },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Add any additional notes...") },
                            minLines = 3,
                            maxLines = 5
                        )
                    }
                }
            }

            // Error Display
            if (uiState is EstimateUiState.Error) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            text = (uiState as EstimateUiState.Error).message,
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            // Bottom spacing for FAB
            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }

    // Product Search Dialog
    if (showProductSearch) {
        ProductSearchDialog(
            onDismiss = { showProductSearch = false },
            onProductSelected = { product, quantity ->
                viewModel.addItem(product, quantity)
                showProductSearch = false
            }
        )
    }
}

@Composable
fun EstimateItemCard(
    item: EstimateItem,
    onQuantityChange: (Int) -> Unit,
    onRemove: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.productName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "₹${item.unitPrice} per unit",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onRemove) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Remove",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Divider()
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Quantity Controls
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(
                        onClick = { if (item.quantity > 1) onQuantityChange(item.quantity - 1) },
                        modifier = Modifier.size(40.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Decrease")
                    }
                    
                    Text(
                        text = item.quantity.toString(),
                        modifier = Modifier.padding(horizontal = 16.dp),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    
                    OutlinedButton(
                        onClick = { onQuantityChange(item.quantity + 1) },
                        modifier = Modifier.size(40.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Increase")
                    }
                }

                // Total
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "₹${item.total}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = PlumberAccent
                    )
                    if (item.gstRate > 0) {
                        Text(
                            text = "+ ${item.gstRate}% GST",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EstimateBottomBar(
    totals: EstimateTotals,
    isSubmitting: Boolean,
    onSubmit: () -> Unit,
    onContinueJob: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = 8.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Subtotal:", style = MaterialTheme.typography.bodyMedium)
                Text("₹${"%.2f".format(totals.subtotal)}", style = MaterialTheme.typography.bodyMedium)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("GST (18%):", style = MaterialTheme.typography.bodyMedium)
                Text("₹${"%.2f".format(totals.taxTotal)}", style = MaterialTheme.typography.bodyMedium)
            }
            if (totals.labourCharge > 0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Labour Charge:", style = MaterialTheme.typography.bodyMedium)
                    Text("₹${"%.2f".format(totals.labourCharge)}", style = MaterialTheme.typography.bodyMedium)
                }
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Line)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Total Amount:",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "₹${"%.2f".format(totals.grandTotal)}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = BentoGreen
                )
            }
            
            Spacer(modifier = Modifier.height(14.dp))
            
            // Primary Submit Button with prominent green styling
            Button(
                onClick = onSubmit,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled = totals.grandTotal > 0 && !isSubmitting,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BentoGreen,
                    contentColor = BentoOnVibrant,
                    disabledContainerColor = Line,
                    disabledContentColor = TextTertiary
                )
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = BentoOnVibrant
                    )
                } else {
                    Icon(Icons.Default.Send, contentDescription = null, tint = BentoOnVibrant)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (totals.grandTotal > 0) "Submit Parchi to Store & Customer" else "Add Items to Submit",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = BentoOnVibrant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Secondary option to continue back to job without blocker
            OutlinedButton(
                onClick = onContinueJob,
                modifier = Modifier.fillMaxWidth().height(46.dp),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, Line)
            ) {
                Text("Continue / Back to Job Screen", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            }
        }
    }
}

@Composable
fun ProductSearchDialog(
    onDismiss: () -> Unit,
    onProductSelected: (Product, Int) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedQuantity by remember { mutableStateOf(1) }
    var selectedProduct by remember { mutableStateOf<Product?>(null) }
    
    val productsFlow = remember { ServiceLocator.productRepository.getPublishedProducts() }
    val productsResult by productsFlow.collectAsStateWithLifecycle(initialValue = Result.success(emptyList()))
    val allProducts = productsResult.getOrNull() ?: emptyList()
    
    val filteredProducts = remember(searchQuery, allProducts) {
        if (searchQuery.isBlank()) allProducts
        else allProducts.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.brand.contains(searchQuery, ignoreCase = true) ||
            it.company.contains(searchQuery, ignoreCase = true) ||
            it.itemType.contains(searchQuery, ignoreCase = true)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (selectedProduct == null) "Select Store Material" else "Specify Quantity",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp)) {
                if (selectedProduct == null) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text("Search materials (pipes, taps, fittings...)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null)
                        }
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    if (filteredProducts.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(160.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No store products match search", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth().weight(1f, fill = false),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(filteredProducts, key = { it.id }) { product ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(PaperDim)
                                        .clickable { selectedProduct = product }
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Product photo thumbnail
                                    Box(
                                        modifier = Modifier
                                            .size(50.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(SurfaceWhite),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (product.mediaUrls.isNotEmpty()) {
                                            AsyncImage(
                                                model = product.mediaUrls.first(),
                                                contentDescription = product.name,
                                                modifier = Modifier.fillMaxSize().padding(2.dp),
                                                contentScale = ContentScale.Fit
                                            )
                                        } else {
                                            Icon(Icons.Rounded.Storefront, null, tint = TextSecondary, modifier = Modifier.size(24.dp))
                                        }
                                    }

                                    Spacer(Modifier.width(10.dp))

                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            text = product.name,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${product.brand.ifBlank { product.company }} • ${product.size} • ${product.unit}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextSecondary
                                        )
                                    }

                                    Spacer(Modifier.width(8.dp))

                                    Text(
                                        text = "${formatRupees(product.unitPrice)}",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = BentoGreen
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // Selected product confirmation + quantity
                    val prod = selectedProduct!!
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(PaperDim)
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(SurfaceWhite),
                            contentAlignment = Alignment.Center
                        ) {
                            if (prod.mediaUrls.isNotEmpty()) {
                                AsyncImage(
                                    model = prod.mediaUrls.first(),
                                    contentDescription = prod.name,
                                    modifier = Modifier.fillMaxSize().padding(2.dp),
                                    contentScale = ContentScale.Fit
                                )
                            } else {
                                Icon(Icons.Rounded.Storefront, null, tint = TextSecondary, modifier = Modifier.size(26.dp))
                            }
                        }

                        Spacer(Modifier.width(12.dp))

                        Column(Modifier.weight(1f)) {
                            Text(prod.name, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                            Text("${formatRupees(prod.unitPrice)} / ${prod.unit}", style = MaterialTheme.typography.bodySmall, color = BentoGreen)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = selectedQuantity.toString(),
                        onValueChange = { 
                            selectedQuantity = it.toIntOrNull() ?: 1
                        },
                        label = { Text("Quantity (${prod.unit})") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            if (selectedProduct != null) {
                Button(
                    onClick = {
                        onProductSelected(selectedProduct!!, selectedQuantity.coerceAtLeast(1))
                    },
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = BentoGreen)
                ) {
                    Text("Add to Estimate", color = BentoOnVibrant, fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    if (selectedProduct != null) {
                        selectedProduct = null
                    } else {
                        onDismiss()
                    }
                }
            ) {
                Text(if (selectedProduct != null) "Back" else "Cancel", color = TextSecondary)
            }
        }
    )
}

data class EstimateTotals(
    val subtotal: Double = 0.0,
    val taxTotal: Double = 0.0,
    val labourCharge: Double = 0.0,
    val grandTotal: Double = 0.0
)
