package com.princesanitary.grahak.ui.consumer

import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.LocalShipping
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import androidx.compose.runtime.produceState
import com.princesanitary.grahak.data.model.PricingType
import com.princesanitary.grahak.data.model.Product
import com.princesanitary.grahak.data.model.ProductCategory
import com.princesanitary.grahak.data.model.StoreSettings
import com.princesanitary.grahak.di.ServiceLocator
import com.princesanitary.grahak.ui.components.calculateProductPricing
import com.princesanitary.grahak.ui.components.formatRupees
import com.princesanitary.grahak.ui.consumer.cart.CartUiState
import com.princesanitary.grahak.ui.consumer.cart.CartViewModel
import com.princesanitary.grahak.ui.consumer.catalog.CatalogUiState
import com.princesanitary.grahak.ui.consumer.catalog.CatalogViewModel
import com.princesanitary.grahak.ui.consumer.catalog.ProductDetailSheet
import com.princesanitary.grahak.ui.consumer.orders.OrderHistoryViewModel
import com.princesanitary.grahak.ui.theme.BentoDark
import com.princesanitary.grahak.ui.theme.ElectricLime
import com.princesanitary.grahak.ui.theme.EmeraldSignal
import com.princesanitary.grahak.ui.theme.EmeraldSignalBorder
import com.princesanitary.grahak.ui.theme.EmeraldSignalSurface
import com.princesanitary.grahak.ui.theme.GrahakRed
import com.princesanitary.grahak.ui.theme.GrahakRedBright
import com.princesanitary.grahak.ui.theme.GrahakBlack
import com.princesanitary.grahak.ui.theme.GrahakCharcoal
import com.princesanitary.grahak.ui.theme.GrahakCharcoalElevated
import com.princesanitary.grahak.ui.theme.GrahakWarmWhite
import com.princesanitary.grahak.ui.theme.Ink
import com.princesanitary.grahak.ui.theme.Line
import com.princesanitary.grahak.ui.theme.LuxeGold
import com.princesanitary.grahak.ui.theme.LuxeGoldBorder
import com.princesanitary.grahak.ui.theme.LuxeGoldSurface
import com.princesanitary.grahak.ui.theme.Paper
import com.princesanitary.grahak.ui.theme.PaperDim
import com.princesanitary.grahak.ui.theme.SurfaceWhite
import com.princesanitary.grahak.ui.theme.TextSecondary
import com.princesanitary.grahak.ui.theme.isDarkThemeActive
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConsumerHomeScreen(
    catalogViewModel: CatalogViewModel,
    cartViewModel: CartViewModel,
    orderHistoryViewModel: OrderHistoryViewModel,
    scrollState: ScrollState = rememberScrollState(),
    onOpenCart: () -> Unit,
    onOpenShop: () -> Unit,
    onOpenServices: () -> Unit,
    onOpenOrders: () -> Unit
) {
    val cartState by cartViewModel.uiState.collectAsStateWithLifecycle()
    val cartCount = (cartState as? CartUiState.Success)?.summary?.itemCount ?: 0
    val cartItems = (cartState as? CartUiState.Success)?.cart?.items.orEmpty()

    val storeSettings by ServiceLocator.orderRepository.observeStoreSettings()
        .collectAsStateWithLifecycle(initialValue = com.princesanitary.grahak.data.local.SettingsStorage.getCachedStoreSettings())

    // Master products flow: guarantees home screen is never wiped by catalog search/filters
    val allProductsMaster by catalogViewModel.allProducts.collectAsStateWithLifecycle()
    val catalogState by catalogViewModel.uiState.collectAsStateWithLifecycle()
    val products = if (allProductsMaster.isNotEmpty()) {
        allProductsMaster
    } else {
        (catalogState as? CatalogUiState.Success)?.products.orEmpty()
    }

    var selectedProductForDetail by remember { mutableStateOf<Product?>(null) }
    var isRefreshing by remember { mutableStateOf(false) }
    val view = LocalView.current
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    var topBarHeightDp by remember { mutableStateOf(145.dp) }

    // =========================================================================
    // STRICT CATEGORY FILTERING (Categories Only, Zero Brands in Category Logic)
    // =========================================================================

    // Best Sellers / Popular
    val bestSellers = remember(products) {
        val featured = products.filter { it.isFeatured }
        if (featured.isNotEmpty()) featured else products.take(6)
    }

    // 1. Ceramics (Wash basins, commodes, seats, vanity counters, pedestals, mirrors)
    val ceramicsProducts = remember(products) {
        val filtered = products.filter { p ->
            p.categoryId.equals("ceramics", true) ||
                p.categoryId.equals("seats", true) ||
                p.categoryId.equals("mirrors", true) ||
                p.categoryId.equals("sanitaryware", true) ||
                p.itemType.contains("basin", true) ||
                p.itemType.contains("seat", true) ||
                p.itemType.contains("mirror", true) ||
                p.itemType.contains("vanity", true) ||
                p.itemType.contains("commode", true) ||
                p.itemType.contains("ceramic", true) ||
                p.name.contains("basin", true) ||
                p.name.contains("seat", true) ||
                p.name.contains("mirror", true) ||
                p.name.contains("commode", true) ||
                p.name.contains("vanity", true)
        }
        if (filtered.isNotEmpty()) filtered else products.take(6)
    }

    // 2. Fittings (Elbows, tees, sockets, unions, reducers, nipples, bushings, couplings, clamps)
    val fittingsProducts = remember(products) {
        val filtered = products.filter { p ->
            p.categoryId.equals("fittings", true) ||
                p.itemType.contains("fitting", true) ||
                p.name.contains("elbow", true) ||
                p.name.contains("tee", true) ||
                p.name.contains("socket", true) ||
                p.name.contains("union", true) ||
                p.name.contains("reducer", true) ||
                p.name.contains("nipple", true) ||
                p.name.contains("bushing", true) ||
                p.name.contains("coupling", true) ||
                p.name.contains("adapter", true) ||
                p.name.contains("clamp", true) ||
                p.name.contains("end cap", true) ||
                p.name.contains("plug", true)
        }
        if (filtered.isNotEmpty()) filtered else products.drop(1).take(6)
    }

    // 3. Pipes (Plumbing pipes only - excluding fittings)
    val pipesProducts = remember(products) {
        val filtered = products.filter { p ->
            (p.categoryId.equals("pipes", true) ||
                p.itemType.contains("pipe", true) ||
                p.name.contains("pipe", true)) &&
                !p.name.contains("elbow", true) &&
                !p.name.contains("tee", true) &&
                !p.name.contains("socket", true) &&
                !p.name.contains("reducer", true) &&
                !p.name.contains("union", true) &&
                !p.name.contains("fitting", true)
        }
        if (filtered.isNotEmpty()) filtered else products.drop(2).take(6)
    }

    // 4. Taps & Showers (Bib cocks, pillar cocks, mixers, showers, angle valves, faucets)
    val tapsAndShowers = remember(products) {
        val filtered = products.filter { p ->
            p.categoryId.equals("taps", true) ||
                p.categoryId.equals("showers", true) ||
                p.itemType.contains("tap", true) ||
                p.itemType.contains("cock", true) ||
                p.itemType.contains("mixer", true) ||
                p.itemType.contains("shower", true) ||
                p.itemType.contains("faucet", true) ||
                p.name.contains("tap", true) ||
                p.name.contains("cock", true) ||
                p.name.contains("mixer", true) ||
                p.name.contains("shower", true) ||
                p.name.contains("faucet", true) ||
                p.name.contains("spout", true)
        }
        if (filtered.isNotEmpty()) filtered else products.drop(3).take(6)
    }

    // 5. Tanks & Drains (Water storage tanks, floor traps, cleanouts, solvent cements, hardware)
    val tanksAndDrains = remember(products) {
        val filtered = products.filter { p ->
            p.categoryId.equals("tanks", true) ||
                p.categoryId.equals("drains", true) ||
                p.categoryId.equals("hardware", true) ||
                p.categoryId.equals("tools", true) ||
                p.itemType.contains("tank", true) ||
                p.itemType.contains("drain", true) ||
                p.itemType.contains("trap", true) ||
                p.itemType.contains("cement", true) ||
                p.name.contains("tank", true) ||
                p.name.contains("drain", true) ||
                p.name.contains("trap", true) ||
                p.name.contains("cement", true) ||
                p.name.contains("solvent", true) ||
                p.name.contains("teflon", true)
        }
        if (filtered.isNotEmpty()) filtered else products.drop(4).take(6)
    }

    val isScrolled by remember {
        derivedStateOf { scrollState.value > 36 }
    }

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
                catalogViewModel.refreshMasterProducts()
                delay(500)
                isRefreshing = false
            }
        },
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Paper)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
            ) {
                // Top spacer matching the pinned header when unscrolled (no overlap on any screen)
                Spacer(Modifier.height(topBarHeightDp))

                // 1. Festive Wishes Banner (Driven strictly from Firestore)
                if (storeSettings.isFestivalBannerActive && storeSettings.festivalHeadline.isNotBlank()) {
                    FestiveGreetingBanner(storeSettings = storeSettings)
                    Spacer(Modifier.height(14.dp))
                }

                PremiumStoreHero(onOpenShop = onOpenShop)

                Spacer(Modifier.height(12.dp))

            PremiumQuickActions(
                onOpenServices = onOpenServices,
                onOpenOrders = onOpenOrders
            )

            Spacer(Modifier.height(24.dp))

            // =================================================================
            // 3. AESTHETIC PRODUCT SHELVES (Inspired by Pinterest Inspiration)
            // =================================================================

            // Shelf 1: Ceramics — FIRST & LARGEST (User requested Ceramics first)
            AestheticProductShelf(
                title = "We have all your ceramics",
                subtitle = "Wash basins, commodes, seats & vanity counters",
                badgeText = "LUXURY",
                badgeColor = LuxeGold,
                products = ceramicsProducts,
                cartItems = cartItems,
                onProductClick = { selectedProductForDetail = it },
                onAddToCart = { prod -> cartViewModel.addToCart(prod, 1) },
                onUpdateQuantity = { prodId, qty ->
                    if (qty <= 0) cartViewModel.removeItem(prodId)
                    else cartViewModel.updateQuantity(prodId, qty)
                },
                onSeeAll = {
                    catalogViewModel.loadProductsByCategory(ProductCategory.CERAMICS)
                    onOpenShop()
                },
                isLarge = true,
                showDiscount = true,
                isCeramicsShelf = true,
                isBestDealsShelf = false
            )

            Spacer(Modifier.height(26.dp))

            // Shelf 2: Best Sellers (Top Rated) — compact cards
            AestheticProductShelf(
                title = "Best sellers",
                subtitle = "Top-ordered pipes, basins & fittings in Saharanpur",
                badgeText = "BEST SELLERS",
                badgeColor = LuxeGold,
                products = bestSellers,
                cartItems = cartItems,
                onProductClick = { selectedProductForDetail = it },
                onAddToCart = { prod -> cartViewModel.addToCart(prod, 1) },
                onUpdateQuantity = { prodId, qty ->
                    if (qty <= 0) cartViewModel.removeItem(prodId)
                    else cartViewModel.updateQuantity(prodId, qty)
                },
                onSeeAll = onOpenShop,
                isLarge = false,
                showDiscount = true,
                isCeramicsShelf = false,
                isBestDealsShelf = true
            )

            Spacer(Modifier.height(26.dp))

            // Shelf 3: Pipes (Construction Essentials)
            AestheticProductShelf(
                title = "Pipes & Plumbing",
                subtitle = "CPVC, UPVC, PVC & SWR wholesale pipes",
                badgeText = "HEAVY DUTY",
                badgeColor = LuxeGold,
                products = pipesProducts,
                cartItems = cartItems,
                onProductClick = { selectedProductForDetail = it },
                onAddToCart = { prod -> cartViewModel.addToCart(prod, 1) },
                onUpdateQuantity = { prodId, qty ->
                    if (qty <= 0) cartViewModel.removeItem(prodId)
                    else cartViewModel.updateQuantity(prodId, qty)
                },
                onSeeAll = {
                    catalogViewModel.loadProductsByCategory(ProductCategory.PIPES)
                    onOpenShop()
                },
                isLarge = false,
                showDiscount = false,
                isCeramicsShelf = false,
                isBestDealsShelf = false
            )

            Spacer(Modifier.height(26.dp))

            // Shelf 4: Fittings & Valves
            AestheticProductShelf(
                title = "Fittings & Valves",
                subtitle = "Elbows, tees, sockets, reducers & unions",
                badgeText = "ESSENTIALS",
                badgeColor = LuxeGold,
                products = fittingsProducts,
                cartItems = cartItems,
                onProductClick = { selectedProductForDetail = it },
                onAddToCart = { prod -> cartViewModel.addToCart(prod, 1) },
                onUpdateQuantity = { prodId, qty ->
                    if (qty <= 0) cartViewModel.removeItem(prodId)
                    else cartViewModel.updateQuantity(prodId, qty)
                },
                onSeeAll = {
                    catalogViewModel.loadProductsByCategory(ProductCategory.FITTINGS)
                    onOpenShop()
                },
                isLarge = false,
                showDiscount = false,
                isCeramicsShelf = false,
                isBestDealsShelf = false
            )

            Spacer(Modifier.height(26.dp))

            // Shelf 5: Taps & Showers
            AestheticProductShelf(
                title = "Taps & Showers",
                subtitle = "Bib cocks, pillar taps, mixers & showers",
                badgeText = "BATH & CHROME",
                badgeColor = LuxeGold,
                products = tapsAndShowers,
                cartItems = cartItems,
                onProductClick = { selectedProductForDetail = it },
                onAddToCart = { prod -> cartViewModel.addToCart(prod, 1) },
                onUpdateQuantity = { prodId, qty ->
                    if (qty <= 0) cartViewModel.removeItem(prodId)
                    else cartViewModel.updateQuantity(prodId, qty)
                },
                onSeeAll = {
                    catalogViewModel.loadProductsByCategory(ProductCategory.TAPS)
                    onOpenShop()
                },
                isLarge = false,
                showDiscount = false,
                isCeramicsShelf = false,
                isBestDealsShelf = false
            )

            Spacer(Modifier.height(26.dp))

            // Shelf 6: Tanks & Drains
            AestheticProductShelf(
                title = "Tanks, Drains & Hardware",
                subtitle = "Water storage tanks, floor traps & solvent cements",
                badgeText = "HARDWARE",
                badgeColor = LuxeGold,
                products = tanksAndDrains,
                cartItems = cartItems,
                onProductClick = { selectedProductForDetail = it },
                onAddToCart = { prod -> cartViewModel.addToCart(prod, 1) },
                onUpdateQuantity = { prodId, qty ->
                    if (qty <= 0) cartViewModel.removeItem(prodId)
                    else cartViewModel.updateQuantity(prodId, qty)
                },
                onSeeAll = {
                    catalogViewModel.loadProductsByCategory(ProductCategory.TANKS)
                    onOpenShop()
                },
                isLarge = false,
                showDiscount = false,
                isCeramicsShelf = false,
                isBestDealsShelf = false
            )

            Spacer(Modifier.height(28.dp))

            // =================================================================
            // 4. BOTTOM TRUST FOOTER
            // =================================================================
            BottomTrustBento()

            Spacer(Modifier.height(56.dp))
        }

        // =================================================================
        // PINNED STICKY TOP BAR (Pins just below camera/status bar on scroll)
        // =================================================================
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .onGloballyPositioned { coordinates ->
                    if (!isScrolled) {
                        val measured = with(density) { coordinates.size.height.toDp() }
                        if (measured > 40.dp) {
                            topBarHeightDp = measured
                        }
                    }
                },
            color = Paper,
            shadowElevation = if (isScrolled) 6.dp else 0.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // Full store beacon (visible at top of screen, collapses smoothly on scroll)
                AnimatedVisibility(
                    visible = !isScrolled,
                    enter = fadeIn(tween(160, easing = FastOutSlowInEasing)) + expandVertically(tween(180, easing = FastOutSlowInEasing)),
                    exit = fadeOut(tween(140, easing = FastOutSlowInEasing)) + shrinkVertically(tween(180, easing = FastOutSlowInEasing))
                ) {
                    Column {
                        Spacer(Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "fixit",
                                        style = MaterialTheme.typography.headlineMedium.copy(
                                            fontWeight = FontWeight.Black,
                                            letterSpacing = (-1.2).sp,
                                            fontSize = 28.sp
                                        ),
                                        color = Ink
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .padding(bottom = 6.dp)
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(LuxeGold)
                                    )
                                }
                                Spacer(Modifier.height(1.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = storeSettings.ownerBusinessName.ifBlank { "Prince Sanitary Store" },
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.5.sp
                                        ),
                                        color = Ink,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Icon(
                                        Icons.Rounded.Verified,
                                        contentDescription = null,
                                        tint = LuxeGold,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Surface(
                                        shape = CircleShape,
                                        color = if (storeSettings.isStoreOpen) EmeraldSignalSurface else Color(0xFF271E0B),
                                        border = BorderStroke(0.6.dp, if (storeSettings.isStoreOpen) EmeraldSignalBorder else LuxeGold.copy(alpha = 0.5f))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(5.dp)
                                                    .clip(CircleShape)
                                                    .background(if (storeSettings.isStoreOpen) EmeraldSignal else LuxeGold)
                                            )
                                            Spacer(Modifier.width(4.dp))
                                            Text(
                                                 text = if (storeSettings.isStoreOpen) "LIVE" else "STORE CLOSED",
                                                 style = MaterialTheme.typography.labelSmall.copy(
                                                     fontWeight = FontWeight.ExtraBold,
                                                     fontSize = 8.sp,
                                                     letterSpacing = 0.6.sp
                                                 ),
                                                 color = if (storeSettings.isStoreOpen) EmeraldSignal else LuxeGold
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = "Saharanpur",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = TextSecondary
                                )
                            }

                            TopCartButton(
                                cartCount = cartCount,
                                onOpenCart = onOpenCart
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                    }
                }

                // Search row - Fixed directly below the camera cutout/status bar
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(SurfaceWhite)
                            .border(1.dp, Line, RoundedCornerShape(16.dp))
                            .clickable { onOpenShop() },
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        ) {
                            Icon(
                                Icons.Rounded.Search,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Search pipes, taps, wash basins, fittings...",
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.5.sp),
                                color = TextSecondary,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = PaperDim,
                                border = BorderStroke(0.8.dp, Line)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Rounded.Tune,
                                        contentDescription = "Filter",
                                        tint = Ink,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = "Filter",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 10.5.sp
                                        ),
                                        color = Ink
                                    )
                                }
                            }
                        }
                    }

                    // Scrolled cart button next to search
                    AnimatedVisibility(
                        visible = isScrolled,
                        enter = fadeIn() + expandHorizontally(),
                        exit = fadeOut() + shrinkHorizontally()
                    ) {
                        Row {
                            Spacer(Modifier.width(8.dp))
                            TopCartButton(
                                cartCount = cartCount,
                                onOpenCart = onOpenCart,
                                compact = true
                            )
                        }
                    }
                }

                // Sliding suggestions row - Pops up right below the search bar
                AnimatedVisibility(
                    visible = isScrolled,
                    enter = fadeIn(tween(160, easing = FastOutSlowInEasing)) + expandVertically(tween(180, easing = FastOutSlowInEasing)),
                    exit = fadeOut(tween(140, easing = FastOutSlowInEasing)) + shrinkVertically(tween(180, easing = FastOutSlowInEasing))
                ) {
                    Column {
                        Spacer(Modifier.height(8.dp))
                        val suggestions = listOf(
                            "Ceramics" to ProductCategory.CERAMICS,
                            "Taps & Faucets" to ProductCategory.TAPS,
                            "Fittings" to ProductCategory.FITTINGS,
                            "Pipes" to ProductCategory.PIPES,
                            "Water Tanks" to ProductCategory.TANKS,
                            "Showers" to ProductCategory.SHOWERS,
                            "Seats & Toilets" to ProductCategory.SEATS,
                            "Mirrors & Vanities" to ProductCategory.MIRRORS,
                            "Drains & Traps" to ProductCategory.DRAINS,
                            "Hardware" to ProductCategory.HARDWARE
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            suggestions.forEach { (label, category) ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFF16161B),
                                    border = BorderStroke(1.dp, Color(0xFF27272A)),
                                    modifier = Modifier.clickable {
                                        catalogViewModel.loadProductsByCategory(category)
                                        onOpenShop()
                                    }
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 12.sp,
                                            letterSpacing = 0.2.sp
                                        ),
                                        color = Color(0xFFE4E4E7),
                                        modifier = Modifier.padding(horizontal = 13.dp, vertical = 7.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
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
}

@Composable
private fun TopCartButton(
    cartCount: Int,
    onOpenCart: () -> Unit,
    compact: Boolean = false
) {
    if (cartCount > 0) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = LuxeGold,
            shadowElevation = 3.dp,
            modifier = Modifier
                .height(44.dp)
                .clickable { onOpenCart() }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = if (compact) 10.dp else 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Rounded.ShoppingCart,
                    contentDescription = "Cart",
                    tint = Color(0xFF09090B),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(5.dp))
                Text(
                    text = if (compact) (if (cartCount > 9) "9+" else "$cartCount")
                    else (if (cartCount > 9) "9+ items" else "$cartCount items"),
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp
                    ),
                    color = Color(0xFF09090B)
                )
            }
        }
    } else {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(SurfaceWhite)
                .border(1.dp, Line, CircleShape)
                .clickable { onOpenCart() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Rounded.ShoppingCart,
                contentDescription = "Cart",
                tint = Ink,
                modifier = Modifier.size(19.dp)
            )
        }
    }
}

