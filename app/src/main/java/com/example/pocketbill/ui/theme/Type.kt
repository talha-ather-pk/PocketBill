package com.example.pocketbill.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle

/**
 * Type scale (UX_SPEC.md section 8.2). Material 3's default scale already matches the spec
 * (Roboto; headlineSmall 24/32, titleLarge 22/28, titleMedium 16/24 medium, bodyLarge 16/24,
 * bodyMedium 14/20, labelLarge 14/20 medium, labelMedium 12/16 medium), so it is used as is.
 */
internal val PocketBillTypography = Typography()

/** Same-width digits so amounts line up in lists and totals: `style = MaterialTheme.typography.titleLarge.tabular`. */
val TextStyle.tabular: TextStyle
    get() = copy(fontFeatureSettings = "tnum")
