package com.princesanitary.grahak.ui.consumer.catalog

import com.princesanitary.grahak.ui.theme.Paper
import com.princesanitary.grahak.ui.theme.Ink

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.princesanitary.grahak.data.model.Product
import com.princesanitary.grahak.data.model.PricingType
import com.princesanitary.grahak.ui.theme.RFQBadge
import com.princesanitary.grahak.ui.theme.StatusSuccess

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailScreen(
    product: Product,
    onBackClick: () -> Unit,
    onAddToCart: (Product, Int) -> Unit,
    onRequestQuote: (Product) -> Unit
) {
    var quantity by remember { mutableStateOf(product.minOrderQty) }
    var showAddToCartDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Product Details") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
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
            BottomActionBar(
                product = product,
                quantity = quantity,
                onAddToCart = {
                    if (product.pricingType == PricingType.FIXED) {
                        onAddToCart(product, quantity)
                    } else {
                        onRequestQuote(product)
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Product Images
            item {
                if (product.mediaUrls.isNotEmpty()) {
                    ProductImageGallery(
                        imageUrls = product.mediaUrls,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = product.name.take(2).uppercase(),
                            style = MaterialTheme.typography.displayLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Product Info Section
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    // Product Name & Brand
                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = product.brand,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Pricing Section
                    when (product.pricingType) {
                        PricingType.FIXED -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "₹${product.unitPrice}",
                                        style = MaterialTheme.typography.headlineLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "Per unit",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                // Stock Status
                                AssistChip(
                                    onClick = {},
                                    label = {
                                        Text(
                                            if (product.stockLevel > 0) "In Stock (${product.stockLevel})"
                                            else "Out of Stock"
                                        )
                                    },
                                    colors = AssistChipDefaults.assistChipColors(
                                        containerColor = if (product.stockLevel > 0)
                                            StatusSuccess.copy(alpha = 0.1f)
                                        else
                                            MaterialTheme.colorScheme.errorContainer,
                                        labelColor = if (product.stockLevel > 0)
                                            StatusSuccess
                                        else
                                            MaterialTheme.colorScheme.error
                                    )
                                )
                            }
                        }
                        PricingType.RFQ_ONLY -> {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = RFQBadge.copy(alpha = 0.1f)
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    Text(
                                        text = "Price on Request",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        color = RFQBadge
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Contact us for best pricing on this commodity item",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // Quantity Selector (only for FIXED pricing)
                    if (product.pricingType == PricingType.FIXED && product.stockLevel > 0) {
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = "Quantity",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        QuantitySelector(
                            quantity = quantity,
                            minQuantity = product.minOrderQty,
                            maxQuantity = product.stockLevel,
                            onQuantityChange = { quantity = it }
                        )
                        Text(
                            text = "Minimum order: ${product.minOrderQty} units",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            // Specifications
            if (product.specifications.isNotEmpty()) {
                item {
                    Divider(modifier = Modifier.padding(vertical = 16.dp))
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        Text(
                            text = "Specifications",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        product.specifications.forEach { (key, value) ->
                            SpecificationRow(key = key, value = value)
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }

            // Tax Information
            item {
                Divider(modifier = Modifier.padding(vertical = 16.dp))
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Text(
                        text = "Tax Information",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "HSN Code: ${product.tax.hsn}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "GST Rate: ${product.tax.gstRate}%",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun ProductImageGallery(
    imageUrls: List<String>,
    modifier: Modifier = Modifier
) {
    var selectedIndex by remember { mutableStateOf(0) }

    Column(modifier = modifier) {
        // Main Image
        AsyncImage(
            model = imageUrls[selectedIndex],
            contentDescription = "Product image",
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentScale = ContentScale.Crop
        )

        // Thumbnail Row
        if (imageUrls.size > 1) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .background(MaterialTheme.colorScheme.surface),
                contentPadding = PaddingValues(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(imageUrls.size) { index ->
                    AsyncImage(
                        model = imageUrls[index],
                        contentDescription = "Thumbnail $index",
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .then(
                                if (selectedIndex == index) {
                                    Modifier.background(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                                } else {
                                    Modifier
                                }
                            ),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }
    }
}

@Composable
fun QuantitySelector(
    quantity: Int,
    minQuantity: Int,
    maxQuantity: Int,
    onQuantityChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        OutlinedButton(
            onClick = { if (quantity > minQuantity) onQuantityChange(quantity - 1) },
            enabled = quantity > minQuantity
        ) {
            Text("-")
        }

        Text(
            text = quantity.toString(),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.widthIn(min = 40.dp)
        )

        OutlinedButton(
            onClick = { if (quantity < maxQuantity) onQuantityChange(quantity + 1) },
            enabled = quantity < maxQuantity
        ) {
            Text("+")
        }
    }
}

@Composable
fun SpecificationRow(
    key: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = key,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun BottomActionBar(
    product: Product,
    quantity: Int,
    onAddToCart: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            when (product.pricingType) {
                PricingType.FIXED -> {
                    Column {
                        Text(
                            text = "Total",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "₹${product.unitPrice * quantity}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Button(
                        onClick = onAddToCart,
                        enabled = product.stockLevel > 0,
                        modifier = Modifier.height(48.dp)
                    ) {
                        Icon(Icons.Default.ShoppingCart, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add to Cart")
                    }
                }
                PricingType.RFQ_ONLY -> {
                    Button(
                        onClick = onAddToCart,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = RFQBadge
                        )
                    ) {
                        Text("Request Quote")
                    }
                }
            }
        }
    }
}