/** The signature dashboard moment: one confident surface, one action. */
@Composable
private fun PremiumStoreHero(onOpenShop: () -> Unit) {
    val shape = RoundedCornerShape(22.dp)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp)
            .defaultMinSize(minHeight = 188.dp)
            .clip(shape)
            .clickable(onClick = onOpenShop),
        shape = shape,
        color = BentoDark,
        border = BorderStroke(1.dp, Line)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.linearGradient(listOf(Color(0xFF141418), Color(0xFF0C0C0F))))
        ) {
            // Quiet decorative rings give the hero depth without adding another colour.
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 38.dp, y = (-34).dp)
                    .size(172.dp)
                    .clip(CircleShape)
                    .border(1.dp, LuxeGold.copy(alpha = 0.10f), CircleShape)
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 72.dp, y = 4.dp)
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(LuxeGold.copy(alpha = 0.05f))
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(LuxeGold)
                    )
                    Spacer(Modifier.width(7.dp))
                    Text(
                        text = "PRINCE SANITARY  ·  SAHARANPUR",
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 0.8.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = Color(0xFF9E9EA4)
                    )
                }

                Column(modifier = Modifier.fillMaxWidth().padding(end = 68.dp)) {
                    Text(
                        text = "Save Up to 25% On\nSanitary & Plumbing",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp,
                            lineHeight = 25.sp
                        ),
                        color = Color.White
                    )
                    Spacer(Modifier.height(5.dp))
                    Text(
                        text = "Direct wholesale rates • Express site delivery",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF9E9EA4)
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = Color.White,
                    modifier = Modifier.clickable(onClick = onOpenShop)
                ) {
                    Row(
                        modifier = Modifier.padding(start = 14.dp, end = 10.dp, top = 8.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Shop Catalog",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF09090B)
                            )
                        )
                        Spacer(Modifier.width(6.dp))
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = Color(0xFF09090B)
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 20.dp)
                    .size(56.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFF222226))
                    .border(1.dp, Color(0xFF33333A), RoundedCornerShape(18.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Storefront,
                    contentDescription = null,
                    modifier = Modifier.size(29.dp),
                    tint = LuxeGold
                )
            }
        }
    }
}

