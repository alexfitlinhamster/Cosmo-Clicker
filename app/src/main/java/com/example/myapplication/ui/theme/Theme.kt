package com.example.myapplication.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = AppColors.Primary,
    secondary = AppColors.Secondary,
    tertiary = AppColors.Warning,
    background = AppColors.BackgroundStart,
    surface = AppColors.CardBackground,
    surfaceVariant = Color(0xFF17263B),
    onPrimary = Color(0xFF001F18),
    onBackground = Color.White,
    onSurface = Color.White,
    error = AppColors.Danger
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
