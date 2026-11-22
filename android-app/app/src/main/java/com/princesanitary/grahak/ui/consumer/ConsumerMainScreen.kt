package com.princesanitary.grahak.ui.consumer

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Plumbing
import androidx.compose.material.icons.rounded.Receipt
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.princesanitary.grahak.di.appViewModel
import com.princesanitary.grahak.ui.components.GrahakBottomNav
import com.princesanitary.grahak.ui.components.NavItem
import com.princesanitary.grahak.ui.components.SwipeBackContainer
import com.princesanitary.grahak.ui.consumer.cart.CartScreen
import com.princesanitary.grahak.ui.consumer.cart.CartUiState
import com.princesanitary.grahak.ui.consumer.cart.CartViewModel
import com.princesanitary.grahak.ui.consumer.cart.ViewCartBar
import com.princesanitary.grahak.ui.consumer.catalog.CatalogScreen
import com.princesanitary.grahak.ui.consumer.catalog.CatalogViewModel
import com.princesanitary.grahak.ui.consumer.checkout.CheckoutScreen
import com.princesanitary.grahak.ui.consumer.orders.OrderDetailScreen
import com.princesanitary.grahak.ui.consumer.orders.OrderHistoryViewModel
import com.princesanitary.grahak.ui.consumer.orders.OrdersHubScreen
import com.princesanitary.grahak.ui.consumer.services.BookPlumberScreen
import com.princesanitary.grahak.data.model.JobStatus
import com.princesanitary.grahak.data.model.Plumber
import com.princesanitary.grahak.di.ServiceLocator
import com.princesanitary.grahak.ui.consumer.services.ActivePlumberCapsule
import com.princesanitary.grahak.ui.consumer.services.MyRequestsViewModel
import com.princesanitary.grahak.ui.consumer.services.RequestsState
import com.princesanitary.grahak.ui.theme.Paper

object ConsumerRoutes {
    const val HOME = "home"
    const val SHOP = "shop"
    const val SERVICES = "services"
    const val ORDERS = "orders"
    const val PROFILE = "profile"
    const val CART = "cart"
    const val CHECKOUT = "checkout"
    const val ORDER_DETAIL = "order_detail/{orderId}"
    const val OWNER = "owner"

    fun orderDetail(orderId: String) = "order_detail/$orderId"
}

private val TAB_ROUTES = setOf(
    ConsumerRoutes.HOME,
    ConsumerRoutes.SHOP,
    ConsumerRoutes.SERVICES,
    ConsumerRoutes.ORDERS,
    ConsumerRoutes.PROFILE
)

/* --------------------------------------------------------------------------
 * Screen transitions
 *
 * Detail screens use an OPAQUE horizontal slide with a parallax on the screen
 * being left behind - the iOS pattern. Nothing is alpha-animated, which is what
 * fixes the "flash of the previous screen" on back: a cross-fade makes both
 * screens briefly translucent, so the window background shows through for a
 * frame or two.
 * -------------------------------------------------------------------------- */
private const val SLIDE_MS = 300
private const val SLIDE_BACK_MS = 260

/* -------------------------------------------------------------------------- */

