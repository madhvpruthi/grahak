package com.princesanitary.grahak.ui.consumer.checkout

import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Extreme-luxury order confirmation overlay.
 * Faithfully reproduces the serene minimalism and upward light-sweep from the reference video:
 * 1. Gentle, breathing multi-spectral radial light orb resting at the bottom during placement.
 * 2. On confirmation, the radiant light wave sweeps smoothly upward through the screen and exits at the top.
 * 3. Delicate Apple hairline spinner morphs into a crisp solid white checkmark circle.
 * 4. Screen resolves into pure, calm pitch black with satisfying haptic feedback.
 */
@Composable
fun OrderCompletionAnimationOverlay(
    orderId: String,
    onAnimationFinished: () -> Unit
) {
    var isConfirmed by remember { mutableStateOf(false) }
    val view = LocalView.current
    val checkSpring = remember { Animatable(0.35f) }

    // Normalized Y position: 0.95 (bottom resting) -> -0.40 (exited off top)
    val orbY = remember { Animatable(0.95f) }
    val orbAlpha = remember { Animatable(1f) }

    // Gentle breathing pulse while resting at bottom
    val transition = rememberInfiniteTransition(label = "ambient_aura")
    val breathScale by transition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1300, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathScale"
    )

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            view.performHapticFeedback(HapticFeedbackConstants.GESTURE_START)
        }
        delay(1200)

        // Launch the upward sweep of the radiant light wave
        launch {
            orbY.animateTo(
                targetValue = -0.45f,
                animationSpec = tween(
                    durationMillis = 850,
                    easing = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)
                )
            )
        }

        // Dissolve the aura as it reaches the top
        launch {
            delay(250)
            orbAlpha.animateTo(
                targetValue = 0f,
                animationSpec = tween(550, easing = FastOutSlowInEasing)
            )
        }

        // Morph to checkmark right as the light sweeps past the center
        delay(220)
        isConfirmed = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
        } else {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }

        checkSpring.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = 0.65f,
                stiffness = Spring.StiffnessMediumLow
            )
        )

        // Hold in calm, serene stillness
        delay(1300)
        onAnimationFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF09090B)),
        contentAlignment = Alignment.Center
    ) {
        // GPU Canvas: Volumetric radiant light orb with true radial gradient falloff & upward sweep
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val currentCenterY = h * orbY.value
            val currentScale = if (isConfirmed) 1.25f else breathScale
            val radius = w * 0.95f * currentScale

            if (orbAlpha.value > 0.01f) {
                drawCircle(
                    brush = Brush.radialGradient(
                        0.0f to Color(0xDDFFFFFF).copy(alpha = 0.70f * orbAlpha.value),
                        0.20f to Color(0xD0EC4899).copy(alpha = 0.62f * orbAlpha.value), // Rose Magenta
                        0.48f to Color(0xAA8B5CF6).copy(alpha = 0.50f * orbAlpha.value), // Violet
                        0.74f to Color(0x66F7AC49).copy(alpha = 0.35f * orbAlpha.value), // Warm Amber
                        1.0f to Color.Transparent,
                        center = Offset(w / 2f, currentCenterY),
                        radius = radius
                    ),
                    radius = radius,
                    center = Offset(w / 2f, currentCenterY)
                )
            }
        }

        // Center Minimal Hero: Pure, serene, matching the reference video
        AnimatedContent(
            targetState = isConfirmed,
            transitionSpec = {
                fadeIn(tween(240)) togetherWith fadeOut(tween(160))
            },
            label = "confirmation_state"
        ) { confirmed ->
            if (!confirmed) {
                // Phase 1: Hairline spinning ring + "Placing order..."
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AppleHairlineSpinner(
                        size = 17.dp,
                        strokeWidth = 1.7.dp,
                        color = Color.White.copy(alpha = 0.88f)
                    )
                    Spacer(Modifier.width(11.dp))
                    Text(
                        text = "Placing order...",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Normal,
                            fontSize = 15.sp,
                            letterSpacing = 0.3.sp
                        ),
                        color = Color.White.copy(alpha = 0.92f)
                    )
                }
            } else {
                // Phase 2: Crisp solid white checkmark circle + "Order placed"
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.graphicsLayer {
                        scaleX = checkSpring.value
                        scaleY = checkSpring.value
                    }
                ) {
                    Box(
                        modifier = Modifier
                            .size(19.dp)
                            .clip(CircleShape)
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Rounded.Check,
                            contentDescription = null,
                            tint = Color(0xFF09090B),
                            modifier = Modifier.size(12.5.dp)
                        )
                    }
                    Spacer(Modifier.width(11.dp))
                    Text(
                        text = "Order placed",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 15.5.sp,
                            letterSpacing = 0.2.sp
                        ),
                        color = Color.White
                    )
                }
            }
        }
    }
}

/**
 * Hairline circular spinner matching Apple Pay / luxury FinTech stroke.
 */
@Composable
private fun AppleHairlineSpinner(
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    size: Dp = 16.dp,
    strokeWidth: Dp = 1.6.dp
) {
    val transition = rememberInfiniteTransition(label = "hairline_spin")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Canvas(
        modifier = modifier.size(size)
    ) {
        val sweepAngle = 270f
        drawArc(
            color = color,
            startAngle = rotation,
            sweepAngle = sweepAngle,
            useCenter = false,
            style = Stroke(
                width = strokeWidth.toPx(),
                cap = StrokeCap.Round
            )
        )
    }
}
