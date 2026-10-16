package com.example.pocketbill.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/** Corner shapes (UX_SPEC.md section 8.3). Buttons use Material's default fully round shape. */
internal val PocketBillShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),   // chips, status chips, text fields
    medium = RoundedCornerShape(16.dp), // cards, lists
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

/** Bottom sheets: rounded top corners only. */
val SheetShape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
