package com.princesanitary.grahak.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.princesanitary.grahak.ui.theme.Coal
import com.princesanitary.grahak.ui.theme.Ink
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * iOS-style edge swipe to go back.
 *
 * Wrap any screen that has a back action. Dragging right from the left edge
 * pulls the screen across with the finger; releasing past ~35% of the width (or
 * with enough flick velocity) completes the dismissal and calls [onBack],
 * otherwise it springs back.
 *
 * Two details do most of the work in making this feel native rather than bolted
 * on:
 *  - the gesture only arms within [edgeWidthDp] of the left edge, so it never
 *    steals horizontal scrolling from carousels and product rails,
 *  - a scrim behind the screen lightens as it slides away, which gives the
 *    motion depth instead of a flat slide.
 */
@Composable
fun SwipeBackContainer(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    edgeWidthDp: Int = 44,
    content: @Composable () -> Unit
) {
    if (!enabled) {
        Box(modifier.fillMaxSize()) { content() }
        return
    }

    BoxWithConstraints(modifier.fillMaxSize()) {
        val widthPx = constraints.maxWidth.toFloat()
        val density = LocalDensity.current
        val edgePx = remember(density, edgeWidthDp) {
            with(density) { edgeWidthDp.dp.toPx() }
        }

        val offsetX = remember { Animatable(0f) }
        val scope = rememberCoroutineScope()

        val dismissThreshold = widthPx * 0.35f
        val velocityThreshold = 900f

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(widthPx) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)

                        // Only arm the gesture near the left edge.
                        if (down.position.x > edgePx) return@awaitEachGesture

                        val tracker = VelocityTracker()
                        tracker.addPosition(down.uptimeMillis, down.position)

                        var dragged = 0f

                        horizontalDrag(down.id) { change: PointerInputChange ->
                            val delta = change.positionChange().x
                            // Only ever drag forward (rightwards).
                            dragged = (dragged + delta).coerceAtLeast(0f)
                            tracker.addPosition(change.uptimeMillis, change.position)

                            if (dragged > 0f) {
                                change.consume()
                                scope.launch { offsetX.snapTo(dragged) }
                            }
                        }

                        val velocity = tracker.calculateVelocity().x

                        if (dragged > dismissThreshold || velocity > velocityThreshold) {
                            scope.launch {
                                offsetX.animateTo(
                                    targetValue = widthPx,
                                    animationSpec = tween(210, easing = FastOutSlowInEasing)
                                )
                                onBack()
                                // Screen is popped and dismissed - do not snap back to 0 to prevent double-screen blink
                            }
                        } else if (abs(dragged) > 0f) {
                            scope.launch {
                                offsetX.animateTo(0f, tween(220, easing = FastOutSlowInEasing))
                            }
                        }
                    }
                }
        ) {
            val progress = (offsetX.value / widthPx).coerceIn(0f, 1f)

            // Scrim sits behind the moving screen and fades out as it leaves.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Coal.copy(alpha = 0.45f * (1f - progress)))
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        translationX = offsetX.value
                        // Very slight shrink as it pulls away adds depth.
                        val s = 1f - (0.04f * progress)
                        scaleX = s
                        scaleY = s
                    }
            ) {
                content()
            }
        }
    }
}
