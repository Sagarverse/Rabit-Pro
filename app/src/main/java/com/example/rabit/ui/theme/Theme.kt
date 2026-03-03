package com.example.rabit.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = AccentBlue,
    secondary = AccentGold,
    tertiary = Silver,
    background = Obsidian,
    surface = Graphite,
    onPrimary = Obsidian,
    onSecondary = Obsidian,
    onTertiary = Obsidian,
    onBackground = Platinum,
    onSurface = Platinum,
    outline = BorderColor
)

private val LightColorScheme = lightColorScheme(
    primary = AccentBlue,
    secondary = AccentGold,
    tertiary = Silver,
    background = Platinum,
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Obsidian,
    onSurface = Obsidian,
    outline = Silver
)

@Composable
fun RabitTheme(
    darkTheme: Boolean = true, // Force Dark mode for premium feel
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
