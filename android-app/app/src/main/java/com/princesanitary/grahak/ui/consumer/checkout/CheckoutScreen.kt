@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.princesanitary.grahak.ui.consumer.checkout

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import java.util.Locale
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.princesanitary.grahak.data.model.*
import com.princesanitary.grahak.di.appViewModel
import com.princesanitary.grahak.ui.components.BackButton
import com.princesanitary.grahak.ui.components.formatRupees
import com.princesanitary.grahak.ui.consumer.services.LocationPickerSheet
import com.princesanitary.grahak.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutScreen(
    viewModel: CheckoutViewModel = appViewModel(),
    onOrderPlaced: (String) -> Unit,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val actionState by viewModel.actionState.collectAsStateWithLifecycle()
    val isPlumber by viewModel.isPlumber.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var orderNotes by remember { mutableStateOf("") }
    var couponInput by remember { mutableStateOf("") }
    var showAddressDialog by remember { mutableStateOf(false) }
    var showMapPicker by remember { mutableStateOf(false) }

    // Standard NPCI UPI Result Contract: Order is ONLY placed if payment status returns SUCCESS
    val upiLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data = result.data
        val responseStr = data?.getStringExtra("response")
            ?: data?.getStringExtra("Status")
            ?: data?.data?.toString()
            ?: ""

        val lower = responseStr.lowercase()
        val isSuccess = (result.resultCode == Activity.RESULT_OK && lower.contains("success") && !lower.contains("fail")) ||
            lower.contains("status=success")

        if (isSuccess) {
            Toast.makeText(context, "UPI Payment Successful! Placing order...", Toast.LENGTH_SHORT).show()
            viewModel.placeOrder(orderNotes)
        } else {
            Toast.makeText(context, "Payment was not completed. Order not placed.", Toast.LENGTH_LONG).show()
        }
    }

    var completedOrderId by remember { mutableStateOf<String?>(null) }

    // Handle order placement
    LaunchedEffect(actionState) {
        when (val state = actionState) {
            is CheckoutActionState.OrderPlaced -> {
                completedOrderId = state.orderId
            }
            is CheckoutActionState.Error -> {
                Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                viewModel.resetActionState()
            }
            else -> {}
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Paper)
    ) {
        Column(Modifier.fillMaxSize()) {
            // ---- Minimalist Top Header (Matching CartScreen) ----
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BackButton(onClick = onBack)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "Checkout",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = 22.sp
                        ),
                        color = Ink
                    )
                    Text(
                        text = "Prince Sanitary Store • Saharanpur",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                        color = TextSecondary
                    )
                }
            }

            when (val state = uiState) {
                is CheckoutUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = LuxeGold, strokeWidth = 2.5.dp)
                    }
                }
                is CheckoutUiState.Success -> {
                    CheckoutContent(
                        state = state,
                        isPlumber = isPlumber,
                        orderNotes = orderNotes,
                        couponInput = couponInput,
                        onCouponInputChange = { couponInput = it },
                        onNotesChange = { orderNotes = it },
                        onUpdateCustomerDetails = { name, phone -> viewModel.updateCustomerDetails(name, phone) },
                        onApplyCoupon = { code -> viewModel.applyCoupon(code) },
                        onRemoveCoupon = { viewModel.removeCoupon() },
                        onSelectAddress = { showAddressDialog = true },
                        onPinOnMap = { showMapPicker = true },
                        onSelectDeliveryType = { viewModel.selectDeliveryType(it) },
                        onSelectPaymentMethod = { viewModel.selectPaymentMethod(it) },
                        onPlaceOrder = {
                            if (state.customerName.trim().length < 2) {
                                Toast.makeText(context, "Please enter your name", Toast.LENGTH_SHORT).show()
                                return@CheckoutContent
                            }
                            if (state.customerPhone.filter { it.isDigit() }.length < 10) {
                                Toast.makeText(context, "Please enter a valid 10-digit mobile number", Toast.LENGTH_SHORT).show()
                                return@CheckoutContent
                            }
                            if (state.selectedAddress == null) {
                                Toast.makeText(context, "Please select delivery address in Saharanpur", Toast.LENGTH_SHORT).show()
                                return@CheckoutContent
                            }

                            if (state.selectedPaymentMethod == PaymentMethod.UPI && state.storeSettings.ownerUpiId.isNotBlank()) {
                                val cleanUpi = state.storeSettings.ownerUpiId.trim()
                                val bizName = state.storeSettings.ownerBusinessName.trim().ifBlank { "Prince Sanitary Store" }
                                val upiUri = Uri.parse("upi://pay").buildUpon()
                                    .appendQueryParameter("pa", cleanUpi)
                                    .appendQueryParameter("pn", bizName)
                                    .appendQueryParameter("mc", "")
                                    .appendQueryParameter("tr", "ORD" + System.currentTimeMillis().toString().takeLast(10))
                                    .appendQueryParameter("tn", "Order payment to $bizName")
                                    .appendQueryParameter("am", String.format(Locale.US, "%.2f", state.grandTotal))
                                    .appendQueryParameter("cu", "INR")
                                    .build()
                                val upiIntent = Intent(Intent.ACTION_VIEW, upiUri)
                                try {
                                    upiLauncher.launch(Intent.createChooser(upiIntent, "Pay ₹${String.format(Locale.US, "%.2f", state.grandTotal)} via UPI"))
                                } catch (_: Exception) {
                                    Toast.makeText(context, "No UPI app found. You can pay on delivery.", Toast.LENGTH_LONG).show()
                                    viewModel.selectPaymentMethod(PaymentMethod.COD)
                                }
                            } else {
                                viewModel.placeOrder(orderNotes)
                            }
                        },
                        isPlacingOrder = actionState is CheckoutActionState.PlacingOrder,
                        modifier = Modifier.weight(1f)
                    )

                    // Address selection dialog
                    if (showAddressDialog) {
                        AddressSelectionDialog(
                            addresses = viewModel.getAvailableAddresses(),
                            selectedAddress = state.selectedAddress,
                            onAddressSelected = { address ->
                                viewModel.selectAddress(address)
                                showAddressDialog = false
                            },
                            onPickOnMap = {
                                showAddressDialog = false
                                showMapPicker = true
                            },
                            onDismiss = { showAddressDialog = false }
                        )
                    }

                    // Saharanpur Map Location Picker Sheet
                    if (showMapPicker) {
                        LocationPickerSheet(
                            onDismiss = { showMapPicker = false },
                            onConfirm = { pinnedAddress ->
                                viewModel.addAndSelectAddress(pinnedAddress)
                                showMapPicker = false
                            }
                        )
                    }
                }
                is CheckoutUiState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Text(
                                text = state.message,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = onBack,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = LuxeGold,
                                    contentColor = Color(0xFF09090B)
                                )
                            ) {
                                Text("Go Back", color = Color(0xFF09090B), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        completedOrderId?.let { orderId ->
            OrderCompletionAnimationOverlay(
                orderId = orderId,
                onAnimationFinished = {
                    onOrderPlaced(orderId)
                }
            )
        }
    }
}

