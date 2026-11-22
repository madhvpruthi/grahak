package com.princesanitary.grahak.ui.owner

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.os.Build
import android.view.HapticFeedbackConstants
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.ui.platform.LocalView
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Locale
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Chat
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.princesanitary.grahak.data.model.*
import com.princesanitary.grahak.di.appViewModel
import com.princesanitary.grahak.ui.components.*
import com.princesanitary.grahak.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.foundation.horizontalScroll
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.text.KeyboardOptions
import java.io.ByteArrayOutputStream

val SANITARY_CATEGORIES = listOf(
    "ceramics" to "Wash Basins & Ceramics",
    "seats" to "Toilets & Commode Seats",
    "taps" to "Taps & Faucets",
    "showers" to "Showers & Bath",
    "mirrors" to "Mirrors & Vanities",
    "pipes" to "Pipes & Plumbing",
    "fittings" to "Pipe Fittings",
    "tanks" to "Water Tanks",
    "drains" to "Drains & Traps",
    "hardware" to "Adhesives & Hardware",
    "tools" to "Tools & Equipment"
)

val SANITARY_ITEM_TYPES = listOf(
    "Wash Basin", "Toilet Seat / Commode", "Tap / Faucet", "Overhead Shower", "Health Faucet",
    "LED Mirror / Vanity", "CPVC Pipe", "UPVC Pipe", "PVC / SWR Pipe", "Pipe Fitting",
    "Brass Ball Valve", "Water Storage Tank", "Waste Coupling / Trap", "Solvent Cement", "Teflon Tape",
    "Brass", "Ceramic", "Stainless Steel 304", "ABS Polymer"
)

val SANITARY_UNITS = listOf(
    "Piece", "Set", "Length (3m)", "Length (6m)", "Metre", "Box", "Packet", "Kg", "Roll"
)

val COMMON_SANITARY_SIZES = listOf(
    "1/2\"", "3/4\"", "1\"", "1.25\"", "1.5\"", "2\"", "2.5\"", "3\"", "4\"",
    "Table Top", "Wall Hung", "Counter Top", "One Piece", "Western EWC", "Orissa Pan",
    "18x24\"", "24x30\"", "500 Litres", "1000 Litres", "1500 Litres", "2000 Litres",
    "Standard", "Universal"
)

val POPULAR_BRANDS = listOf(
    "Jaquar", "Cera", "Hindware", "Parryware", "Supreme", "Astral", "Ashirvad", "Finolex", "Sintex", "Prince", "Kajaria", "Generic"
)

/**
 * Main Shell for Store Owner.
 * Has primary focus on Shop Operations & Catalog, with bottom floating dock.
 */
