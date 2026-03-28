package com.example.rabit.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush

// ── Core Dark Palette ──
val Obsidian = Color(0xFF000000)
val Graphite = Color(0xFF1C1C1E)
val SoftGrey = Color(0xFF2C2C2E)
val Silver = Color(0xFF8E8E93)
val Platinum = Color(0xFFF2F2F7)

// ── Accent Colors ──
val AccentBlue = Color(0xFF0A84FF)
val AccentGold = Color(0xFFD4AF37)
val SuccessGreen = Color(0xFF32D74B)
val ErrorRed = Color(0xFFFF453A)
val WarningYellow = Color(0xFFFFCC00)
val AccentPurple = Color(0xFFBF5AF2)
val AccentTeal = Color(0xFF64D2FF)
val AccentOrange = Color(0xFFFF9F0A)
val AccentPink = Color(0xFFFF375F)

// ── Premium Gradients ──
val PremiumBlueGradient = Brush.verticalGradient(listOf(Color(0xFF007AFF), Color(0xFF00C6FF)))
val DarkGlassGradient = Brush.verticalGradient(listOf(Color(0xBB1C1C1E), Color(0x661C1C1E)))
val PremiumDarkGradient = Brush.verticalGradient(
    listOf(Color(0xFF2A2A2E), Color(0xFF1C1C1E), Color(0xFF141416))
)
val PremiumGoldGradient = Brush.horizontalGradient(
    listOf(Color(0xFFD4AF37), Color(0xFFE8C857), Color(0xFFD4AF37))
)
val GlowBlue = Color(0x400A84FF)
val GlowGreen = Color(0x4032D74B)
val GlowGold = Color(0x40D4AF37)

// ── Functional Colors ──
val GlassOverlay = Color(0x661C1C1E)
val BorderColor = Color(0xFF3A3A3C)
val KeyBackground = Color(0xFF1C1C1E)
val KeyText = Color(0xFFFFFFFF)
val CardDark = Color(0xFF1A1A1C)
val CardDarkBorder = Color(0xFF2E2E30)

// ── Device Type Colors ──
val MacDeviceColor = Color(0xFF0A84FF)
val AndroidDeviceColor = Color(0xFF32D74B)
val UnknownDeviceColor = Color(0xFF8E8E93)

// ── Push Control Colors ──
val PausedAmber = Color(0xFFFF9F0A)
val StopRed = Color(0xFFFF453A)