@Composable
fun CheckoutContent(
    state: CheckoutUiState.Success,
    isPlumber: Boolean,
    orderNotes: String,
    couponInput: String,
    onCouponInputChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    onUpdateCustomerDetails: (name: String, phone: String) -> Unit,
    onApplyCoupon: (String) -> Unit,
    onRemoveCoupon: () -> Unit,
    onSelectAddress: () -> Unit,
    onPinOnMap: () -> Unit,
    onSelectDeliveryType: (DeliveryType) -> Unit,
    onSelectPaymentMethod: (PaymentMethod) -> Unit,
    onPlaceOrder: () -> Unit,
    isPlacingOrder: Boolean,
    modifier: Modifier = Modifier
) {
    val summary = state.summary
    val plumberCashback = Math.round(summary.subtotal * (state.storeSettings.plumberCommissionRate / 100.0) * 100.0) / 100.0
    val freeDeliveryRemaining = (state.storeSettings.freeDeliveryThreshold - summary.subtotal).coerceAtLeast(0.0)

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 4.dp,
                bottom = 150.dp
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Plumber Perk Card (if applicable)
            if (isPlumber) {
                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = LuxeGoldSurface,
                        border = BorderStroke(1.dp, LuxeGoldBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Stars,
                                contentDescription = null,
                                tint = LuxeGold,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = "Plumber Perk: ${state.storeSettings.plumberCommissionRate.toInt()}% Incentive",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = Ink
                                )
                                Text(
                                    text = "₹$plumberCashback will be credited to your wallet",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }
            }

            // Store Closed Notice (When Owner has turned off store)
            if (!state.storeSettings.isStoreOpen) {
                item {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = Color(0xFF1E1A11),
                        border = BorderStroke(1.dp, LuxeGold.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(LuxeGold.copy(alpha = 0.18f))
                                    .border(1.dp, LuxeGold.copy(alpha = 0.4f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Schedule,
                                    contentDescription = null,
                                    tint = LuxeGold,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = "Scheduled Dispatch: ${state.storeSettings.nextDispatchSchedule.ifBlank { "Tomorrow 10:00 AM" }}",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = LuxeGold
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = "Store is currently closed for live dispatches. Your order will be placed & queued for the scheduled delivery.",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                                    color = Color(0xFFD4D4D8)
                                )
                            }
                        }
                    }
                }
            }

            // 1. Delivery Address Card (Matching Cart Address Tile)
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = SurfaceWhite,
                    border = BorderStroke(1.dp, Line)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(CircleShape)
                                        .background(LuxeGoldSurface),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Rounded.Place,
                                        contentDescription = null,
                                        tint = LuxeGold,
                                        modifier = Modifier.size(17.dp)
                                    )
                                }
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Delivery Address",
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        ),
                                        color = Ink
                                    )
                                    Text(
                                        text = "Prince Sanitary Store • Express Dispatch",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = TextSecondary
                                    )
                                }
                            }

                            Row {
                                TextButton(
                                    onClick = onPinOnMap,
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Icon(Icons.Rounded.Map, contentDescription = null, modifier = Modifier.size(15.dp), tint = LuxeGold)
                                    Spacer(Modifier.width(4.dp))
                                    Text("Map", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = LuxeGold)
                                }
                                TextButton(
                                    onClick = onSelectAddress,
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text("Change", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = TextSecondary)
                                }
                            }
                        }

                        if (state.selectedAddress != null) {
                            Spacer(Modifier.height(10.dp))
                            HorizontalDivider(color = Line.copy(alpha = 0.6f))
                            Spacer(Modifier.height(10.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = PaperDim
                                ) {
                                    Text(
                                        text = state.selectedAddress.tag.uppercase(),
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.sp),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        color = Ink
                                    )
                                }
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = state.selectedAddress.fullAddress,
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                    color = Ink,
                                    maxLines = 2
                                )
                            }
                        } else {
                            Spacer(Modifier.height(10.dp))
                            Text(
                                text = "Please select or pin a delivery address in Saharanpur",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            // 2. Customer Contact Details (Minimalist)
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = SurfaceWhite,
                    border = BorderStroke(1.dp, Line)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Rounded.Person,
                                contentDescription = null,
                                tint = Ink,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Contact Information",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                ),
                                color = Ink
                            )
                        }

                        Spacer(Modifier.height(12.dp))

                        OutlinedTextField(
                            value = state.customerName,
                            onValueChange = { onUpdateCustomerDetails(it, state.customerPhone) },
                            label = { Text("Your Name *", fontSize = 12.sp) },
                            placeholder = { Text("e.g. Ramesh Kumar", fontSize = 12.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = LuxeGold,
                                focusedLabelColor = LuxeGold,
                                unfocusedBorderColor = Line
                            )
                        )

                        Spacer(Modifier.height(8.dp))

                        OutlinedTextField(
                            value = state.customerPhone,
                            onValueChange = { onUpdateCustomerDetails(state.customerName, it) },
                            label = { Text("10-Digit Mobile Number *", fontSize = 12.sp) },
                            placeholder = { Text("for delivery coordination call", fontSize = 12.sp) },
                            prefix = { Text("+91 ", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = LuxeGold,
                                focusedLabelColor = LuxeGold,
                                unfocusedBorderColor = Line
                            )
                        )
                    }
                }
            }

            // 3. Delivery Mode (Clean Radio Options)
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = SurfaceWhite,
                    border = BorderStroke(1.dp, Line)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Rounded.LocalShipping,
                                    contentDescription = null,
                                    tint = Ink,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "Delivery Mode",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    ),
                                    color = Ink
                                )
                            }

                            if (state.deliveryFee == 0.0 && state.selectedDeliveryType == DeliveryType.SITE_DROP) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = EmeraldSignalSurface
                                ) {
                                    Text(
                                        text = "FREE DELIVERY",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 9.sp
                                        ),
                                        color = EmeraldSignal,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.5.dp)
                                    )
                                }
                            }
                        }

                        if (freeDeliveryRemaining > 0 && state.selectedDeliveryType == DeliveryType.SITE_DROP) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "🚚 Add ₹${"%.0f".format(freeDeliveryRemaining)} more for FREE delivery",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = LuxeGold
                            )
                            Spacer(Modifier.height(5.dp))
                            val progress = if (state.storeSettings.freeDeliveryThreshold > 0)
                                (state.summary.subtotal / state.storeSettings.freeDeliveryThreshold).toFloat().coerceIn(0f, 1f)
                            else 1f
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(CircleShape),
                                color = LuxeGold,
                                trackColor = Line
                            )
                        }

                        Spacer(Modifier.height(10.dp))

                        DeliveryType.values().forEach { type ->
                            val isSelected = state.selectedDeliveryType == type
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) PaperDim else Color.Transparent,
                                border = BorderStroke(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) LuxeGold else Line
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelectDeliveryType(type) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { onSelectDeliveryType(type) },
                                        colors = RadioButtonDefaults.colors(selectedColor = LuxeGold),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = when (type) {
                                                DeliveryType.SITE_DROP -> "Site Drop (${if (state.deliveryFee == 0.0) "FREE" else "₹${state.storeSettings.deliveryFee.toInt()}"})"
                                                DeliveryType.STORE_PICKUP -> "Store Pickup (Prince Sanitary, Numaish Camp)"
                                            },
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 13.sp
                                            ),
                                            color = Ink
                                        )
                                        Text(
                                            text = when (type) {
                                                DeliveryType.SITE_DROP -> "Express dispatch in Saharanpur"
                                                DeliveryType.STORE_PICKUP -> "Ready in 30 mins at shop"
                                            },
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.5.sp),
                                            color = TextSecondary
                                        )
                                    }
                                }
                            }
                            if (type != DeliveryType.values().last()) Spacer(Modifier.height(6.dp))
                        }
                    }
                }
            }

            // 4. Coupons & Voucher Card (Clean, Premium & Perforated)
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = SurfaceWhite,
                    border = BorderStroke(1.dp, Line)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(LuxeGoldSurface),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Rounded.LocalOffer,
                                        contentDescription = null,
                                        tint = LuxeGold,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    text = "Coupons & Offers",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    ),
                                    color = Ink
                                )
                            }

                            if (state.appliedCoupon != null) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = EmeraldSignalSurface
                                ) {
                                    Text(
                                        text = "1 APPLIED",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 9.sp,
                                            letterSpacing = 0.5.sp
                                        ),
                                        color = EmeraldSignal,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.5.dp)
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        if (state.appliedCoupon != null) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = EmeraldSignalSurface,
                                border = BorderStroke(1.dp, EmeraldSignalBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .background(EmeraldSignal),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Rounded.Check,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        Spacer(Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = "Coupon '${state.appliedCoupon.code}' Applied",
                                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                color = Ink
                                            )
                                            Text(
                                                text = "You saved ₹${"%.0f".format(state.discountAmount)} on this order",
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    fontSize = 11.5.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                ),
                                                color = EmeraldSignal
                                            )
                                        }
                                    }
                                    TextButton(onClick = onRemoveCoupon) {
                                        Text(
                                            text = "Remove",
                                            color = MaterialTheme.colorScheme.error,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = couponInput,
                                    onValueChange = onCouponInputChange,
                                    placeholder = { Text("Enter coupon code", fontSize = 12.5.sp) },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Ink,
                                        unfocusedBorderColor = Line
                                    )
                                )
                                Spacer(Modifier.width(8.dp))
                                Button(
                                    onClick = { onApplyCoupon(couponInput) },
                                    shape = RoundedCornerShape(12.dp),
                                    enabled = couponInput.isNotBlank(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Ink,
                                        contentColor = SurfaceWhite
                                    )
                                ) {
                                    Text("Apply", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            if (state.couponError != null) {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = state.couponError,
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = MaterialTheme.colorScheme.error
                                )
                            }

                            if (state.availableCoupons.isNotEmpty()) {
                                Spacer(Modifier.height(12.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    state.availableCoupons.forEach { coupon ->
                                        val isEligible = state.summary.subtotal >= coupon.minOrderAmount
                                        val diff = (coupon.minOrderAmount - state.summary.subtotal).toInt()

                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = if (isEligible) PaperDim else PaperDim.copy(alpha = 0.5f),
                                            border = BorderStroke(
                                                1.dp,
                                                if (isEligible) Ink.copy(alpha = 0.25f) else Line
                                            ),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Text(
                                                            text = coupon.code,
                                                            style = MaterialTheme.typography.labelMedium.copy(
                                                                fontWeight = FontWeight.ExtraBold,
                                                                letterSpacing = 0.5.sp
                                                            ),
                                                            color = Ink
                                                        )
                                                        Spacer(Modifier.width(6.dp))
                                                        Surface(
                                                            shape = RoundedCornerShape(4.dp),
                                                            color = if (isEligible) LuxeGoldSurface else Line
                                                        ) {
                                                            Text(
                                                                text = if (coupon.discountType == DiscountType.PERCENTAGE)
                                                                    "${coupon.discountValue.toInt()}% OFF"
                                                                else
                                                                    "₹${coupon.discountValue.toInt()} FLAT",
                                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp),
                                                                style = MaterialTheme.typography.labelSmall.copy(
                                                                    fontWeight = FontWeight.ExtraBold,
                                                                    fontSize = 8.5.sp
                                                                ),
                                                                color = if (isEligible) LuxeGold else TextSecondary
                                                            )
                                                        }
                                                    }
                                                    Text(
                                                        text = if (isEligible)
                                                            coupon.description.ifBlank { "Valid on this order" }
                                                        else
                                                            "Add ₹$diff more to avail",
                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                            fontSize = 10.sp,
                                                            fontWeight = if (!isEligible) FontWeight.Bold else FontWeight.Normal
                                                        ),
                                                        color = if (!isEligible) LuxeGoldLight else TextSecondary
                                                    )
                                                }

                                                if (isEligible) {
                                                    Surface(
                                                        shape = RoundedCornerShape(8.dp),
                                                        color = Ink,
                                                        modifier = Modifier.clickable { onApplyCoupon(coupon.code) }
                                                    ) {
                                                        Text(
                                                            text = "Apply",
                                                            color = SurfaceWhite,
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 11.5.sp,
                                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                                        )
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

            // 5. Payment Method (Clean, Minimalist, No Rainbow Circles)
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = SurfaceWhite,
                    border = BorderStroke(1.dp, Line)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Rounded.Payment,
                                contentDescription = null,
                                tint = Ink,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Payment Method",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                ),
                                color = Ink
                            )
                        }

                        Spacer(Modifier.height(10.dp))

                        // Option 1: Cash on Delivery
                        val isCodSelected = state.selectedPaymentMethod == PaymentMethod.COD
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isCodSelected) PaperDim else Color.Transparent,
                            border = BorderStroke(
                                width = if (isCodSelected) 1.5.dp else 1.dp,
                                color = if (isCodSelected) LuxeGold else Line
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectPaymentMethod(PaymentMethod.COD) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 11.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isCodSelected,
                                    onClick = { onSelectPaymentMethod(PaymentMethod.COD) },
                                    colors = RadioButtonDefaults.colors(selectedColor = LuxeGold),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    text = "Cash on Delivery",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp
                                    ),
                                    color = Ink
                                )
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        // Option 2: Pay via UPI
                        val isUpiSelected = state.selectedPaymentMethod == PaymentMethod.UPI
                        val isOwnerUpiConfigured = state.storeSettings.ownerUpiId.isNotBlank()
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isUpiSelected) PaperDim else Color.Transparent,
                            border = BorderStroke(
                                width = if (isUpiSelected) 1.5.dp else 1.dp,
                                color = if (isUpiSelected) LuxeGold else Line
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = isOwnerUpiConfigured) { onSelectPaymentMethod(PaymentMethod.UPI) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 11.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isUpiSelected,
                                    onClick = { if (isOwnerUpiConfigured) onSelectPaymentMethod(PaymentMethod.UPI) },
                                    colors = RadioButtonDefaults.colors(selectedColor = LuxeGold),
                                    enabled = isOwnerUpiConfigured,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    text = if (isOwnerUpiConfigured) "UPI (GPay, PhonePe, Paytm)" else "UPI (Not Available)",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp
                                    ),
                                    color = if (isOwnerUpiConfigured) Ink else TextSecondary
                                )
                            }
                        }
                    }
                }
            }

            // 6. Bill Summary (Exact Cart Screen Style)
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = SurfaceWhite,
                    border = BorderStroke(1.dp, Line)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Rounded.Receipt,
                                contentDescription = null,
                                tint = Ink,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Bill Summary",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                ),
                                color = Ink
                            )
                        }

                        Spacer(Modifier.height(10.dp))

                        // Items line
                        state.cart.items.forEach { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${item.productName} × ${item.quantity}",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                                    color = TextSecondary,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1
                                )
                                Text(
                                    text = formatRupees(item.total),
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    color = Ink
                                )
                            }
                        }

                        Spacer(Modifier.height(8.dp))
                        HorizontalDivider(color = Line.copy(alpha = 0.6f))
                        Spacer(Modifier.height(8.dp))

                        // Subtotal
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Items Total", style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp), color = TextSecondary)
                            Text(formatRupees(summary.subtotal), style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp), color = Ink)
                        }

                        Spacer(Modifier.height(4.dp))

                        // Delivery
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Delivery Fee", style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp), color = TextSecondary)
                            if (state.deliveryFee == 0.0) {
                                Text(
                                    "FREE",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold),
                                    color = EmeraldSignal
                                )
                            } else {
                                Text(formatRupees(state.deliveryFee), style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp), color = Ink)
                            }
                        }

                        // Discount
                        if (state.discountAmount > 0.0) {
                            Spacer(Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "Coupon Discount",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, fontWeight = FontWeight.SemiBold),
                                    color = EmeraldSignal
                                )
                                Text(
                                    "- ${formatRupees(state.discountAmount)}",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold),
                                    color = EmeraldSignal
                                )
                            }
                        }

                        Spacer(Modifier.height(8.dp))
                        HorizontalDivider(color = Line.copy(alpha = 0.6f))
                        Spacer(Modifier.height(8.dp))

                        // Grand Total
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Grand Total",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    fontSize = 15.sp
                                ),
                                color = Ink
                            )
                            Text(
                                formatRupees(state.grandTotal),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    fontSize = 16.sp
                                ),
                                color = Ink
                            )
                        }
                    }
                }
            }

            // 7. Delivery Instructions
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = SurfaceWhite,
                    border = BorderStroke(1.dp, Line)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Delivery Instructions (Optional)",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            ),
                            color = Ink
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = orderNotes,
                            onValueChange = onNotesChange,
                            placeholder = { Text("e.g. Near clock tower, call before arrival…", fontSize = 12.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = LuxeGold,
                                unfocusedBorderColor = Line
                            )
                        )
                    }
                }
            }
        }

        // ---- Pinned Bottom Dock (Matching AestheticCheckoutPanel in CartScreen) ----
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            color = SurfaceWhite,
            shadowElevation = 20.dp,
            border = BorderStroke(1.dp, Line)
        ) {
            Column(
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(horizontal = 18.dp, vertical = 14.dp)
            ) {
                // Payment preview row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(PaperDim)
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Rounded.Payment,
                            contentDescription = null,
                            tint = LuxeGold,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (state.selectedPaymentMethod == PaymentMethod.UPI)
                                "Payment: UPI App"
                            else
                                "Payment: Cash on Delivery",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.5.sp
                            ),
                            color = Ink
                        )
                    }
                    Text(
                        text = "100% Secure",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        ),
                        color = EmeraldSignal
                    )
                }

                Spacer(Modifier.height(10.dp))

                // Full width tactile Place Order button
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = LuxeGold,
                    shadowElevation = 4.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .clickable(enabled = !isPlacingOrder) { onPlaceOrder() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (state.selectedPaymentMethod == PaymentMethod.UPI && state.storeSettings.ownerUpiId.isNotBlank())
                                "Pay via UPI App"
                            else
                                "Place Order",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = 15.sp
                            ),
                            color = Color(0xFF09090B)
                        )

                        if (isPlacingOrder) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color(0xFF09090B),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = formatRupees(state.grandTotal),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Black,
                                        fontSize = 16.sp
                                    ),
                                    color = Color(0xFF09090B)
                                )
                                Spacer(Modifier.width(6.dp))
                                Icon(
                                    Icons.AutoMirrored.Rounded.ArrowForward,
                                    contentDescription = null,
                                    tint = Color(0xFF09090B),
                                    modifier = Modifier.size(18.dp)
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
 * Saharanpur-focused Saved Addresses Dialog with direct "Pin on Map" action
 */
@Composable
fun AddressSelectionDialog(
    addresses: List<SavedAddress>,
    selectedAddress: SavedAddress?,
    onAddressSelected: (SavedAddress) -> Unit,
    onPickOnMap: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Place, contentDescription = null, tint = LuxeGold)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Select Delivery Address", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Pin on Map CTA
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = LuxeGoldSurface,
                    border = BorderStroke(1.dp, LuxeGoldBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPickOnMap() }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.AddLocationAlt, contentDescription = null, tint = LuxeGold)
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("Pin New Location on Map", fontWeight = FontWeight.Bold, color = LuxeGold, fontSize = 13.sp)
                            Text("Choose exact location in Saharanpur", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp), color = TextSecondary)
                        }
                    }
                }

                Text("Saved Addresses (Saharanpur)", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp), color = TextSecondary, fontWeight = FontWeight.Bold)

                addresses.forEach { address ->
                    val isSelected = address == selectedAddress
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onAddressSelected(address) },
                        border = if (isSelected) BorderStroke(1.5.dp, LuxeGold) else BorderStroke(1.dp, Line),
                        color = if (isSelected) LuxeGoldSurface else SurfaceWhite
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = address.tag,
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (isSelected) LuxeGold else Ink
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "• Saharanpur",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                                    color = TextSecondary
                                )
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = address.fullAddress,
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                                color = Ink
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = TextSecondary)
            }
        },
        containerColor = SurfaceWhite
    )
}
