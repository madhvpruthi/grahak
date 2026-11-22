package com.princesanitary.grahak.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.princesanitary.grahak.ui.components.PrimaryButton
import com.princesanitary.grahak.ui.theme.Amber
import com.princesanitary.grahak.ui.theme.BrandTeal
import com.princesanitary.grahak.ui.theme.ElectricLime
import com.princesanitary.grahak.ui.theme.Ink
import com.princesanitary.grahak.ui.theme.InkDark
import com.princesanitary.grahak.ui.theme.Paper
import com.princesanitary.grahak.ui.theme.SurfaceWhite
import com.princesanitary.grahak.ui.theme.TextSecondary
import com.princesanitary.grahak.ui.theme.TextTertiary
import com.princesanitary.grahak.ui.theme.TintMint
import com.princesanitary.grahak.ui.theme.TintPeach
import com.princesanitary.grahak.ui.theme.TintSky

/**
 * First-run welcome.
 *
 * Soft out-of-focus colour blooms behind a single tall column of type keep the
 * screen calm; the only action is one ink pill.
 */
@Composable
fun WelcomeScreen(
    onGetStarted: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Paper)
    ) {
        /*
         * Ambient colour washes.
         *
         * These were three 200-280dp circles behind Modifier.blur(90.dp).
         * Modifier.blur forces an offscreen render pass per layer and is a
         * no-op below API 31 anyway, so this is now drawn as radial gradients:
         * visually equivalent, effectively free, and it works on every device.
         */
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(TintMint, Paper.copy(alpha = 0f)),
                        center = Offset(-60f, 40f),
                        radius = 900f
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(TintPeach.copy(alpha = 0.9f), Paper.copy(alpha = 0f)),
                        center = Offset(1150f, 260f),
                        radius = 820f
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(TintSky.copy(alpha = 0.75f), Paper.copy(alpha = 0f)),
                        center = Offset(1000f, 1500f),
                        radius = 780f
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(horizontal = 26.dp, vertical = 28.dp)
        ) {
            Spacer(Modifier.height(12.dp))

            // Brand mark
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(15.dp))
                        .background(InkDark),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.WaterDrop,
                        contentDescription = null,
                        tint = ElectricLime,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        "Grahak",
                        style = MaterialTheme.typography.titleLarge,
                        color = Ink
                    )
                    Text(
                        "Prince Sanitary Store",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextTertiary
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            Text(
                text = "Sanitary ware\nand trusted\nplumbers.",
                style = MaterialTheme.typography.displaySmall,
                color = Ink
            )

            Spacer(Modifier.height(16.dp))

            Text(
                text = "Order pipes, fittings and fixtures at store rates, " +
                    "or book a verified plumber who quotes before they start.",
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary,
                modifier = Modifier.fillMaxWidth(0.92f)
            )

            Spacer(Modifier.height(28.dp))

            ValueRow("Live prices, no haggling")
            Spacer(Modifier.height(10.dp))
            ValueRow("Digital estimates you approve")
            Spacer(Modifier.height(10.dp))
            ValueRow("Track every order and job")

            Spacer(Modifier.weight(1f))

            PrimaryButton(
                text = "Get started",
                onClick = onGetStarted
            )

            Spacer(Modifier.height(14.dp))

            Text(
                text = "By continuing you agree to our Terms of Service and Privacy Policy.",
                style = MaterialTheme.typography.bodySmall,
                color = TextTertiary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun ValueRow(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            Icons.Rounded.CheckCircle,
            contentDescription = null,
            tint = BrandTeal,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = Ink
        )
    }
}
