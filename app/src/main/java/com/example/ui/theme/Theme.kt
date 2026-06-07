package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = CosmicLavender,
    onPrimary = DeepIndigoVelvet,
    primaryContainer = DeepIndigoVelvet,
    onPrimaryContainer = CosmicLavender,
    secondary = CosmicLavender,
    onSecondary = DeepIndigoVelvet,
    background = DarkCharcoal,
    onBackground = BrightText,
    surface = SurfaceCard,
    onSurface = BrightText,
    surfaceVariant = LineBorderColors,
    onSurfaceVariant = MutedGrayText,
    outline = LineBorderColors,
    error = HighlightRed
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force sleek deep dark editor color modes
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    // Always use our custom DarkColorScheme for the professional film workstation aesthetic
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
