package com.example.pocketbill.ui.clients

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pocketbill.R
import com.example.pocketbill.domain.Problem
import com.example.pocketbill.ui.appContainer
import com.example.pocketbill.ui.components.CurrencyField
import com.example.pocketbill.ui.settings.FormTextField
import com.example.pocketbill.ui.theme.Spacing

/** New client or edit (UX_SPEC 6, D-015). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientEditorScreen(clientId: Long, onCancel: () -> Unit, onDone: (ClientEditorResult) -> Unit) {
    val container = appContainer()
    val vm: ClientEditorViewModel = viewModel { ClientEditorViewModel(container.clients, clientId) }
    val state by vm.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.result) { state.result?.let(onDone) }

    // The app's outer Scaffold already handles the status bar (see BusinessProfileScreen).
    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0),
                title = { Text(stringResource(if (state.isNew) R.string.title_new_client else R.string.title_edit_client)) },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = stringResource(R.string.action_back))
                    }
                },
            )
        },
    ) { padding ->
        if (state.loading) return@Scaffold
        val form = state.form
        Column(
            verticalArrangement = Arrangement.spacedBy(Spacing.item),
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.screen, vertical = Spacing.l),
        ) {
            FormTextField(
                value = form.name,
                onValueChange = { v -> vm.onFormChange { it.copy(name = v) } },
                label = stringResource(R.string.client_field_name),
                error = if (state.errors[ClientField.NAME] != null) stringResource(R.string.client_error_name) else null,
                capitalization = KeyboardCapitalization.Words,
            )
            FormTextField(
                value = form.email,
                onValueChange = { v -> vm.onFormChange { it.copy(email = v) } },
                label = stringResource(R.string.client_field_email),
                helper = stringResource(R.string.client_field_email_help),
                error = if (state.errors[ClientField.EMAIL] == Problem.BAD_EMAIL) stringResource(R.string.client_error_email) else null,
                capitalization = KeyboardCapitalization.None,
                keyboardType = KeyboardType.Email,
            )
            FormTextField(
                value = form.country,
                onValueChange = { v -> vm.onFormChange { it.copy(country = v) } },
                label = stringResource(R.string.client_field_country),
                capitalization = KeyboardCapitalization.Words,
            )
            FormTextField(
                value = form.address,
                onValueChange = { v -> vm.onFormChange { it.copy(address = v) } },
                label = stringResource(R.string.client_field_address),
                singleLine = false,
                minLines = 2,
                capitalization = KeyboardCapitalization.Words,
            )
            FormTextField(
                value = form.taxId,
                onValueChange = { v -> vm.onFormChange { it.copy(taxId = v) } },
                label = stringResource(R.string.field_tax_id),
                helper = stringResource(R.string.field_tax_id_help),
                capitalization = KeyboardCapitalization.Characters,
            )

            Text(stringResource(R.string.client_section_defaults), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = Spacing.s))
            CurrencyField(
                label = stringResource(R.string.client_field_currency),
                code = form.preferredCurrency,
                onChange = { code -> vm.onFormChange { it.copy(preferredCurrency = code) } },
                noneLabel = stringResource(R.string.client_no_preference),
            )
            Text(stringResource(R.string.client_field_terms), style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                FilterChip(
                    selected = form.preferredTermsDays == null,
                    onClick = { vm.onFormChange { it.copy(preferredTermsDays = null) } },
                    label = { Text(stringResource(R.string.client_no_preference)) },
                )
                CLIENT_TERMS_CHOICES.forEach { days ->
                    FilterChip(
                        selected = form.preferredTermsDays == days,
                        onClick = { vm.onFormChange { it.copy(preferredTermsDays = days) } },
                        label = { Text(if (days == 0) stringResource(R.string.terms_on_receipt) else pluralStringResource(R.plurals.terms_days, days, days)) },
                    )
                }
            }
            Text(
                stringResource(R.string.client_defaults_help),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            FormTextField(
                value = form.notes,
                onValueChange = { v -> vm.onFormChange { it.copy(notes = v) } },
                label = stringResource(R.string.client_field_notes),
                helper = stringResource(R.string.client_field_notes_help),
                singleLine = false,
                minLines = 2,
                modifier = Modifier.padding(top = Spacing.s),
            )

            if (state.failed) Text(stringResource(R.string.save_failed), color = MaterialTheme.colorScheme.error)
            Button(
                onClick = vm::onSave,
                enabled = !state.saving,
                modifier = Modifier.fillMaxWidth().heightIn(min = Spacing.buttonHeight).padding(top = Spacing.s),
            ) { Text(stringResource(R.string.action_save)) }
        }
    }

    when (val prompt = state.prompt) {
        is ClientPrompt.Duplicate -> AlertDialog(
            onDismissRequest = vm::onPromptDismiss,
            title = { Text(stringResource(R.string.dup_title)) },
            text = { Text(stringResource(R.string.dup_body, prompt.existingName)) },
            confirmButton = {
                if (prompt.isNew) {
                    TextButton(onClick = vm::onUseExisting) { Text(stringResource(R.string.dup_use_existing)) }
                } else {
                    TextButton(onClick = vm::onSaveAnyway) { Text(stringResource(R.string.dup_save_anyway)) }
                }
            },
            dismissButton = {
                if (prompt.isNew) {
                    TextButton(onClick = vm::onSaveAnyway) { Text(stringResource(R.string.dup_create_anyway)) }
                } else {
                    TextButton(onClick = vm::onPromptDismiss) { Text(stringResource(R.string.action_cancel)) }
                }
            },
        )
        is ClientPrompt.UpdateUnpaid -> AlertDialog(
            onDismissRequest = vm::onPromptDismiss,
            title = { Text(stringResource(R.string.update_unpaid_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
                    Text(pluralStringResource(R.plurals.update_unpaid_body, prompt.unpaidCount, prompt.unpaidCount))
                    ChoiceRow(stringResource(R.string.update_unpaid_only_new), selected = !prompt.alsoUpdate) { vm.onUpdateChoice(false) }
                    ChoiceRow(pluralStringResource(R.plurals.update_unpaid_also, prompt.unpaidCount, prompt.unpaidCount), selected = prompt.alsoUpdate) {
                        vm.onUpdateChoice(true)
                    }
                    Text(
                        stringResource(R.string.update_unpaid_paid_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            confirmButton = { TextButton(onClick = vm::onUpdateConfirm) { Text(stringResource(R.string.action_save)) } },
            dismissButton = { TextButton(onClick = vm::onPromptDismiss) { Text(stringResource(R.string.action_cancel)) } },
        )
        null -> Unit
    }
}

@Composable
private fun ChoiceRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = Spacing.touchTarget)
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton),
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(label, modifier = Modifier.padding(start = Spacing.s))
    }
}
