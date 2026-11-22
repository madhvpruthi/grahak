package com.princesanitary.grahak.ui.plumber.jobs

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Directions
import androidx.compose.material.icons.rounded.LocationOff
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.princesanitary.grahak.data.model.ServiceJob
import com.princesanitary.grahak.ui.theme.BrandTeal
import com.princesanitary.grahak.ui.theme.Ink
import com.princesanitary.grahak.ui.theme.PaperDim
import com.princesanitary.grahak.ui.theme.StatusSuccess
import com.princesanitary.grahak.ui.theme.SurfaceWhite
import com.princesanitary.grahak.ui.theme.TextSecondary
import com.princesanitary.grahak.ui.theme.TextTertiary
import com.princesanitary.grahak.ui.theme.TextWhite

/**
 * Site card for an accepted job: where it is, and how to get in touch.
 *
 * The two things a plumber needs the moment they accept are a route and a phone
 * number, so both are primary actions here rather than buried in a list. The map
 * is non-interactive on purpose - it is an orientation aid, and tapping through
 * to Google Maps gives real turn-by-turn navigation.
 */
@Composable
fun JobSiteCard(
    job: ServiceJob,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coords = job.siteAddress.coordinates
    val hasCoords = coords.latitude != 0.0 || coords.longitude != 0.0
    val target = LatLng(coords.latitude, coords.longitude)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(SurfaceWhite)
    ) {
        // ---------------- map ----------------
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(170.dp)
                .background(PaperDim)
        ) {
            if (hasCoords) {
                val camera = rememberCameraPositionState {
                    position = CameraPosition.fromLatLngZoom(target, 16f)
                }
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = camera,
                    properties = MapProperties(mapType = com.google.maps.android.compose.MapType.NORMAL),
                    uiSettings = MapUiSettings(
                        zoomControlsEnabled = false,
                        scrollGesturesEnabled = false,
                        zoomGesturesEnabled = false,
                        rotationGesturesEnabled = false,
                        tiltGesturesEnabled = false,
                        mapToolbarEnabled = false
                    )
                ) {
                    Marker(state = MarkerState(position = target), title = "Job site")
                }
            } else {
                // Older jobs were saved with GeoPoint(0,0) before the map picker
                // existed, so there is genuinely nothing to show.
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Rounded.LocationOff,
                        contentDescription = null,
                        modifier = Modifier.size(26.dp),
                        tint = TextTertiary
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "No map pin on this booking",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextTertiary
                    )
                }
            }
        }

        // ---------------- address ----------------
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    Icons.Rounded.Place,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = BrandTeal
                )
                Spacer(Modifier.width(9.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = job.siteAddress.tag.ifBlank { "Site" },
                        style = MaterialTheme.typography.labelSmall,
                        color = TextTertiary
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = job.siteAddress.fullAddress.ifBlank { "Address not provided" },
                        style = MaterialTheme.typography.bodyMedium,
                        color = Ink
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // ---------------- actions ----------------
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                val phone = job.customerPhone

                ActionButton(
                    icon = Icons.Rounded.Call,
                    label = if (phone.isNotBlank()) "Call Customer" else "No Phone",
                    enabled = phone.isNotBlank(),
                    container = com.princesanitary.grahak.ui.theme.BentoGreen,
                    contentColor = androidx.compose.ui.graphics.Color.White,
                    modifier = Modifier.weight(1f)
                ) {
                    try {
                        context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")))
                    } catch (_: Exception) {}
                }

                ActionButton(
                    icon = Icons.Rounded.Directions,
                    label = "Directions",
                    enabled = true,
                    container = com.princesanitary.grahak.ui.theme.LuxeGold,
                    contentColor = androidx.compose.ui.graphics.Color(0xFF09090B),
                    modifier = Modifier.weight(1f)
                ) {
                    try {
                        if (hasCoords) {
                            val uri = Uri.parse("google.navigation:q=${coords.latitude},${coords.longitude}")
                            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                                setPackage("com.google.android.apps.maps")
                            }
                            if (intent.resolveActivity(context.packageManager) != null) {
                                context.startActivity(intent)
                            } else {
                                context.startActivity(
                                    Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse("geo:${coords.latitude},${coords.longitude}?q=${coords.latitude},${coords.longitude}")
                                    )
                                )
                            }
                        } else {
                            val query = Uri.encode(job.siteAddress.fullAddress.ifBlank { "Saharanpur" })
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=$query")))
                        }
                    } catch (_: Exception) {
                        val query = Uri.encode(job.siteAddress.fullAddress.ifBlank { "Saharanpur" })
                        try {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=$query")))
                        } catch (_: Exception) {}
                    }
                }
            }

            if (job.customerName.isNotBlank() || job.customerPhone.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = buildString {
                        append(job.customerName.ifBlank { "Customer" })
                        if (job.customerPhone.isNotBlank()) append(" · +91 ${job.customerPhone}")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            } else {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "This customer hasn't added a phone number yet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextTertiary
                )
            }
        }
    }
}

@Composable
private fun ActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    enabled: Boolean,
    container: androidx.compose.ui.graphics.Color,
    contentColor: androidx.compose.ui.graphics.Color = TextWhite,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(15.dp),
        color = if (enabled) container else PaperDim
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = if (enabled) contentColor else TextTertiary
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
                color = if (enabled) contentColor else TextTertiary
            )
        }
    }
}
