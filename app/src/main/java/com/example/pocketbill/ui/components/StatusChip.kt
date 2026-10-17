package com.example.pocketbill.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.pocketbill.R
import com.example.pocketbill.domain.invoice.InvoiceStatus
import com.example.pocketbill.domain.invoice.isOverdue
import com.example.pocketbill.ui.theme.PocketBillTheme
import com.example.pocketbill.ui.theme.Spacing

/** What a status chip shows (UX_SPEC 5). Overdue is calculated, never stored. */
enum class DisplayStatus { DRAFT, UNPAID, OVERDUE, PAID, VOID }

fun displayStatus(status: InvoiceStatus, dueDate: Long, today: Long): DisplayStatus = when (status) {
    InvoiceStatus.DRAFT -> DisplayStatus.DRAFT
    InvoiceStatus.ISSUED -> if (isOverdue(status, dueDate, today)) DisplayStatus.OVERDUE else DisplayStatus.UNPAID
    InvoiceStatus.PAID -> DisplayStatus.PAID
    InvoiceStatus.VOID -> DisplayStatus.VOID
}

/** A status chip with a text label, so colour is never the only signal (UX_SPEC 7.1). */
@Composable
fun StatusChip(status: DisplayStatus, modifier: Modifier = Modifier) {
    val colors = PocketBillTheme.statusColors
    val (color, label) = when (status) {
        DisplayStatus.DRAFT -> colors.draft to R.string.status_draft
        DisplayStatus.UNPAID -> colors.unpaid to R.string.status_unpaid
        DisplayStatus.OVERDUE -> colors.overdue to R.string.status_overdue
        DisplayStatus.PAID -> colors.paid to R.string.status_paid
        DisplayStatus.VOID -> colors.void to R.string.status_void
    }
    Surface(
        color = color.container,
        contentColor = color.content,
        shape = MaterialTheme.shapes.small,
        border = if (color.container == Color.Transparent) BorderStroke(1.dp, MaterialTheme.colorScheme.outline) else null,
        modifier = modifier,
    ) {
        Text(
            text = (if (status == DisplayStatus.OVERDUE) "! " else "") + stringResource(label),
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = Spacing.s, vertical = 2.dp),
        )
    }
}
