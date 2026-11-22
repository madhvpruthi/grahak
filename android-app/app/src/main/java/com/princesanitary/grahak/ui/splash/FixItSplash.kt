package com.princesanitary.grahak.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.princesanitary.grahak.ui.theme.Line
import com.princesanitary.grahak.ui.theme.LuxeGold
import com.princesanitary.grahak.ui.theme.TextSecondary
import kotlinx.coroutines.delay

/**
 * FixIt luxury launch screen.
 *
 * Deep onyx pitch black with crisp white wordmark, luxe gold signature dot,
 * and an ultra-refined, feather-light soundwave loader.
 */
@Composable
fun FixItSplash(
    onFinished: () -> Unit
) {
    val markAlpha = remember { Animatable(0f) }
    val markScale = remember { Animatable(0.92f) }
    val surfaceAlpha = remember { Animatable(1f) }
    val surfaceScale = remember { Animatable(1f) }
    val progress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        // 1. Wordmark and floating loader fade and settle in smoothly
        markAlpha.animateTo(1f, tween(250, easing = LinearOutSlowInEasing))
        markScale.animateTo(1f, tween(250, easing = LinearOutSlowInEasing))

        // 2. Crisp, deliberate hold for visual elegance
        delay(700)

        // 3. Hand over with subtle scale and fade
        surfaceScale.animateTo(1.02f, tween(200, easing = FastOutSlowInEasing))
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                alpha = surfaceAlpha.value
                scaleX = surfaceScale.value
                scaleY = surfaceScale.value
            }
            .background(Color(0xFF09090B)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .scale(markScale.value)
                    .graphicsLayer { alpha = markAlpha.value }
            ) {
                Text(
                    text = "fixit",
                    style = TextStyle(
                        fontSize = 58.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-2.5).sp
                    ),
                    color = Color.White
                )
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .padding(bottom = 12.dp)
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(LuxeGold)
                )
            }

            Spacer(Modifier.height(8.dp))

            // Subtitle
            Text(
                text = "Prince Sanitary Store  ·  Saharanpur",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp,
                    fontSize = 12.sp
                ),
                color = TextSecondary,
                modifier = Modifier.graphicsLayer { alpha = markAlpha.value }
            )

            Spacer(Modifier.height(34.dp))

            // Minimalist floating yellow ball loader
            com.princesanitary.grahak.ui.components.FloatingBallLoader(
                ballColor = LuxeGold,
                ballSize = 13.dp,
                dropHeight = 16.dp,
                modifier = Modifier.graphicsLayer { alpha = markAlpha.value }
            )
        }
    }
}

/**
 * Ultra-refined, calm luxury rhythm progress capsule.
 * Inspired by high-end minimal dark dashboard interfaces.
 */
@Composable
fun LuxuryRhythmLoader(
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "rhythm")
    val sweepPosition by transition.animateFloat(
        initialValue = -0.3f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sweep"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        // Track capsule
        Box(
            modifier = Modifier
                .width(130.dp)
                .height(4.dp)
                .clip(CircleShape)
                .background(Color(0xFF202026)),
            contentAlignment = Alignment.CenterStart
        ) {
            // Animated beam
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        androidx.compose.ui.graphics.Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                LuxeGold.copy(alpha = 0.85f),
                                LuxeGold,
                                LuxeGold.copy(alpha = 0.85f),
                                Color.Transparent
                            ),
                            startX = sweepPosition * 300f - 120f,
                            endX = sweepPosition * 300f + 120f
                        )
                    )
            )
        }
    }
}