@Composable
private fun PremiumQuickActions(
    onOpenServices: () -> Unit,
    onOpenOrders: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        PremiumActionCard(
            modifier = Modifier.weight(1.2f),
            eyebrow = "SERVICES",
            title = "Book Plumber",
            icon = Icons.Rounded.Build,
            onClick = onOpenServices,
            featured = true
        )
        PremiumActionCard(
            modifier = Modifier.weight(1f),
            eyebrow = "ACTIVITY",
            title = "My Orders",
            icon = Icons.Rounded.History,
            onClick = onOpenOrders,
            featured = false
        )
    }
}

@Composable
private fun PremiumActionCard(
    modifier: Modifier,
    eyebrow: String,
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    featured: Boolean
) {
    val shape = RoundedCornerShape(20.dp)
    Surface(
        modifier = modifier
            .defaultMinSize(minHeight = 96.dp)
            .clip(shape)
            .clickable(onClick = onClick),
        shape = shape,
        color = BentoDark,
        border = BorderStroke(1.dp, Line)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(15.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = eyebrow,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.8.sp
                    ),
                    color = LuxeGold
                )
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(LuxeGoldSurface),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(17.dp),
                        tint = LuxeGold
                    )
                }
            }
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = if (featured) "Verified experts in Saharanpur" else "Live tracking & receipts",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF9E9EA4),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Aesthetic Product Shelf: Pure Category Name + Strict Category Filtering + Ultra Cards
 */
