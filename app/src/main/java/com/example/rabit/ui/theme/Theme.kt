package com.example.rabit.ui.theme

import android.app.Activity
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = AccentBlue,
    secondary = Color(0xFF8EA2C4),
    tertiary = Color(0xFF6FB7FF),
    background = Obsidian,
    surface = Graphite,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Platinum,
    onSurface = Platinum,
)

private val RabitTypography = Typography(
    headlineLarge = TextStyle(
        fontSize = 34.sp,
        lineHeight = 38.sp,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = (-0.9).sp
    ),
    headlineMedium = TextStyle(
        fontSize = 27.sp,
        lineHeight = 31.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.4).sp
    ),
    titleLarge = TextStyle(
        fontSize = 20.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontSize = 16.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.15.sp
    ),
    bodyLarge = TextStyle(
        fontSize = 15.sp,
        lineHeight = 22.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.15.sp
    ),
    bodyMedium = TextStyle(
        fontSize = 13.sp,
        lineHeight = 18.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.2.sp
    ),
    labelLarge = TextStyle(
        fontSize = 12.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.5.sp
    )
)

private val RabitShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun RabitTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = RabitTypography,
        shapes = RabitShapes,
        content = content
    )
}
