package com.princesanitary.grahak.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Dynamic Theme State (manipulated by ThemeManager)
var isDarkThemeActive by mutableStateOf(true)

/** Core premium palette: midnight navy, porcelain and one deep teal accent. */
val GrahakRed = Color(0xFF22C55E)
val GrahakRedBright = Color(0xFF4ADE80)
val GrahakRedDeep = Color(0xFF16A34A)
val GrahakRedSoft = Color(0xFF0F3D2E)
val GrahakBlack = Color(0xFF09090B)
val GrahakCharcoal = Color(0xFF141419)
val GrahakCharcoalElevated = Color(0xFF1B1B22)
val GrahakWarmWhite = Color(0xFFFAFAFA)

/*
 * Premium Dark Theme Palette
 *
 * Inspired by modern delivery apps (Swiggy / Zomato) with a dark, high-contrast
 * base and electric neon accents. The core tokens are flipped to a dark surface
 * so most screens re-theme automatically:
 *
 *   - Ink      → near-white (primary text on dark surfaces)
 *   - Paper    → deep near-black (app background)
 *   - Surface  → dark card surface
 *   - InkDark  → rich charcoal for dark elements (buttons, hero cards, selected pills)
 *   - ElectricLime → the signature neon accent
 */

// ---- Dynamic Gradients (tuned for dark backgrounds) -----------------------
val PremiumGradient = Brush.horizontalGradient(listOf(GrahakCharcoal, GrahakBlack))
val GradientSunrise = Brush.horizontalGradient(listOf(Color(0xFFFF8A00), Color(0xFFFFC400), Color(0xFFC6FF3D)))

val GradientOcean = Brush.horizontalGradient(listOf(Color(0xFF667EEA), Color(0xFF764BA2), Color(0xFFA78BFA)))

val GradientForest = Brush.horizontalGradient(listOf(Color(0xFF134E5E), Color(0xFF34D399)))

val GradientSunset = Brush.horizontalGradient(listOf(Color(0xFFFF512F), Color(0xFFF09819)))

val GradientMint = Brush.horizontalGradient(listOf(Color(0xFF00D2FF), Color(0xFF3A7BD5)))

val GradientBerry = Brush.horizontalGradient(listOf(Color(0xFFEC008C), Color(0xFFFC6767)))

val GradientPurple = Brush.horizontalGradient(listOf(Color(0xFF8E2DE2), Color(0xFF4A00E0)))

val GradientTeal = Brush.horizontalGradient(listOf(Color(0xFF11998E), Color(0xFF38EF7D)))

val GradientCoral = Brush.horizontalGradient(listOf(Color(0xFFFA709A), Color(0xFFFEE140)))

val GradientNight = Brush.horizontalGradient(listOf(Color(0xFF1E1E2A), Color(0xFF2D2D3A)))

val GradientRose = Brush.horizontalGradient(listOf(Color(0xFFF857A6), Color(0xFFFF5858)))

val GradientAurora = PremiumGradient

/** Signature neon-lime gradient used for premium CTAs and highlights. */
val GradientLime = Brush.horizontalGradient(listOf(Color(0xFFC6FF3D), Color(0xFF84CC16)))

/** Rich dark-to-charcoal gradient for hero cards and feature panels. */
val GradientInk = Brush.linearGradient(listOf(Color(0xFF1A1A26), Color(0xFF0F0F17)))

// ---- Vibrant Primary Colors ----------------------------------------------
val ElectricBlue = Color(0xFF4F46E5)
val ElectricPurple = Color(0xFFA78BFA)
val ElectricPink = Color(0xFFEC4899)
val ElectricOrange = Color(0xFFFB923C)
val ElectricGreen = Color(0xFF34D399)
val ElectricTeal = Color(0xFF22D3EE)
val ElectricCyan = Color(0xFF22D3EE)
val ElectricRed = Color(0xFFF87171)
val ElectricYellow = Color(0xFFFBBF24)

/** Signature yellow brand accent - updated to official requested shade #F7AC49 */
val ElectricLime = Color(0xFFF7AC49)

// ---- Unified Luxury 3-Color Tokens ---------------------------------------
val LuxeGold = Color(0xFFF7AC49)
val LuxeGoldLight = Color(0xFFFFC57A)
val LuxeGoldDark = Color(0xFFE09330)
val LuxeGoldMuted = Color(0xFFB46E19)
val LuxeGoldSurface = Color(0x24F7AC49)
val LuxeGoldBorder = Color(0x59F7AC49)

val EmeraldSignal = Color(0xFF10B981)
val EmeraldSignalSurface = Color(0x1F10B981)
val EmeraldSignalBorder = Color(0x4D10B981)

