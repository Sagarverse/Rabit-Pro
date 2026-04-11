package com.example.rabit.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush

object AppThemeMode {
    var isMonochrome by mutableStateOf(true)
}

// ── Core Dark Palette ──
val Obsidian = Color(0xFF000000)
val Graphite = Color(0xFF0F0F11) // Sleek dark charcoal
val SoftGrey = Color(0xFF1A1A1C)
val Silver = Color(0xFF8E8E93)
val Platinum = Color(0xFFFAFAFA)

// ── Accent Colors (Dynamic) ──
val AccentBlue: Color get() = if (AppThemeMode.isMonochrome) Platinum else Color(0xFF0A84FF)
val AccentGold: Color get() = if (AppThemeMode.isMonochrome) Color(0xFFD1D1D6) else Color(0xFFFFD60A)
val SuccessGreen: Color get() = Color(0xFF32D74B) // Always green for connection indicators
val ErrorRed: Color get() = Color(0xFFFF453A)
val WarningYellow: Color get() = if (AppThemeMode.isMonochrome) Color(0xFFE5E5EA) else Color(0xFFFF9F0A)
val AccentPurple: Color get() = if (AppThemeMode.isMonochrome) Color(0xFFE5E5EA) else Color(0xFFBF5AF2)
val AccentTeal: Color get() = if (AppThemeMode.isMonochrome) Color(0xFFD1D1D6) else Color(0xFF64D2FF)
val AccentOrange: Color get() = if (AppThemeMode.isMonochrome) Color(0xFFF2F2F7) else Color(0xFFFF9F0A)
val AccentPink: Color get() = if (AppThemeMode.isMonochrome) Color(0xFFFFFFFF) else Color(0xFFFF375F)

// ── Premium Gradients (Dynamic) ──
val PremiumBlueGradient: Brush get() = Brush.verticalGradient(
    if (AppThemeMode.isMonochrome) listOf(Color(0xFFFFFFFF), Color(0xFFD1D1D6))
    else listOf(Color(0xFF0A84FF), Color(0xFF005BB5))
)
val DarkGlassGradient = Brush.verticalGradient(listOf(Color(0x99000000), Color(0x660F0F11)))
val PremiumDarkGradient = Brush.verticalGradient(
    listOf(Color(0xFF1C1C1E), Color(0xFF0F0F11), Color(0xFF000000))
)
val PremiumGoldGradient: Brush get() = Brush.horizontalGradient(
    if (AppThemeMode.isMonochrome) listOf(Color(0xFFE5E5EA), Color(0xFFFFFFFF), Color(0xFFE5E5EA))
    else listOf(Color(0xFFFFD60A), Color(0xFFFFF59D), Color(0xFFFFD60A))
)
val GlowBlue: Color get() = if (AppThemeMode.isMonochrome) Color(0x40FFFFFF) else Color(0x400A84FF)
val GlowGreen = Color(0x2032D74B)
val GlowGold: Color get() = if (AppThemeMode.isMonochrome) Color(0x40D1D1D6) else Color(0x40FFD60A)

// ── Functional Colors ──
val GlassOverlay = Color(0x66000000)
val BorderColor = Color(0xFF2C2C2E)
val KeyBackground = Color(0xFF141415)
val KeyText = Color(0xFFE5E5EA)
val CardDark = Color(0xFF0F0F11)
val CardDarkBorder = Color(0xFF2C2C2E)

// ── Device Type Colors ──
val MacDeviceColor: Color get() = if (AppThemeMode.isMonochrome) Color(0xFFFFFFFF) else Color(0xFF0A84FF)
val AndroidDeviceColor: Color get() = if (AppThemeMode.isMonochrome) Color(0xFFD1D1D6) else Color(0xFF32D74B)
val WindowsDeviceColor: Color get() = if (AppThemeMode.isMonochrome) Color(0xFFAEAEC0) else Color(0xFF64D2FF)
val UnknownDeviceColor = Color(0xFF8E8E93)

// ── Push Control Colors ──
val PausedAmber: Color get() = if (AppThemeMode.isMonochrome) Color(0xFFD1D1D6) else Color(0xFFFF9F0A)
val StopRed: Color get() = if (AppThemeMode.isMonochrome) Color(0xFFFFFFFF) else Color(0xFFFF453A)

// ── AI Chat Colors ──
val AiViolet: Color get() = if (AppThemeMode.isMonochrome) Color(0xFFFFFFFF) else Color(0xFFBF5AF2)
val AiIndigo: Color get() = if (AppThemeMode.isMonochrome) Color(0xFFE5E5EA) else Color(0xFF5E5CE6)
val ChatSurface = Color(0xFF000000) // Pure OLED black
val InputBarGlass = Color(0xFF0F0F11)
val AiOrbGlow: Color get() = if (AppThemeMode.isMonochrome) Color(0xFFFFFFFF) else Color(0xFFBF5AF2)

// ── AI Chat Gradients ──
val UserBubbleGradient = Brush.linearGradient(
    listOf(Color(0xFF2C2C2E), Color(0xFF1C1C1E))
)
val AiBubbleGradient = Brush.verticalGradient(
    listOf(Color(0xFF0F0F11), Color(0xFF0A0A0C))
)
val AiOrbGradient: Brush get() = Brush.radialGradient(
    if (AppThemeMode.isMonochrome) listOf(Color(0x80FFFFFF), Color.Transparent)
    else listOf(Color(0x80BF5AF2), Color.Transparent)
)
val SuggestionChipGradient = Brush.horizontalGradient(
    listOf(Color(0xFF1C1C1E), Color(0xFF141415))
)
