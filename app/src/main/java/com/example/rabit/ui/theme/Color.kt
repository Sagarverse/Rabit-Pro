package com.example.rabit.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush

// Single Premium Minimal Theme (fixed)
val Obsidian = Color(0xFF0B0F14)
val Graphite = Color(0xFF141A22)
val SoftGrey = Color(0xFF1F2630)
val Silver = Color(0xFFA9B3C2)
val Platinum = Color(0xFFF5F8FF)

// Keep semantic names but map to one cohesive monochrome palette.
val AccentBlue = Color(0xFF1E6BFF)
val AccentGold = Color(0xFF8EA2C4)
val SuccessGreen = Color(0xFF1FA75A)
val ErrorRed: Color get() = Color(0xFFD93030)
val WarningYellow = Color(0xFFD9B443)
val AccentPurple = Color(0xFF3A7BFF)
val AccentTeal = Color(0xFF2A8DFF)
val AccentOrange = Color(0xFF9EB2D4)
val AccentPink = Color(0xFF4A8BFF)

val PremiumBlueGradient = Brush.verticalGradient(
    listOf(Color(0xFF2A79FF), Color(0xFF1654C6))
)
val DarkGlassGradient = Brush.verticalGradient(listOf(Color(0xCC0B0F14), Color(0xAA111822)))
val AppAtmosphereGradient = Brush.verticalGradient(
    listOf(Color(0xFF0A1018), Color(0xFF0E1622), Color(0xFF0B0F14))
)
val GlassCardGradient = Brush.verticalGradient(
    listOf(Color(0xCC1A2230), Color(0xB3141A22))
)
val PremiumDarkGradient = Brush.verticalGradient(
    listOf(Color(0xFF1B2430), Color(0xFF111822), Color(0xFF0B0F14))
)
val PremiumGoldGradient = Brush.horizontalGradient(
    listOf(Color(0xFF7F94B8), Color(0xFF9CB2D5), Color(0xFF7F94B8))
)
val GlowBlue = Color(0x402A79FF)
val GlowGreen = Color(0x301FA75A)
val GlowGold = Color(0x408EA2C4)

// ── Functional Colors ──
val GlassOverlay = Color(0x660B0F14)
val BorderColor = Color(0xFF2A3544)
val BorderStrong = Color(0xFF3A4A5F)
val KeyBackground = Color(0xFF1A2230)
val KeyText = Color(0xFFF0F4FF)
val CardDark = Color(0xFF121922)
val CardDarkBorder = Color(0xFF2A3544)

// Device colors remain neutral to preserve one-theme consistency.
val MacDeviceColor = Color(0xFFFFFFFF)
val AndroidDeviceColor = Color(0xFFD1D1D6)
val WindowsDeviceColor = Color(0xFFAEAEC0)
val UnknownDeviceColor = Color(0xFF8E8E93)

val PausedAmber = Color(0xFFD1D1D6)
val StopRed = Color(0xFFD93030)

val AiViolet = Color(0xFF2A79FF)
val AiIndigo = Color(0xFF1654C6)
val ChatSurface = Color(0xFF0B0F14)
val InputBarGlass = Color(0xFF121922)
val AiOrbGlow = Color(0x552A79FF)

// ── AI Chat Gradients ──
val UserBubbleGradient = Brush.linearGradient(
    listOf(Color(0xFF24406D), Color(0xFF1C3153))
)
val AiBubbleGradient = Brush.verticalGradient(
    listOf(Color(0xFF121922), Color(0xFF0E141D))
)
val AiOrbGradient = Brush.radialGradient(
    listOf(Color(0x662A79FF), Color.Transparent)
)
val SuggestionChipGradient = Brush.horizontalGradient(
    listOf(Color(0xFF1C2532), Color(0xFF151C27))
)