// ---- Rich Accent Colors --------------------------------------------------
val RoyalBlue = Color(0xFF60A5FA)
val DeepPurple = Color(0xFF8B5CF6)
val HotPink = Color(0xFFF472B6)
val Coral = Color(0xFFFF8A80)
val Amber = Color(0xFFFBBF24)
val Emerald = Color(0xFF34D399)
val Sky = Color(0xFF38BDF8)
val Rose = Color(0xFFFB7185)

// ---- Soft Pastels for Backgrounds (tinted dark surfaces) -----------------
val SoftPurple = Color(0xFF1E1B2E)
val SoftBlue = Color(0xFF172033)
val SoftPink = Color(0xFF2E1A22)
val SoftOrange = Color(0xFF2E2417)
val SoftGreen = Color(0xFF16241E)
val SoftYellow = Color(0xFF2E2A17)
val SoftTeal = Color(0xFF142628)
val SoftRose = Color(0xFF2E1A22)

// ---- Core Neutrals (Dynamic: Shifts on Light/Dark Mode) ---------------------
val Ink: Color get() = if (isDarkThemeActive) Color(0xFFFAFAFA) else Color(0xFF111827)
val InkLight: Color get() = if (isDarkThemeActive) Color(0xFFD4D4D8) else Color(0xFF4B5563)
val Paper: Color get() = if (isDarkThemeActive) Color(0xFF09090B) else Color(0xFFF8F9FA)
val PaperDim: Color get() = if (isDarkThemeActive) Color(0xFF12121A) else Color(0xFFFFFFFF)
val SurfaceWhite: Color get() = if (isDarkThemeActive) Color(0xFF18181B) else Color(0xFFFFFFFF)
val SurfaceElevated: Color get() = if (isDarkThemeActive) Color(0xFF1F1F26) else Color(0xFFF3F4F6)
val Line: Color get() = if (isDarkThemeActive) Color(0xFF27272A) else Color(0xFFE5E7EB)
val LineSoft: Color get() = if (isDarkThemeActive) Color(0xFF1F1F23) else Color(0xFFF3F4F6)

/** Rich charcoal for dark elements (primary buttons, hero cards, selected pills). */
val InkDark: Color get() = if (isDarkThemeActive) Color(0xFF111118) else Color(0xFF1F2937)

// ---- Bento Palette (Inspired by modern high-contrast dashboard design) ----
val BentoGreen = Color(0xFF22C55E)
val BentoGreenLight = Color(0xFF4ADE80)
val BentoBlue = Color(0xFF0EA5E9)
val BentoBlueDark = Color(0xFF0284C7)
val BentoPink = Color(0xFFEC4899)
val BentoPinkLight = Color(0xFFF472B6)
val BentoYellow = Color(0xFFFACC15)
val BentoYellowDark = Color(0xFFEAB308)
val BentoDark: Color get() = if (isDarkThemeActive) Color(0xFF141419) else Color(0xFFF1F5F9)
val BentoDarkElevated: Color get() = if (isDarkThemeActive) Color(0xFF1B1B22) else Color(0xFFE2E8F0)
val BentoOnVibrant = Color(0xFF09090B)
val BentoOnDark: Color get() = if (isDarkThemeActive) Color(0xFFFAFAFA) else Color(0xFF0F172A)

// Floating Nav Dock
val NavDockBg: Color get() = if (isDarkThemeActive) Color(0xF2121217) else Color(0xF2FFFFFF)
val NavActivePill: Color get() = if (isDarkThemeActive) Color(0xFFFFFFFF) else Color(0xFF111827)
val NavActiveText: Color get() = if (isDarkThemeActive) Color(0xFF09090B) else Color(0xFFFFFFFF)
val NavInactiveText: Color get() = if (isDarkThemeActive) Color(0xFF8E8E98) else Color(0xFF6B7280)

// ---- Text Colors ---------------------------------------------------------
val TextPrimary: Color get() = Ink
val TextSecondary: Color get() = if (isDarkThemeActive) Color(0xFFA1A1AA) else Color(0xFF4B5563)
val TextTertiary: Color get() = if (isDarkThemeActive) Color(0xFF71717A) else Color(0xFF9CA3AF)
val TextWhite = Color(0xFFFFFFFF)
val TextMuted: Color get() = if (isDarkThemeActive) Color(0xFF52525B) else Color(0xFF9CA3AF)

