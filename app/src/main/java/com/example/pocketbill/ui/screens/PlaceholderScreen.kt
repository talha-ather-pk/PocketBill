package com.example.pocketbill.ui.screens

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.pocketbill.R

/**
 * Stands in for a screen whose route exists (so navigation can be built and tested) but whose
 * content comes in a later task. Removed screen by screen as Phase 4 builds them.
 */
@Composable
fun PlaceholderScreen(
    @StringRes titleRes: Int,
    modifier: Modifier = Modifier,
) {
    EmptyStateScreen(titleRes = titleRes, bodyRes = R.string.placeholder_body, modifier = modifier)
}
