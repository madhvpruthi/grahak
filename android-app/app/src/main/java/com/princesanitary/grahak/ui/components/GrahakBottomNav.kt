package com.princesanitary.grahak.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.princesanitary.grahak.ui.theme.ElectricLime
import com.princesanitary.grahak.ui.theme.Ink
import com.princesanitary.grahak.ui.theme.InkDark
import com.princesanitary.grahak.ui.theme.SurfaceWhite
import com.princesanitary.grahak.ui.theme.TextWhite

import androidx.compose.ui.text.font.FontWeight
import com.princesanitary.grahak.ui.theme.NavActivePill
import com.princesanitary.grahak.ui.theme.NavActiveText
import com.princesanitary.grahak.ui.theme.NavDockBg
import com.princesanitary.grahak.ui.theme.NavInactiveText

/** One entry in the floating nav bar. */
data class NavItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val badgeCount: Int = 0
)

/**
 * Floating "liquid glass" navigation bar inspired by the reference dashboard.
 *
 * Compact floating dock with dark frosted body and hairline border.
 * Active item is an illuminated crisp white pill with bold black icon and label.
 */
@Composable
fun GrahakBottomNav(
    items: List<NavItem>,
    currentRoute: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(32.dp),
            color = Color.Transparent,
            shadowElevation = 24.dp
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(32.dp))
                    .background(NavDockBg)
                    .border(
                        width = 1.dp,
                        color = Color(0x26FFFFFF),
                        shape = RoundedCornerShape(32.dp)
                    )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    items.forEach { item ->
                        NavPill(
                            item = item,
                            selected = currentRoute == item.route,
                            onClick = { onSelect(item.route) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NavPill(
    item: NavItem,
    selected: Boolean,
    onClick: () -> Unit
) {
    val container by animateColorAsState(
        targetValue = if (selected) NavActivePill else Color.Transparent,
        animationSpec = tween(220),
        label = "navContainer"
    )
    val content by animateColorAsState(
        targetValue = if (selected) NavActiveText else NavInactiveText,
        animationSpec = tween(220),
        label = "navContent"
    )
    val elevation by animateDpAsState(
        targetValue = if (selected) 8.dp else 0.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "navElevation"
    )

    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = container,
        shadowElevation = elevation
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = if (selected) 12.dp else 8.dp,
                vertical = 8.dp
            ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BadgedBox(
                badge = {
                    if (item.badgeCount > 0) {
                        Badge(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = TextWhite
                        ) {
                            Text(
                                text = if (item.badgeCount > 9) "9+" else item.badgeCount.toString(),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = item.label,
                    tint = content,
                    modifier = Modifier.size(21.dp)
                )
            }

            AnimatedVisibility(
                visible = selected,
                enter = fadeIn(tween(180)) + expandHorizontally(tween(220)),
                exit = fadeOut(tween(100)) + shrinkHorizontally(tween(150))
            ) {
                Row {
                    Spacer(Modifier.width(7.dp))
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = content
                    )
                }
            }
        }
    }
}