@Composable
private fun AestheticProductShelf(
    title: String,
    subtitle: String,
    badgeText: String,
    badgeColor: Color,
    products: List<Product>,
    cartItems: List<com.princesanitary.grahak.data.model.CartItem>,
    onProductClick: (Product) -> Unit,
    onAddToCart: (Product) -> Unit,
    onUpdateQuantity: (String, Int) -> Unit,
    onSeeAll: () -> Unit,
    isLarge: Boolean = false,
    showDiscount: Boolean = false,
    isCeramicsShelf: Boolean = false,
    isBestDealsShelf: Boolean = false
) {
    if (products.isEmpty()) return

    Column {
        // Shelf Header: Pure category name without brands
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 17.5.sp
                        ),
                        color = Ink
                    )
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        shape = CircleShape,
                        color = badgeColor.copy(alpha = 0.14f),
                        border = BorderStroke(0.5.dp, badgeColor.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = badgeText,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 8.5.sp,
                                letterSpacing = 0.5.sp
                            ),
                            color = badgeColor,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp)
                        )
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = badgeColor.copy(alpha = 0.1f),
                border = BorderStroke(0.8.dp, badgeColor.copy(alpha = 0.3f)),
                modifier = Modifier.clickable { onSeeAll() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "See all",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        ),
                        color = badgeColor
                    )
                    Spacer(Modifier.width(3.dp))
                    Icon(
                        Icons.AutoMirrored.Rounded.ArrowForward,
                        contentDescription = null,
                        tint = badgeColor,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Shelf Horizontal Scroll (LazyRow recycles off-screen cards for 120 FPS performance)
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(products, key = { it.id }) { product ->
                val qtyInCart = cartItems.find { it.productId == product.id }?.quantity ?: 0
                AestheticProductCard(
                    product = product,
                    quantityInCart = qtyInCart,
                    cardWidth = if (isLarge) 168.dp else 148.dp,
                    imageHeight = if (isLarge) 130.dp else 108.dp,
                    showDiscount = showDiscount,
                    isCeramicsShelf = isCeramicsShelf,
                    isBestDealsShelf = isBestDealsShelf,
                    onClick = { onProductClick(product) },
                    onAddToCart = { onAddToCart(product) },
                    onUpdateQuantity = { newQty -> onUpdateQuantity(product.id, newQty) }
                )
            }
        }
    }
}

