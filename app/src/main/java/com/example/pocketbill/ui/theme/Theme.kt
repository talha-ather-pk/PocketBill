package com.example.pocketbill.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

// Dynamic (wallpaper-based) color is intentionally not used so the app looks consistent on every phone.
// Dark mode follows the phone's setting; there is no in-app switch in the MVP (UX_SPEC 8.1).
@Composable
fun PocketBillTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalStatusColors provides if (darkTheme) DarkStatusColors else LightStatusColors) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = PocketBillTypography,
            shapes = PocketBillShapes,
            content = content,
        )
    }
}

/** PocketBill-specific tokens alongside MaterialTheme: `PocketBillTheme.statusColors.overdue`. */
object PocketBillTheme {
    val statusColors: StatusColors
        @Composable @ReadOnlyComposable
        get() = LocalStatusColors.current
}
