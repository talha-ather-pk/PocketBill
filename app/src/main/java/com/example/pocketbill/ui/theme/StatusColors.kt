package com.example.pocketbill.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** Text and fill colours for one invoice status chip (UX_SPEC.md sections 5 and 8.1). */
@Immutable
data class StatusColor(val content: Color, val container: Color)

/** Invoice status colours, which Material's colour scheme has no roles for. */
@Immutable
data class StatusColors(
    val paid: StatusColor,
    val unpaid: StatusColor,
    val overdue: StatusColor,
    /** Drafts are outlined, not filled: [StatusColor.container] is transparent. */
    val draft: StatusColor,
    val void: StatusColor,
)

internal val LightStatusColors = StatusColors(
    paid = StatusColor(Color(0xFF00574E), Color(0xFFD5F2EC)),
    unpaid = StatusColor(Color(0xFF3C4A5A), Color(0xFFE3E9F1)),
    overdue = StatusColor(Color(0xFF9C2218), Color(0xFFFBE1DE)),
    draft = StatusColor(Color(0xFF566476), Color.Transparent),
    void = StatusColor(Color(0xFF566476), Color(0xFFE3E9F1)),
)

internal val DarkStatusColors = StatusColors(
    paid = StatusColor(Color(0xFF7FE0D0), Color(0xFF123D38)),
    unpaid = StatusColor(Color(0xFFC3CFDB), Color(0xFF26303B)),
    overdue = StatusColor(Color(0xFFFFB4AB), Color(0xFF4A1D19)),
    draft = StatusColor(Color(0xFF9EABBA), Color.Transparent),
    void = StatusColor(Color(0xFF9EABBA), Color(0xFF26303B)),
)

internal val LocalStatusColors = staticCompositionLocalOf { LightStatusColors }