/**
 * Ultra-Aesthetic Product Card:
 * - High-end physical feel with rounded 20dp geometry (Inspired by Chitato / Zemart Pinterest reference)
 * - Soft off-white / dark-zinc image container
 * - Optional discount badge on top-left (only for Best Sellers & Ceramics)
 * - Size capsule on top-right (e.g. 1/2", 1", 24x18")
 * - Clean brand pill on bottom-left
 * - Multi-line clear title & specification tag
 * - Bold wholesale price with striking retail MRP strikethrough
 * - Tactile, interactive ADD button or quantity stepper
 */
@Composable
private fun AestheticProductCard(
    product: Product,
    quantityInCart: Int,
    cardWidth: androidx.compose.ui.unit.Dp = 168.dp,
    imageHeight: androidx.compose.ui.unit.Dp = 130.dp,
    showDiscount: Boolean = false,
    isCeramicsShelf: Boolean = false,
    isBestDealsShelf: Boolean = false,
    onClick: () -> Unit,
    onAddToCart: () -> Unit,
    onUpdateQuantity: (Int) -> Unit
) {
    val pricing = remember(product.id, isCeramicsShelf, isBestDealsShelf) {
        calculateProductPricing(
            product = product,
            isCeramicsShelf = isCeramicsShelf,
            isBestDealsShelf = isBestDealsShelf
        )
    }

    Card(
        modifier = Modifier
            .width(cardWidth)
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        border = BorderStroke(1.dp, Line),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column {
            // Product Image Stage
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(imageHeight)
                    .background(if (isDarkThemeActive) Color(0xFF16161B) else Color(0xFFF6F7F9)),
                contentAlignment = Alignment.Center
            ) {
                if (product.mediaUrls.isNotEmpty()) {
                    AsyncImage(
                        model = product.mediaUrls.first(),
                        contentDescription = product.name,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(10.dp),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(SurfaceWhite),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Rounded.Storefront,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // Discount Pill on Top-Left (Matching user rules: only when showDiscountTag is true)
                if (pricing.showDiscountTag) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp),
                        shape = RoundedCornerShape(6.dp),
                        color = LuxeGold
                    ) {
                        Text(
                            text = pricing.tagText,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 8.5.sp
                            ),
                            color = Color(0xFF09090B),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.5.dp)
                        )
                    }
                }

                // Prominent Product Size Tag on Image top-right (e.g. 1/2", 1", 24x18")
                if (product.size.isNotBlank()) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp),
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xCC000000),
                        border = BorderStroke(0.6.dp, Color.White.copy(alpha = 0.25f))
                    ) {
                        Text(
                            text = product.size,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp
                            ),
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                }

                // Brand Pill at bottom-start with gold bullet dot
                if (product.brand.isNotBlank()) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(6.dp),
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xEE111116)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(4.5.dp)
                                    .clip(CircleShape)
                                    .background(LuxeGold)
                            )
                            Spacer(Modifier.width(3.dp))
                            Text(
                                text = product.brand.uppercase(),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 7.5.sp,
                                    letterSpacing = 0.5.sp
                                ),
                                color = Color.White
                            )
                        }
                    }
                }
            }

            // Product Details Area
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = product.name,
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

                // Item Type Specs
                val subText = product.itemType.ifBlank {
                    ProductCategory.fromId(product.categoryId)?.displayName ?: product.categoryId
                }
                Text(
                    text = "$subText • Per ${product.unit}",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.5.sp),
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(10.dp))

                // Price and Stepper/Add Button
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
                                    fontWeight = FontWeight.ExtraBold,
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

                    Spacer(Modifier.width(4.dp))

                    // Stepper or ADD Button (Pinterest Reference & High-contrast style)
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
                                    .clickable { onUpdateQuantity(quantityInCart - 1) },
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
                                    .clickable { onUpdateQuantity(quantityInCart + 1) },
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
                                .clickable(onClick = onAddToCart)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Rounded.Add,
                                    contentDescription = null,
                                    tint = Ink,
                                    modifier = Modifier.size(14.dp)
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

