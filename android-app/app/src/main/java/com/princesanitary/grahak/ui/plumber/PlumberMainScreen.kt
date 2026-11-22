package com.princesanitary.grahak.ui.plumber

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.ui.draw.clip
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.graphicsLayer
import com.princesanitary.grahak.di.appViewModel
import com.princesanitary.grahak.ui.plumber.wallet.PlumberWalletViewModel
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.WorkOutline
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.princesanitary.grahak.ui.components.GrahakBottomNav
import com.princesanitary.grahak.ui.components.LoadingState
import com.princesanitary.grahak.ui.components.NavItem
import com.princesanitary.grahak.ui.components.SwipeBackContainer
import com.princesanitary.grahak.ui.plumber.onboarding.PlumberRegistrationScreen
import com.princesanitary.grahak.ui.consumer.parallaxIn
import com.princesanitary.grahak.ui.consumer.parallaxOut
import com.princesanitary.grahak.ui.consumer.slideIn
import com.princesanitary.grahak.ui.consumer.slideOut
import com.princesanitary.grahak.ui.consumer.cart.CartScreen
import com.princesanitary.grahak.ui.consumer.cart.CartUiState
import com.princesanitary.grahak.ui.consumer.cart.CartViewModel
import com.princesanitary.grahak.ui.consumer.cart.ViewCartBar
import com.princesanitary.grahak.ui.consumer.catalog.CatalogScreen
import com.princesanitary.grahak.ui.consumer.catalog.CatalogViewModel
import com.princesanitary.grahak.ui.consumer.checkout.CheckoutScreen
import com.princesanitary.grahak.ui.plumber.estimate.CreateEstimateScreen
import com.princesanitary.grahak.ui.plumber.estimate.EstimateListScreen
import com.princesanitary.grahak.ui.plumber.jobs.ActiveJobScreen
import com.princesanitary.grahak.ui.plumber.profile.PlumberProfileScreen
import com.princesanitary.grahak.ui.plumber.wallet.PlumberWalletScreen
import com.princesanitary.grahak.ui.theme.Paper

object PlumberRoutes {
    const val JOBS = "jobs"
    const val ESTIMATES = "estimates"
    const val WALLET = "wallet"
    const val PROFILE = "profile"
    const val STORE = "plumber_store"
    const val CART = "plumber_cart"
    const val CHECKOUT = "plumber_checkout"
    const val ACTIVE_JOB = "active_job/{jobId}"
    const val CREATE_ESTIMATE = "create_estimate/{jobId}/{customerId}"

    fun activeJob(jobId: String) = "active_job/$jobId"
    fun createEstimate(jobId: String, customerId: String) = "create_estimate/$jobId/$customerId"
}

private val TAB_ROUTES = setOf(
    PlumberRoutes.JOBS,
    PlumberRoutes.STORE,
    PlumberRoutes.ESTIMATES,
    PlumberRoutes.WALLET,
    PlumberRoutes.PROFILE
)

/**
 * Entry point for the plumber side.
 *
 * Gates on whether a `plumbers/{uid}` profile exists and has been approved. This
 * routing did not exist before, which is why a plumber could sign up, never be
 * asked for a profile, and never appear to customers.
 */
@Composable
fun PlumberMainScreen(
    onLogout: () -> Unit,
    gateViewModel: PlumberGateViewModel = appViewModel()
) {
    val gate by gateViewModel.gate.collectAsStateWithLifecycle()

    when (val g = gate) {
        PlumberGate.Loading -> LoadingState()

        PlumberGate.NeedsOnboarding -> PlumberRegistrationScreen(
            onRegistrationComplete = { gateViewModel.refresh() }
        )

        is PlumberGate.AwaitingApproval -> PlumberPendingScreen(
            plumber = g.plumber,
            rejected = false,
            onLogout = onLogout
        )

        is PlumberGate.Rejected -> PlumberPendingScreen(
            plumber = g.plumber,
            rejected = true,
            onLogout = onLogout
        )

        is PlumberGate.Active -> PlumberWorkspace(
            plumber = g.plumber,
            onToggleOnline = gateViewModel::setOnline,
            onLogout = onLogout
        )
    }
}

