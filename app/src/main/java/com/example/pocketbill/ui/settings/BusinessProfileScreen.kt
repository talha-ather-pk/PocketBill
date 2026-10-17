package com.example.pocketbill.ui.settings

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
import com.example.pocketbill.domain.invoice.InvoiceNumbering
import com.example.pocketbill.domain.invoice.PaperSize
import com.example.pocketbill.ui.appContainer
import com.example.pocketbill.ui.components.CurrencyField
import com.example.pocketbill.ui.theme.Spacing

/** Settings → Business profile: every field of MVP_SPEC F-01, plus the DATA_MODEL 5.5 dialog. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusinessProfileScreen(onBack: () -> Unit) {
    val container = appContainer()
    val vm: BusinessProfileViewModel = viewModel { BusinessProfileViewModel(container.profile) }
    val state by vm.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.done) { if (state.done) onBack() }

    // The app's outer Scaffold already handles the status bar; without zero insets here the gap is doubled.
    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0),
                title = { Text(stringResource(R.string.title_business_profile)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
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
            LogoSection()
            FormTextField(
                value = form.name,
                onValueChange = { v -> vm.onFormChange { it.copy(name = v) } },
                label = stringResource(R.string.field_business_name),
                error = profileErrorText(state.errors[ProfileField.NAME]),
                capitalization = KeyboardCapitalization.Words,
            )
            FormTextField(
                value = form.contactLines,
                onValueChange = { v -> vm.onFormChange { it.copy(contactLines = v) } },
                label = stringResource(R.string.field_contact),
                helper = stringResource(R.string.field_contact_help),
                singleLine = false,
                minLines = 2,
                capitalization = KeyboardCapitalization.None,
            )
            FormTextField(
                value = form.address,
                onValueChange = { v -> vm.onFormChange { it.copy(address = v) } },
                label = stringResource(R.string.field_address),
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
            FormTextField(
                value = form.paymentInstructions,
                onValueChange = { v -> vm.onFormChange { it.copy(paymentInstructions = v) } },
                label = stringResource(R.string.field_payment),
                helper = stringResource(R.string.field_payment_help),
                singleLine = false,
                minLines = 3,
            )

            SectionTitle(stringResource(R.string.settings_section_defaults))
            CurrencyField(
                label = stringResource(R.string.field_currency),
                code = form.currency,
                onChange = { code -> if (code != null) vm.onFormChange { it.copy(currency = code) } },
            )
            Text(stringResource(R.string.field_terms), style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                TERMS_CHOICES.forEach { days ->
                    FilterChip(
                        selected = form.termsDays == days,
                        onClick = { vm.onFormChange { it.copy(termsDays = days) } },
                        label = { Text(if (days == 0) stringResource(R.string.terms_on_receipt) else pluralStringResource(R.plurals.terms_days, days, days)) },
                    )
                }
            }
            FormTextField(
                value = form.taxRate,
                onValueChange = { v -> vm.onFormChange { it.copy(taxRate = v) } },
                label = stringResource(R.string.field_tax_rate),
                error = profileErrorText(state.errors[ProfileField.TAX_RATE]),
                keyboardType = KeyboardType.Decimal,
            )

            SectionTitle(stringResource(R.string.settings_section_numbering))
            FormTextField(
                value = form.prefix,
                onValueChange = { v -> vm.onFormChange { it.copy(prefix = v) } },
                label = stringResource(R.string.field_prefix),
                error = profileErrorText(state.errors[ProfileField.PREFIX]),
                capitalization = KeyboardCapitalization.Characters,
            )
            FormTextField(
                value = form.nextNumber,
                onValueChange = { v -> vm.onFormChange { it.copy(nextNumber = v) } },
                label = stringResource(R.string.field_next_number),
                helper = nextNumberPreview(form),
                error = profileErrorText(state.errors[ProfileField.NEXT_NUMBER], form.minNextNumber),
                keyboardType = KeyboardType.Number,
            )

            SectionTitle(stringResource(R.string.field_paper))
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                FilterChip(
                    selected = form.paperSize == PaperSize.A4,
                    onClick = { vm.onPaperSize(PaperSize.A4) },
                    label = { Text(stringResource(R.string.paper_a4)) },
                )
                FilterChip(
                    selected = form.paperSize == PaperSize.LETTER,
                    onClick = { vm.onPaperSize(PaperSize.LETTER) },
                    label = { Text(stringResource(R.string.paper_letter)) },
                )
            }

            if (state.failed) {
                Text(stringResource(R.string.save_failed), color = MaterialTheme.colorScheme.error)
            }
            Button(
                onClick = vm::onSave,
                enabled = !state.saving,
                modifier = Modifier.fillMaxWidth().heightIn(min = Spacing.buttonHeight).padding(top = Spacing.s),
            ) {
                Text(stringResource(R.string.action_save))
            }
        }
    }

    state.prompt?.let { prompt ->
        UpdateUnpaidDialog(
            prompt = prompt,
            onChoice = vm::onPromptChoice,
            onConfirm = vm::onPromptConfirm,
            onDismiss = vm::onPromptDismiss,
        )
    }
}

@Composable
internal fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = Spacing.l))
}

/** "Next invoice: INV-0058", using the same numbering rule as issuing (DATA_MODEL N-2). */
@Composable
private fun nextNumberPreview(form: ProfileForm): String? {
    val next = form.nextNumber.trim().toLongOrNull()?.takeIf { it >= 1 } ?: return null
    return stringResource(R.string.field_next_number_help, InvoiceNumbering.format(form.prefix, next))
}

/** DATA_MODEL 5.5: "Update unpaid invoices too?", pre-selected when payment details changed. */
@Composable
private fun UpdateUnpaidDialog(
    prompt: UpdateUnpaidPrompt,
    onChoice: (Boolean) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.update_unpaid_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
                if (prompt.paymentDetailsChanged) {
                    Text(stringResource(R.string.update_unpaid_payment_warning), color = MaterialTheme.colorScheme.error)
                }
                Text(pluralStringResource(R.plurals.update_unpaid_body, prompt.unpaidCount, prompt.unpaidCount))
                ChoiceRow(stringResource(R.string.update_unpaid_only_new), selected = !prompt.alsoUpdate) { onChoice(false) }
                ChoiceRow(pluralStringResource(R.plurals.update_unpaid_also, prompt.unpaidCount, prompt.unpaidCount), selected = prompt.alsoUpdate) { onChoice(true) }
                Text(
                    stringResource(R.string.update_unpaid_paid_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(R.string.action_save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
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