/**
 * Bottom Trust Bento: 3-column micro trust signals
 */
@Composable
private fun BottomTrustBento() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp),
        shape = RoundedCornerShape(20.dp),
        color = SurfaceWhite,
        border = BorderStroke(1.dp, Line)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(PaperDim),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.LocalShipping,
                        contentDescription = null,
                        tint = LuxeGold,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "Direct Delivery From Store",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp
                        ),
                        color = Ink
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "Prince Sanitary Store • Express site dispatch in Saharanpur",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                        color = TextSecondary
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Trust item 1
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = LuxeGold,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "100% Genuine",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = Ink
                    )
                }

                // Trust item 2
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = LuxeGold,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "Wholesale Rates",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = Ink
                    )
                }

                // Trust item 3
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = LuxeGold,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "Express Site Drop",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = Ink
                    )
                }
            }
        }
    }
}

@Composable
private fun FestiveGreetingBanner(
    storeSettings: StoreSettings,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFF16161B),
        border = BorderStroke(1.dp, Color(0xFF27272A))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color(0xFF222228))
                        .border(1.dp, LuxeGold.copy(alpha = 0.35f), CircleShape)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "✨ " + storeSettings.festivalTag.trim(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.5.sp,
                            letterSpacing = 0.8.sp
                        ),
                        color = LuxeGold
                    )
                }

                Text(
                    text = storeSettings.ownerBusinessName.ifBlank { "PRINCE SANITARY" }.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        letterSpacing = 1.sp
                    ),
                    color = Color(0xFF71717A)
                )
            }

            Spacer(Modifier.height(10.dp))

            Text(
                text = storeSettings.festivalHeadline,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    lineHeight = 21.sp
                ),
                color = Color.White
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = "Warm festive greetings & exclusive store discounts for your home.",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 11.5.sp
                ),
                color = Color(0xFFA1A1AA)
            )
        }
    }
}
