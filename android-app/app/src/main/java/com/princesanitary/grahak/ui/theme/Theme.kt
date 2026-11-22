package com.princesanitary.grahak.ui.theme

import android.app.Activity
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue

private fun getLightColors() = lightColorScheme(
    primary = BentoGreen,
    onPrimary = BentoOnVibrant,
    primaryContainer = Color(0xFFE8F5E9),
    onPrimaryContainer = BentoGreen,

    secondary = ElectricBlue,
    onSecondary = TextWhite,
    secondaryContainer = Color(0xFFE0E7FF),
    onSecondaryContainer = ElectricBlue,

    tertiary = BentoYellow,
    onTertiary = BentoOnVibrant,
    tertiaryContainer = Color(0xFFFEF3C7),
    onTertiaryContainer = BentoYellowDark,

    background = Color(0xFFF8F9FA),
    onBackground = Color(0xFF111827),

    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF111827),
    surfaceVariant = Color(0xFFFFFFFF),
    onSurfaceVariant = Color(0xFF4B5563),
    surfaceContainer = Color(0xFFFFFFFF),
    surfaceContainerHigh = Color(0xFFF1F5F9),
    surfaceContainerLow = Color(0xFFFFFFFF),
    surfaceContainerLowest = Color(0xFFF8F9FA),
    surfaceContainerHighest = Color(0xFFE2E8F0),

    outline = Color(0xFFE5E7EB),
    outlineVariant = Color(0xFFE5E7EB),

    error = StatusError,
    onError = TextWhite,
    errorContainer = StatusErrorSoft,
    onErrorContainer = StatusError,

    scrim = Color(0x66000000)
)

private fun getDarkColors() = darkColorScheme(
    primary = ElectricLime,
    onPrimary = Color(0xFF111118),
    primaryContainer = Color(0xFF1E1E2A),
    onPrimaryContainer = ElectricLime,

    secondary = ElectricPurple,
    onSecondary = TextWhite,
    secondaryContainer = SoftPurple,
    onSecondaryContainer = ElectricPurple,

    tertiary = Amber,
    onTertiary = Color(0xFF111118),
    tertiaryContainer = Color(0xFF3D2E0F),
    onTertiaryContainer = Amber,

    background = Color(0xFF09090B),
    onBackground = Color(0xFFFAFAFA),

    surface = Color(0xFF18181B),
    onSurface = Color(0xFFFAFAFA),
    surfaceVariant = Color(0xFF12121A),
    onSurfaceVariant = Color(0xFFA1A1AA),
    surfaceContainer = Color(0xFF18181B),
    surfaceContainerHigh = Color(0xFF1E1E2A),
    surfaceContainerLow = Color(0xFF18181B),
    surfaceContainerLowest = Color(0xFF09090B),
    surfaceContainerHighest = Color(0xFF1E1E2A),

    outline = Color(0xFF27272A),
    outlineVariant = Color(0xFF27272A),

    error = StatusError,
    onError = Color(0xFF111118),
    errorContainer = StatusErrorSoft,
    onErrorContainer = StatusError,

    scrim = Color(0xFF09090B)
)

/** Generous rounded corners for modern, premium feel */
private val GrahakShapes = Shapes(
    extraSmall = RoundedCornerShape(12.dp),
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp)
)

/**
 * Grahak theme.
 *
 * Supports Dark, Light, and System themes dynamically via ThemeManager.
 */
@Composable
fun GrahakTheme(
    darkTheme: Boolean? = null,
    content: @Composable () -> Unit
) {
    val themeMode by ThemeManager.themeMode.collectAsStateWithLifecycle()
    val isSystemDark = isSystemInDarkTheme()
    val isDark = darkTheme ?: when (themeMode) {
        AppThemeMode.DARK -> true
        AppThemeMode.LIGHT -> false
        AppThemeMode.SYSTEM -> isSystemDark
    }

    SideEffect {
        isDarkThemeActive = isDark
    }

    val colorScheme = if (isDark) getDarkColors() else getLightColors()

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !isDark
                isAppearanceLightNavigationBars = !isDark
            }
        }
    }

    val currentDensity = LocalDensity.current
    val clampedDensity = Density(
        density = currentDensity.density,
        fontScale = currentDensity.fontScale.coerceIn(0.90f, 1.05f)
    )

    CompositionLocalProvider(LocalDensity provides clampedDensity) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = GrahakShapes,
            content = content
        )
    }
}