@Composable
private fun PlumberWorkspace(
    plumber: com.princesanitary.grahak.data.model.Plumber,
    onToggleOnline: (Boolean) -> Unit,
    onLogout: () -> Unit
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    // Resolved above the NavHost so it is Activity-scoped. The dashboard and the
    // Wallet tab previously each created their own instance, meaning the wallet
    // was re-fetched from Firestore on every tab switch.
    val walletViewModel: PlumberWalletViewModel = appViewModel()
    val cartViewModel: CartViewModel = appViewModel()
    val catalogViewModel: CatalogViewModel = appViewModel()
    val cartState by cartViewModel.uiState.collectAsStateWithLifecycle()
    val cartSuccess = cartState as? CartUiState.Success
    val cartCount = cartSuccess?.summary?.itemCount ?: 0
    val cartTotal = cartSuccess?.summary?.grandTotal ?: 0.0

    val showNav = currentRoute in TAB_ROUTES
    var activeJobId by remember { mutableStateOf<String?>(null) }

    val navItems = listOf(
        NavItem(PlumberRoutes.JOBS, "Jobs", Icons.Rounded.WorkOutline),
        NavItem(PlumberRoutes.STORE, "Store", Icons.Rounded.Storefront),
        NavItem(PlumberRoutes.ESTIMATES, "Quotes", Icons.Rounded.Description),
        NavItem(PlumberRoutes.WALLET, "Wallet", Icons.Rounded.AccountBalanceWallet),
        NavItem(PlumberRoutes.PROFILE, "You", Icons.Rounded.Person)
    )

    // Returning to the start destination is a pop, not a navigate - the same bug
    // that made the consumer Home tab inert.
    // Destination read live off the controller - see ConsumerMainScreen for why
    // capturing it in a local fun made the first tab silently inert.
    val selectTab = remember(navController) {
        { route: String ->
            val live = navController.currentDestination?.route
            if (route != live) {
                if (!navController.popBackStack(route, inclusive = false, saveState = false)) {
                    navController.navigate(route) {
                        popUpTo(PlumberRoutes.JOBS) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            } else if (route == PlumberRoutes.JOBS) {
                navController.popBackStack(PlumberRoutes.JOBS, inclusive = false)
            }
        }
    }

    val context = LocalContext.current
    var lastBackPressTime by remember { mutableLongStateOf(0L) }

    BackHandler(enabled = true) {
        if (!showNav) {
            navController.popBackStack()
        } else if (currentRoute != PlumberRoutes.JOBS) {
            selectTab(PlumberRoutes.JOBS)
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
            // Constant height, offset-only animation: changing bottomBar height
            // re-measures the whole content subtree every frame.
            val barOffset by animateFloatAsState(
                targetValue = if (showNav) 0f else 1f,
                animationSpec = tween(240, easing = FastOutSlowInEasing),
                label = "barOffset"
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        translationY = barOffset * size.height
                        alpha = 1f - barOffset
                    }
            ) {
                GrahakBottomNav(
                    items = navItems,
                    currentRoute = currentRoute,
                    onSelect = selectTab
                )
            }
        }
    ) { scaffoldPadding ->
        val effectiveBottomPadding = if (showNav) scaffoldPadding.calculateBottomPadding() else 0.dp
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Paper)
                .padding(bottom = effectiveBottomPadding)
        ) {
            NavHost(
                navController = navController,
                startDestination = PlumberRoutes.JOBS,
                // Short, DEFINED transitions. `None` leaves AnimatedContent with
                // nothing to resolve and keeps both screens opaque on system
                // back - see ConsumerMainScreen for the full explanation.
                enterTransition = { fadeIn(tween(130)) },
                exitTransition = { fadeOut(tween(130)) },
                popEnterTransition = { fadeIn(tween(130)) },
                popExitTransition = { fadeOut(tween(130)) }
            ) {
                composable(PlumberRoutes.JOBS) {
                    PlumberJobsScreen(
                        plumber = plumber,
                        walletViewModel = walletViewModel,
                        onToggleOnline = onToggleOnline,
                        onOpenJob = { jobId ->
                            activeJobId = jobId
                            navController.navigate(PlumberRoutes.activeJob(jobId))
                        },
                        onOpenWallet = { navController.navigate(PlumberRoutes.WALLET) }
                    )
                }

                composable(PlumberRoutes.ESTIMATES) {
                    EstimateListScreen(onEstimateClick = { })
                }

                composable(PlumberRoutes.WALLET) {
                    PlumberWalletScreen(viewModel = walletViewModel)
                }

                composable(PlumberRoutes.PROFILE) {
                    PlumberProfileScreen(
                        onOrderMaterials = { navController.navigate(PlumberRoutes.STORE) },
                        onLogout = onLogout
                    )
                }

                composable(
                    route = PlumberRoutes.STORE,
                    enterTransition = { slideIn() },
                    exitTransition = { parallaxOut() },
                    popEnterTransition = { parallaxIn() },
                    popExitTransition = { slideOut() }
                ) {
                    SwipeBackContainer(onBack = { navController.popBackStack() }) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            CatalogScreen(
                                viewModel = catalogViewModel,
                                cartViewModel = cartViewModel,
                                onProductClick = { }
                            )
                            if (cartCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .padding(bottom = 16.dp)
                                ) {
                                    ViewCartBar(
                                        itemCount = cartCount,
                                        total = cartTotal,
                                        onClick = { navController.navigate(PlumberRoutes.CART) }
                                    )
                                }
                            }
                        }
                    }
                }

                composable(
                    route = PlumberRoutes.CART,
                    enterTransition = { slideIn() },
                    exitTransition = { parallaxOut() },
                    popEnterTransition = { parallaxIn() },
                    popExitTransition = { slideOut() }
                ) {
                    SwipeBackContainer(onBack = { navController.popBackStack() }) {
                        CartScreen(
                            viewModel = cartViewModel,
                            onCheckout = { navController.navigate(PlumberRoutes.CHECKOUT) },
                            onBack = { navController.popBackStack() }
                        )
                    }
                }

                composable(
                    route = PlumberRoutes.CHECKOUT,
                    enterTransition = { slideIn() },
                    exitTransition = { parallaxOut() },
                    popEnterTransition = { parallaxIn() },
                    popExitTransition = { slideOut() }
                ) {
                    SwipeBackContainer(onBack = { navController.popBackStack() }) {
                        CheckoutScreen(
                            onOrderPlaced = { _ ->
                                navController.popBackStack(PlumberRoutes.PROFILE, inclusive = false)
                            },
                            onBack = { navController.popBackStack() }
                        )
                    }
                }

                composable(
                    route = PlumberRoutes.ACTIVE_JOB,
                    arguments = listOf(navArgument("jobId") { type = NavType.StringType }),
                    enterTransition = { slideIn() },
                    exitTransition = { parallaxOut() },
                    popEnterTransition = { parallaxIn() },
                    popExitTransition = { slideOut() }
                ) { entry ->
                    val jobId = entry.arguments?.getString("jobId") ?: return@composable
                    SwipeBackContainer(onBack = { navController.popBackStack() }) {
                        ActiveJobScreen(
                            jobId = jobId,
                            onBack = { navController.popBackStack() },
                            onCreateEstimate = { customerId ->
                                navController.navigate(
                                    PlumberRoutes.createEstimate(jobId, customerId)
                                )
                            }
                        )
                    }
                }

                composable(
                    route = PlumberRoutes.CREATE_ESTIMATE,
                    arguments = listOf(
                        navArgument("jobId") { type = NavType.StringType },
                        navArgument("customerId") { type = NavType.StringType }
                    ),
                    enterTransition = { slideIn() },
                    exitTransition = { parallaxOut() },
                    popEnterTransition = { parallaxIn() },
                    popExitTransition = { slideOut() }
                ) { entry ->
                    val jobId = entry.arguments?.getString("jobId") ?: return@composable
                    val customerId = entry.arguments?.getString("customerId") ?: return@composable
                    SwipeBackContainer(onBack = { navController.popBackStack() }) {
                        CreateEstimateScreen(
                            jobId = jobId,
                            customerId = customerId,
                            onEstimateCreated = { navController.popBackStack() },
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }

            // Floating Active Job Pill in bottom-right corner when job is running and user navigated away
            if (activeJobId != null && currentRoute?.startsWith("active_job") != true) {
                androidx.compose.material3.Surface(
                    onClick = {
                        activeJobId?.let { jId ->
                            navController.navigate(PlumberRoutes.activeJob(jId))
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = if (showNav) 78.dp else 24.dp),
                    shape = androidx.compose.foundation.shape.CircleShape,
                    color = com.princesanitary.grahak.ui.theme.BentoGreen,
                    shadowElevation = 8.dp,
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, androidx.compose.ui.graphics.Color.White.copy(alpha = 0.35f))
                ) {
                    androidx.compose.foundation.layout.Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .clip(androidx.compose.foundation.shape.CircleShape)
                                .background(androidx.compose.ui.graphics.Color.White)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Active Job • Return",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                            ),
                            color = com.princesanitary.grahak.ui.theme.BentoOnVibrant
                        )
                    }
                }
            }
        }
    }
}
