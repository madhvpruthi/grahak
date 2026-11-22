package com.princesanitary.grahak.ui.consumer.services

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.CameraMoveStartedReason
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.rememberCameraPositionState
import com.princesanitary.grahak.data.model.SavedAddress
import com.princesanitary.grahak.ui.components.PillChip
import com.princesanitary.grahak.ui.components.PrimaryButton
import com.princesanitary.grahak.ui.theme.Ink
import com.princesanitary.grahak.ui.theme.Line
import com.princesanitary.grahak.ui.theme.Paper
import com.princesanitary.grahak.ui.theme.StatusError
import com.princesanitary.grahak.ui.theme.StatusErrorSoft
import com.princesanitary.grahak.ui.theme.StatusSuccess
import com.princesanitary.grahak.ui.theme.SurfaceWhite
import com.princesanitary.grahak.ui.theme.TextSecondary
import com.princesanitary.grahak.ui.theme.TextTertiary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/* ===========================================================================
 * Service area: Saharanpur, Uttar Pradesh - 247001
 *
 * The store delivers and dispatches plumbers locally, so the map is clamped to
 * the city rather than letting someone pin a site in another state. The camera
 * physically cannot be panned outside these bounds, which is a friendlier
 * constraint than allowing it and rejecting the choice afterwards.
 * =========================================================================== */
private val SAHARANPUR_CENTER = LatLng(29.9680, 77.5552)

private val SERVICE_AREA: LatLngBounds = LatLngBounds(
    LatLng(29.8750, 77.4400), // south-west
    LatLng(30.0600, 77.6800)  // north-east
)

private const val SERVICE_CITY = "Saharanpur"
private const val SERVICE_PINCODE = "247001"
private const val MIN_ZOOM = 12f

/**
 * Map site picker for the FixIt service area.
 *
 * Flow, top to bottom:
 *  1. type an address and submit -> it is geocoded *within Saharanpur* and the
 *     camera flies there,
 *  2. fine-tune by dragging the map under the fixed centre pin,
 *  3. the panel underneath shows the resolved address for confirmation.
 *
 * Reverse geocoding only runs when the camera stops, and is skipped while the
 * user is typing, so the address field never fights the user for control.
 */
