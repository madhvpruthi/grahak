package com.princesanitary.grahak.ui.consumer

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.princesanitary.grahak.data.repository.admin.AdminRepository
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.BrightnessAuto
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Chat
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Logout
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PhoneMissed
import androidx.compose.material.icons.rounded.Receipt
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.produceState
import com.princesanitary.grahak.data.model.StoreSettings
import com.princesanitary.grahak.ui.consumer.orders.OrderHistoryUiState
import com.princesanitary.grahak.ui.consumer.orders.OrderHistoryViewModel
import com.princesanitary.grahak.di.ServiceLocator
import com.princesanitary.grahak.di.appViewModel
import com.princesanitary.grahak.ui.components.CircleIconButton
import com.princesanitary.grahak.ui.components.GrahakCard
import com.princesanitary.grahak.ui.components.SecondaryButton
import com.princesanitary.grahak.ui.consumer.profile.ConsumerProfileViewModel
import com.princesanitary.grahak.ui.theme.AppThemeMode
import com.princesanitary.grahak.ui.theme.EmeraldSignal
import com.princesanitary.grahak.ui.theme.Ink
import com.princesanitary.grahak.ui.theme.Line
import com.princesanitary.grahak.ui.theme.LuxeGold
import com.princesanitary.grahak.ui.theme.LuxeGoldSurface
import com.princesanitary.grahak.ui.theme.PaperDim
import com.princesanitary.grahak.ui.theme.StatusError
import com.princesanitary.grahak.ui.theme.StatusWarning
import com.princesanitary.grahak.ui.theme.StatusWarningSoft
import com.princesanitary.grahak.ui.theme.SurfaceWhite
import com.princesanitary.grahak.ui.theme.TextSecondary
import com.princesanitary.grahak.ui.theme.TextTertiary
import com.princesanitary.grahak.ui.theme.ThemeManager
import com.princesanitary.grahak.ui.theme.TintMint

