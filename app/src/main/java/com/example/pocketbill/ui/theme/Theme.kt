package com.example.pocketbill.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = Navy40,
    secondary = Slate40,
    tertiary = Teal40,
)

private val DarkColors = darkColorScheme(
    primary = Navy80,
    secondary = Slate80,
    tertiary = Teal80,
)

// Dynamic (wallpaper-based) color is intentionally not used so the app looks consistent on every phone.
@Composable
fun PocketBillTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
