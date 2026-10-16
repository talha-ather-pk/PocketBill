package com.example.pocketbill.ui.theme

import androidx.compose.ui.unit.dp

/** Spacing and sizes on a 4 dp grid (UX_SPEC.md section 8.4). Use these instead of raw dp values. */
object Spacing {
    val xs = 4.dp
    val s = 8.dp
    val m = 12.dp
    val l = 16.dp
    val xl = 24.dp
    val xxl = 32.dp

    /** Left and right screen margins. */
    val screen = 16.dp

    /** Between sections on a screen. */
    val section = 24.dp

    /** Between cards or fields in a group. */
    val item = 12.dp

    /** Minimum tap target (UX_SPEC 7.1). */
    val touchTarget = 48.dp

    /** Minimum height of an invoice or client row. */
    val rowHeight = 64.dp

    /** Button height. */
    val buttonHeight = 48.dp
}