@Composable
fun ConsumerProfileScreen(
    onLogout: () -> Unit,
    onOpenOwnerPanel: (() -> Unit)? = null,
    onNavigateToOrders: () -> Unit = {},
    viewModel: ConsumerProfileViewModel = appViewModel(),
    orderHistoryViewModel: OrderHistoryViewModel = appViewModel()
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val orderHistoryState by orderHistoryViewModel.uiState.collectAsStateWithLifecycle()

    val storeSettings by ServiceLocator.orderRepository.observeStoreSettings()
        .collectAsStateWithLifecycle(initialValue = com.princesanitary.grahak.data.local.SettingsStorage.getCachedStoreSettings())
    val ownerContactPhone = storeSettings.ownerPhone

    val savedAddresses = remember(orderHistoryState) {
        val orders = (orderHistoryState as? OrderHistoryUiState.Success)?.orders.orEmpty()
        orders.map { it.delivery.address }
            .filter { it.fullAddress.isNotBlank() }
            .distinctBy { it.fullAddress.trim().lowercase() }
            .take(5)
    }

    val user = remember { ServiceLocator.auth.currentUser }
    val isOwner = remember(user) {
        AdminRepository.isOwnerEmail(user?.email)
    }

    val name = state.profile?.name?.takeIf { it.isNotBlank() }
        ?: user?.displayName?.takeIf { it.isNotBlank() }
        ?: user?.email?.substringBefore("@")
        ?: "Guest"
    val email = state.profile?.email?.takeIf { it.isNotBlank() }
        ?: user?.email
        ?: "Not signed in"
    val phone = state.profile?.phone.orEmpty()

    var confirmLogout by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf(false) }
    var showSavedAddresses by remember { mutableStateOf(false) }
    var showHelpSupport by remember { mutableStateOf(false) }
    var showPrivacy by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(18.dp))

        Text(
            text = "Account",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
            color = Ink
        )

        Spacer(Modifier.height(20.dp))

        // ---- identity card ----
        GrahakCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(LuxeGoldSurface, CircleShape)
                        .border(1.dp, LuxeGold.copy(alpha = 0.4f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = name.take(1).uppercase(),
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = LuxeGold
                    )
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = Ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = email,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (phone.isNotBlank()) {
                        Text(
                            text = "+91 $phone",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = LuxeGold
                        )
                    }
                }

                CircleIconButton(
                    icon = Icons.Rounded.Edit,
                    onClick = { editing = true },
                    contentDescription = "Edit details"
                )
            }
        }

        // Missing-phone warning
        if (!state.loading && phone.isBlank()) {
            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(StatusWarningSoft)
                    .clickable { editing = true }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Rounded.PhoneMissed,
                    contentDescription = null,
                    tint = StatusWarning,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "Add your mobile number",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = Ink
                    )
                    Text(
                        text = "Plumbers can't call you without it. Tap to add.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
                Icon(Icons.Rounded.ChevronRight, null, Modifier.size(20.dp), tint = StatusWarning)
            }
        }

        Spacer(Modifier.height(20.dp))

        // ---- Appearance (Dark / Light / System Mode) ----
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Rounded.Palette,
                contentDescription = null,
                tint = LuxeGold,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Appearance",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Ink
            )
        }
        Spacer(Modifier.height(10.dp))

        val currentTheme by ThemeManager.themeMode.collectAsStateWithLifecycle()

        GrahakCard(contentPadding = 6.dp) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AppThemeMode.entries.forEach { mode ->
                    val isSelected = currentTheme == mode
                    val (label, icon) = when (mode) {
                        AppThemeMode.DARK -> "Dark" to Icons.Rounded.DarkMode
                        AppThemeMode.LIGHT -> "Light" to Icons.Rounded.LightMode
                        AppThemeMode.SYSTEM -> "System" to Icons.Rounded.BrightnessAuto
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isSelected) Ink else PaperDim)
                            .clickable { ThemeManager.setThemeMode(mode) }
                            .padding(vertical = 12.dp, horizontal = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                icon,
                                contentDescription = null,
                                tint = if (isSelected) SurfaceWhite else TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                ),
                                color = if (isSelected) SurfaceWhite else Ink
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(22.dp))

        // ---- Settings List with interactive actions ----
        GrahakCard(contentPadding = 6.dp) {
            ProfileRow(
                icon = Icons.Rounded.LocationOn,
                title = "Saved addresses",
                subtitle = "Home, work and site drops",
                onClick = { showSavedAddresses = true }
            )
            RowDivider()
            ProfileRow(
                icon = Icons.Rounded.Receipt,
                title = "Order history",
                subtitle = "Invoices and past orders",
                onClick = onNavigateToOrders
            )
            RowDivider()
            ProfileRow(
                icon = Icons.Rounded.Headphones,
                title = "Help & support",
                subtitle = "Talk to Prince Sanitary Store",
                onClick = { showHelpSupport = true }
            )
            RowDivider()
            ProfileRow(
                icon = Icons.Rounded.Shield,
                title = "Privacy & Security",
                subtitle = "Manage your data & permissions",
                onClick = { showPrivacy = true }
            )
        }

        if (isOwner && onOpenOwnerPanel != null) {
            Spacer(Modifier.height(18.dp))
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .clickable { onOpenOwnerPanel() },
                shape = RoundedCornerShape(22.dp),
                color = SurfaceWhite,
                border = BorderStroke(1.dp, Line)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(LuxeGoldSurface),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Rounded.Storefront,
                            contentDescription = null,
                            tint = LuxeGold,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Store Owner Dashboard",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Ink
                            )
                            Spacer(Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(LuxeGold)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "ADMIN",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 8.sp
                                    ),
                                    color = Color(0xFF09090B)
                                )
                            }
                        }
                        Text(
                            text = "Add products, verify plumbers, dispatch orders",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                    Icon(
                        Icons.Rounded.ChevronRight,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(26.dp))

        SecondaryButton(
            text = "Log out",
            leadingIcon = Icons.Rounded.Logout,
            onClick = { confirmLogout = true }
        )

        Spacer(Modifier.height(24.dp))

        Text(
            text = "Grahak · Prince Sanitary Store",
            style = MaterialTheme.typography.bodySmall,
            color = TextTertiary,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(30.dp))
    }

    // ---- Dialog: Saved Addresses ----
    if (showSavedAddresses) {
        AlertDialog(
            onDismissRequest = { showSavedAddresses = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.LocationOn, null, tint = LuxeGold, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Saved Delivery Locations", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                }
            },
            text = {
                if (savedAddresses.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(PaperDim),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Rounded.LocationOn,
                                    contentDescription = null,
                                    tint = TextSecondary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(Modifier.height(10.dp))
                            Text(
                                text = "No saved addresses",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = Ink
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Locations from your placed orders will appear here automatically.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        savedAddresses.forEachIndexed { index, address ->
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = PaperDim,
                                border = BorderStroke(1.dp, Line),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = address.tag.ifBlank { "Delivery Location" },
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.titleSmall,
                                                color = Ink
                                            )
                                            if (index == 0) {
                                                Spacer(Modifier.width(6.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .clip(CircleShape)
                                                        .background(EmeraldSignal.copy(alpha = 0.15f))
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        "RECENT",
                                                        color = EmeraldSignal,
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.ExtraBold
                                                    )
                                                }
                                            }
                                        }
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .clickable {
                                                    clipboardManager.setText(AnnotatedString(address.fullAddress))
                                                    Toast.makeText(context, "Address copied to clipboard", Toast.LENGTH_SHORT).show()
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Rounded.ContentCopy,
                                                contentDescription = "Copy",
                                                tint = TextSecondary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = address.fullAddress,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSavedAddresses = false }) {
                    Text("Done", color = LuxeGold, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = SurfaceWhite
        )
    }

    // ---- Dialog: Help & Support (Store Helpline & WhatsApp) ----
    if (showHelpSupport) {
        val cleanPhone = ownerContactPhone.filter(Char::isDigit)
        AlertDialog(
            onDismissRequest = { showHelpSupport = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Headphones, null, tint = LuxeGold, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Store Support", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        text = "Direct connection with Prince Sanitary Store order dispatch team & master technicians.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )

                    // Call Store CTA
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = PaperDim,
                        border = BorderStroke(1.dp, Line),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable {
                                try {
                                    val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:+91$cleanPhone"))
                                    context.startActivity(dialIntent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Helpline: +91 $cleanPhone", Toast.LENGTH_LONG).show()
                                }
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(EmeraldSignal.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Rounded.Call, contentDescription = null, tint = EmeraldSignal, modifier = Modifier.size(20.dp))
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text("Call Prince Sanitary Store", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall, color = Ink)
                                Text(
                                    text = if (ownerContactPhone.isNotBlank()) "+91 $ownerContactPhone (Direct Helpline)" else "Direct Store Helpline",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                            Icon(Icons.Rounded.ChevronRight, null, tint = TextTertiary, modifier = Modifier.size(20.dp))
                        }
                    }

                    // WhatsApp Support CTA
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = PaperDim,
                        border = BorderStroke(1.dp, Line),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable {
                                try {
                                    val waIntent = Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse("https://wa.me/91$cleanPhone?text=${Uri.encode("Hello Prince Sanitary Store, I need assistance with an order.")}")
                                    )
                                    context.startActivity(waIntent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "WhatsApp: +91 $cleanPhone", Toast.LENGTH_LONG).show()
                                }
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF25D366).copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Rounded.Chat, contentDescription = null, tint = Color(0xFF25D366), modifier = Modifier.size(20.dp))
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text("Chat on WhatsApp", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall, color = Ink)
                                Text("Instant queries, bills & stock verification", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            }
                            Icon(Icons.Rounded.ChevronRight, null, tint = TextTertiary, modifier = Modifier.size(20.dp))
                        }
                    }

                    // Store Hours
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.AccessTime, contentDescription = null, tint = LuxeGold, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Mon - Sat: 9:00 AM - 8:30 PM", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showHelpSupport = false }) {
                    Text("Close", color = LuxeGold, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = SurfaceWhite
        )
    }

    // ---- Dialog: Privacy & Security ----
    if (showPrivacy) {
        AlertDialog(
            onDismissRequest = { showPrivacy = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Security, null, tint = LuxeGold, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Privacy & Security", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Your privacy and commercial data are protected with strict standards.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Ink
                    )
                    Spacer(Modifier.height(4.dp))
                    Row {
                        Icon(Icons.Rounded.Lock, null, tint = EmeraldSignal, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "End-to-end encrypted order and billing details. No external advertising networks.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                    Row {
                        Icon(Icons.Rounded.Shield, null, tint = LuxeGold, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Phone number is only disclosed to the specific certified plumber assigned to your site.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPrivacy = false }) {
                    Text("Understood", color = LuxeGold, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = SurfaceWhite
        )
    }

    // ---- Dialog: Edit Details ----
    if (editing) {
        var draftName by remember { mutableStateOf(name.takeIf { it != "Guest" } ?: "") }
        var draftPhone by remember { mutableStateOf(phone) }

        AlertDialog(
            onDismissRequest = {
                editing = false
                viewModel.clearMessages()
            },
            title = { Text("Your details", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)) },
            text = {
                Column {
                    OutlinedTextField(
                        value = draftName,
                        onValueChange = { draftName = it },
                        label = { Text("Full name") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = LuxeGold,
                            unfocusedBorderColor = Line
                        )
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = draftPhone,
                        onValueChange = { draftPhone = it.filter(Char::isDigit).take(10) },
                        label = { Text("Mobile number") },
                        placeholder = { Text("10-digit number") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = LuxeGold,
                            unfocusedBorderColor = Line
                        )
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "Shared with the plumber you book so they can reach you on site. Applies to new bookings.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    state.error?.let {
                        Spacer(Modifier.height(10.dp))
                        Text(it, style = MaterialTheme.typography.bodySmall, color = StatusError)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !state.saving,
                    onClick = { viewModel.save(draftName, draftPhone) }
                ) {
                    Text(if (state.saving) "Saving…" else "Save", color = LuxeGold, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    editing = false
                    viewModel.clearMessages()
                }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = SurfaceWhite
        )

        LaunchedEffect(state.savedMessage) {
            if (state.savedMessage != null) {
                editing = false
                viewModel.clearMessages()
            }
        }
    }

    if (confirmLogout) {
        AlertDialog(
            onDismissRequest = { confirmLogout = false },
            title = { Text("Log out?", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)) },
            text = {
                Text(
                    "You'll need to sign in again to place orders or track site deliveries.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmLogout = false
                    onLogout()
                }) {
                    Text("Log out", color = StatusError, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmLogout = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = SurfaceWhite
        )
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
            .clip(RoundedCornerShape(12.dp))
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
            Icon(icon, null, Modifier.size(18.dp), tint = LuxeGold)
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold), color = Ink)
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
