package com.princesanitary.grahak.ui.consumer.catalog

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.LocalShipping
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.princesanitary.grahak.data.model.Product
import com.princesanitary.grahak.ui.consumer.cart.CartUiState
import com.princesanitary.grahak.ui.consumer.cart.CartViewModel
import com.princesanitary.grahak.ui.theme.EmeraldSignal
import com.princesanitary.grahak.ui.theme.Ink
import com.princesanitary.grahak.ui.theme.Line
import com.princesanitary.grahak.ui.theme.LuxeGold
import com.princesanitary.grahak.ui.theme.LuxeGoldSurface
import com.princesanitary.grahak.ui.theme.PaperDim
import com.princesanitary.grahak.ui.theme.StatusError
import com.princesanitary.grahak.ui.theme.StatusSuccess
import com.princesanitary.grahak.ui.theme.SurfaceWhite
import com.princesanitary.grahak.ui.theme.TextSecondary

/**
 * Modern Blinkit-style Product Detail Sheet:
 * Shows high-resolution product imagery, sanitary specifications, wholesale rates,
 * and live interactive Add to Cart / Quantity controls.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailSheet(
    product: Product,
    cartViewModel: CartViewModel,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val cartUiState by cartViewModel.uiState.collectAsStateWithLifecycle()

    val cartQuantity = (cartUiState as? CartUiState.Success)?.cart?.items
        ?.firstOrNull { it.productId == product.id }?.quantity ?: 0

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SurfaceWhite,
        dragHandle = null,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            // Header Bar with close icon
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(LuxeGoldSurface)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = product.categoryId.replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = LuxeGold
                        )
                    }
                    if (product.itemType.isNotBlank()) {
                        Spacer(Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(PaperDim)
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = product.itemType,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = TextSecondary
                            )
                        }
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(PaperDim)
                ) {
                    Icon(
                        Icons.Rounded.Close,
                        contentDescription = "Close",
                        tint = Ink,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Scrollable Content
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
            ) {
                // Big Hero Product Photo (Blinkit style high-res viewer)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(PaperDim)
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (product.mediaUrls.isNotEmpty()) {
                        AsyncImage(
                            model = product.mediaUrls.first(),
                            contentDescription = product.name,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Rounded.Storefront,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = product.name.take(2).uppercase(),
                                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                                color = Ink
                            )
                        }
                    }
                }

                Spacer(Modifier.height(18.dp))

                // Product Title & Brand
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = Ink
                )

                if (product.brand.isNotBlank() || product.company.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "By ${product.company.ifEmpty { product.brand }}",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = LuxeGold
                    )
                }

                Spacer(Modifier.height(14.dp))

                // Sanitary Specs Chips Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (product.size.isNotBlank()) {
                        SpecChip(label = "Size", value = product.size)
                    }
                    if (product.unit.isNotBlank()) {
                        SpecChip(label = "Unit", value = product.unit)
                    }
                    SpecChip(
                        label = "Stock",
                        value = if (product.stockLevel > 0) "${product.stockLevel} In Stock" else "Out of stock",
                        valueColor = if (product.stockLevel > 0) StatusSuccess else StatusError
                    )
                }

                Spacer(Modifier.height(16.dp))

                // Wholesale Store Guarantee Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = PaperDim),
                    border = BorderStroke(1.dp, Line)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Rounded.LocalShipping,
                            contentDescription = null,
                            tint = LuxeGold,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Direct Wholesale Supply",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = Ink
                            )
                            Text(
                                text = "Supplied directly from Prince Sanitary wholesale warehouse.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))
            }

            // Fixed Bottom Bar with Price & Add to Cart
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = SurfaceWhite,
                shadowElevation = 12.dp,
                border = BorderStroke(1.dp, Line)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "₹${product.unitPrice.toInt()}",
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                            color = Ink
                        )
                        Text(
                            text = "per ${product.unit.lowercase()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }

                    if (cartQuantity == 0) {
                        Button(
                            onClick = { cartViewModel.addToCart(product, 1) },
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = LuxeGold,
                                contentColor = Color(0xFF09090B)
                            ),
                            modifier = Modifier.height(48.dp)
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null, tint = Color(0xFF09090B))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "ADD TO CART",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF09090B)
                            )
                        }
                    } else {
                        // High-contrast quantity stepper
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(LuxeGold)
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                        ) {
                            IconButton(
                                onClick = { cartViewModel.updateQuantity(product.id, cartQuantity - 1) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Rounded.Remove, contentDescription = "Decrease", tint = Color(0xFF09090B))
                            }
                            Text(
                                text = "$cartQuantity",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF09090B),
                                modifier = Modifier.padding(horizontal = 14.dp)
                            )
                            IconButton(
                                onClick = { cartViewModel.updateQuantity(product.id, cartQuantity + 1) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Rounded.Add, contentDescription = "Increase", tint = Color(0xFF09090B))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SpecChip(
    label: String,
    value: String,
    valueColor: Color = Ink
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(PaperDim)
            .border(1.dp, Line, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            Text(value, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = valueColor)
        }
    }
}
