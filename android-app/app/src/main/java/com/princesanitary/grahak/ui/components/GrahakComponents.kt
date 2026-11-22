package com.princesanitary.grahak.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.princesanitary.grahak.ui.theme.ElectricLime
import com.princesanitary.grahak.ui.theme.GradientLime
import com.princesanitary.grahak.ui.theme.Ink
import com.princesanitary.grahak.ui.theme.InkDark
import com.princesanitary.grahak.ui.theme.Line
import com.princesanitary.grahak.ui.theme.PaperDim
import com.princesanitary.grahak.ui.theme.SurfaceWhite
import com.princesanitary.grahak.ui.theme.TextSecondary
import com.princesanitary.grahak.ui.theme.TextTertiary
import com.princesanitary.grahak.ui.theme.TextWhite

/* =========================================================================
 * Buttons
 * ========================================================================= */

import com.princesanitary.grahak.ui.theme.BentoBlue
import com.princesanitary.grahak.ui.theme.BentoDark
import com.princesanitary.grahak.ui.theme.BentoGreen
import com.princesanitary.grahak.ui.theme.BentoOnDark
import com.princesanitary.grahak.ui.theme.BentoOnVibrant
import com.princesanitary.grahak.ui.theme.BentoPink
import com.princesanitary.grahak.ui.theme.BentoYellow
import com.princesanitary.grahak.ui.theme.CoalElevated
import com.princesanitary.grahak.ui.theme.GrahakRed
import com.princesanitary.grahak.ui.theme.GrahakRedBright

/* =========================================================================
 * Buttons
 * ========================================================================= */

/** Full-width high-tactile pill button. The single primary action on a screen. */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    leadingIcon: ImageVector? = null,
    icon: ImageVector? = leadingIcon,
    containerColor: Color? = null,
    contentColor: Color? = null
) {
    val effectiveIcon = icon ?: leadingIcon
    val isActive = enabled && !loading
    val textColor = contentColor ?: InkDark

    Surface(
        onClick = onClick,
        enabled = isActive,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = CircleShape,
        color = Color.Transparent,
        shadowElevation = if (isActive) 6.dp else 0.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = if (isActive) {
                        if (containerColor != null) {
                            Brush.linearGradient(listOf(containerColor, containerColor))
                        } else {
                            GradientLime
                        }
                    } else {
                        Brush.linearGradient(
                            listOf(InkDark.copy(alpha = 0.5f), InkDark.copy(alpha = 0.4f))
                        )
                    },
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = textColor,
                    strokeWidth = 2.dp
                )
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    if (effectiveIcon != null) {
                        Icon(effectiveIcon, null, Modifier.size(20.dp), tint = textColor)
                        Spacer(Modifier.width(10.dp))
                    }
                    Text(
                        text = text,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = textColor
                    )
                }
            }
        }
    }
}

/** Quiet bordered alternative. Used for secondary paths. */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
    leadingContent: (@Composable () -> Unit)? = null
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = CircleShape,
        color = PaperDim,
        border = BorderStroke(1.dp, Line)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            when {
                leadingContent != null -> {
                    leadingContent()
                    Spacer(Modifier.width(10.dp))
                }
                leadingIcon != null -> {
                    Icon(leadingIcon, null, Modifier.size(20.dp), tint = Ink)
                    Spacer(Modifier.width(10.dp))
                }
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = if (enabled) Ink else TextTertiary
            )
        }
    }
}

/* =========================================================================
 * Bento Containers & Cards
 * ========================================================================= */

/**
 * Standard Bento Card with generous rounded corners and subtle border.
 * Inspired by modern dashboard widgets.
 */
@Composable
fun GrahakCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    color: Color = SurfaceWhite,
    border: Boolean = true,
    borderColor: Color = Line,
    cornerRadius: Dp = 24.dp,
    contentPadding: Dp = 18.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    val base = modifier
        .fillMaxWidth()
        .clip(shape)
        .background(color)
        .then(if (border) Modifier.border(1.dp, borderColor, shape) else Modifier)
        .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)

    Column(modifier = base.padding(contentPadding), content = content)
}

/**
 * Vibrant or Dark Bento Card with title, badge/icon, and metric.
 */
@Composable
fun BentoStatCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    badgeText: String? = null,
    icon: ImageVector? = null,
    containerColor: Color = BentoDark,
    contentColor: Color = BentoOnDark,
    borderColor: Color = Line,
    onClick: (() -> Unit)? = null
) {
    val shape = RoundedCornerShape(24.dp)
    Surface(
        onClick = { onClick?.invoke() },
        enabled = onClick != null,
        modifier = modifier.clip(shape),
        shape = shape,
        color = containerColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = contentColor.copy(alpha = 0.8f)
                )
                if (badgeText != null) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(contentColor.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = badgeText,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = contentColor
                        )
                    }
                } else if (icon != null) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(contentColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(icon, null, Modifier.size(16.dp), tint = contentColor)
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = contentColor
            )

            if (subtitle != null) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor.copy(alpha = 0.7f)
                )
            }
        }
    }
}

