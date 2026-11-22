package com.princesanitary.grahak.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.princesanitary.grahak.ui.theme.Line
import com.princesanitary.grahak.ui.theme.PaperDim
import com.princesanitary.grahak.ui.theme.SurfaceWhite

/**
 * Shimmer placeholders.
 *
 * Every list in this app is backed by a Firestore listener, so there is always a
 * first-paint gap. Showing the *shape* of the content instead of a centred
 * spinner makes that gap read as "loading fast" rather than "frozen", and it
 * stops the layout jumping when data lands.
 */
@Composable
private fun shimmerBrush(): Brush {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val x by transition.animateFloat(
        initialValue = -600f,
        targetValue = 1200f,
        animationSpec = infiniteRepeatable(
            animation = tween(1300),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerX"
    )

    return Brush.linearGradient(
        colors = listOf(PaperDim, Line.copy(alpha = 0.55f), PaperDim),
        start = Offset(x, 0f),
        end = Offset(x + 600f, 0f)
    )
}

/** A single shimmering block. */
@Composable
fun SkeletonBox(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(12.dp)
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(shimmerBrush())
    )
}

/** Shimmering text line. */
@Composable
fun SkeletonLine(
    widthFraction: Float = 1f,
    height: Dp = 13.dp,
    modifier: Modifier = Modifier
) {
    SkeletonBox(
        modifier = modifier
            .fillMaxWidth(widthFraction)
            .height(height),
        shape = RoundedCornerShape(6.dp)
    )
}

/** Matches the two-column product grid on the Shop tab. */
@Composable
fun ProductGridSkeleton(
    modifier: Modifier = Modifier,
    rows: Int = 3
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        repeat(rows) {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                repeat(2) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(22.dp))
                            .background(SurfaceWhite)
                            .padding(12.dp)
                    ) {
                        SkeletonBox(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f),
                            shape = RoundedCornerShape(16.dp)
                        )
                        Spacer(Modifier.height(12.dp))
                        SkeletonLine(0.5f, 9.dp)
                        Spacer(Modifier.height(7.dp))
                        SkeletonLine(0.95f)
                        Spacer(Modifier.height(5.dp))
                        SkeletonLine(0.6f)
                        Spacer(Modifier.height(12.dp))
                        Row {
                            SkeletonLine(0.4f, 18.dp)
                            Spacer(Modifier.weight(1f))
                            SkeletonBox(Modifier.size(34.dp), CircleShape)
                        }
                    }
                }
            }
        }
    }
}

/** Matches a horizontally scrolling product rail. */
@Composable
fun ProductRailSkeleton(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        repeat(3) {
            Column(
                modifier = Modifier
                    .width(158.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(SurfaceWhite)
                    .padding(12.dp)
            ) {
                SkeletonBox(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f),
                    shape = RoundedCornerShape(16.dp)
                )
                Spacer(Modifier.height(12.dp))
                SkeletonLine(0.9f)
                Spacer(Modifier.height(6.dp))
                SkeletonLine(0.45f, 17.dp)
            }
        }
    }
}

/** Generic stacked-card skeleton for order / job / estimate lists. */
@Composable
fun ListCardSkeleton(
    modifier: Modifier = Modifier,
    count: Int = 4,
    height: Dp = 96.dp
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        repeat(count) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(height)
                    .clip(RoundedCornerShape(20.dp))
                    .background(SurfaceWhite)
                    .padding(14.dp)
            ) {
                SkeletonBox(Modifier.size(56.dp), RoundedCornerShape(14.dp))
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    SkeletonLine(0.55f)
                    Spacer(Modifier.height(8.dp))
                    SkeletonLine(0.85f, 11.dp)
                    Spacer(Modifier.height(8.dp))
                    SkeletonLine(0.35f, 11.dp)
                }
            }
        }
    }
}