// ---- Status Colors (Vibrant on dark) -------------------------------------
val StatusSuccess = Color(0xFF34D399)
val StatusSuccessSoft = Color(0xFF0F3D2E)
val StatusWarning = Color(0xFFFBBF24)
val StatusWarningSoft = Color(0xFF3D2E0F)
val StatusError = Color(0xFFF87171)
val StatusErrorSoft = Color(0xFF3D1F1F)
val StatusInfo = Color(0xFF60A5FA)
val StatusInfoSoft = Color(0xFF1E293B)

// ---- Category Colors (Rich Variety) --------------------------------------
val CategoryPlumbing = Color(0xFF60A5FA)
val CategoryElectrical = Color(0xFFFBBF24)
val CategoryCarpentry = Color(0xFFD4A574)
val CategoryPainting = Color(0xFFA78BFA)
val CategoryCleaning = Color(0xFF22D3EE)
val CategoryLandscaping = Color(0xFF34D399)
val CategoryHVAC = Color(0xFFF87171)
val CategoryRoofing = Color(0xFFA1A1AA)

// ---- Dark Theme surfaces -------------------------------------------------
val Coal = Color(0xFF09090B)
val CoalSurface = Color(0xFF12121A)
val CoalElevated = Color(0xFF1E1E2A)
val CoalLine = Color(0xFF27272A)

// ---- Glassmorphism & Effects ---------------------------------------------
val GlassSurface = Color(0xE61A1A24)
val CardShadow = Color(0x66000000)        // stronger shadow on dark
val OverlayShadow = Color(0x99000000)

// ---- Feature-Specific Palettes -------------------------------------------
/** Consumer shopping - electric lime */
val ConsumerPrimary = ElectricLime
val ConsumerSecondary = GrahakWarmWhite
val ConsumerAccentColor = ElectricLime
val ConsumerGradient = GradientLime

/** Plumber dashboard - warm amber/orange */
val PlumberPrimary = ElectricOrange
val PlumberSecondary = Amber
val PlumberAccentColor = ElectricYellow
val PlumberGradient = GradientSunrise

/** Orders - fresh green */
val OrderGradient = GradientTeal
val OrderAccent = ElectricGreen

/** Jobs - dynamic coral */
val JobGradient = GradientCoral
val JobAccent = Coral

/** Wallet - rich gold */
val WalletGradient = GradientSunset
val WalletAccent = Amber

/** Notifications - bright cyan */
val NotificationGradient = GradientMint
val NotificationAccent = ElectricTeal

// ---- Legacy Compatibility ------------------------------------------------
val InkColor = Ink
val InkSoftColor = InkLight
val FixitYellow = Color(0xFFF7AC49)
val FixitYellowDeep = Color(0xFFE09330)
val FixitYellowSoft = Color(0x24F7AC49)
val BrandTeal = ElectricTeal
val BrandTealDark = Emerald
val BrandTealSoft = SoftTeal
val BrandPurple = ElectricPurple
val BrandPurpleDeep = DeepPurple
val BrandPurpleLight = Color(0xFFC4B5FD)
val BrandPurpleSoft = SoftPurple
val BrandOrange = ElectricOrange
val BrandOrangeDeep = Color(0xFFEA580C)
val BrandOrangeLight = Color(0xFFFDBA74)
val BrandOrangeSoft = SoftOrange
val BrandGreen = ElectricGreen
val BrandGreenDeep = Emerald
val BrandGreenSoft = SoftGreen
val AmberSoft = SoftYellow
val BackgroundLight = Paper
val SurfaceLight = SurfaceWhite
val BackgroundDark = Coal
val SurfaceDark = CoalSurface
val PrimaryBlue = RoyalBlue
val PrimaryBlueDark = Color(0xFF1E40AF)
val PrimaryBlueLight = Color(0xFF93C5FD)
val SecondaryOrange = ElectricOrange
val SecondaryOrangeLight = SoftOrange
val PlumberAccentLight = SoftOrange
val RFQBadge = ElectricYellow
val DividerColor = Line
val BorderColor = Line
val TintMint = SoftGreen
val TintPeach = SoftOrange
val TintSky = SoftBlue
val TintButter = SoftYellow
val TintLilac = SoftPurple
val TintBlush = SoftPink
val TintSage = SoftGreen
val TintClay = Color(0xFF3D2E1F)
val GradientPrimary = GradientLime
val GradientAccent = GradientSunrise
val GradientSuccess = GradientTeal
val CardShadowLight = CardShadow
val CardShadowMedium = Color(0x80000000)
val PlumberAccent = PlumberPrimary
val ConsumerAccent = ConsumerPrimary
val ConsumerAccentGradient = ConsumerGradient
val PlumberAccentGradient = PlumberGradient