/** Small eyebrow + optional trailing action, used above each content block. */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold
            ),
            color = Ink,
            modifier = Modifier.weight(1f)
        )
        if (actionText != null && onActionClick != null) {
            Text(
                text = actionText,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = ElectricLime,
                modifier = Modifier.clickable { onActionClick() }
            )
        }
    }
}

/* =========================================================================
 * Chips & pills
 * ========================================================================= */

@Composable
fun PillChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    activeColor: Color = BentoPink,
    activeTextColor: Color = BentoOnVibrant
) {
    val bg by animateColorAsState(if (selected) activeColor else SurfaceWhite, label = "chipBg")
    val fg by animateColorAsState(if (selected) activeTextColor else TextSecondary, label = "chipFg")

    Surface(
        onClick = onClick,
        modifier = modifier.heightIn(min = 38.dp),
        shape = CircleShape,
        color = bg,
        border = if (selected) BorderStroke(1.dp, activeColor) else BorderStroke(1.dp, Line)
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                ),
                color = fg
            )
        }
    }
}

/** Tinted status label, e.g. order/job state. */
@Composable
fun StatusPill(
    text: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(containerColor)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(contentColor)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold
            ),
            color = contentColor
        )
    }
}

/* =========================================================================
 * Segmented control (Customer / Plumber, Requests / Active)
 * ========================================================================= */

@Composable
fun SegmentedToggle(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(CircleShape)
            .background(PaperDim)
            .border(1.dp, Line, CircleShape)
            .padding(4.dp)
    ) {
        options.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            val bg by animateColorAsState(
                if (selected) Color.White else Color.Transparent,
                label = "segBg"
            )
            val fg by animateColorAsState(
                if (selected) InkDark else TextSecondary,
                label = "segFg"
            )
            val elevation by animateDpAsState(
                if (selected) 4.dp else 0.dp,
                label = "segEl"
            )

            Surface(
                onClick = { onSelect(index) },
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp),
                shape = CircleShape,
                color = bg,
                shadowElevation = elevation
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                        ),
                        color = fg
                    )
                }
            }
        }
    }
}

/* =========================================================================
 * Misc
 * ========================================================================= */

@Composable
fun CircleIconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    container: Color = SurfaceWhite,
    tint: Color = Ink,
    size: Dp = 44.dp
) {
    Surface(
        onClick = onClick,
        modifier = modifier.size(size),
        shape = CircleShape,
        color = container,
        border = BorderStroke(1.dp, Line)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription, Modifier.size(20.dp), tint = tint)
        }
    }
}

@Composable
fun BackButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    CircleIconButton(
        icon = Icons.Rounded.ArrowBack,
        onClick = onClick,
        modifier = modifier,
        contentDescription = "Back"
    )
}

/** Compact -/qty/+ stepper for cart and estimate rows. */
@Composable
fun QuantityStepper(
    quantity: Int,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(PaperDim)
            .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StepperButton(Icons.Rounded.Remove, "Decrease", onDecrease)
        Text(
            text = quantity.toString(),
            style = MaterialTheme.typography.titleMedium,
            color = Ink,
            modifier = Modifier.padding(horizontal = 14.dp)
        )
        StepperButton(Icons.Rounded.Add, "Increase", onIncrease)
    }
}

@Composable
private fun StepperButton(icon: ImageVector, description: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.size(30.dp),
        shape = CircleShape,
        color = SurfaceWhite
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, description, Modifier.size(16.dp), tint = Ink)
        }
    }
}

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onAction: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(84.dp)
                .clip(CircleShape)
                .background(PaperDim),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, Modifier.size(34.dp), tint = TextTertiary)
        }
        Spacer(Modifier.height(18.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = Ink,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
        if (actionText != null && onAction != null) {
            Spacer(Modifier.height(22.dp))
            PrimaryButton(text = actionText, onClick = onAction)
        }
    }
}

@Composable
fun LoadingState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = ElectricLime, strokeWidth = 2.5.dp)
    }
}

/** Inline error banner. */
@Composable
fun ErrorBanner(
    message: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.errorContainer)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** ₹ formatting used consistently across the app. */
fun formatRupees(amount: Double): String {
    val rounded = Math.round(amount * 100.0) / 100.0
    return if (rounded % 1.0 == 0.0) {
        "₹${rounded.toLong()}"
    } else {
        "₹%.2f".format(rounded)
    }
}