@Composable
fun LocationPickerSheet(
    onDismiss: () -> Unit,
    onConfirm: (SavedAddress) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val keyboard = LocalSoftwareKeyboardController.current

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(SAHARANPUR_CENTER, 14f)
    }

    // What the user typed / what we resolved.
    var searchQuery by remember { mutableStateOf("") }
    var resolvedAddress by remember { mutableStateOf("") }
    var houseDetail by remember { mutableStateOf("") }
    var tag by remember { mutableStateOf("Site") }

    var searching by remember { mutableStateOf(false) }
    var resolving by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf<String?>(null) }

    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    /* ----------------------------- geocoding ----------------------------- */

    /** Address text -> coordinates */
    suspend fun geocodeForward(query: String): LatLng? = withContext(Dispatchers.IO) {
        try {
            @Suppress("DEPRECATION")
            val results = Geocoder(context).getFromLocationName(
                "$query, Uttar Pradesh",
                5
            )
            results?.firstOrNull()?.let { LatLng(it.latitude, it.longitude) }
        } catch (_: Exception) {
            null
        }
    }

    /** Coordinates -> readable address. */
    suspend fun geocodeReverse(point: LatLng): Address? = withContext(Dispatchers.IO) {
        try {
            @Suppress("DEPRECATION")
            Geocoder(context)
                .getFromLocation(point.latitude, point.longitude, 1)
                ?.firstOrNull()
        } catch (_: Exception) {
            null
        }
    }

    fun runSearch() {
        val q = searchQuery.trim()
        if (q.isEmpty()) return

        keyboard?.hide()
        scope.launch {
            searching = true
            notice = null
            val hit = geocodeForward(q)
            searching = false

            if (hit == null) {
                notice = "Couldn't find \"$q\". Try a nearby landmark or pan the map."
            } else {
                // Camera move triggers the reverse-geocode below.
                cameraPositionState.position = CameraPosition.fromLatLngZoom(hit, 17f)
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun centreOnUser() {
        if (!hasLocationPermission) return
        scope.launch {
            try {
                val fused = LocationServices.getFusedLocationProviderClient(context)
                var loc = fused.lastLocation.await()
                if (loc == null) {
                    loc = fused.getCurrentLocation(
                        com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY,
                        null
                    ).await()
                }

                if (loc != null) {
                    val here = LatLng(loc.latitude, loc.longitude)
                    cameraPositionState.position = CameraPosition.fromLatLngZoom(here, 17f)
                    if (!SERVICE_AREA.contains(here)) {
                        notice = "Note: FixIt delivery operates primarily in Saharanpur."
                    } else {
                        notice = null
                    }
                } else {
                    notice = "Turn on device GPS location to center automatically."
                }
            } catch (_: Exception) {
                notice = "Couldn't get your location. Please pan the map to your site."
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val granted = perms.values.any { it }
        hasLocationPermission = granted
        if (granted) centreOnUser()
    }

    // Reverse-geocode once the camera settles.
    LaunchedEffect(cameraPositionState.isMoving) {
        if (cameraPositionState.isMoving) return@LaunchedEffect

        resolving = true
        val address = geocodeReverse(cameraPositionState.position.target)
        resolving = false

        if (address != null) {
            resolvedAddress = listOfNotNullBlank(
                address.subLocality ?: address.featureName,
                address.locality ?: address.subAdminArea ?: address.adminArea ?: SERVICE_CITY,
                address.postalCode ?: SERVICE_PINCODE
            ).joinToString(", ")
        }
    }

    /* -------------------------------- UI -------------------------------- */

    val pinInArea = SERVICE_AREA.contains(cameraPositionState.position.target)

    Surface(modifier = Modifier.fillMaxSize(), color = Paper) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {

            // ---------------- header + address search ----------------
            Column(Modifier.padding(horizontal = 16.dp)) {
                Row(
                    modifier = Modifier.padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        onClick = onDismiss,
                        shape = CircleShape,
                        color = SurfaceWhite,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.Close, "Close", Modifier.size(18.dp), tint = Ink)
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Where's the work?",
                            style = MaterialTheme.typography.titleLarge,
                            color = Ink
                        )
                        Text(
                            text = "$SERVICE_CITY, Uttar Pradesh $SERVICE_PINCODE",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextTertiary
                        )
                    }
                }

                // Type an address -> map flies to it.
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            "Enter full address or landmark",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextTertiary
                        )
                    },
                    leadingIcon = {
                        Icon(Icons.Rounded.Search, null, Modifier.size(20.dp), tint = TextTertiary)
                    },
                    trailingIcon = {
                        if (searching) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = Ink
                            )
                        } else {
                            IconButton(onClick = { runSearch() }) {
                                Icon(
                                    Icons.Rounded.Check,
                                    "Find address",
                                    Modifier.size(20.dp),
                                    tint = Ink
                                )
                            }
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Search
                    ),
                    shape = RoundedCornerShape(16.dp),
                    textStyle = MaterialTheme.typography.bodyMedium,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SurfaceWhite,
                        unfocusedContainerColor = SurfaceWhite,
                        focusedBorderColor = Ink,
                        unfocusedBorderColor = Line,
                        cursorColor = Ink
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                notice?.let { message ->
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(StatusErrorSoft)
                            .padding(horizontal = 12.dp, vertical = 9.dp)
                    ) {
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodySmall,
                            color = StatusError
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))
            }

            // ---------------- map ----------------
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(24.dp))
            ) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    properties = MapProperties(
                        mapType = MapType.NORMAL,
                        isMyLocationEnabled = hasLocationPermission,
                        minZoomPreference = 4f
                    ),
                    uiSettings = MapUiSettings(
                        zoomControlsEnabled = false,
                        myLocationButtonEnabled = false,
                        mapToolbarEnabled = false,
                        tiltGesturesEnabled = false,
                        rotationGesturesEnabled = false
                    )
                )

                // Fixed centre pin - the map moves under it.
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Place,
                        contentDescription = null,
                        tint = Ink,
                        modifier = Modifier.size(44.dp)
                    )
                    // Small ground shadow so the pin reads as standing on the map.
                    Box(
                        modifier = Modifier
                            .size(width = 10.dp, height = 4.dp)
                            .clip(CircleShape)
                            .background(Ink.copy(alpha = 0.25f))
                    )
                }

                Surface(
                    onClick = {
                        val hasFine = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                        val hasCoarse = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED

                        if (hasFine || hasCoarse) {
                            hasLocationPermission = true
                            centreOnUser()
                        } else {
                            permissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        }
                    },
                    shape = CircleShape,
                    color = SurfaceWhite,
                    shadowElevation = 6.dp,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(14.dp)
                        .size(46.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Rounded.MyLocation,
                            "Use my location",
                            Modifier.size(20.dp),
                            tint = Ink
                        )
                    }
                }
            }

            // ---------------- confirm panel ----------------
            Surface(
                shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
                color = SurfaceWhite,
                shadowElevation = 14.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp)
            ) {
                Column(
                    modifier = Modifier
                        .navigationBarsPadding()
                        .imePadding()
                        .padding(18.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Rounded.Place,
                            null,
                            Modifier.size(17.dp),
                            tint = if (pinInArea) StatusSuccess else Color(0xFFF59E0B)
                        )
                        Spacer(Modifier.width(7.dp))
                        Text(
                            text = when {
                                resolving -> "Locating…"
                                resolvedAddress.isNotBlank() -> resolvedAddress
                                else -> "Move the map to your site"
                            },
                            style = MaterialTheme.typography.titleSmall,
                            color = Ink
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    OutlinedTextField(
                        value = houseDetail,
                        onValueChange = { houseDetail = it },
                        placeholder = {
                            Text(
                                "House / shop no., floor, landmark",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextTertiary
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        textStyle = MaterialTheme.typography.bodyMedium,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Paper,
                            unfocusedContainerColor = Paper,
                            focusedBorderColor = Ink,
                            unfocusedBorderColor = Line,
                            cursorColor = Ink
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(12.dp))

                    Text(
                        text = "SAVE AS",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Home", "Shop", "Site").forEach { option ->
                            PillChip(
                                text = option,
                                selected = tag == option,
                                onClick = { tag = option }
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    if (houseDetail.isBlank()) {
                        Text(
                            text = "Tip: a house or shop number helps the plumber find you faster.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextTertiary
                        )
                        Spacer(Modifier.height(10.dp))
                    }

                    PrimaryButton(
                        text = "Confirm address",
                        // The house number is a strong nice-to-have, not a
                        // blocker: exact GPS coordinates are saved either way,
                        // and a disabled button with no explanation is worse.
                        enabled = resolvedAddress.isNotBlank(),
                        onClick = {
                            val target = cameraPositionState.position.target
                            onConfirm(
                                SavedAddress(
                                    tag = tag,
                                    fullAddress = listOfNotNullBlank(
                                        houseDetail.trim(),
                                        resolvedAddress
                                    ).joinToString(", "),
                                    lat = target.latitude,
                                    lng = target.longitude
                                )
                            )
                        }
                    )
                }
            }
        }
    }
}

/** Keeps only non-blank parts, so joined addresses never contain ", ,". */
private fun listOfNotNullBlank(vararg parts: String?): List<String> =
    parts.mapNotNull { it?.trim()?.takeIf(String::isNotEmpty) }
