package com.example.pocketbill.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.example.pocketbill.R

/**
 * For forms with a Save button (business profile, client): leaving with edits asks
 * "Save your changes?" instead of silently dropping them. Without edits it leaves at once.
 * Handles the system back gesture; returns the action for the top bar's back arrow.
 */
@Composable
fun unsavedChangesGuard(hasChanges: Boolean, onSave: () -> Unit, onLeave: () -> Unit): () -> Unit {
    var asking by rememberSaveable { mutableStateOf(false) }
    BackHandler(enabled = hasChanges) { asking = true }
    if (asking) {
        AlertDialog(
            onDismissRequest = { asking = false },
            title = { Text(stringResource(R.string.editor_unsaved_title)) },
            text = { Text(stringResource(R.string.form_unsaved_body)) },
            confirmButton = {
                TextButton(onClick = {
                    asking = false
                    onSave()
                }) { Text(stringResource(R.string.editor_save_changes)) }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        asking = false
                        onLeave()
                    }) { Text(stringResource(R.string.editor_discard)) }
                    TextButton(onClick = { asking = false }) { Text(stringResource(R.string.editor_keep_editing)) }
                }
            },
        )
    }
    return { if (hasChanges) asking = true else onLeave() }
}
