package com.example.pocketbill.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.pocketbill.R
import com.example.pocketbill.ui.theme.PocketBillTheme
import com.example.pocketbill.ui.theme.Spacing

/** Where a draft's autosave stands (UX_SPEC.md section 3, "Autosave"). */
enum class SaveStatus { IDLE, SAVING, SAVED, FAILED }

/**
 * Shows "Saving…" while typing and "Draft saved ✓" after each save, clearly visible rather than
 * faint: in P1-T07 the one participant who gave up wasn't sure items were being saved (D-013).
 * TalkBack announces changes politely; the status stays "Saving…" while typing, so it speaks once per pause.
 */
@Composable
fun SaveStatusIndicator(status: SaveStatus, modifier: Modifier = Modifier) {
    if (status == SaveStatus.IDLE) return
    val color = when (status) {
        SaveStatus.FAILED -> MaterialTheme.colorScheme.error
        SaveStatus.SAVED -> PocketBillTheme.statusColors.paid.content
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Row(
        modifier = modifier.semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        when (status) {
            SaveStatus.SAVING -> CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = color)
            SaveStatus.SAVED -> Icon(painterResource(R.drawable.ic_check), contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            SaveStatus.FAILED -> Icon(painterResource(R.drawable.ic_error), contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            SaveStatus.IDLE -> Unit
        }
        Text(
            text = stringResource(
                when (status) {
                    SaveStatus.SAVING -> R.string.save_status_saving
                    SaveStatus.SAVED -> R.string.save_status_saved
                    else -> R.string.save_status_failed
                },
            ),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
            color = color,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SaveStatusIndicatorPreview() {
    PocketBillTheme {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
            SaveStatusIndicator(SaveStatus.SAVING)
            SaveStatusIndicator(SaveStatus.SAVED)
            SaveStatusIndicator(SaveStatus.FAILED)
        }
    }
}
