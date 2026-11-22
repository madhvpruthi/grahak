package com.princesanitary.grahak.ui.consumer.catalog

import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.ui.platform.LocalView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.princesanitary.grahak.data.model.PricingType
import com.princesanitary.grahak.data.model.Product
import com.princesanitary.grahak.data.model.ProductCategory
import com.princesanitary.grahak.ui.components.calculateProductPricing
import com.princesanitary.grahak.ui.components.formatRupees
import com.princesanitary.grahak.ui.consumer.cart.CartUiState
import com.princesanitary.grahak.ui.consumer.cart.CartViewModel
import com.princesanitary.grahak.ui.theme.ElectricLime
import com.princesanitary.grahak.ui.theme.EmeraldSignal
import com.princesanitary.grahak.ui.theme.Ink
import com.princesanitary.grahak.ui.theme.InkDark
import com.princesanitary.grahak.ui.theme.Line
import com.princesanitary.grahak.ui.theme.LuxeGold
import com.princesanitary.grahak.ui.theme.LuxeGoldBorder
import com.princesanitary.grahak.ui.theme.LuxeGoldSurface
import com.princesanitary.grahak.ui.theme.Paper
import com.princesanitary.grahak.ui.theme.PaperDim
import com.princesanitary.grahak.ui.theme.SurfaceWhite
import com.princesanitary.grahak.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogScreen(
    viewModel: CatalogViewModel,
    cartViewModel: CartViewModel,
    onProductClick: (Product) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val allProductsMaster by viewModel.allProducts.collectAsStateWithLifecycle()
    val cartUiState by cartViewModel.uiState.collectAsStateWithLifecycle()
    val cartItems = (cartUiState as? CartUiState.Success)?.cart?.items.orEmpty()
    var selectedProductForDetail by remember { mutableStateOf<Product?>(null) }

    // Search and Filter States (Handled cleanly in side filter sheet)
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryFilter by remember { mutableStateOf<ProductCategory?>(null) }
    var selectedMaterial by remember { mutableStateOf("All") }
    val materialTypes = listOf("All", "CPVC", "UPVC", "PVC / SWR", "Brass", "Ceramic", "SS / Chrome", "GI")

    var selectedBrand by remember { mutableStateOf("All") }
    val popularBrands = listOf("All", "Tirupati", "Supreme", "Astral", "Jaquar", "Cera", "Hindware", "Sintex", "Finolex", "Prince")

    var showFilterSheet by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }
    val view = LocalView.current
    val coroutineScope = rememberCoroutineScope()

    // Active filter counter
    val activeFilterCount = (if (selectedCategoryFilter != null) 1 else 0) +
        (if (selectedMaterial != "All") 1 else 0) +
        (if (selectedBrand != "All") 1 else 0)

    val productsToFilter = if (allProductsMaster.isNotEmpty()) {
        allProductsMaster
    } else when (val state = uiState) {
        is CatalogUiState.Success -> state.products
        else -> emptyList()
    }

    val resetAllFilters = {
        searchQuery = ""
        selectedCategoryFilter = null
        selectedMaterial = "All"
        selectedBrand = "All"
        viewModel.clearCategoryFilter()
    }

    // Instant client-side filter: Reliable, zero-latency, never affects dashboard
    val filteredProducts = remember(
        productsToFilter,
        searchQuery,
        selectedCategoryFilter,
        selectedMaterial,
        selectedBrand
    ) {
        productsToFilter.filter { p ->
            val matchesSearch = if (searchQuery.isBlank()) true else {
                val tokens = searchQuery.trim().lowercase().split("\\s+".toRegex()).filter { it.isNotEmpty() }
                val targetText = "${p.name} ${p.itemType} ${p.brand} ${p.company} ${p.size} ${p.categoryId} ${p.specifications.values.joinToString(" ")}".lowercase()
                tokens.all { token -> targetText.contains(token) }
            }

            val matchesCategory = if (selectedCategoryFilter == null) true else {
                p.categoryId.equals(selectedCategoryFilter?.id, ignoreCase = true) ||
                    p.name.contains(selectedCategoryFilter?.displayName ?: "", ignoreCase = true)
            }

            val matchesMaterial = if (selectedMaterial == "All") true else {
                val matKey = if (selectedMaterial == "PVC / SWR") "PVC" else selectedMaterial
                p.itemType.contains(matKey, ignoreCase = true) ||
                    p.name.contains(matKey, ignoreCase = true)
            }

            val matchesBrand = if (selectedBrand == "All") true else {
                p.brand.equals(selectedBrand, ignoreCase = true) ||
                    p.company.equals(selectedBrand, ignoreCase = true) ||
                    p.name.contains(selectedBrand, ignoreCase = true)
            }

            matchesSearch && matchesCategory && matchesMaterial && matchesBrand
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Paper)
    ) {
        // Top Header Section
        Column(
            Modifier
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Store Catalog",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = Ink
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "Prince Sanitary Store • Wholesale rates",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // Search Bar + Dedicated Filter Side Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            text = "Search product, size, brand...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Rounded.Search,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    Icons.Rounded.Clear,
                                    contentDescription = "Clear",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = PaperDim,
                        unfocusedContainerColor = PaperDim,
                        focusedBorderColor = LuxeGold,
                        unfocusedBorderColor = Line,
                        focusedTextColor = Ink,
                        unfocusedTextColor = Ink
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                )

                Spacer(Modifier.width(10.dp))

                // Modern Filter Button with Badge
                Box(
                    modifier = Modifier
                        .height(52.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (activeFilterCount > 0) LuxeGold else PaperDim)
                        .border(1.dp, if (activeFilterCount > 0) LuxeGold else Line, RoundedCornerShape(16.dp))
                        .clickable { showFilterSheet = true }
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Tune,
                            contentDescription = "Filter",
                            tint = if (activeFilterCount > 0) Color(0xFF09090B) else Ink,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Filter",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = if (activeFilterCount > 0) Color(0xFF09090B) else Ink
                        )
                        if (activeFilterCount > 0) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF09090B)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = activeFilterCount.toString(),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    ),
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // Quick Scrollable Categories Bar directly below search (Compact height 32dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val isAllSelected = selectedCategoryFilter == null
                Box(
                    modifier = Modifier
                        .height(32.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isAllSelected) LuxeGold else SurfaceWhite)
                        .border(1.dp, if (isAllSelected) LuxeGold else Line, RoundedCornerShape(10.dp))
                        .clickable {
                            selectedCategoryFilter = null
                            viewModel.clearCategoryFilter()
                        }
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "All",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = if (isAllSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 11.5.sp
                        ),
                        color = if (isAllSelected) Color(0xFF09090B) else Ink
                    )
                }

                ProductCategory.entries.forEach { category ->
                    val isSelected = selectedCategoryFilter == category
                    Box(
                        modifier = Modifier
                            .height(32.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) LuxeGold else SurfaceWhite)
                            .border(1.dp, if (isSelected) LuxeGold else Line, RoundedCornerShape(10.dp))
                        .clickable {
                            selectedCategoryFilter = if (isSelected) null else category
                        }
                        .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = category.displayName,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 11.5.sp
                            ),
                            color = if (isSelected) Color(0xFF09090B) else Ink
                        )
                    }
                }
            }

            // Quick Dismissable Active Filter Chips Row (Visible only when filters applied)
            if (activeFilterCount > 0) {
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    selectedCategoryFilter?.let { cat ->
                        ActiveFilterChip(
                            text = cat.displayName,
                            onRemove = { selectedCategoryFilter = null }
                        )
                    }
                    if (selectedMaterial != "All") {
                        ActiveFilterChip(
                            text = selectedMaterial,
                            onRemove = { selectedMaterial = "All" }
                        )
                    }
                    if (selectedBrand != "All") {
                        ActiveFilterChip(
                            text = selectedBrand,
                            onRemove = { selectedBrand = "All" }
                        )
                    }

                    Text(
                        text = "Reset All",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = LuxeGold
                        ),
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable {
                                selectedCategoryFilter = null
                                selectedMaterial = "All"
                                selectedBrand = "All"
                            }
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
        }

        // Product Catalog Grid with Pull-to-Refresh
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                } else {
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                }
                isRefreshing = true
                coroutineScope.launch {
                    viewModel.refreshMasterProducts()
                    delay(500)
                    isRefreshing = false
                }
            },
            modifier = Modifier.fillMaxSize()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (uiState is CatalogUiState.Loading && allProductsMaster.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        com.princesanitary.grahak.ui.components.FloatingBallLoader(
                            ballColor = LuxeGold,
                            label = "Loading products..."
                        )
                    }
                } else if (filteredProducts.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "No matching items found",
                                style = MaterialTheme.typography.titleMedium,
                                color = Ink
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = "Try clearing search or adjusting your filters",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                            Spacer(Modifier.height(16.dp))
                            Button(
                                onClick = resetAllFilters,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = LuxeGold,
                                    contentColor = Color(0xFF09090B)
                                ),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text("Clear All Filters", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(filteredProducts, key = { it.id }) { product ->
                            val cartItem = cartItems.find { it.productId == product.id }
                            val quantityInCart = cartItem?.quantity ?: 0
                            ProductCard(
                                product = product,
                                quantityInCart = quantityInCart,
                                onAddToCart = { cartViewModel.addToCart(product, 1) },
                                onUpdateQuantity = { newQty -> cartViewModel.updateQuantity(product.id, newQty) },
                                onClick = {
                                    selectedProductForDetail = product
                                    onProductClick(product)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Modal Filter Sheet housing all category, material, and brand filters cleanly
    if (showFilterSheet) {
        CatalogFilterSheet(
            selectedCategory = selectedCategoryFilter,
            selectedMaterial = selectedMaterial,
            selectedBrand = selectedBrand,
            materialTypes = materialTypes,
            popularBrands = popularBrands,
            onCategoryChange = { selectedCategoryFilter = it },
            onMaterialChange = { selectedMaterial = it },
            onBrandChange = { selectedBrand = it },
            onReset = resetAllFilters,
            onDismiss = { showFilterSheet = false }
        )
    }

    // High-res product detail sheet with interactive cart
    selectedProductForDetail?.let { prod ->
        ProductDetailSheet(
            product = prod,
            cartViewModel = cartViewModel,
            onDismiss = { selectedProductForDetail = null }
        )
    }
}

/**
 * Modern Filter Modal Bottom Sheet providing complete, organized filtering:
 * 1. Product Categories
 * 2. Material sub-types (CPVC, UPVC, PVC, Brass, Ceramic...)
 * 3. Brands (Supreme, Astral, Jaquar, Cera...)
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun CatalogFilterSheet(
    selectedCategory: ProductCategory?,
    selectedMaterial: String,
    selectedBrand: String,
    materialTypes: List<String>,
    popularBrands: List<String>,
    onCategoryChange: (ProductCategory?) -> Unit,
    onMaterialChange: (String) -> Unit,
    onBrandChange: (String) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

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
                .padding(20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.Tune,
                        contentDescription = null,
                        tint = LuxeGold,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Filter Products",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = Ink
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Reset All",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = LuxeGold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onReset() }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Close,
                            contentDescription = "Close",
                            tint = Ink
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
            ) {
                // Section 1: Categories
                Text(
                    text = "Categories",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = Ink
                )
                Spacer(Modifier.height(10.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterOptionChip(
                        text = "All Categories",
                        isSelected = selectedCategory == null,
                        onClick = { onCategoryChange(null) }
                    )
                    ProductCategory.entries.forEach { category ->
                        FilterOptionChip(
                            text = category.displayName,
                            isSelected = selectedCategory == category,
                            onClick = { onCategoryChange(category) }
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                // Section 2: Material / Sub-Type
                Text(
                    text = "Material / Type",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = Ink
                )
                Spacer(Modifier.height(10.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    materialTypes.forEach { mat ->
                        FilterOptionChip(
                            text = mat,
                            isSelected = selectedMaterial == mat,
                            onClick = { onMaterialChange(mat) }
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                // Section 3: Popular Brands
                Text(
                    text = "Brand",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = Ink
                )
                Spacer(Modifier.height(10.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    popularBrands.forEach { br ->
                        FilterOptionChip(
                            text = br,
                            isSelected = selectedBrand == br,
                            onClick = { onBrandChange(br) }
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))
            }

            Spacer(Modifier.height(14.dp))

            // Action Buttons
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = LuxeGold,
                    contentColor = Color(0xFF09090B)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = "Apply Filters",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}

@Composable
private fun FilterOptionChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) LuxeGold else PaperDim)
            .border(1.dp, if (isSelected) LuxeGold else Line, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            ),
            color = if (isSelected) Color(0xFF09090B) else Ink
        )
    }
}

@Composable
private fun ActiveFilterChip(
    text: String,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(PaperDim)
            .border(1.dp, Line, RoundedCornerShape(10.dp))
            .clickable(onClick = onRemove)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = Ink
        )
        Icon(
            Icons.Rounded.Close,
            contentDescription = "Remove",
            tint = TextSecondary,
            modifier = Modifier.size(12.dp)
        )
    }
}

/**
 * Product Card in Catalog:
 * - Product Image with Brand chip & Luxury badge
 * - Prominent Product Size tag (e.g. 1/2", 3/4", 24x18")
 * - Product Title & item type
 * - GST inclusive price
 * - Quick tactile Add to Cart button
 */
@Composable
private fun ProductCard(
    product: Product,
    quantityInCart: Int = 0,
    onAddToCart: () -> Unit,
    onUpdateQuantity: (Int) -> Unit = {},
    onClick: () -> Unit
) {
    val view = LocalView.current
    val pricing = remember(product.id) { calculateProductPricing(product) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        border = BorderStroke(1.dp, Line)
    ) {
        Column {
            // Product Image Container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(135.dp)
                    .background(PaperDim),
                contentAlignment = Alignment.Center
            ) {
                if (product.mediaUrls.isNotEmpty()) {
                    AsyncImage(
                        model = product.mediaUrls.first(),
                        contentDescription = product.name,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(6.dp),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(SurfaceWhite),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Rounded.Storefront,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                // Discount Pill on top-left (Matching Dashboard rules)
                if (pricing.showDiscountTag) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(LuxeGold)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = pricing.tagText,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 8.5.sp
                            ),
                            color = Color(0xFF09090B)
                        )
                    }
                } else if (product.isFeatured) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                            .clip(CircleShape)
                            .background(LuxeGold)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Rounded.Star,
                                contentDescription = null,
                                tint = Color(0xFF09090B),
                                modifier = Modifier.size(10.dp)
                            )
                            Spacer(Modifier.width(2.dp))
                            Text(
                                text = "LUXURY",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 8.sp
                                ),
                                color = Color(0xFF09090B)
                            )
                        }
                    }
                }

                // Prominent Size Tag on image top-right (e.g. 1/2", 1", 24x18")
                if (product.size.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(InkDark.copy(alpha = 0.85f))
                            .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = product.size,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp
                            ),
                            color = Color.White
                        )
                    }
                }

                // Brand Pill (if present) at bottom-start
                if (product.brand.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(InkDark.copy(alpha = 0.85f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = product.brand.uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 8.sp,
                                letterSpacing = 0.5.sp
                            ),
                            color = LuxeGold
                        )
                    }
                }
            }

            // Product Details
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = Ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))

                // Size Badge + Item Type / Category Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (product.size.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(PaperDim)
                                .border(1.dp, Line, RoundedCornerShape(6.dp))
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = product.size,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                ),
                                color = Ink
                            )
                        }
                    }

                    val subText = product.itemType.ifBlank {
                        ProductCategory.fromId(product.categoryId)?.displayName ?: product.categoryId
                    }
                    Text(
                        text = subText,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        if (product.pricingType == PricingType.FIXED) {
                            Text(
                                text = formatRupees(pricing.sellingPrice),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                ),
                                color = Ink
                            )
                            if (pricing.mrp > pricing.sellingPrice) {
                                Text(
                                    text = formatRupees(pricing.mrp),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.sp,
                                        textDecoration = TextDecoration.LineThrough
                                    ),
                                    color = TextSecondary
                                )
                            }
                        } else {
                            Text(
                                text = "Get Quote",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = LuxeGold
                            )
                        }
                    }

                    Spacer(Modifier.width(6.dp))

                    // Stepper or ADD Button (matching reference images)
                    if (quantityInCart > 0) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .height(32.dp)
                                .clip(RoundedCornerShape(9.dp))
                                .background(Ink)
                                .padding(horizontal = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                        } else {
                                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                        }
                                        onUpdateQuantity(quantityInCart - 1)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Rounded.Remove,
                                    contentDescription = "Decrease",
                                    tint = SurfaceWhite,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = quantityInCart.toString(),
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 12.5.sp
                                ),
                                color = SurfaceWhite
                            )
                            Spacer(Modifier.width(4.dp))
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                        } else {
                                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                        }
                                        onUpdateQuantity(quantityInCart + 1)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Rounded.Add,
                                    contentDescription = "Increase",
                                    tint = SurfaceWhite,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(9.dp),
                            color = Ink.copy(alpha = 0.08f),
                            border = BorderStroke(1.2.dp, Ink.copy(alpha = 0.25f)),
                            modifier = Modifier
                                .height(32.dp)
                                .clip(RoundedCornerShape(9.dp))
                                .clickable {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                                    } else {
                                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                    }
                                    onAddToCart()
                                }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 9.dp)
                            ) {
                                Icon(
                                    Icons.Rounded.Add,
                                    contentDescription = null,
                                    tint = Ink,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(Modifier.width(2.dp))
                                Text(
                                    text = "ADD",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 11.5.sp,
                                        letterSpacing = 0.5.sp
                                    ),
                                    color = Ink
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
