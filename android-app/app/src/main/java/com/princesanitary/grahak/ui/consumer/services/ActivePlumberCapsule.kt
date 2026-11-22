package com.princesanitary.grahak.ui.consumer.services

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.princesanitary.grahak.data.model.JobStatus
import com.princesanitary.grahak.ui.theme.EmeraldSignal
import com.princesanitary.grahak.ui.theme.Ink
import com.princesanitary.grahak.ui.theme.Line
import com.princesanitary.grahak.ui.theme.SurfaceWhite

/**
 * Compact, high-end floating capsule for an assigned active plumber.
 * Shorter in height and lighter in weight than the cart bar, as requested.
 */
@Composable
fun ActivePlumberCapsule(
    plumberName: String,
    plumberPhone: String,
    plumberPhotoUrl: String = "",
    jobStatus: JobStatus,
    serviceCategory: String = "",
    onClick: () -> Unit
) {
    val context = LocalContext.current

    val statusLabel = when (jobStatus) {
        JobStatus.EN_ROUTE -> "En route to site"
        JobStatus.INSPECTING -> "Inspecting at site"
        JobStatus.IN_PROGRESS -> "Work in progress"
        else -> "Plumber assigned"
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(44.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        color = SurfaceWhite,
        border = BorderStroke(1.dp, Line),
        shadowElevation = 5.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Plumber Photo / Avatar with live beacon
            Box(
                modifier = Modifier.size(30.dp),
                contentAlignment = Alignment.Center
            ) {
                if (plumberPhotoUrl.isNotBlank()) {
                    AsyncImage(
                        model = plumberPhotoUrl,
                        contentDescription = plumberName,
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF27272A)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = plumberName.take(1).uppercase().ifBlank { "P" },
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            ),
                            color = Color.White
                        )
                    }
                }

                // Live green dot
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(EmeraldSignal)
                )
            }

            Spacer(Modifier.width(10.dp))

            // Details
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = plumberName.ifBlank { "Assigned Plumber" },
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    ),
                    color = Ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = statusLabel,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 10.sp
                    ),
                    color = EmeraldSignal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.width(8.dp))

            // One-tap Call Action
            if (plumberPhone.isNotBlank()) {
                Surface(
                    shape = CircleShape,
                    color = EmeraldSignal,
                    modifier = Modifier
                        .size(30.dp)
                        .clickable {
                            val intent = Intent(Intent.ACTION_DIAL).apply {
                                data = Uri.parse("tel:$plumberPhone")
                            }
                            context.startActivity(intent)
                        }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Rounded.Call,
                            contentDescription = "Call Plumber",
                            tint = Color.White,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }
        }
    }
}