@Composable
fun OwnerMainScreen(
    onLogout: () -> Unit,
    viewModel: OwnerViewModel = appViewModel()
) {
    val isPinUnlocked by viewModel.isPinUnlocked.collectAsStateWithLifecycle()

    if (!isPinUnlocked) {
        OwnerSecurityPinPrompt(
            viewModel = viewModel,
            onLogout = onLogout
        )
        return
    }

    var selectedTab by remember { mutableStateOf(0) } // 0: Overview, 1: Products, 2: Orders, 3: Plumbers, 4: Payouts

    val context = LocalContext.current
    var lastBackPressTime by remember { mutableLongStateOf(0L) }

    BackHandler(enabled = true) {
        if (selectedTab != 0) {
            selectedTab = 0
        } else {
            val now = System.currentTimeMillis()
            if (now - lastBackPressTime < 2000) {
                (context as? Activity)?.finish()
            } else {
                lastBackPressTime = now
                Toast.makeText(context, "Press back again to quit the app", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val navItems = listOf(
        NavItem("overview", "Store", Icons.Rounded.Storefront),
        NavItem("products", "Catalog", Icons.Rounded.Inventory2),
        NavItem("orders", "Orders", Icons.Rounded.LocalShipping),
        NavItem("plumbers", "Plumbers", Icons.Rounded.Engineering),
        NavItem("payouts", "Payouts", Icons.Rounded.AccountBalanceWallet)
    )

    Scaffold(
        containerColor = Paper,
        bottomBar = {
            GrahakBottomNav(
                items = navItems,
                currentRoute = when (selectedTab) {
                    0 -> "overview"
                    1 -> "products"
                    2 -> "orders"
                    3 -> "plumbers"
                    else -> "payouts"
                },
                onSelect = { route ->
                    selectedTab = when (route) {
                        "overview" -> 0
                        "products" -> 1
                        "orders" -> 2
                        "plumbers" -> 3
                        else -> 4
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = padding.calculateBottomPadding())
        ) {
            when (selectedTab) {
                0 -> OwnerOverviewTab(
                    viewModel = viewModel,
                    onNavigateToTab = { selectedTab = it },
                    onLogout = onLogout
                )
                1 -> OwnerProductsManagementTab(viewModel = viewModel)
                2 -> OwnerOrdersManagementTab(viewModel = viewModel)
                3 -> OwnerPlumbersManagementTab(viewModel = viewModel)
                4 -> OwnerPayoutsManagementTab(viewModel = viewModel)
            }
        }
    }
}

// =========================================================================
// 0. STORE OVERVIEW DASHBOARD
// =========================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OwnerOverviewTab(
    viewModel: OwnerViewModel,
    onNavigateToTab: (Int) -> Unit,
    onLogout: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    var confirmLogout by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    val view = LocalView.current
    val context = LocalContext.current

    LaunchedEffect(isRefreshing) {
        if (isRefreshing) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
            } else {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            }
        }
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
            } else {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            }
            viewModel.refreshAllData()
        },
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 20.dp, vertical = 14.dp)
        ) {
            // Top Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Prince Sanitary Store",
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                            color = Ink
                        )
                        Spacer(Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(BentoYellow)
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "OWNER",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 9.sp
                                ),
                                color = BentoOnVibrant
                            )
                        }
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "Welcome Madhav • Store Operations Hub",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircleIconButton(
                        icon = Icons.Rounded.Tune,
                        onClick = { showSettingsDialog = true },
                        contentDescription = "Store Settings"
                    )
                    CircleIconButton(
                        icon = Icons.Rounded.Logout,
                        onClick = { confirmLogout = true },
                        contentDescription = "Log Out"
                    )
                }
            }

        Spacer(Modifier.height(16.dp))

        // Store Status Quick Toggle (Open / Closed)
        val storeSettings by viewModel.storeSettings.collectAsStateWithLifecycle()
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = if (storeSettings.isStoreOpen) BentoGreen.copy(alpha = 0.12f) else StatusError.copy(alpha = 0.12f),
            border = BorderStroke(1.dp, if (storeSettings.isStoreOpen) BentoGreen.copy(alpha = 0.35f) else StatusError.copy(alpha = 0.35f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (storeSettings.isStoreOpen) BentoGreen else StatusError)
                    )
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (storeSettings.isStoreOpen) "STORE IS OPEN" else "STORE IS CLOSED",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = 11.sp,
                                letterSpacing = 0.8.sp
                            ),
                            color = if (storeSettings.isStoreOpen) BentoGreen else StatusError
                        )
                        Text(
                            text = if (storeSettings.isStoreOpen) {
                                "Express dispatch active • Customer orders dispatched immediately"
                            } else {
                                "Next delivery: ${storeSettings.nextDispatchSchedule.ifBlank { "Tomorrow 10:00 AM" }}"
                            },
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = TextSecondary
                        )
                    }
                }

                Switch(
                    checked = storeSettings.isStoreOpen,
                    onCheckedChange = { isOpen ->
                        viewModel.updateStoreSettings(storeSettings.copy(isStoreOpen = isOpen))
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = BentoGreen,
                        checkedTrackColor = BentoGreen.copy(alpha = 0.35f),
                        uncheckedThumbColor = StatusError,
                        uncheckedTrackColor = StatusError.copy(alpha = 0.35f)
                    )
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        val data = uiState as? OwnerUiState.Success
        val productCount = data?.products?.size ?: 0
        val pendingOrders = data?.orders?.count { it.status == OrderStatus.PLACED || it.status == OrderStatus.CONFIRMED } ?: 0
        val unverifiedPlumbers = data?.plumbers?.count { !it.kyc.verified } ?: 0
        val pendingPayoutsCount = data?.pendingPayouts?.size ?: 0

        // Hero Metric Bento - Clicking opens Catalog tab
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(26.dp))
                .background(BentoGreen)
                .clickable { onNavigateToTab(1) }
                .padding(22.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ACTIVE STORE METRICS",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5f.sp
                        ),
                        color = BentoOnVibrant.copy(alpha = 0.8f)
                    )
                    Icon(Icons.Rounded.Store, null, tint = BentoOnVibrant, modifier = Modifier.size(20.dp))
                }

                Spacer(Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text(
                            text = "$productCount Products",
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                            color = BentoOnVibrant
                        )
                        Text(
                            text = "Catalog published at wholesale rates",
                            style = MaterialTheme.typography.bodySmall,
                            color = BentoOnVibrant.copy(alpha = 0.85f)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Quick Action Bento Cards Grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Left Bento: Orders
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(145.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(BentoPink)
                    .clickable { onNavigateToTab(2) }
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("DISPATCH", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = BentoOnVibrant.copy(alpha = 0.75f))
                        Icon(Icons.Rounded.LocalShipping, null, tint = BentoOnVibrant, modifier = Modifier.size(18.dp))
                    }
                    Column {
                        Text("$pendingOrders Pending", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = BentoOnVibrant)
                        Text("Site drop orders", style = MaterialTheme.typography.bodySmall, color = BentoOnVibrant.copy(alpha = 0.8f))
                    }
                }
            }

            // Right Bento: Plumbers
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(145.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(BentoBlue)
                    .clickable { onNavigateToTab(3) }
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("KYC APPROVALS", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = BentoOnVibrant.copy(alpha = 0.75f))
                        Icon(Icons.Rounded.Engineering, null, tint = BentoOnVibrant, modifier = Modifier.size(18.dp))
                    }
                    Column {
                        Text("$unverifiedPlumbers Need Review", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = BentoOnVibrant)
                        Text("Plumber partner pool", style = MaterialTheme.typography.bodySmall, color = BentoOnVibrant.copy(alpha = 0.8f))
                    }
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        // Digital Parchis from Plumbers Card
        val parchiCount = data?.estimates?.size ?: 0
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(LuxeGoldSurface)
                .border(1.dp, LuxeGoldBorder, RoundedCornerShape(22.dp))
                .clickable { onNavigateToTab(2) }
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(LuxeGold.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Description, null, tint = LuxeGold, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "$parchiCount Digital Parchis from Plumbers",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Ink
                    )
                    Text(
                        text = "Material estimates prepared at customer job sites",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
                Icon(Icons.Rounded.ChevronRight, null, tint = Ink, modifier = Modifier.size(20.dp))
            }
        }

        Spacer(Modifier.height(14.dp))

        // Payouts Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(BentoYellow)
                .clickable { onNavigateToTab(4) }
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(BentoOnVibrant.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.AccountBalanceWallet, null, tint = BentoOnVibrant, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "$pendingPayoutsCount Pending UPI Payouts",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = BentoOnVibrant
                    )
                    Text(
                        text = "Settle commissions & repair balances to plumbers",
                        style = MaterialTheme.typography.bodySmall,
                        color = BentoOnVibrant.copy(alpha = 0.85f)
                    )
                }
                Icon(Icons.Rounded.ChevronRight, null, tint = BentoOnVibrant, modifier = Modifier.size(20.dp))
            }
        }

        Spacer(Modifier.height(24.dp))

        // Quick Catalog Shortcut
        Text("Quick Management", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Ink)
        Spacer(Modifier.height(12.dp))

        GrahakCard(contentPadding = 8.dp) {
            ProfileRow(
                icon = Icons.Rounded.AddCircle,
                title = "Add Sanitary Item",
                subtitle = "Pipes, tanks, fittings, ceramics with background remover",
                onClick = { onNavigateToTab(1) }
            )
            RowDivider()
            ProfileRow(
                icon = Icons.Rounded.Inventory,
                title = "Stock & Wholesale Rates",
                subtitle = "Manage inventory and store prices",
                onClick = { onNavigateToTab(1) }
            )
            RowDivider()
            ProfileRow(
                icon = Icons.Rounded.Tune,
                title = "Store Settings & Coupons",
                subtitle = "Delivery charges, owner UPI ID, plumber commission & coupons",
                onClick = { showSettingsDialog = true }
            )
        }

        Spacer(Modifier.height(30.dp))
    }
    }

    if (showSettingsDialog) {
        OwnerStoreSettingsDialog(
            viewModel = viewModel,
            onDismiss = { showSettingsDialog = false }
        )
    }

    if (confirmLogout) {
        AlertDialog(
            onDismissRequest = { confirmLogout = false },
            title = { Text("Log out of Store Owner Account?") },
            text = { Text("You can sign back in at any time with your registered store owner credentials.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmLogout = false
                    onLogout()
                }) {
                    Text("Log out", color = StatusError)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmLogout = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}

// =========================================================================
// 1. PRODUCTS MANAGEMENT TAB (Sanitary Specific + Background Remover)
// =========================================================================

@Composable
private fun OwnerProductsManagementTab(
    viewModel: OwnerViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showDialog by remember { mutableStateOf(false) }
    var editingProduct by remember { mutableStateOf<Product?>(null) }
    var selectedCategoryFilter by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }

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
            Column {
                Text("Sanitary Catalog", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold), color = Ink)
                Text("Pipes, tanks, fittings & ceramics", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }

            Button(
                onClick = {
                    editingProduct = null
                    showDialog = true
                },
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = BentoGreen)
            ) {
                Icon(Icons.Rounded.Add, null, modifier = Modifier.size(18.dp), tint = BentoOnVibrant)
                Spacer(Modifier.width(6.dp))
                Text("Add Item", color = BentoOnVibrant, fontWeight = FontWeight.Bold)
            }
        }

        // Search Bar for Catalog
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 4.dp),
            shape = RoundedCornerShape(16.dp),
            color = SurfaceWhite,
            border = BorderStroke(1.dp, Line)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.Search, null, tint = TextSecondary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                androidx.compose.foundation.text.BasicTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = Ink),
                    decorationBox = { innerTextField ->
                        if (searchQuery.isEmpty()) {
                            Text(
                                "Search item name, #PS item code, brand...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextTertiary
                            )
                        }
                        innerTextField()
                    }
                )
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Rounded.Close, null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        Spacer(Modifier.height(4.dp))

        // Category Filter Row (Horizontally Scrollable)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PillChip(
                text = "All",
                selected = selectedCategoryFilter == null,
                onClick = { selectedCategoryFilter = null },
                activeColor = BentoPink,
                activeTextColor = BentoOnVibrant
            )
            SANITARY_CATEGORIES.forEach { (catId, catName) ->
                PillChip(
                    text = catName,
                    selected = selectedCategoryFilter == catId,
                    onClick = { selectedCategoryFilter = catId },
                    activeColor = BentoPink,
                    activeTextColor = BentoOnVibrant
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        val data = uiState as? OwnerUiState.Success
        val allProducts = data?.products ?: emptyList()
        val q = searchQuery.trim().lowercase()
        val filtered = allProducts.filter { p ->
            val matchesCat = selectedCategoryFilter == null || p.categoryId == selectedCategoryFilter
            val matchesQuery = q.isEmpty() ||
                p.name.lowercase().contains(q) ||
                p.brand.lowercase().contains(q) ||
                p.company.lowercase().contains(q) ||
                p.itemType.lowercase().contains(q) ||
                p.size.lowercase().contains(q) ||
                p.displayItemCode.lowercase().contains(q)
            matchesCat && matchesQuery
        }

        if (filtered.isEmpty()) {
            EmptyState(
                icon = Icons.Rounded.Inventory2,
                title = "No products found",
                message = "Tap 'Add Item' above to add sanitaryware with photos, size, company, and wholesale rates."
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 40.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(filtered, key = { it.id }) { product ->
                    ProductManagementCard(
                        product = product,
                        onEdit = {
                            editingProduct = product
                            showDialog = true
                        },
                        onTogglePublish = { id, pub ->
                            viewModel.toggleProductPublish(id, pub)
                            android.widget.Toast.makeText(
                                context,
                                if (pub) "Product published to customer shop" else "Product hidden from customer shop",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        },
                        onToggleFeatured = { id, feat ->
                            viewModel.toggleProductFeatured(id, feat)
                            android.widget.Toast.makeText(
                                context,
                                if (feat) "Showcased on main customer dashboard" else "Removed from dashboard hero row",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        },
                        onDelete = { id -> viewModel.deleteProduct(id) }
                    )
                }
            }
        }
    }

    if (showDialog) {
        ComprehensiveProductDialog(
            existing = editingProduct,
            existingProducts = (uiState as? OwnerUiState.Success)?.products ?: emptyList(),
            onDismiss = { showDialog = false },
            onSave = { product, existingUrls, newBytesList, useAiCutout, callback ->
                viewModel.saveProductWithMultipleImages(product, existingUrls, newBytesList, useAiCutout) { success, err ->
                    callback(success, err)
                }
            }
        )
    }
}

@Composable
private fun ProductManagementCard(
    product: Product,
    onEdit: () -> Unit,
    onTogglePublish: (String, Boolean) -> Unit,
    onToggleFeatured: (String, Boolean) -> Unit,
    onDelete: (String) -> Unit
) {
    GrahakCard(cornerRadius = 20.dp, contentPadding = 16.dp) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            // Product photo / thumbnail
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(PaperDim),
                contentAlignment = Alignment.Center
            ) {
                if (product.mediaUrls.isNotEmpty()) {
                    AsyncImage(
                        model = product.mediaUrls.first(),
                        contentDescription = product.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(Icons.Rounded.Plumbing, null, tint = TextTertiary, modifier = Modifier.size(30.dp))
                }
            }

            Spacer(Modifier.width(14.dp))

            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = LuxeGoldSurface,
                        border = BorderStroke(1.dp, LuxeGoldBorder),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = product.displayItemCode,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.5.sp,
                                color = LuxeGold
                            ),
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (product.isPublished) BentoGreen.copy(alpha = 0.15f) else StatusError.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (product.isPublished) "ACTIVE" else "HIDDEN",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 8.sp),
                            color = if (product.isPublished) BentoGreen else StatusError
                        )
                    }
                    if (product.isFeatured) {
                        Spacer(Modifier.width(4.dp))
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(BentoYellow.copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "HERO",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 8.sp),
                                color = BentoYellowDark
                            )
                        }
                    }
                }

                Spacer(Modifier.height(2.dp))

                // Brand, Type, Category badges
                Text(
                    text = buildString {
                        if (product.company.isNotBlank()) append("${product.company} • ")
                        else if (product.brand.isNotBlank()) append("${product.brand} • ")
                        if (product.itemType.isNotBlank()) append("${product.itemType} • ")
                        if (product.size.isNotBlank()) append("${product.size} • ")
                        append(ProductCategory.fromId(product.categoryId)?.displayName ?: product.categoryId)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${formatRupees(product.unitPrice)} / ${product.unit}",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = BentoGreen
                    )

                    Text(
                        text = "Stock: ${product.stockLevel}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (product.stockLevel > 0) TextSecondary else StatusError
                    )
                }
            }

            // Action icons
            Column(horizontalAlignment = Alignment.End) {
                IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Rounded.Edit, null, tint = Ink, modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = { onToggleFeatured(product.id, !product.isFeatured) }, modifier = Modifier.size(32.dp)) {
                    Icon(
                        if (product.isFeatured) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                        contentDescription = "Feature on Dashboard",
                        tint = if (product.isFeatured) BentoYellow else TextTertiary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(onClick = { onTogglePublish(product.id, !product.isPublished) }, modifier = Modifier.size(32.dp)) {
                    Icon(
                        if (product.isPublished) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                        contentDescription = if (product.isPublished) "Hide product" else "Unhide product",
                        tint = if (product.isPublished) TextSecondary else BentoGreen,
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(onClick = { onDelete(product.id) }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Rounded.Delete, null, tint = StatusError, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

// =========================================================================
// COMPREHENSIVE PRODUCT DIALOG (SMART ITEM CODE & CAMERA + GALLERY)
// =========================================================================

private fun generateSmartItemCode(category: String, name: String, existingProducts: List<Product>): String {
    val cleanName = name.lowercase().trim()
    val prefix = when {
        cleanName.contains("elbow") || cleanName.contains("bend") -> "EL"
        cleanName.contains("tee") -> "TE"
        cleanName.contains("tap") || cleanName.contains("faucet") || category == "taps" -> "TP"
        cleanName.contains("tank") || category == "water_tanks" -> "WT"
        category == "ceramics" || cleanName.contains("basin") || cleanName.contains("commode") -> "CR"
        category == "fittings" -> "FT"
        category == "pipes" -> "PP"
        category == "sanitaryware" -> "ST"
        else -> "IT"
    }

    val usedNumbers = existingProducts.mapNotNull { p ->
        val code = p.itemNumber.trim()
        val regex = Regex("PS-$prefix-(\\d+)", RegexOption.IGNORE_CASE)
        val match = regex.find(code)
        match?.groupValues?.get(1)?.toIntOrNull()
    }.toSet()

    var nextNum = 1
    while (usedNumbers.contains(nextNum)) {
        nextNum++
    }
    return "PS-$prefix-%02d".format(nextNum)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ComprehensiveProductDialog(
    existing: Product?,
    existingProducts: List<Product> = emptyList(),
    onDismiss: () -> Unit,
    onSave: (Product, List<String>, List<ByteArray>, Boolean, (Boolean, String?) -> Unit) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var name by remember { mutableStateOf(existing?.name.orEmpty()) }
    var categoryId by remember { mutableStateOf(existing?.categoryId?.ifEmpty { "pipes" } ?: "pipes") }
    var company by remember { mutableStateOf(existing?.company?.ifEmpty { existing.brand } ?: existing?.brand.orEmpty()) }
    var itemType by remember { mutableStateOf(existing?.itemType?.ifEmpty { "CPVC" } ?: "CPVC") }
    var size by remember { mutableStateOf(existing?.size.orEmpty()) }
    var unit by remember { mutableStateOf(existing?.unit?.ifEmpty { "Piece" } ?: "Piece") }

    var isCodeManuallyEdited by remember { mutableStateOf(existing?.itemNumber?.isNotBlank() == true) }
    var itemNumber by remember {
        mutableStateOf(
            existing?.itemNumber?.ifBlank { generateSmartItemCode(categoryId, name, existingProducts) }
                ?: generateSmartItemCode(categoryId, name, existingProducts)
        )
    }

    LaunchedEffect(categoryId, name) {
        if (!isCodeManuallyEdited && existing == null) {
            itemNumber = generateSmartItemCode(categoryId, name, existingProducts)
        }
    }

    var priceText by remember { mutableStateOf(if (existing != null) existing.unitPrice.toString() else "") }
    var stockText by remember { mutableStateOf(if (existing != null) existing.stockLevel.toString() else "100") }
    var isPublished by remember { mutableStateOf(existing?.isPublished ?: true) }
    var isFeatured by remember { mutableStateOf(existing?.isFeatured ?: false) }

    // Dropdown states
    var unitExpanded by remember { mutableStateOf(false) }
    var sizeExpanded by remember { mutableStateOf(false) }

    // Saving and Error states
    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Multi-photo management (up to 4 pictures limit per item)
    var existingUrls by remember { mutableStateOf(existing?.mediaUrls.orEmpty().take(4)) }
    var newPhotoBitmaps by remember { mutableStateOf<List<Bitmap>>(emptyList()) }

    val cameraPhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            val availableSlots = 4 - (existingUrls.size + newPhotoBitmaps.size)
            if (availableSlots > 0) {
                val maxDim = 1024
                val w = bitmap.width
                val h = bitmap.height
                val finalBmp = if (w > maxDim || h > maxDim) {
                    val ratio = Math.min(maxDim.toFloat() / w, maxDim.toFloat() / h)
                    Bitmap.createScaledBitmap(bitmap, (w * ratio).toInt(), (h * ratio).toInt(), true)
                } else bitmap
                newPhotoBitmaps = (newPhotoBitmaps + finalBmp).take(4 - existingUrls.size)
            }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            try {
                cameraPhotoLauncher.launch(null)
            } catch (e: Exception) {
                android.widget.Toast.makeText(context, "Could not open camera: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
            }
        } else {
            android.widget.Toast.makeText(context, "Camera permission needed to take picture", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    fun openCameraSafely() {
        val hasCam = androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.CAMERA
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        if (hasCam) {
            try {
                cameraPhotoLauncher.launch(null)
            } catch (e: Exception) {
                android.widget.Toast.makeText(context, "Could not open camera: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
            }
        } else {
            cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
        }
    }

    val multiplePhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            val availableSlots = 4 - (existingUrls.size + newPhotoBitmaps.size)
            if (availableSlots > 0) {
                val toLoad = uris.take(availableSlots)
                coroutineScope.launch {
                    val loaded = mutableListOf<Bitmap>()
                    for (u in toLoad) {
                        loadOriginalBitmap(context, u)?.let { bmp ->
                            val maxDim = 1024
                            val w = bmp.width
                            val h = bmp.height
                            val finalBmp = if (w > maxDim || h > maxDim) {
                                val ratio = Math.min(maxDim.toFloat() / w, maxDim.toFloat() / h)
                                Bitmap.createScaledBitmap(bmp, (w * ratio).toInt(), (h * ratio).toInt(), true)
                            } else bmp
                            loaded.add(finalBmp)
                        }
                    }
                    newPhotoBitmaps = (newPhotoBitmaps + loaded).take(4 - existingUrls.size)
                }
            }
        }
    }

    Dialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.65f))
                .clickable(onClick = { if (!isSaving) onDismiss() }),
            contentAlignment = Alignment.BottomCenter
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.94f)
                    .imePadding()
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                color = Paper
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .width(42.dp)
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(Line)
                    )
                    Spacer(Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (existing == null) "Add Sanitary Product" else "Edit Sanitary Product",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = Ink
                        )
                        IconButton(onClick = onDismiss, enabled = !isSaving) {
                            Icon(Icons.Rounded.Close, "Close", tint = TextSecondary)
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                // Error message banner if upload or save fails
                if (errorMessage != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(StatusError.copy(alpha = 0.12f))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = errorMessage!!,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = StatusError
                        )
                    }
                }

                // Multi-Photo Gallery (Up to 4 pictures per item as requested)
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = SurfaceWhite,
                    border = BorderStroke(1.dp, Line)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Product Pictures",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Ink
                                )
                                Spacer(Modifier.width(6.dp))
                                Surface(
                                    shape = CircleShape,
                                    color = LuxeGoldSurface,
                                    border = BorderStroke(0.6.dp, LuxeGoldBorder)
                                ) {
                                    Text(
                                        text = "${existingUrls.size + newPhotoBitmaps.size}/4",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp
                                        ),
                                        color = LuxeGold,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Max 4 photos",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                        }

                        Spacer(Modifier.height(12.dp))

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Existing Photos from Server
                            itemsIndexed(existingUrls) { idx, url ->
                                Box(
                                    modifier = Modifier
                                        .size(105.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(PaperDim)
                                        .border(1.dp, Line, RoundedCornerShape(14.dp))
                                ) {
                                    AsyncImage(
                                        model = url,
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                    // Remove button
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(4.dp)
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(Color.Black.copy(alpha = 0.7f))
                                            .clickable {
                                                existingUrls = existingUrls.filterIndexed { i, _ -> i != idx }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Rounded.Close,
                                            contentDescription = "Remove",
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }

                            // Newly Selected Bitmaps
                            itemsIndexed(newPhotoBitmaps) { idx, bmp ->
                                Box(
                                    modifier = Modifier
                                        .size(105.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(PaperDim)
                                        .border(1.dp, Line, RoundedCornerShape(14.dp))
                                ) {
                                    Image(
                                        bitmap = bmp.asImageBitmap(),
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                    // "NEW" Tag
                                    Surface(
                                        modifier = Modifier
                                            .align(Alignment.BottomStart)
                                            .padding(4.dp),
                                        shape = RoundedCornerShape(4.dp),
                                        color = EmeraldSignal
                                    ) {
                                        Text(
                                            text = "NEW",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                    // Remove button
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(4.dp)
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(Color.Black.copy(alpha = 0.7f))
                                            .clickable {
                                                newPhotoBitmaps = newPhotoBitmaps.filterIndexed { i, _ -> i != idx }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Rounded.Close,
                                            contentDescription = "Remove",
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }

                            // Add Slots if total < 4
                            if (existingUrls.size + newPhotoBitmaps.size < 4) {
                                // 1. Live Camera Slot
                                item {
                                    Box(
                                        modifier = Modifier
                                            .size(105.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(LuxeGoldSurface)
                                            .border(1.dp, LuxeGoldBorder, RoundedCornerShape(14.dp))
                                            .clickable { openCameraSafely() },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier.padding(8.dp)
                                        ) {
                                            Icon(
                                                Icons.Rounded.PhotoCamera,
                                                contentDescription = "Live Camera",
                                                tint = LuxeGold,
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Spacer(Modifier.height(4.dp))
                                            Text(
                                                text = "Live Camera",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.sp
                                                ),
                                                color = LuxeGold
                                            )
                                            Text(
                                                text = "Take photo",
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                                color = TextSecondary
                                            )
                                        }
                                    }
                                }

                                // 2. Gallery Album Slot
                                item {
                                    Box(
                                        modifier = Modifier
                                            .size(105.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(PaperDim)
                                            .border(1.dp, Line, RoundedCornerShape(14.dp))
                                            .clickable {
                                                multiplePhotoPickerLauncher.launch("image/*")
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier.padding(8.dp)
                                        ) {
                                            Icon(
                                                Icons.Rounded.AddPhotoAlternate,
                                                contentDescription = null,
                                                tint = Ink,
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Spacer(Modifier.height(4.dp))
                                            Text(
                                                text = "+ Gallery",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.sp
                                                ),
                                                color = Ink
                                            )
                                            Text(
                                                text = "(${4 - (existingUrls.size + newPhotoBitmaps.size)} left)",
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                                color = TextSecondary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Visibility / Publish Status Switch
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(PaperDim)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isPublished) "Published in Store" else "Hidden (Draft)",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (isPublished) BentoGreen else StatusError
                        )
                        Text(
                            text = if (isPublished) "Visible to customers & plumbers in shop" else "Hidden from store catalog",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                    Switch(
                        checked = isPublished,
                        onCheckedChange = { isPublished = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = BentoOnVibrant,
                            checkedTrackColor = BentoGreen
                        )
                    )
                }

                // 1. Item Code & Product Name
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = itemNumber,
                        onValueChange = {
                            itemNumber = it
                            isCodeManuallyEdited = true
                        },
                        label = { Text("Code (#PS)") },
                        placeholder = { Text("PS-CR-01") },
                        supportingText = { Text("Auto code: ${itemNumber.ifBlank { "PS-CR-01" }}", fontSize = 10.sp) },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1.15f)
                    )
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Product Name *") },
                        placeholder = { Text("e.g. CPVC Brass Elbow") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(2.0f)
                    )
                }

                // 2. Category Chips
                Text("Category", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = TextSecondary)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SANITARY_CATEGORIES.forEach { (id, label) ->
                        PillChip(
                            text = label,
                            selected = categoryId == id,
                            onClick = { categoryId = id },
                            activeColor = BentoGreen,
                            activeTextColor = BentoOnVibrant
                        )
                    }
                }

                // 3. Company / Brand
                OutlinedTextField(
                    value = company,
                    onValueChange = { company = it },
                    label = { Text("Company / Brand (e.g. Supreme, Astral, Jaquar)") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // 4. Material Type Chips
                Text("Material Type", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = TextSecondary)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SANITARY_ITEM_TYPES.forEach { type ->
                        PillChip(
                            text = type,
                            selected = itemType == type,
                            onClick = { itemType = type },
                            activeColor = BentoBlue,
                            activeTextColor = BentoOnVibrant
                        )
                    }
                }

                // 5. Size (Dropdown + Free Input) & Unit (Dropdown + Free Input)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Size Dropdown Field
                    ExposedDropdownMenuBox(
                        expanded = sizeExpanded,
                        onExpandedChange = { sizeExpanded = it },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = size,
                            onValueChange = { size = it },
                            label = { Text("Size") },
                            placeholder = { Text("e.g. 1\", 1/2\"") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sizeExpanded) },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = sizeExpanded,
                            onDismissRequest = { sizeExpanded = false }
                        ) {
                            COMMON_SANITARY_SIZES.forEach { s ->
                                DropdownMenuItem(
                                    text = { Text(s) },
                                    onClick = {
                                        size = s
                                        sizeExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Unit Dropdown Field
                    ExposedDropdownMenuBox(
                        expanded = unitExpanded,
                        onExpandedChange = { unitExpanded = it },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = unit,
                            onValueChange = { unit = it },
                            label = { Text("Unit") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = unitExpanded) },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = unitExpanded,
                            onDismissRequest = { unitExpanded = false }
                        ) {
                            SANITARY_UNITS.forEach { u ->
                                DropdownMenuItem(
                                    text = { Text(u) },
                                    onClick = {
                                        unit = u
                                        unitExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // 6. Rate & Stock
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = priceText,
                        onValueChange = { priceText = it },
                        label = { Text("Rate (₹ / unit)") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = stockText,
                        onValueChange = { stockText = it },
                        label = { Text("Stock Qty") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f)
                    )
                }

                // 7. Showcase on Dashboard (Luxury & Ceramics Hero Shelf)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(PaperDim)
                        .border(1.dp, Line, RoundedCornerShape(14.dp))
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Star, null, tint = BentoYellow, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "Showcase on Main Dashboard",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = Ink
                            )
                        }
                        Spacer(Modifier.height(2.dp))
                        Text(
                            "Feature in top luxury & ceramics shelf on Customer Home screen",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                    Switch(
                        checked = isFeatured,
                        onCheckedChange = { isFeatured = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = BentoOnVibrant,
                            checkedTrackColor = BentoGreen
                        )
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // Fixed Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    enabled = !isSaving,
                    shape = CircleShape,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                ) {
                    Text("Cancel", color = TextSecondary)
                }
                Button(
                    onClick = {
                        if (isSaving) return@Button
                        errorMessage = null

                        val cleanName = name.trim()
                        if (cleanName.isBlank()) {
                            errorMessage = "Product name cannot be empty. Please enter a valid name."
                            return@Button
                        }
                        val price = priceText.toDoubleOrNull()
                        if (price == null || price <= 0.0) {
                            errorMessage = "Please enter a valid rate (₹ / unit) greater than 0."
                            return@Button
                        }

                        isSaving = true

                        val finalItemCode = itemNumber.trim().ifBlank {
                            generateSmartItemCode(categoryId, cleanName, existingProducts)
                        }

                        val product = (existing ?: Product()).copy(
                            name = cleanName,
                            itemNumber = finalItemCode,
                            brand = company.trim(),
                            company = company.trim(),
                            categoryId = categoryId,
                            itemType = itemType,
                            size = size.trim(),
                            unit = unit.trim(),
                            unitPrice = price,
                            stockLevel = stockText.toIntOrNull() ?: 0,
                            isPublished = isPublished,
                            isFeatured = isFeatured
                        )

                        coroutineScope.launch(Dispatchers.IO) {
                            val newBytesList = mutableListOf<ByteArray>()
                            for (bmp in newPhotoBitmaps) {
                                val stream = java.io.ByteArrayOutputStream()
                                bmp.compress(Bitmap.CompressFormat.JPEG, 92, stream)
                                newBytesList.add(stream.toByteArray())
                            }

                            withContext(Dispatchers.Main) {
                                onSave(product, existingUrls, newBytesList, false) { success, err ->
                                    isSaving = false
                                    if (success) {
                                        onDismiss()
                                    } else {
                                        errorMessage = err ?: "Failed to save product"
                                    }
                                }
                            }
                        }
                    },
                    enabled = !isSaving,
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = BentoGreen),
                    modifier = Modifier
                        .weight(1.5f)
                        .height(50.dp)
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            color = BentoOnVibrant,
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Saving...", color = BentoOnVibrant, fontWeight = FontWeight.Bold)
                    } else {
                        Text("Save to Store", color = BentoOnVibrant, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
    }
}

/**
 * Load bitmap from Uri with sensible max dimension to keep memory and performance optimal.
 */
private suspend fun loadOriginalBitmap(context: android.content.Context, uri: Uri): Bitmap? = withContext(Dispatchers.IO) {
    try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return@withContext null
        val original = android.graphics.BitmapFactory.decodeStream(inputStream) ?: return@withContext null
        val maxDim = 800
        val w = original.width
        val h = original.height
        if (w > maxDim || h > maxDim) {
            val ratio = Math.min(maxDim.toFloat() / w, maxDim.toFloat() / h)
            Bitmap.createScaledBitmap(original, (w * ratio).toInt(), (h * ratio).toInt(), true)
        } else {
            original
        }
    } catch (_: Exception) {
        null
    }
}

/**
 * Convert any bitmap into studio plain white background
 */
private suspend fun convertBitmapToPlainWhite(original: Bitmap): Bitmap = withContext(Dispatchers.IO) {
    val maxDim = 800
    val w = original.width
    val h = original.height
    val scaled = if (w > maxDim || h > maxDim) {
        val ratio = Math.min(maxDim.toFloat() / w, maxDim.toFloat() / h)
        Bitmap.createScaledBitmap(original, (w * ratio).toInt(), (h * ratio).toInt(), true)
    } else {
        original
    }

    val sw = scaled.width
    val sh = scaled.height
    val pixels = IntArray(sw * sh)
    scaled.getPixels(pixels, 0, sw, 0, 0, sw, sh)

    fun colorDist(p1: Int, p2: Int): Double {
        val dr = AndroidColor.red(p1) - AndroidColor.red(p2)
        val dg = AndroidColor.green(p1) - AndroidColor.green(p2)
        val db = AndroidColor.blue(p1) - AndroidColor.blue(p2)
        return Math.sqrt((dr * dr + dg * dg + db * db).toDouble())
    }

    // Sample corner seeds (top-left, top-right, bottom-left, bottom-right)
    val cornerSamples = mutableListOf<Int>()
    val margin = 8.coerceAtMost(sw / 10).coerceAtMost(sh / 10)
    for (y in 0 until margin) {
        for (x in 0 until margin) {
            cornerSamples.add(pixels[y * sw + x])
            cornerSamples.add(pixels[y * sw + (sw - 1 - x)])
            cornerSamples.add(pixels[(sh - 1 - y) * sw + x])
            cornerSamples.add(pixels[(sh - 1 - y) * sw + (sw - 1 - x)])
        }
    }

    // Average corner ambient color
    var sr = 0L; var sg = 0L; var sb = 0L
    cornerSamples.forEach {
        sr += AndroidColor.red(it)
        sg += AndroidColor.green(it)
        sb += AndroidColor.blue(it)
    }
    val avgCornerColor = AndroidColor.rgb(
        (sr / cornerSamples.size.coerceAtLeast(1)).toInt(),
        (sg / cornerSamples.size.coerceAtLeast(1)).toInt(),
        (sb / cornerSamples.size.coerceAtLeast(1)).toInt()
    )

    // Compute edge gradient map
    val gradient = FloatArray(sw * sh)
    for (y in 0 until sh - 1) {
        for (x in 0 until sw - 1) {
            val idx = y * sw + x
            val dRight = colorDist(pixels[idx], pixels[idx + 1])
            val dDown = colorDist(pixels[idx], pixels[idx + sw])
            gradient[idx] = Math.max(dRight, dDown).toFloat()
        }
    }

    // Flood fill background from all 4 borders
    val isBackground = BooleanArray(sw * sh)
    val queue = java.util.ArrayDeque<Int>()

    for (x in 0 until sw) {
        isBackground[x] = true
        queue.add(x)
        isBackground[(sh - 1) * sw + x] = true
        queue.add((sh - 1) * sw + x)
    }
    for (y in 0 until sh) {
        isBackground[y * sw] = true
        queue.add(y * sw)
        isBackground[y * sw + (sw - 1)] = true
        queue.add(y * sw + (sw - 1))
    }

    val dx = intArrayOf(1, -1, 0, 0)
    val dy = intArrayOf(0, 0, 1, -1)
    val edgeStopThreshold = 36.0f

    while (!queue.isEmpty()) {
        val curr = queue.poll()
        val cx = curr % sw
        val cy = curr / sw

        for (d in 0..3) {
            val nx = cx + dx[d]
            val ny = cy + dy[d]
            if (nx in 0 until sw && ny in 0 until sh) {
                val nIdx = ny * sw + nx
                if (!isBackground[nIdx]) {
                    if (gradient[nIdx] >= edgeStopThreshold) continue

                    val distToCorner = colorDist(pixels[nIdx], avgCornerColor)
                    val distToNeighbor = colorDist(pixels[nIdx], pixels[curr])
                    
                    if (distToNeighbor <= 28.0 || distToCorner <= 65.0) {
                        isBackground[nIdx] = true
                        queue.add(nIdx)
                    }
                }
            }
        }
    }

    val output = Bitmap.createBitmap(sw, sh, Bitmap.Config.ARGB_8888)
    val resultPixels = IntArray(sw * sh)
    val pureWhite = AndroidColor.WHITE

    for (y in 0 until sh) {
        for (x in 0 until sw) {
            val idx = y * sw + x
            val origPixel = pixels[idx]

            if (isBackground[idx]) {
                resultPixels[idx] = pureWhite
            } else {
                // Soft boundary anti-aliasing into white
                var adjacentBg = false
                for (d in 0..3) {
                    val nx = x + dx[d]
                    val ny = y + dy[d]
                    if (nx in 0 until sw && ny in 0 until sh && isBackground[ny * sw + nx]) {
                        adjacentBg = true
                        break
                    }
                }

                if (adjacentBg && gradient[idx] < 36f) {
                    val t = (gradient[idx] / 36f).coerceIn(0f, 1f)
                    val r = (AndroidColor.red(origPixel) * t + 255 * (1f - t)).toInt()
                    val g = (AndroidColor.green(origPixel) * t + 255 * (1f - t)).toInt()
                    val b = (AndroidColor.blue(origPixel) * t + 255 * (1f - t)).toInt()
                    resultPixels[idx] = AndroidColor.rgb(r, g, b)
                } else {
                    resultPixels[idx] = origPixel
                }
            }
        }
    }

    output.setPixels(resultPixels, 0, sw, 0, 0, sw, sh)
    output
}

/**
 * Studio Plain White Background Generator for Uri
 */
private suspend fun makeImageBackgroundPlainWhite(context: android.content.Context, uri: Uri): Bitmap? = withContext(Dispatchers.IO) {
    try {
        val original = loadOriginalBitmap(context, uri) ?: return@withContext null
        convertBitmapToPlainWhite(original)
    } catch (_: Exception) {
        null
    }
}

// =========================================================================
// 2. ORDERS MANAGEMENT TAB
// =========================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OwnerOrdersManagementTab(
    viewModel: OwnerViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val data = uiState as? OwnerUiState.Success
    val orders = data?.orders ?: emptyList()
    val view = LocalView.current
    val context = LocalContext.current

    LaunchedEffect(isRefreshing) {
        if (isRefreshing) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
            } else {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            }
        }
    }

    var selectedOrderForDetail by remember { mutableStateOf<Order?>(null) }
    var selectedEstimateForDetail by remember { mutableStateOf<com.princesanitary.grahak.data.model.Estimate?>(null) }
    var orderFilter by remember { mutableStateOf("ALL") }

    val estimates = data?.estimates ?: emptyList()
    val plumbers = data?.plumbers ?: emptyList()

    val plumberOrders = remember(orders) {
        orders.filter { it.attributedPlumberId != null || it.plumberCommission > 0 || it.source == com.princesanitary.grahak.data.model.OrderSource.PLUMBER_ESTIMATE }
    }
    val customerOrders = remember(orders) {
        orders.filter { it.attributedPlumberId == null && it.plumberCommission <= 0 && it.source != com.princesanitary.grahak.data.model.OrderSource.PLUMBER_ESTIMATE }
    }
    val filteredOrders = remember(orders, orderFilter) {
        when (orderFilter) {
            "PLUMBER" -> plumberOrders
            "CUSTOMER" -> customerOrders
            else -> orders
        }
    }

    if (selectedOrderForDetail != null) {
        OwnerOrderDetailSubScreen(
            order = selectedOrderForDetail!!,
            onBack = { selectedOrderForDetail = null },
            onUpdateStatus = { newStatus ->
                viewModel.updateOrderStatus(selectedOrderForDetail!!.id, newStatus)
                selectedOrderForDetail = selectedOrderForDetail!!.copy(status = newStatus)
            }
        )
    } else if (selectedEstimateForDetail != null) {
        OwnerEstimateDetailSubScreen(
            estimate = selectedEstimateForDetail!!,
            plumbers = plumbers,
            onBack = { selectedEstimateForDetail = null },
            onAcceptAsOrder = {
                viewModel.acceptEstimateAsOrder(selectedEstimateForDetail!!) { success, err ->
                    if (success) {
                        Toast.makeText(context, "Parchi accepted as Store Order!", Toast.LENGTH_SHORT).show()
                        selectedEstimateForDetail = null
                        orderFilter = "ALL"
                    } else {
                        Toast.makeText(context, err ?: "Failed to convert parchi", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    } else {
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                } else {
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                }
                viewModel.refreshAllData()
            },
            modifier = Modifier.fillMaxSize()
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
                    Column(Modifier.weight(1f)) {
                        Text("Fulfillment & Dispatches", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold), color = Ink)
                        Text("Site drops & customer pickup orders", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                }

                // Filter Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = orderFilter == "ALL",
                        onClick = { orderFilter = "ALL" },
                        label = { Text("All Orders (${orders.size})") }
                    )
                    FilterChip(
                        selected = orderFilter == "PARCHIS",
                        onClick = { orderFilter = "PARCHIS" },
                        label = { Text("📝 Digital Parchis (${estimates.size})") }
                    )
                    FilterChip(
                        selected = orderFilter == "PLUMBER",
                        onClick = { orderFilter = "PLUMBER" },
                        label = { Text("⭐ Plumber Orders (${plumberOrders.size})") }
                    )
                    FilterChip(
                        selected = orderFilter == "CUSTOMER",
                        onClick = { orderFilter = "CUSTOMER" },
                        label = { Text("Customer Orders (${customerOrders.size})") }
                    )
                }

                Spacer(Modifier.height(8.dp))

                if (orderFilter == "PARCHIS") {
                    if (estimates.isEmpty()) {
                        EmptyState(
                            icon = Icons.Rounded.Description,
                            title = "No Digital Parchis Yet",
                            message = "Estimates submitted by plumbers at customer job sites will appear here in real time."
                        )
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 40.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            items(estimates, key = { it.id }) { est: com.princesanitary.grahak.data.model.Estimate ->
                                val plumber = plumbers.find { it.id == est.plumberId }
                                GrahakCard(cornerRadius = 20.dp, contentPadding = 16.dp) {
                                    Column {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Surface(
                                                    color = LuxeGoldSurface,
                                                    border = BorderStroke(1.dp, LuxeGoldBorder),
                                                    shape = RoundedCornerShape(8.dp)
                                                ) {
                                                    Text(
                                                        text = "Parchi #${kotlin.math.abs(est.id.hashCode() % 9000 + 1000)}",
                                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                        color = LuxeGold,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                    )
                                                }
                                                Spacer(Modifier.width(6.dp))
                                                Surface(
                                                    color = BentoYellow.copy(alpha = 0.2f),
                                                    shape = RoundedCornerShape(6.dp)
                                                ) {
                                                    Text(
                                                        text = "📝 Site Estimate",
                                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                                                        color = BentoOnVibrant,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                                    )
                                                }
                                            }
                                            Surface(
                                                color = when (est.status) {
                                                    com.princesanitary.grahak.data.model.EstimateStatus.CONVERTED_TO_ORDER,
                                                    com.princesanitary.grahak.data.model.EstimateStatus.APPROVED -> BentoGreen.copy(alpha = 0.15f)
                                                    com.princesanitary.grahak.data.model.EstimateStatus.REJECTED -> StatusError.copy(alpha = 0.15f)
                                                    else -> BentoYellow.copy(alpha = 0.15f)
                                                },
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text(
                                                    text = when (est.status) {
                                                        com.princesanitary.grahak.data.model.EstimateStatus.CONVERTED_TO_ORDER -> "Order Created"
                                                        com.princesanitary.grahak.data.model.EstimateStatus.APPROVED -> "Approved by Customer"
                                                        com.princesanitary.grahak.data.model.EstimateStatus.SUBMITTED -> "Pending Customer"
                                                        com.princesanitary.grahak.data.model.EstimateStatus.REJECTED -> "Customer Declined"
                                                        else -> "Draft"
                                                    },
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                                                    color = when (est.status) {
                                                        com.princesanitary.grahak.data.model.EstimateStatus.CONVERTED_TO_ORDER,
                                                        com.princesanitary.grahak.data.model.EstimateStatus.APPROVED -> BentoGreen
                                                        com.princesanitary.grahak.data.model.EstimateStatus.REJECTED -> StatusError
                                                        else -> BentoYellowDark
                                                    },
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }
                                        }

                                        Spacer(Modifier.height(10.dp))

                                        Text(
                                            text = "Plumber: ${plumber?.name.orEmpty().ifBlank { "Partner Plumber" }} • ${SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(est.createdAt)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextSecondary
                                        )

                                        Spacer(Modifier.height(8.dp))

                                        Text(
                                            text = "${est.items.size} item(s): " + est.items.take(3).joinToString(", ") { "${it.quantity}x ${it.productName}" } + if (est.items.size > 3) "..." else "",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Ink,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )

                                        Spacer(Modifier.height(12.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text("Material Total", style = MaterialTheme.typography.labelSmall, color = TextTertiary)
                                                Text(
                                                    text = "₹${"%.2f".format(est.subtotal)}",
                                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                                    color = Ink
                                                )
                                            }

                                            Button(
                                                onClick = { selectedEstimateForDetail = est },
                                                shape = CircleShape,
                                                colors = ButtonDefaults.buttonColors(containerColor = LuxeGold),
                                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                            ) {
                                                Text("View Parchi Details", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = InkDark)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else if (filteredOrders.isEmpty()) {
                    EmptyState(
                        icon = Icons.Rounded.LocalShipping,
                        title = if (orderFilter == "PLUMBER") "No plumber orders" else "No orders yet",
                        message = if (orderFilter == "PLUMBER") "Orders placed by or credited to plumbers will appear here." else "New material orders will appear here."
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 40.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(filteredOrders, key = { it.id }) { order ->
                            GrahakCard(cornerRadius = 20.dp, contentPadding = 16.dp) {
                                Column {
                                    // Top Row: Code Pill, Status Pill, and Time
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Surface(
                                                color = LuxeGoldSurface,
                                                border = BorderStroke(1.dp, LuxeGoldBorder),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text(
                                                    text = "Order #${order.getDisplayOrderNumber()}",
                                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                    color = LuxeGold,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }
                                            if (order.attributedPlumberId != null || order.plumberCommission > 0) {
                                                Spacer(Modifier.width(6.dp))
                                                Surface(
                                                    color = BentoYellow.copy(alpha = 0.2f),
                                                    shape = RoundedCornerShape(6.dp)
                                                ) {
                                                    Text(
                                                        text = "⭐ Partner",
                                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                                                        color = BentoOnVibrant,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                                    )
                                                }
                                            }
                                        }
                                        Surface(
                                            color = when (order.status) {
                                                OrderStatus.DELIVERED -> BentoGreen.copy(alpha = 0.15f)
                                                OrderStatus.CANCELLED -> StatusError.copy(alpha = 0.15f)
                                                OrderStatus.SHIPPED -> BentoBlue.copy(alpha = 0.15f)
                                                else -> BentoYellow.copy(alpha = 0.15f)
                                            },
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = order.status.name,
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                                                color = when (order.status) {
                                                    OrderStatus.DELIVERED -> BentoGreen
                                                    OrderStatus.CANCELLED -> StatusError
                                                    OrderStatus.SHIPPED -> BentoBlue
                                                    else -> LuxeGold
                                                },
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }
                                    }

                                    Spacer(Modifier.height(10.dp))

                                    // Customer Name & Ordered Date
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Column(Modifier.weight(1f)) {
                                            Text(
                                                text = order.customerName.ifBlank { "Customer" },
                                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                                color = Ink
                                            )
                                            if (order.customerPhone.isNotBlank()) {
                                                Text(
                                                    text = "+91 ${order.customerPhone}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = TextSecondary
                                                )
                                            }
                                        }
                                        Text(
                                            text = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(order.createdAt),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextTertiary
                                        )
                                    }

                                    Spacer(Modifier.height(8.dp))

                                    // Items Summary
                                    Text(
                                        text = "${order.items.size} item(s): " + order.items.take(3).joinToString(", ") { "${it.qty}x ${it.name}" } + if (order.items.size > 3) "..." else "",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    Spacer(Modifier.height(12.dp))

                                    // Bottom row: Grand Total and View Details Button
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text("TOTAL AMOUNT", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = TextTertiary)
                                            Text(
                                                text = formatRupees(order.billing.grandTotal),
                                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                                color = BentoGreen
                                            )
                                        }

                                        Button(
                                            onClick = { selectedOrderForDetail = order },
                                            shape = CircleShape,
                                            colors = ButtonDefaults.buttonColors(containerColor = LuxeGoldSurface),
                                            border = BorderStroke(1.dp, LuxeGoldBorder)
                                        ) {
                                            Text("View Full Details →", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = LuxeGold)
                                        }
                                    }
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
// 3. PLUMBERS MANAGEMENT TAB
// =========================================================================

@Composable
private fun OwnerPlumbersManagementTab(
    viewModel: OwnerViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val data = uiState as? OwnerUiState.Success
    val allPlumbers = data?.plumbers ?: emptyList()
    val context = LocalContext.current

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Approved, 1: Pending
    var inspectingPlumber by remember { mutableStateOf<Plumber?>(null) }
    var plumberToDelete by remember { mutableStateOf<Plumber?>(null) }

    val approvedPlumbers = allPlumbers.filter { it.kyc.verified }
    val pendingPlumbers = allPlumbers.filter { !it.kyc.verified }
    val activeList = if (selectedTab == 0) approvedPlumbers else pendingPlumbers

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
            Text("Plumber Partner Pool", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold), color = Ink)
            Text("Review KYC, verify Aadhaar, activate plumbers", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }

        // Two Segmented Tabs: Approved Plumbers vs Pending Approvals
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 4.dp)
                .clip(CircleShape)
                .background(PaperDim)
                .border(1.dp, Line, CircleShape)
                .padding(4.dp)
        ) {
            Surface(
                onClick = { selectedTab = 0 },
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp),
                shape = CircleShape,
                color = if (selectedTab == 0) BentoGreen else Color.Transparent
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "Approved Plumbers (${approvedPlumbers.size})",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (selectedTab == 0) BentoOnVibrant else TextSecondary
                        )
                    )
                }
            }

            Surface(
                onClick = { selectedTab = 1 },
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp),
                shape = CircleShape,
                color = if (selectedTab == 1) BentoPink else Color.Transparent
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "Pending Approvals (${pendingPlumbers.size})",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (selectedTab == 1) BentoOnVibrant else TextSecondary
                        )
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        if (activeList.isEmpty()) {
            EmptyState(
                icon = if (selectedTab == 0) Icons.Rounded.Engineering else Icons.Rounded.HourglassTop,
                title = if (selectedTab == 0) "No approved plumbers yet" else "No pending applications",
                message = if (selectedTab == 0) {
                    "Plumbers approved by you will appear here with their app earnings and balances."
                } else {
                    "New plumber registrations and Aadhaar documents will show here for your review."
                }
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 40.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(activeList, key = { it.id }) { plumber ->
                    GrahakCard(cornerRadius = 20.dp, contentPadding = 16.dp) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Top
                            ) {
                                // Profile photo
                                Box(
                                    modifier = Modifier
                                        .size(62.dp)
                                        .clip(CircleShape)
                                        .background(PaperDim),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (plumber.photoUrl.isNotBlank()) {
                                        AsyncImage(
                                            model = plumber.photoUrl,
                                            contentDescription = plumber.name,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Icon(Icons.Rounded.Person, null, tint = TextSecondary, modifier = Modifier.size(32.dp))
                                    }
                                }

                                Spacer(Modifier.width(14.dp))

                                Column(Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = plumber.name,
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = Ink
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        if (plumber.kyc.verified) {
                                            Icon(Icons.Rounded.Verified, null, Modifier.size(17.dp), tint = BentoGreen)
                                        } else {
                                            Surface(
                                                color = BentoYellow.copy(alpha = 0.2f),
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    text = "KYC PENDING",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.sp),
                                                    color = BentoOnVibrant,
                                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                        if (plumber.hiddenFromOrders) {
                                            Spacer(Modifier.width(6.dp))
                                            Surface(
                                                color = StatusError.copy(alpha = 0.15f),
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    text = "HIDDEN",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.sp),
                                                    color = StatusError,
                                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                    Spacer(Modifier.height(3.dp))
                                    Text(
                                        text = "Aadhaar: ${plumber.kyc.aadhaarNo.ifBlank { "Not provided" }}",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = Ink
                                    )
                                    Text(
                                        text = "Area: ${plumber.baseAddress.ifBlank { "Saharanpur" }} (${plumber.servicePincode.ifBlank { "247001" }}) • Exp: ${plumber.experienceYears}y",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                    if (plumber.rates.visitCharge > 0) {
                                        Text(
                                            text = "Visit Charge: ₹${"%.0f".format(plumber.rates.visitCharge)}",
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                                            color = TextSecondary
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.height(12.dp))

                            // If Approved: show earnings and current owed balance
                            if (plumber.kyc.verified) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(PaperDim)
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("EARNED FROM APP", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = TextSecondary)
                                        Text(
                                            text = "₹${"%.2f".format(plumber.wallet.totalEarnings)}",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = BentoGreen
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("CURRENT BALANCE OWED", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = TextSecondary)
                                        Text(
                                            text = "₹${"%.2f".format(plumber.wallet.withdrawableBalance)}",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = LuxeGold
                                        )
                                    }
                                }
                                Spacer(Modifier.height(10.dp))
                            }

                            // If Pending: show KYC documents count
                            if (!plumber.kyc.verified && plumber.kyc.docUrls.isNotEmpty()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(PaperDim)
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Rounded.Description, null, tint = LuxeGold, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = "${plumber.kyc.docUrls.size} KYC ID Proof photo(s) uploaded",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = Ink
                                    )
                                }
                                Spacer(Modifier.height(10.dp))
                            }

                            // Action buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { inspectingPlumber = plumber },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Rounded.Visibility, null, modifier = Modifier.size(16.dp), tint = Ink)
                                    Spacer(Modifier.width(6.dp))
                                    Text("View Details & KYC", style = MaterialTheme.typography.labelSmall, color = Ink)
                                }

                                if (!plumber.kyc.verified) {
                                    Button(
                                        onClick = { viewModel.verifyPlumber(plumber.id, true) },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = BentoGreen)
                                    ) {
                                        Icon(Icons.Rounded.Check, null, modifier = Modifier.size(16.dp), tint = BentoOnVibrant)
                                        Spacer(Modifier.width(4.dp))
                                        Text("Approve", color = BentoOnVibrant, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (inspectingPlumber != null) {
        OwnerPlumberDetailsDialog(
            plumber = inspectingPlumber!!,
            onDismiss = { inspectingPlumber = null },
            onVerify = { approved ->
                viewModel.verifyPlumber(inspectingPlumber!!.id, approved)
                inspectingPlumber = inspectingPlumber!!.copy(kyc = inspectingPlumber!!.kyc.copy(verified = approved))
            },
            onToggleVisibility = { isVisible ->
                viewModel.setPlumberVisibility(inspectingPlumber!!.id, isVisible)
                inspectingPlumber = inspectingPlumber!!.copy(hiddenFromOrders = !isVisible)
            },
            onDelete = {
                plumberToDelete = inspectingPlumber
            }
        )
    }

    if (plumberToDelete != null) {
        AlertDialog(
            onDismissRequest = { plumberToDelete = null },
            title = { Text("Delete Plumber Partner?") },
            text = { Text("Are you sure you want to permanently delete ${plumberToDelete!!.name}? They will be removed from the plumber pool and their profile will be erased.") },
            confirmButton = {
                Button(
                    onClick = {
                        val pId = plumberToDelete!!.id
                        viewModel.deletePlumber(pId)
                        plumberToDelete = null
                        inspectingPlumber = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusError)
                ) {
                    Text("Delete Permanently", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { plumberToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// =========================================================================
// 4. PAYOUTS MANAGEMENT TAB
// =========================================================================

@Composable
private fun OwnerPayoutsManagementTab(
    viewModel: OwnerViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val data = uiState as? OwnerUiState.Success
    val payouts = data?.pendingPayouts ?: emptyList()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 14.dp)) {
            Text("Plumber Payouts", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold), color = Ink)
            Text("Direct UPI settlements for commissions & service charges", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }

        if (payouts.isEmpty()) {
            EmptyState(
                icon = Icons.Rounded.CheckCircle,
                title = "All payouts settled",
                message = "There are no pending withdrawal requests from plumbers."
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 40.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(payouts, key = { it.id }) { req ->
                    val context = LocalContext.current
                    GrahakCard(cornerRadius = 20.dp, contentPadding = 16.dp) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text("Plumber Payout Request", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Ink)
                                    Spacer(Modifier.height(2.dp))
                                    Text("UPI ID: ${req.upiId}", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium), color = Ink)
                                    Text("Requested: ${req.requestedAt.let { DateFormat.getDateInstance(DateFormat.MEDIUM).format(it) }}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                }
                                Text(formatRupees(req.amount), style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = BentoGreen)
                            }
                            Spacer(Modifier.height(14.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        val uri = Uri.parse("upi://pay?pa=${req.upiId}&pn=Plumber&am=${req.amount}&cu=INR&tn=Prince%20Sanitary%20Payout")
                                        val intent = Intent(Intent.ACTION_VIEW, uri)
                                        try {
                                            context.startActivity(intent)
                                        } catch (_: Exception) {
                                            Toast.makeText(context, "No UPI app (GPay/PhonePe) found on device", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Rounded.AccountBalance, null, modifier = Modifier.size(16.dp), tint = Ink)
                                    Spacer(Modifier.width(6.dp))
                                    Text("Pay with UPI", style = MaterialTheme.typography.labelSmall, color = Ink)
                                }

                                Button(
                                    onClick = { viewModel.approvePayout(req.id, "UPI-${System.currentTimeMillis()}") },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = BentoGreen),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Mark as Paid", color = BentoOnVibrant, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .background(PaperDim, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, Modifier.size(18.dp), tint = Ink)
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = Ink)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
        Icon(Icons.Rounded.ChevronRight, null, Modifier.size(20.dp), tint = TextTertiary)
    }
}

@Composable
private fun RowDivider() {
    Box(
        modifier = Modifier
            .padding(start = 64.dp)
            .fillMaxWidth()
            .height(1.dp)
            .background(Line)
    )
}

// =========================================================================
// DEDICATED FULL-PAGE ORDER DETAILS SCREEN
// =========================================================================

@Composable
private fun OwnerOrderDetailSubScreen(
    order: Order,
    onBack: () -> Unit,
    onUpdateStatus: (OrderStatus) -> Unit
) {
    val context = LocalContext.current
    BackHandler(onBack = onBack)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Paper)
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        // Top App Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = Ink)
                }
                Spacer(Modifier.width(4.dp))
                Column {
                    Text(
                        text = "Order #${order.getDisplayOrderNumber()}",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = Ink
                    )
                    Text(
                        text = SimpleDateFormat("dd MMMM yyyy, hh:mm a", Locale.getDefault()).format(order.createdAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
            }

            Surface(
                color = when (order.status) {
                    OrderStatus.DELIVERED -> BentoGreen.copy(alpha = 0.15f)
                    OrderStatus.CANCELLED -> StatusError.copy(alpha = 0.15f)
                    OrderStatus.SHIPPED -> BentoBlue.copy(alpha = 0.15f)
                    else -> BentoYellow.copy(alpha = 0.15f)
                },
                shape = CircleShape
            ) {
                Text(
                    text = order.status.name,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = when (order.status) {
                        OrderStatus.DELIVERED -> BentoGreen
                        OrderStatus.CANCELLED -> StatusError
                        OrderStatus.SHIPPED -> BentoBlue
                        else -> LuxeGold
                    },
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }

        HorizontalDivider(color = Line)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Order Status Management Card
            GrahakCard(cornerRadius = 18.dp, contentPadding = 16.dp) {
                Column {
                    Text("ORDER STATUS & DISPATCH", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = TextSecondary)
                    Spacer(Modifier.height(8.dp))

                    val isLocked = order.status in setOf(OrderStatus.DELIVERED, OrderStatus.CANCELLED)
                    if (isLocked) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = PaperDim,
                            border = BorderStroke(1.dp, Line),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Rounded.Lock, null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "Order is ${order.status.name.lowercase().replaceFirstChar { it.uppercase() }} and locked. Status cannot be modified.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "Tap to update fulfillment status:",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextTertiary
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                OrderStatus.PLACED to "Placed",
                                OrderStatus.CONFIRMED to "Accept & Confirm",
                                OrderStatus.PACKED to "Mark Packed",
                                OrderStatus.SHIPPED to "Dispatch",
                                OrderStatus.DELIVERED to "Delivered & Paid",
                                OrderStatus.CANCELLED to "Cancel Order"
                            ).forEach { (st, label) ->
                                val isSelected = order.status == st
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { onUpdateStatus(st) },
                                    label = {
                                        Text(
                                            text = label,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // 2. Customer Contact & Delivery Info Card
            GrahakCard(cornerRadius = 18.dp, contentPadding = 16.dp) {
                Column {
                    Text("CUSTOMER & DELIVERY DESTINATION", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = TextSecondary)
                    Spacer(Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = order.customerName.ifBlank { "Customer" },
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Ink
                            )
                            if (order.customerPhone.isNotBlank()) {
                                Text(
                                    text = "+91 ${order.customerPhone}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                            }
                        }

                        if (order.customerPhone.isNotBlank()) {
                            val cleanPhone = order.customerPhone.filter { it.isDigit() }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilledTonalButton(
                                    onClick = {
                                        try {
                                            val waIntent = Intent(
                                                Intent.ACTION_VIEW,
                                                Uri.parse("https://wa.me/91$cleanPhone?text=${Uri.encode("Hello ${order.customerName}, regarding your order #${order.getDisplayOrderNumber()} from Prince Sanitary Store...")}")
                                            )
                                            context.startActivity(waIntent)
                                        } catch (_: Exception) {}
                                    },
                                    shape = CircleShape,
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        containerColor = Color(0xFF25D366).copy(alpha = 0.15f),
                                        contentColor = Color(0xFF25D366)
                                    )
                                ) {
                                    Icon(Icons.Rounded.Chat, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("WhatsApp", style = MaterialTheme.typography.labelSmall)
                                }

                                Button(
                                    onClick = {
                                        try {
                                            context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$cleanPhone")))
                                        } catch (_: Exception) {}
                                    },
                                    shape = CircleShape,
                                    colors = ButtonDefaults.buttonColors(containerColor = BentoGreen)
                                ) {
                                    Icon(Icons.Rounded.Phone, null, modifier = Modifier.size(16.dp), tint = BentoOnVibrant)
                                    Spacer(Modifier.width(4.dp))
                                    Text("Call", style = MaterialTheme.typography.labelSmall, color = BentoOnVibrant, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = PaperDim,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(Icons.Rounded.LocationOn, null, tint = LuxeGold, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = order.delivery.address.tag.ifBlank { "Delivery Address" },
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = TextSecondary
                                )
                                Text(
                                    text = order.delivery.address.fullAddress.ifBlank { "Saharanpur, UP" },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Ink
                                )
                            }

                            TextButton(
                                onClick = {
                                    try {
                                        val query = Uri.encode(order.delivery.address.fullAddress.ifBlank { "Saharanpur" })
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=$query")))
                                    } catch (_: Exception) {}
                                }
                            ) {
                                Text("Map", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = LuxeGold)
                            }
                        }
                    }
                }
            }

            // 3. Itemized Products
            GrahakCard(cornerRadius = 18.dp, contentPadding = 16.dp) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ORDER ITEMS (${order.items.size})",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = TextSecondary
                        )
                        Text(
                            text = "TOTAL",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = TextSecondary
                        )
                    }
                    Spacer(Modifier.height(8.dp))

                    order.items.forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = LuxeGoldSurface,
                                    border = BorderStroke(1.dp, LuxeGoldBorder),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = item.displayItemCode,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.5.sp),
                                        color = LuxeGold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = item.name,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = Ink
                                    )
                                    Text(
                                        text = buildString {
                                            if (item.brand.isNotBlank()) append("${item.brand} • ")
                                            if (item.size.isNotBlank()) append("${item.size} • ")
                                            append("Qty: ${item.qty} × ₹${"%.2f".format(item.unitPrice)}")
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                }
                            }

                            Text(
                                text = "₹${"%.2f".format(item.total)}",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = Ink
                            )
                        }
                        HorizontalDivider(color = Line, modifier = Modifier.padding(vertical = 4.dp))
                    }
                }
            }

            // 4. Financial Bill Breakdown
            GrahakCard(cornerRadius = 18.dp, contentPadding = 16.dp) {
                Column {
                    Text("BILL BREAKDOWN", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = TextSecondary)
                    Spacer(Modifier.height(10.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Subtotal", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        Text("₹${"%.2f".format(order.billing.subtotal)}", style = MaterialTheme.typography.bodyMedium, color = Ink)
                    }
                    if (order.billing.taxTotal > 0) {
                        Spacer(Modifier.height(4.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("GST Included (18%)", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                            Text("₹${"%.2f".format(order.billing.taxTotal)}", style = MaterialTheme.typography.bodyMedium, color = Ink)
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Delivery Charges", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        val fee = order.billing.deliveryFee
                        Text(if (fee <= 0) "FREE" else "₹${"%.2f".format(fee)}", style = MaterialTheme.typography.bodyMedium, color = if (fee <= 0) BentoGreen else Ink)
                    }
                    if (order.billing.discountAmount > 0) {
                        Spacer(Modifier.height(4.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Coupon Discount (${order.billing.couponCode})", style = MaterialTheme.typography.bodyMedium, color = BentoGreen)
                            Text("-₹${"%.2f".format(order.billing.discountAmount)}", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = BentoGreen)
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = Line)

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("GRAND TOTAL", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = Ink)
                        Text("₹${"%.2f".format(order.billing.grandTotal)}", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = BentoGreen)
                    }
                }
            }

            // 5. Plumber Attribution (if any)
            if (order.attributedPlumberId != null || order.plumberCommission > 0) {
                GrahakCard(cornerRadius = 18.dp, contentPadding = 16.dp) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Star, null, tint = BentoYellow, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("PLUMBER PARTNER EARNING", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = Ink)
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text(order.attributedPlumberName.ifBlank { "Registered Partner" }, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = Ink)
                                Text("Attributed from Digital Parchi / Partner Referral", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            }
                            Text(
                                text = "Commission: ₹${"%.2f".format(order.plumberCommission)}",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = BentoGreen
                            )
                        }
                    }
                }
            }
        }
    }
}

// =========================================================================
// PLUMBER KYC & DETAILS INSPECTION DIALOG
// =========================================================================

@Composable
private fun OwnerPlumberDetailsDialog(
    plumber: Plumber,
    onDismiss: () -> Unit,
    onVerify: (Boolean) -> Unit,
    onToggleVisibility: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    var isLiveOnApp by remember { mutableStateOf(!plumber.hiddenFromOrders) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.65f))
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.BottomCenter
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.92f)
                    .imePadding()
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                color = Paper
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .width(42.dp)
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(Line)
                    )
                    Spacer(Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Plumber Partner KYC", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = Ink)
                            Text("Verify ID, inspect Aadhaar & manage account", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Rounded.Close, "Close", tint = TextSecondary)
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // 1. Identity & Live Photo Card
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            color = PaperDim,
                            border = BorderStroke(1.dp, Line)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(72.dp)
                                        .clip(CircleShape)
                                        .background(Paper),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (plumber.photoUrl.isNotBlank()) {
                                        AsyncImage(
                                            model = plumber.photoUrl,
                                            contentDescription = plumber.name,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Icon(Icons.Rounded.Person, null, modifier = Modifier.size(36.dp), tint = TextSecondary)
                                    }
                                }

                                Spacer(Modifier.width(14.dp))

                                Column(Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(plumber.name, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Ink)
                                        Spacer(Modifier.width(6.dp))
                                        if (plumber.kyc.verified) {
                                            Icon(Icons.Rounded.Verified, null, Modifier.size(17.dp), tint = BentoGreen)
                                        }
                                    }
                                    Text("+91 ${plumber.phone}", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                                    Text("Experience: ${plumber.experienceYears} Years", style = MaterialTheme.typography.bodySmall, color = TextTertiary)
                                }

                                Button(
                                    onClick = {
                                        try {
                                            context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${plumber.phone}")))
                                        } catch (_: Exception) {}
                                    },
                                    shape = CircleShape,
                                    colors = ButtonDefaults.buttonColors(containerColor = BentoGreen)
                                ) {
                                    Icon(Icons.Rounded.Phone, null, modifier = Modifier.size(16.dp), tint = BentoOnVibrant)
                                    Spacer(Modifier.width(4.dp))
                                    Text("Call", style = MaterialTheme.typography.labelSmall, color = BentoOnVibrant)
                                }
                            }
                        }

                        // 2. Hide / Show Live on App Toggle
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = if (isLiveOnApp) BentoGreen.copy(alpha = 0.08f) else StatusError.copy(alpha = 0.08f),
                            border = BorderStroke(1.dp, if (isLiveOnApp) BentoGreen.copy(alpha = 0.3f) else StatusError.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        text = if (isLiveOnApp) "🟢 Plumber is LIVE for Orders" else "🔴 Plumber is HIDDEN from Orders",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (isLiveOnApp) BentoGreen else StatusError
                                    )
                                    Text(
                                        text = if (isLiveOnApp) "Customers can see and book this plumber on the app." else "Hidden from booking list. Will not receive new orders.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                }

                                Switch(
                                    checked = isLiveOnApp,
                                    onCheckedChange = { newState ->
                                        isLiveOnApp = newState
                                        onToggleVisibility(newState)
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = BentoOnVibrant,
                                        checkedTrackColor = BentoGreen
                                    )
                                )
                            }
                        }

                        // 3. Aadhaar Number & Physical ID Proof Preview
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            color = PaperDim,
                            border = BorderStroke(1.dp, Line)
                        ) {
                            Column(Modifier.padding(14.dp)) {
                                Text("AADHAAR CARD & ID VERIFICATION", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = TextSecondary)
                                Spacer(Modifier.height(8.dp))

                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    color = Paper,
                                    border = BorderStroke(1.dp, Line)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text("Typed Aadhaar Number", style = MaterialTheme.typography.labelSmall, color = TextTertiary)
                                            Text(
                                                text = plumber.kyc.aadhaarNo.ifBlank { "Not Provided" },
                                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                                                color = Ink
                                            )
                                        }
                                        Icon(Icons.Rounded.Fingerprint, null, tint = LuxeGold, modifier = Modifier.size(24.dp))
                                    }
                                }

                                Spacer(Modifier.height(12.dp))

                                Text("Uploaded Document Photos (${plumber.kyc.docUrls.size})", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = TextSecondary)
                                Text("Inspect the physical card photo to match details:", style = MaterialTheme.typography.bodySmall, color = TextTertiary)
                                Spacer(Modifier.height(8.dp))

                                if (plumber.kyc.docUrls.isEmpty()) {
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(10.dp),
                                        color = Paper
                                    ) {
                                        Text(
                                            text = "No document photos uploaded yet.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextSecondary,
                                            modifier = Modifier.padding(14.dp)
                                        )
                                    }
                                } else {
                                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        plumber.kyc.docUrls.forEachIndexed { idx, docUrl ->
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(200.dp)
                                                    .clip(RoundedCornerShape(14.dp))
                                                    .background(Paper)
                                                    .border(1.dp, Line, RoundedCornerShape(14.dp))
                                            ) {
                                                AsyncImage(
                                                    model = docUrl,
                                                    contentDescription = "Document ${idx + 1}",
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentScale = ContentScale.Fit
                                                )
                                                Surface(
                                                    modifier = Modifier
                                                        .align(Alignment.TopStart)
                                                        .padding(8.dp),
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = Color.Black.copy(alpha = 0.65f)
                                                ) {
                                                    Text(
                                                        text = "Doc #${idx + 1}",
                                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                        color = Color.White,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // 4. Area & Service Rates
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            color = PaperDim,
                            border = BorderStroke(1.dp, Line)
                        ) {
                            Column(Modifier.padding(14.dp)) {
                                Text("OPERATIONAL DETAILS", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = TextSecondary)
                                Spacer(Modifier.height(8.dp))
                                Text("Base Address: ${plumber.baseAddress.ifBlank { "Saharanpur" }}", style = MaterialTheme.typography.bodyMedium, color = Ink)
                                Text("Service Pincode: ${plumber.servicePincode.ifBlank { "247001" }}", style = MaterialTheme.typography.bodyMedium, color = Ink)
                                Text("Standard Visit Charge: ₹${"%.0f".format(plumber.rates.visitCharge)}", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = BentoGreen)
                                Spacer(Modifier.height(6.dp))
                                Text("Services: " + plumber.specialities.joinToString(", ").ifBlank { "General Plumbing" }, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    // Fixed Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDelete,
                            shape = CircleShape,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusError),
                            border = BorderStroke(1.dp, StatusError.copy(alpha = 0.5f)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Rounded.Delete, null, modifier = Modifier.size(16.dp), tint = StatusError)
                            Spacer(Modifier.width(4.dp))
                            Text("Delete Plumber", style = MaterialTheme.typography.labelSmall)
                        }

                        if (plumber.kyc.verified) {
                            Button(
                                onClick = { onVerify(false) },
                                shape = CircleShape,
                                colors = ButtonDefaults.buttonColors(containerColor = BentoYellow),
                                modifier = Modifier.weight(1.2f)
                            ) {
                                Text("Revoke KYC", color = BentoOnVibrant, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Button(
                                onClick = { onVerify(true) },
                                shape = CircleShape,
                                colors = ButtonDefaults.buttonColors(containerColor = BentoGreen),
                                modifier = Modifier.weight(1.2f)
                            ) {
                                Icon(Icons.Rounded.Check, null, modifier = Modifier.size(16.dp), tint = BentoOnVibrant)
                                Spacer(Modifier.width(4.dp))
                                Text("Approve & Activate", color = BentoOnVibrant, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

// =========================================================================
// STORE CONFIGURATION & COUPONS MANAGEMENT DIALOG
// =========================================================================

@Composable
private fun OwnerStoreSettingsDialog(
    viewModel: OwnerViewModel,
    onDismiss: () -> Unit
) {
    val settings by viewModel.storeSettings.collectAsStateWithLifecycle()
    val coupons by viewModel.coupons.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var selectedSection by rememberSaveable { mutableStateOf(0) } // 0: Settings, 1: Coupons

    // Settings fields - initialized with rememberSaveable to prevent resets during recomposition
    var upiId by rememberSaveable { mutableStateOf(settings.ownerUpiId) }
    var businessName by rememberSaveable { mutableStateOf(settings.ownerBusinessName) }
    var phone by rememberSaveable { mutableStateOf(settings.ownerPhone) }
    var freeThreshold by rememberSaveable { 
        mutableStateOf(if (settings.freeDeliveryThreshold % 1.0 == 0.0) settings.freeDeliveryThreshold.toInt().toString() else settings.freeDeliveryThreshold.toString()) 
    }
    var deliveryFee by rememberSaveable { 
        mutableStateOf(if (settings.deliveryFee % 1.0 == 0.0) settings.deliveryFee.toInt().toString() else settings.deliveryFee.toString()) 
    }
    var plumberComm by rememberSaveable { 
        mutableStateOf(if (settings.plumberCommissionRate % 1.0 == 0.0) settings.plumberCommissionRate.toInt().toString() else settings.plumberCommissionRate.toString()) 
    }
    var isStoreOpen by rememberSaveable { mutableStateOf(settings.isStoreOpen) }
    var nextDispatchSchedule by rememberSaveable { mutableStateOf(settings.nextDispatchSchedule) }
    var isFestivalActive by rememberSaveable { mutableStateOf(settings.isFestivalBannerActive) }
    var festivalTag by rememberSaveable { mutableStateOf(settings.festivalTag) }
    var festivalHeadline by rememberSaveable { mutableStateOf(settings.festivalHeadline) }
    var ownerNewPin by rememberSaveable { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }

    var hasLoadedInitialSettings by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(settings) {
        if (!hasLoadedInitialSettings) {
            upiId = settings.ownerUpiId
            businessName = settings.ownerBusinessName
            phone = settings.ownerPhone
            freeThreshold = if (settings.freeDeliveryThreshold % 1.0 == 0.0) settings.freeDeliveryThreshold.toInt().toString() else settings.freeDeliveryThreshold.toString()
            deliveryFee = if (settings.deliveryFee % 1.0 == 0.0) settings.deliveryFee.toInt().toString() else settings.deliveryFee.toString()
            plumberComm = if (settings.plumberCommissionRate % 1.0 == 0.0) settings.plumberCommissionRate.toInt().toString() else settings.plumberCommissionRate.toString()
            isStoreOpen = settings.isStoreOpen
            nextDispatchSchedule = settings.nextDispatchSchedule.ifBlank { "Tomorrow 10:00 AM" }
            isFestivalActive = settings.isFestivalBannerActive
            festivalTag = settings.festivalTag.ifBlank { "FESTIVE WISHES" }
            festivalHeadline = settings.festivalHeadline
            hasLoadedInitialSettings = true
        }
    }

    // New Coupon fields
    var newCouponCode by rememberSaveable { mutableStateOf("") }
    var newCouponType by rememberSaveable { mutableStateOf<String>(DiscountType.PERCENTAGE) }
    var newCouponValue by rememberSaveable { mutableStateOf("") }
    var newCouponMinOrder by rememberSaveable { mutableStateOf("") }
    var newCouponMaxDiscount by rememberSaveable { mutableStateOf("") }
    var newCouponDesc by rememberSaveable { mutableStateOf("") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.65f))
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.BottomCenter
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.92f)
                    .imePadding()
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                color = Paper
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .width(42.dp)
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(Line)
                    )
                    Spacer(Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Store Configuration", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge, color = Ink)
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Rounded.Close, "Close", tint = TextSecondary)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedSection == 0,
                            onClick = { selectedSection = 0 },
                            label = { Text("Delivery & UPI") }
                        )
                        FilterChip(
                            selected = selectedSection == 1,
                            onClick = { selectedSection = 1 },
                            label = { Text("Coupons (${coupons.size})") }
                        )
                    }
                    Spacer(Modifier.height(12.dp))

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                    ) {
                if (selectedSection == 0) {
                    // STORE OPERATING STATUS
                    Text("STORE OPERATING STATUS & DISPATCH TIMING", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = TextSecondary)
                    Text("Toggle store open or closed. When closed, customers can still place orders with the scheduled dispatch note.", style = MaterialTheme.typography.bodySmall, color = TextTertiary)
                    Spacer(Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isStoreOpen) BentoGreen.copy(alpha = 0.12f) else LuxeGold.copy(alpha = 0.12f))
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                if (isStoreOpen) "🟢 Store is OPEN (Express Dispatch)" else "🌙 Store is CLOSED (Scheduled Dispatch)",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isStoreOpen) BentoGreen else LuxeGold
                            )
                            Text(
                                if (isStoreOpen) "Orders will be dispatched immediately." else "Orders will be queued for next scheduled dispatch.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                        Switch(
                            checked = isStoreOpen,
                            onCheckedChange = { isStoreOpen = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = BentoGreen,
                                checkedTrackColor = BentoGreen.copy(alpha = 0.35f),
                                uncheckedThumbColor = LuxeGold,
                                uncheckedTrackColor = LuxeGold.copy(alpha = 0.35f)
                            )
                        )
                    }

                    if (!isStoreOpen) {
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = nextDispatchSchedule,
                            onValueChange = { nextDispatchSchedule = it },
                            label = { Text("Next Dispatch Schedule") },
                            placeholder = { Text("e.g. Tomorrow 10:00 AM") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Rounded.Schedule, null, tint = LuxeGold) }
                        )
                    }

                    Spacer(Modifier.height(14.dp))

                    // STORE & DELIVERY SETTINGS
                    Text("OWNER UPI PAYMENT ID", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = TextSecondary)
                    Text("Enables online UPI payments (GPay/PhonePe/Paytm) for customers.", style = MaterialTheme.typography.bodySmall, color = TextTertiary)
                    Spacer(Modifier.height(4.dp))
                    OutlinedTextField(
                        value = upiId,
                        onValueChange = { upiId = it.trim() },
                        label = { Text("e.g. princesanitary@paytm") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Rounded.AccountBalance, null) }
                    )

                    Spacer(Modifier.height(14.dp))
                    Text("FREE DELIVERY THRESHOLD (₹)", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = TextSecondary)
                    Text("Orders above this amount get Free Delivery.", style = MaterialTheme.typography.bodySmall, color = TextTertiary)
                    Spacer(Modifier.height(4.dp))
                    OutlinedTextField(
                        value = freeThreshold,
                        onValueChange = { freeThreshold = it.filter { ch -> ch.isDigit() || ch == '.' } },
                        label = { Text("Threshold amount (₹)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Rounded.LocalShipping, null) }
                    )

                    Spacer(Modifier.height(14.dp))
                    Text("DELIVERY FEE (₹)", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = TextSecondary)
                    Text("Charge applied when order is below threshold.", style = MaterialTheme.typography.bodySmall, color = TextTertiary)
                    Spacer(Modifier.height(4.dp))
                    OutlinedTextField(
                        value = deliveryFee,
                        onValueChange = { deliveryFee = it.filter { ch -> ch.isDigit() || ch == '.' } },
                        label = { Text("Delivery Fee (₹)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Rounded.CurrencyRupee, null) }
                    )

                    Spacer(Modifier.height(14.dp))
                    Text("PLUMBER COMMISSION RATE (%)", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = TextSecondary)
                    Text("Incentive awarded to plumbers on material orders.", style = MaterialTheme.typography.bodySmall, color = TextTertiary)
                    Spacer(Modifier.height(4.dp))
                    OutlinedTextField(
                        value = plumberComm,
                        onValueChange = { plumberComm = it.filter { ch -> ch.isDigit() || ch == '.' } },
                        label = { Text("Commission rate (%)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Rounded.Percent, null) }
                    )

                    Spacer(Modifier.height(14.dp))
                    Text("STORE CONTACT DETAILS", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = TextSecondary)
                    Spacer(Modifier.height(4.dp))
                    OutlinedTextField(
                        value = businessName,
                        onValueChange = { businessName = it },
                        label = { Text("Business Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Support Phone Number") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // Festive Headline Banner Section
                    Spacer(Modifier.height(16.dp))
                    Text("FESTIVE / EVENT ANNOUNCEMENT", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = TextSecondary)
                    Text("Display greeting wishes or event headlines on the customer home screen.", style = MaterialTheme.typography.bodySmall, color = TextTertiary)
                    Spacer(Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(PaperDim)
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Show Festive Banner on Home", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, color = Ink)
                            Text(if (isFestivalActive) "Banner is currently active" else "Banner is disabled", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }
                        Switch(
                            checked = isFestivalActive,
                            onCheckedChange = { isFestivalActive = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = LuxeGold, checkedTrackColor = LuxeGold.copy(alpha = 0.35f))
                        )
                    }

                    if (isFestivalActive) {
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = festivalTag,
                            onValueChange = { festivalTag = it },
                            label = { Text("Banner Tag (e.g. DIWALI SPECIAL)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = festivalHeadline,
                            onValueChange = { festivalHeadline = it },
                            label = { Text("Festive Greeting / Headline") },
                            placeholder = { Text("Wishing you and your family a Happy & Prosperous Diwali!") },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 3
                        )
                    }

                    // Owner 2FA Master Security PIN Section
                    Spacer(Modifier.height(16.dp))
                    Text("OWNER 2FA MASTER SECURITY PIN", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = TextSecondary)
                    Text("Update the 6-digit master passcode required to open Owner dashboard.", style = MaterialTheme.typography.bodySmall, color = TextTertiary)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = ownerNewPin,
                        onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) ownerNewPin = it },
                        label = { Text("Set New 6-digit Master PIN") },
                        placeholder = { Text("Leave blank to keep unchanged") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Rounded.Lock, null) }
                    )

                    Spacer(Modifier.height(18.dp))
                    PrimaryButton(
                        text = if (isSaving) "Saving..." else "Save Store Settings",
                        onClick = {
                            if (isSaving) return@PrimaryButton
                            isSaving = true
                            val updated = settings.copy(
                                isStoreOpen = isStoreOpen,
                                nextDispatchSchedule = nextDispatchSchedule.trim().ifBlank { "Tomorrow 10:00 AM" },
                                ownerUpiId = upiId.trim(),
                                ownerBusinessName = businessName.trim().ifBlank { "Prince Sanitary & Hardware Store" },
                                ownerPhone = phone.trim(),
                                freeDeliveryThreshold = freeThreshold.toDoubleOrNull() ?: 1000.0,
                                deliveryFee = deliveryFee.toDoubleOrNull() ?: 50.0,
                                plumberCommissionRate = plumberComm.toDoubleOrNull() ?: 3.0,
                                isFestivalBannerActive = isFestivalActive,
                                festivalTag = festivalTag.trim().ifBlank { "FESTIVE WISHES" },
                                festivalHeadline = festivalHeadline.trim()
                            )
                            if (ownerNewPin.isNotBlank() && ownerNewPin.length >= 4) {
                                viewModel.updateOwnerPin(ownerNewPin.trim()) { _, _ -> }
                            }
                            viewModel.updateStoreSettings(updated) { success, err ->
                                isSaving = false
                                if (success) {
                                    Toast.makeText(context, "Store settings saved successfully!", Toast.LENGTH_SHORT).show()
                                    onDismiss()
                                } else {
                                    Toast.makeText(context, err ?: "Failed to save settings", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    )
                } else {
                    // COUPONS SECTION
                    Text("ACTIVE COUPONS", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = TextSecondary)
                    Spacer(Modifier.height(8.dp))
                    if (coupons.isEmpty()) {
                        Text("No discount coupons created yet.", style = MaterialTheme.typography.bodySmall, color = TextTertiary)
                    } else {
                        coupons.forEach { c ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                colors = CardDefaults.cardColors(containerColor = PaperDim),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(c.code, fontWeight = FontWeight.Bold, color = BentoGreen, style = MaterialTheme.typography.titleSmall)
                                        val desc = if (c.discountType == DiscountType.PERCENTAGE) {
                                            "${c.discountValue}% OFF" + if (c.maxDiscountAmount > 0) " up to ₹${c.maxDiscountAmount}" else ""
                                        } else {
                                            "₹${c.discountValue} FLAT OFF"
                                        }
                                        Text(desc, style = MaterialTheme.typography.bodySmall, color = Ink)
                                        if (c.minOrderAmount > 0) {
                                            Text("Min order ₹${c.minOrderAmount}", style = MaterialTheme.typography.labelSmall, color = TextTertiary)
                                        }
                                    }
                                    IconButton(
                                        onClick = {
                                            viewModel.deleteCoupon(c.id) { success, _ ->
                                                if (success) {
                                                    Toast.makeText(context, "Coupon deleted", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        }
                                    ) {
                                        Icon(Icons.Rounded.DeleteOutline, "Delete", tint = StatusError)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(18.dp))
                    HorizontalDivider(color = Line)
                    Spacer(Modifier.height(14.dp))

                    Text("CREATE NEW COUPON", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = Ink)
                    Spacer(Modifier.height(8.dp))

                    OutlinedTextField(
                        value = newCouponCode,
                        onValueChange = { newCouponCode = it.uppercase().trim() },
                        label = { Text("Coupon Code (e.g. FESTIVE10)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = newCouponType == DiscountType.PERCENTAGE,
                            onClick = { newCouponType = DiscountType.PERCENTAGE },
                            label = { Text("Percentage (%)") }
                        )
                        FilterChip(
                            selected = newCouponType == DiscountType.FLAT,
                            onClick = { newCouponType = DiscountType.FLAT },
                            label = { Text("Flat Amount (₹)") }
                        )
                    }

                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newCouponValue,
                        onValueChange = { newCouponValue = it.filter { ch -> ch.isDigit() || ch == '.' } },
                        label = { Text(if (newCouponType == DiscountType.PERCENTAGE) "Discount % (e.g. 10)" else "Discount ₹ (e.g. 100)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newCouponMinOrder,
                        onValueChange = { newCouponMinOrder = it.filter { ch -> ch.isDigit() || ch == '.' } },
                        label = { Text("Min Order Amount ₹ (optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    if (newCouponType == DiscountType.PERCENTAGE) {
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = newCouponMaxDiscount,
                            onValueChange = { newCouponMaxDiscount = it.filter { ch -> ch.isDigit() || ch == '.' } },
                            label = { Text("Max Discount Amount ₹ (optional cap)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }

                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newCouponDesc,
                        onValueChange = { newCouponDesc = it },
                        label = { Text("Description (e.g. 10% off for Saharanpur)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(Modifier.height(14.dp))
                    PrimaryButton(
                        text = "Add Coupon",
                        onClick = {
                            if (newCouponCode.isBlank()) {
                                Toast.makeText(context, "Please enter a coupon code", Toast.LENGTH_SHORT).show()
                                return@PrimaryButton
                            }
                            val v = newCouponValue.toDoubleOrNull() ?: 0.0
                            if (v <= 0) {
                                Toast.makeText(context, "Please enter a valid discount value", Toast.LENGTH_SHORT).show()
                                return@PrimaryButton
                            }
                            val coupon = Coupon(
                                id = "coupon_${System.currentTimeMillis()}",
                                code = newCouponCode.trim().uppercase(),
                                discountType = newCouponType,
                                discountValue = v,
                                minOrderAmount = newCouponMinOrder.toDoubleOrNull() ?: 0.0,
                                maxDiscountAmount = if (newCouponType == DiscountType.PERCENTAGE) {
                                    val parsedCap = newCouponMaxDiscount.toDoubleOrNull() ?: 0.0
                                    if (parsedCap > 0) parsedCap else 10000.0
                                } else v,
                                description = if (newCouponDesc.isNotBlank()) {
                                    newCouponDesc
                                } else {
                                    if (newCouponType == DiscountType.PERCENTAGE) "${v.toInt()}% OFF on orders" else "₹${v.toInt()} FLAT OFF"
                                },
                                isActive = true
                            )
                            viewModel.saveCoupon(coupon) { success, err ->
                                if (success) {
                                    Toast.makeText(context, "Coupon $newCouponCode added!", Toast.LENGTH_SHORT).show()
                                    newCouponCode = ""
                                    newCouponValue = ""
                                    newCouponMinOrder = ""
                                    newCouponMaxDiscount = ""
                                    newCouponDesc = ""
                                } else {
                                    Toast.makeText(context, err ?: "Failed to save coupon", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    )
                }

                }

                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = CircleShape
                ) {
                    Text("Close")
                }
            }
        }
    }
}
}

@Composable
private fun OwnerSecurityPinPrompt(
    viewModel: OwnerViewModel,
    onLogout: () -> Unit
) {
    var pin by remember { mutableStateOf("") }
    val pinError by viewModel.pinError.collectAsStateWithLifecycle()
    val context = LocalContext.current

    BackHandler(enabled = true) {
        (context as? Activity)?.finish()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF09090B)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF141418))
                .border(1.dp, Color(0xFF27272A), RoundedCornerShape(24.dp))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(LuxeGold.copy(alpha = 0.15f))
                    .border(1.dp, LuxeGold.copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Lock, null, tint = LuxeGold, modifier = Modifier.size(26.dp))
            }

            Spacer(Modifier.height(14.dp))

            Text(
                text = "STORE OWNER VERIFICATION",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 1.sp
                ),
                color = LuxeGold
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = "Enter Master Passcode",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                ),
                color = Color.White
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = "Enter your 6-digit owner PIN to unlock store catalog, site dispatches & finances.",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, textAlign = TextAlign.Center),
                color = Color(0xFFA1A1AA)
            )

            Spacer(Modifier.height(20.dp))

            OutlinedTextField(
                value = pin,
                onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) pin = it },
                label = { Text("6-digit Master PIN") },
                placeholder = { Text("••••••", color = Color(0xFF71717A)) },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                textStyle = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    letterSpacing = 6.sp,
                    textAlign = TextAlign.Center
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF18181B),
                    unfocusedContainerColor = Color(0xFF18181B),
                    focusedBorderColor = LuxeGold,
                    unfocusedBorderColor = Color(0xFF27272A),
                    cursorColor = LuxeGold
                )
            )

            if (pinError != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = pinError ?: "",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, color = StatusError),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(20.dp))

            Surface(
                onClick = {
                    if (pin.length >= 4) {
                        viewModel.verifyOwnerPin(pin)
                    } else {
                        Toast.makeText(context, "Please enter your 6-digit Master PIN", Toast.LENGTH_SHORT).show()
                    }
                },
                shape = CircleShape,
                color = LuxeGold,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "Unlock Owner Portal",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color(0xFF09090B)
                        )
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            TextButton(onClick = onLogout) {
                Text(
                    text = "Switch Account / Log Out",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 13.sp,
                        color = Color(0xFFA1A1AA)
                    )
                )
            }
        }
    }
}

// =========================================================================
// 2B. DEDICATED OWNER ESTIMATE / PARCHI DETAIL FULL-SCREEN
// =========================================================================

@Composable
private fun OwnerEstimateDetailSubScreen(
    estimate: com.princesanitary.grahak.data.model.Estimate,
    plumbers: List<Plumber>,
    onBack: () -> Unit,
    onAcceptAsOrder: () -> Unit
) {
    val context = LocalContext.current
    val plumber = plumbers.find { it.id == estimate.plumberId }
    val cleanPlumberPhone = plumber?.phone?.filter(Char::isDigit)?.takeLast(10).orEmpty()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Paper)
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = Ink)
                }
                Spacer(Modifier.width(4.dp))
                Column {
                    Text(
                        text = "Digital Parchi #${kotlin.math.abs(estimate.id.hashCode() % 9000 + 1000)}",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = Ink
                    )
                    Text(
                        text = "Prepared at site by ${plumber?.name.orEmpty().ifBlank { "Plumber" }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            Surface(
                color = when (estimate.status) {
                    com.princesanitary.grahak.data.model.EstimateStatus.CONVERTED_TO_ORDER,
                    com.princesanitary.grahak.data.model.EstimateStatus.APPROVED -> BentoGreen.copy(alpha = 0.15f)
                    com.princesanitary.grahak.data.model.EstimateStatus.REJECTED -> StatusError.copy(alpha = 0.15f)
                    else -> BentoYellow.copy(alpha = 0.15f)
                },
                shape = CircleShape
            ) {
                Text(
                    text = when (estimate.status) {
                        com.princesanitary.grahak.data.model.EstimateStatus.CONVERTED_TO_ORDER -> "Order Created"
                        com.princesanitary.grahak.data.model.EstimateStatus.APPROVED -> "Approved"
                        com.princesanitary.grahak.data.model.EstimateStatus.SUBMITTED -> "Submitted"
                        com.princesanitary.grahak.data.model.EstimateStatus.REJECTED -> "Declined"
                        else -> "Draft"
                    },
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = when (estimate.status) {
                        com.princesanitary.grahak.data.model.EstimateStatus.CONVERTED_TO_ORDER,
                        com.princesanitary.grahak.data.model.EstimateStatus.APPROVED -> BentoGreen
                        com.princesanitary.grahak.data.model.EstimateStatus.REJECTED -> StatusError
                        else -> BentoYellowDark
                    },
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }

        HorizontalDivider(color = Line)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Plumber Card
            GrahakCard(cornerRadius = 20.dp, contentPadding = 16.dp) {
                Column {
                    Text(
                        text = "Plumber Information",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Ink
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Name: ${plumber?.name.orEmpty().ifBlank { "Partner Plumber" }}",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = Ink
                    )
                    if (cleanPlumberPhone.isNotBlank()) {
                        Text(
                            text = "Phone: +91 $cleanPlumberPhone",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    try {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/91$cleanPlumberPhone")))
                                    } catch (_: Exception) {}
                                },
                                shape = CircleShape,
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = Color(0xFF25D366).copy(alpha = 0.15f),
                                    contentColor = Color(0xFF25D366)
                                )
                            ) {
                                Icon(Icons.Rounded.Chat, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("WhatsApp", style = MaterialTheme.typography.labelSmall)
                            }
                            Button(
                                onClick = {
                                    try {
                                        context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$cleanPlumberPhone")))
                                    } catch (_: Exception) {}
                                },
                                shape = CircleShape,
                                colors = ButtonDefaults.buttonColors(containerColor = BentoGreen)
                            ) {
                                Icon(Icons.Rounded.Phone, null, modifier = Modifier.size(16.dp), tint = BentoOnVibrant)
                                Spacer(Modifier.width(4.dp))
                                Text("Call Plumber", style = MaterialTheme.typography.labelSmall, color = BentoOnVibrant)
                            }
                        }
                    }
                }
            }

            // Material Items List
            GrahakCard(cornerRadius = 20.dp, contentPadding = 16.dp) {
                Column {
                    Text(
                        text = "Requested Materials (${estimate.items.size} items)",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Ink
                    )
                    Spacer(Modifier.height(12.dp))

                    estimate.items.forEachIndexed { idx, item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = item.productName,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Ink
                                )
                                Text(
                                    text = "Qty: ${item.quantity} × ₹${"%.2f".format(item.unitPrice)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                            Text(
                                text = "₹${"%.2f".format(item.total)}",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = Ink
                            )
                        }
                        if (idx < estimate.items.size - 1) {
                            HorizontalDivider(color = Line.copy(alpha = 0.5f))
                        }
                    }
                }
            }

            // Bill Breakdown
            GrahakCard(cornerRadius = 20.dp, contentPadding = 16.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Parchi Cost Summary",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Ink
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Materials Subtotal", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        Text("₹${"%.2f".format(estimate.subtotal)}", style = MaterialTheme.typography.bodyMedium, color = Ink)
                    }
                    if (estimate.labourCharge > 0) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Plumber Labour Charge", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                            Text("₹${"%.2f".format(estimate.labourCharge)}", style = MaterialTheme.typography.bodyMedium, color = Ink)
                        }
                    }
                    if (estimate.taxTotal > 0) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Estimated Tax (GST)", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                            Text("₹${"%.2f".format(estimate.taxTotal)}", style = MaterialTheme.typography.bodyMedium, color = Ink)
                        }
                    }
                    HorizontalDivider(color = Line)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total Amount", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Ink)
                        Text("₹${"%.2f".format(estimate.grandTotal)}", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = LuxeGold)
                    }

                    val commission = estimate.subtotal * 0.05
                    Surface(
                        color = BentoGreen.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Plumber 5% Commission", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = BentoGreen)
                            Text("+₹${"%.2f".format(commission)}", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = BentoGreen)
                        }
                    }
                }
            }

            // Accept & Pack Button
            if (estimate.status != com.princesanitary.grahak.data.model.EstimateStatus.CONVERTED_TO_ORDER) {
                Button(
                    onClick = onAcceptAsOrder,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BentoGreen)
                ) {
                    Icon(Icons.Rounded.CheckCircle, null, modifier = Modifier.size(20.dp), tint = BentoOnVibrant)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Accept & Pack Order in Store",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = BentoOnVibrant
                    )
                }
            }
        }
    }
}
