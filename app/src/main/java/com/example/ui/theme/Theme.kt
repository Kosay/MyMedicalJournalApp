package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = HighlightTeal,
    onPrimary = Color(0xFF0E1624),
    primaryContainer = SlateCardBg,
    onPrimaryContainer = Color.White,
    secondary = ActiveTabBg,
    onSecondary = Color.White,
    background = SlateDarkBg,
    onBackground = Color.White,
    surface = SlateCardBg,
    onSurface = Color.White,
    surfaceVariant = Color(0xFF1D2C42),
    onSurfaceVariant = Color(0xFFE2E2E2)
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryLight,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFC0ECDB),
    onPrimaryContainer = Color(0xFF002115),
    secondary = SecondaryLight,
    onSecondary = Color.White,
    background = LightBg,
    onBackground = Color(0xFF191C1B),
    surface = LightCardBg,
    onSurface = Color(0xFF191C1B),
    surfaceVariant = Color(0xFFDEE5E0),
    onSurfaceVariant = Color(0xFF404944)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