@Composable
fun ConsumerMainScreen(
    onLogout: () -> Unit
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    // Activity-scoped so tab switches don't re-query Firestore.
    val catalogViewModel: CatalogViewModel = appViewModel()
    val cartViewModel: CartViewModel = appViewModel()
    val orderHistoryViewModel: OrderHistoryViewModel = appViewModel()
    val myRequestsViewModel: MyRequestsViewModel = appViewModel()

    val requestsState by myRequestsViewModel.state.collectAsStateWithLifecycle()
    val activeJob = remember(requestsState) {
        (requestsState as? RequestsState.Ready)?.jobs?.firstOrNull { job ->
            job.status in listOf(
                JobStatus.ACCEPTED,
                JobStatus.EN_ROUTE,
                JobStatus.INSPECTING,
                JobStatus.IN_PROGRESS
            ) && !job.plumberId.isNullOrBlank()
        }
    }
    var activePlumber by remember { mutableStateOf<Plumber?>(null) }
    LaunchedEffect(activeJob?.plumberId) {
        val pid = activeJob?.plumberId
        if (!pid.isNullOrBlank()) {
            val plumber = withContext(Dispatchers.IO) {
                ServiceLocator.plumberRepository.getPlumberById(pid).getOrNull()
            }
            activePlumber = plumber
        } else {
            activePlumber = null
        }
    }

    val cartState by cartViewModel.uiState.collectAsStateWithLifecycle()
    val cartSuccess = cartState as? CartUiState.Success
    val cartCount = cartSuccess?.summary?.itemCount ?: 0
    val cartTotal = cartSuccess?.summary?.grandTotal ?: 0.0

    val onTab = currentRoute in TAB_ROUTES
    val showCartPill = cartCount > 0 &&
        currentRoute in setOf(ConsumerRoutes.HOME, ConsumerRoutes.SHOP, ConsumerRoutes.SERVICES)

    val navItems = listOf(
        NavItem(ConsumerRoutes.HOME, "Home", Icons.Rounded.Home),
        NavItem(ConsumerRoutes.SHOP, "Shop", Icons.Rounded.Storefront),
        NavItem(ConsumerRoutes.SERVICES, "Book", Icons.Rounded.Plumbing),
        NavItem(ConsumerRoutes.ORDERS, "Orders", Icons.Rounded.Receipt),
        NavItem(ConsumerRoutes.PROFILE, "You", Icons.Rounded.Person)
    )

    /**
     * Tab switching.
     *
     * Two bugs lived here, both worth spelling out.
     *
     * 1. This used to be a local `fun` that closed over `currentRoute`, passed
     *    down as `::selectTab`. The nav bar sits inside an AnimatedVisibility -
     *    a sub-composition that stays alive for as long as you are on a tab - so
     *    it held a reference captured when `currentRoute` was still "home".
     *    The `route == currentRoute` guard then matched on every Home tap and
     *    returned early, which is exactly why *only* the Home button did
     *    nothing. The destination is now read live off the controller, so there
     *    is no captured state to go stale.
     *
     * 2. `navigate()` can never return you to the start destination: it pops
     *    back to Home and then asks to navigate to the destination now on top,
     *    which `launchSingleTop` correctly discards. Returning to Home is a
     *    *pop*.
     */
    val homeScrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    val selectTab = remember(navController, coroutineScope, homeScrollState) {
        { route: String ->
            val live = navController.currentDestination?.route
            if (route != live) {
                if (!navController.popBackStack(route, inclusive = false, saveState = false)) {
                    navController.navigate(route) {
                        popUpTo(ConsumerRoutes.HOME) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            } else if (route == ConsumerRoutes.HOME) {
                coroutineScope.launch {
                    homeScrollState.animateScrollTo(0)
                }
            }
        }
    }

    val context = LocalContext.current
    var lastBackPressTime by remember { mutableLongStateOf(0L) }

    BackHandler(enabled = true) {
        if (!onTab) {
            navController.popBackStack()
        } else if (currentRoute != ConsumerRoutes.HOME) {
            selectTab(ConsumerRoutes.HOME)
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

    Scaffold(
        containerColor = Paper,
        bottomBar = {
            /*
             * The bar's HEIGHT is constant; only its vertical offset animates.
             *
             * Previously this was an AnimatedVisibility, which grows/shrinks the
             * bottomBar. Scaffold feeds bottomBar height into the content's
             * padding, so every frame of that 280ms animation re-measured the
             * whole NavHost subtree - including the incoming screen's LazyColumn.
             * That is what made returning to the dashboard hang for seconds.
             *
             * Translating via graphicsLayer never triggers measurement, so the
             * content above is untouched while the bar slides.
             */
            val barOffset by animateFloatAsState(
                targetValue = if (onTab) 0f else 1f,
                animationSpec = tween(240, easing = FastOutSlowInEasing),
                label = "barOffset"
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        translationY = barOffset * size.height
                        alpha = 1f - barOffset
                    }
            ) {
                // Active plumber capsule: Shown ABOVE the cart capsule, shorter in height and weight
                if (activeJob != null && activePlumber != null) {
                    ActivePlumberCapsule(
                        plumberName = activePlumber?.name.orEmpty(),
                        plumberPhone = activePlumber?.phone.orEmpty(),
                        plumberPhotoUrl = activePlumber?.photoUrl.orEmpty(),
                        jobStatus = activeJob.status,
                        serviceCategory = activeJob.serviceCategory,
                        onClick = { selectTab(ConsumerRoutes.ORDERS) }
                    )
                    Spacer(Modifier.height(8.dp))
                }

                // Reserve the pill's slot at all times too, for the same reason.
                val pillAlpha by animateFloatAsState(
                    targetValue = if (showCartPill) 1f else 0f,
                    animationSpec = tween(200),
                    label = "pillAlpha"
                )
                if (pillAlpha > 0.01f) {
                    Box(
                        Modifier.graphicsLayer {
                            alpha = pillAlpha
                            translationY = (1f - pillAlpha) * 40f
                        }
                    ) {
                        ViewCartBar(
                            itemCount = cartCount,
                            total = cartTotal,
                            onClick = { navController.navigate(ConsumerRoutes.CART) }
                        )
                    }
                }

                GrahakBottomNav(
                    items = navItems,
                    currentRoute = currentRoute,
                    onSelect = selectTab
                )
            }
        }
    ) { scaffoldPadding ->
        val effectiveBottomPadding = if (onTab) scaffoldPadding.calculateBottomPadding() else 0.dp
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Paper)
                .padding(bottom = effectiveBottomPadding)
        ) {
            NavHost(
                navController = navController,
                startDestination = ConsumerRoutes.HOME,
                /*
                 * Tabs get a short, explicit cross-fade.
                 *
                 * These were EnterTransition.None / ExitTransition.None, which
                 * turned out to be the cause of the "previous screen sitting on
                 * top of the new one" on system back: AnimatedContent with an
                 * undefined transition keeps BOTH children composed and fully
                 * opaque while the back gesture settles.
                 *
                 * A defined transition always resolves to a single visible
                 * screen. 130ms is short enough to read as immediate.
                 */
                enterTransition = { fadeIn(tween(130)) },
                exitTransition = { fadeOut(tween(130)) },
                popEnterTransition = { fadeIn(tween(130)) },
                popExitTransition = { fadeOut(tween(130)) }
            ) {
                composable(ConsumerRoutes.HOME) {
                    ConsumerHomeScreen(
                        catalogViewModel = catalogViewModel,
                        cartViewModel = cartViewModel,
                        orderHistoryViewModel = orderHistoryViewModel,
                        scrollState = homeScrollState,
                        onOpenCart = { navController.navigate(ConsumerRoutes.CART) },
                        onOpenShop = { selectTab(ConsumerRoutes.SHOP) },
                        onOpenServices = { selectTab(ConsumerRoutes.SERVICES) },
                        onOpenOrders = { selectTab(ConsumerRoutes.ORDERS) }
                    )
                }

                composable(ConsumerRoutes.SHOP) {
                    CatalogScreen(
                        viewModel = catalogViewModel,
                        cartViewModel = cartViewModel,
                        onProductClick = { }
                    )
                }

                composable(ConsumerRoutes.SERVICES) {
                    BookPlumberScreen(
                        onBookingSuccess = { selectTab(ConsumerRoutes.ORDERS) },
                        onBack = { selectTab(ConsumerRoutes.HOME) }
                    )
                }

                composable(ConsumerRoutes.ORDERS) {
                    // Product orders AND plumber bookings behind one tab - the
                    // customer had no way to see service requests at all before.
                    OrdersHubScreen(
                        orderHistoryViewModel = orderHistoryViewModel,
                        onOrderClick = { orderId ->
                            navController.navigate(ConsumerRoutes.orderDetail(orderId))
                        }
                    )
                }

                composable(ConsumerRoutes.PROFILE) {
                    ConsumerProfileScreen(
                        onLogout = onLogout,
                        onOpenOwnerPanel = { navController.navigate(ConsumerRoutes.OWNER) },
                        onNavigateToOrders = { selectTab(ConsumerRoutes.ORDERS) }
                    )
                }

                composable(
                    route = ConsumerRoutes.OWNER,
                    enterTransition = { slideIn() },
                    exitTransition = { parallaxOut() },
                    popEnterTransition = { parallaxIn() },
                    popExitTransition = { slideOut() }
                ) {
                    SwipeBackContainer(onBack = { navController.popBackStack() }) {
                        com.princesanitary.grahak.ui.owner.OwnerDashboardScreen(
                            onBack = { navController.popBackStack() }
                        )
                    }
                }

                // ---------------- detail screens: slide + swipe back ----------

                composable(
                    route = ConsumerRoutes.CART,
                    enterTransition = { slideIn() },
                    exitTransition = { parallaxOut() },
                    popEnterTransition = { parallaxIn() },
                    popExitTransition = { slideOut() }
                ) {
                    SwipeBackContainer(onBack = { navController.popBackStack() }) {
                        CartScreen(
                            viewModel = cartViewModel,
                            onCheckout = { navController.navigate(ConsumerRoutes.CHECKOUT) },
                            onBack = { navController.popBackStack() }
                        )
                    }
                }

                composable(
                    route = ConsumerRoutes.CHECKOUT,
                    enterTransition = { slideIn() },
                    exitTransition = { parallaxOut() },
                    popEnterTransition = { parallaxIn() },
                    popExitTransition = { slideOut() }
                ) {
                    SwipeBackContainer(onBack = { navController.popBackStack() }) {
                        CheckoutScreen(
                            onOrderPlaced = {
                                navController.navigate(ConsumerRoutes.ORDERS) {
                                    popUpTo(ConsumerRoutes.HOME)
                                }
                            },
                            onBack = { navController.popBackStack() }
                        )
                    }
                }

                composable(
                    route = ConsumerRoutes.ORDER_DETAIL,
                    arguments = listOf(navArgument("orderId") { type = NavType.StringType }),
                    enterTransition = { slideIn() },
                    exitTransition = { parallaxOut() },
                    popEnterTransition = { parallaxIn() },
                    popExitTransition = { slideOut() }
                ) { entry ->
                    val orderId = entry.arguments?.getString("orderId") ?: return@composable
                    SwipeBackContainer(onBack = { navController.popBackStack() }) {
                        OrderDetailScreen(
                            orderId = orderId,
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}

/* ---------------------- shared transition builders ------------------------ */

internal fun slideIn() = slideInHorizontally(
    animationSpec = tween(SLIDE_MS, easing = FastOutSlowInEasing)
) { fullWidth -> fullWidth }

internal fun slideOut() = slideOutHorizontally(
    animationSpec = tween(SLIDE_BACK_MS, easing = FastOutSlowInEasing)
) { fullWidth -> fullWidth }

/** The screen left behind drifts a fifth of the way out, creating depth. */
internal fun parallaxOut() = slideOutHorizontally(
    animationSpec = tween(SLIDE_MS, easing = FastOutSlowInEasing)
) { fullWidth -> -fullWidth / 5 }

internal fun parallaxIn() = slideInHorizontally(
    animationSpec = tween(SLIDE_BACK_MS, easing = FastOutSlowInEasing)
) { fullWidth -> -fullWidth / 5 }
