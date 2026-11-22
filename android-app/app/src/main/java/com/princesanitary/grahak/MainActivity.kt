package com.princesanitary.grahak

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.princesanitary.grahak.di.ServiceLocator
import com.princesanitary.grahak.ui.GrahakApp
import com.princesanitary.grahak.ui.theme.GrahakTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    /**
     * Runtime permissions for notifications and fine/coarse device location
     */
    private val runtimePermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> /* In-app fallbacks and location sheets continue working seamlessly */ }

    private var knownOrderIds: Set<String>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        // Draws behind the status/navigation bars; individual screens apply
        // their own window-inset padding.
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        requestPermissionsIfNeeded()
        syncPushToken()
        startOwnerLiveOrderMonitor()

        setContent {
            GrahakTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    GrahakApp()
                }
            }
        }
    }

    private fun requestPermissionsIfNeeded() {
        val permissionsToRequest = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val notifGranted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!notifGranted) permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        if (permissionsToRequest.isNotEmpty()) {
            runtimePermissionsLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    private fun startOwnerLiveOrderMonitor() {
        CoroutineScope(Dispatchers.IO).launch {
            ServiceLocator.auth.addAuthStateListener { auth ->
                val email = auth.currentUser?.email?.lowercase().orEmpty()
                if (com.princesanitary.grahak.data.repository.admin.AdminRepository.isOwnerEmail(email)) {
                    CoroutineScope(Dispatchers.IO).launch {
                        ServiceLocator.adminRepository.getAllOrders().collect { res ->
                            res.onSuccess { orders ->
                                val currentIds = orders.map { it.id }.toSet()
                                if (knownOrderIds != null) {
                                    val newOrders = orders.filter { !knownOrderIds!!.contains(it.id) }
                                    for (order in newOrders) {
                                        com.princesanitary.grahak.data.service.NotificationHelper.showOrderNotification(
                                            context = applicationContext,
                                            title = "New order received",
                                            body = "Order #${order.getDisplayOrderNumber()} • ₹${"%.2f".format(order.billing.grandTotal)} • ${order.items.size} item(s) by ${order.customerName.ifBlank { "Customer" }}",
                                            orderId = order.id
                                        )
                                    }
                                }
                                knownOrderIds = currentIds
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Refreshes this device's FCM token on every launch.
     *
     * `onNewToken` only fires when the token actually changes, so without this a
     * device that was signed out when the token was issued would never be
     * reachable again.
     */
    private fun syncPushToken() {
        CoroutineScope(Dispatchers.IO).launch {
            ServiceLocator.userRepository.syncDeviceToken()
        }
    }
}
