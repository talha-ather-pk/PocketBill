package com.example.pocketbill.ui.editor

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pocketbill.R
import com.example.pocketbill.data.db.ClientEntity
import com.example.pocketbill.domain.format.DateFormatter
import com.example.pocketbill.domain.time.localDayOf
import com.example.pocketbill.domain.invoice.DiscountType
import com.example.pocketbill.domain.money.CurrencyCode
import com.example.pocketbill.domain.money.MoneyFormatter
import com.example.pocketbill.domain.text.normalizeForMatching
import com.example.pocketbill.ui.appContainer
import com.example.pocketbill.ui.components.CurrencyField
import com.example.pocketbill.ui.components.SaveStatusIndicator
import com.example.pocketbill.ui.settings.FormTextField
import com.example.pocketbill.ui.theme.Spacing
import com.example.pocketbill.ui.theme.tabular

private const val DAY_MS = 86_400_000L

/** The invoice editor (UX_SPEC 3). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceEditorScreen(
    args: EditorArgs,
    pickedClientId: Long?,
    onPickedClientConsumed: () -> Unit,
    onNewClient: () -> Unit,
    onDone: (EditorResult) -> Unit,
) {
    val c = appContainer()
    val vm: InvoiceEditorViewModel = viewModel { InvoiceEditorViewModel(c.invoices, c.clients, c.profile, c.presets, c.clock, args) }
    val state by vm.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.result) { state.result?.let(onDone) }
    LaunchedEffect(pickedClientId) {
        if (pickedClientId != null) {
            vm.onClientPicked(pickedClientId)
            onPickedClientConsumed()
        }
    }
    // Save at once when the app goes to the background, so a call or app switch loses nothing (UX_SPEC 3).
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { vm.flush() }
    BackHandler(onBack = vm::onBack)

    var menuOpen by rememberSaveable { mutableStateOf(false) }
    Scaffold(
        // The app's outer Scaffold already handles the status bar (see BusinessProfileScreen).
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0),
                title = {
                    Text(
                        if (state.mode == EditorMode.ISSUED) stringResource(R.string.editor_title_edit, state.issuedNumber.orEmpty())
                        else stringResource(R.string.title_new_invoice),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = vm::onBack) {
                        Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = stringResource(R.string.action_back))
                    }
                },
                actions = {
                    SaveStatusIndicator(state.saveStatus, Modifier.padding(end = Spacing.xs))
                    if (state.mode == EditorMode.DRAFT) {
                        Box {
                            IconButton(onClick = { menuOpen = true }) {
                                Icon(painterResource(R.drawable.ic_more_vert), contentDescription = stringResource(R.string.client_more))
                            }
                            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.editor_delete_draft)) },
                                    onClick = { menuOpen = false; vm.onDeleteRequest() },
                                )
                            }
                        }
                    }
                },
            )
        },
        bottomBar = { if (!state.loading) TotalBar(state, onIssue = { vm.onIssue() }, onSaveChanges = vm::onSaveChanges) },
        modifier = Modifier.imePadding(),
    ) { padding ->
        if (state.loading) return@Scaffold
        EditorContent(state, vm, onNewClient, Modifier.padding(padding))
    }

    EditorDialogs(state, vm)
}

@Composable
private fun EditorContent(state: EditorUiState, vm: InvoiceEditorViewModel, onNewClient: () -> Unit, modifier: Modifier) {
    val form = state.form
    val errors = state.errors
    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.item),
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = Spacing.screen, vertical = Spacing.m),
    ) {
        if (state.mode == EditorMode.ISSUED) {
            val banner = state.lastSentAt?.let {
                stringResource(R.string.editor_issued_banner_sent, DateFormatter.full(localDayOf(it)))
            } ?: stringResource(R.string.editor_issued_banner)
            Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = MaterialTheme.shapes.small) {
                Text(banner, modifier = Modifier.padding(Spacing.m), style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            state.nextNumberPreview?.let {
                Text(stringResource(R.string.editor_number_preview, it), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (errors.isNotEmpty()) {
            Text(stringResource(R.string.editor_fix_fields), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        }

        ClientSection(state, vm::onClientPicked, onNewClient, showError = EditorError.NoClient in errors)
        DetailsSection(state, vm)

        Text(stringResource(R.string.editor_items), style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
        if (EditorError.NoLines in errors) Text(stringResource(R.string.editor_error_no_lines), color = MaterialTheme.colorScheme.error)
        form.lines.forEachIndexed { index, line ->
            LineCard(index, line, state, vm)
        }
        TextButton(onClick = vm::onAddLine) {
            Icon(painterResource(R.drawable.ic_add), contentDescription = null)
            Text(stringResource(R.string.editor_add_item), modifier = Modifier.padding(start = Spacing.s))
        }

        FormTextField(
            value = form.notes,
            onValueChange = vm::onNotesChange,
            label = stringResource(R.string.editor_notes),
            singleLine = false,
            minLines = 2,
        )
    }
}

// ---- Client -------------------------------------------------------------------------------------

@Composable
private fun ClientSection(state: EditorUiState, onPick: (Long) -> Unit, onNewClient: () -> Unit, showError: Boolean) {
    var pickerOpen by rememberSaveable { mutableStateOf(false) }
    Text(stringResource(R.string.editor_client), style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
    val client = state.selectedClient
    if (client != null) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f)) {
                Text(client.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                client.email?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            TextButton(onClick = { pickerOpen = true }) { Text(stringResource(R.string.action_change)) }
        }
    } else {
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s), modifier = Modifier.horizontalScroll(rememberScrollState())) {
            state.recentClients.forEach { recent ->
                FilterChip(selected = false, onClick = { onPick(recent.id) }, label = { Text(recent.name, maxLines = 1) })
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
            AssistChip(onClick = { pickerOpen = true }, label = { Text(stringResource(R.string.editor_all_clients)) })
            AssistChip(onClick = onNewClient, label = { Text(stringResource(R.string.editor_new_client)) })
        }
        if (showError) Text(stringResource(R.string.editor_error_client), color = MaterialTheme.colorScheme.error)
    }
    if (pickerOpen) {
        ClientPickerDialog(
            clients = state.allClients,
            onPick = { onPick(it); pickerOpen = false },
            onNewClient = { pickerOpen = false; onNewClient() },
            onDismiss = { pickerOpen = false },
        )
    }
}

@Composable
private fun ClientPickerDialog(clients: List<ClientEntity>, onPick: (Long) -> Unit, onNewClient: () -> Unit, onDismiss: () -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    val wanted = normalizeForMatching(query)
    val matches = clients.filter { wanted.isEmpty() || normalizeForMatching(it.name).contains(wanted) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.editor_choose_client)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
                OutlinedTextField(
                    value = query, onValueChange = { query = it }, singleLine = true,
                    label = { Text(stringResource(R.string.clients_search)) }, modifier = Modifier.fillMaxWidth(),
                )
                LazyColumn(Modifier.heightIn(max = 320.dp)) {
                    items(matches, key = { it.id }) { client ->
                        Text(
                            client.name,
                            modifier = Modifier.fillMaxWidth().heightIn(min = Spacing.touchTarget).clickable { onPick(client.id) }.padding(vertical = Spacing.m),
                        )
                    }
                }
                TextButton(onClick = onNewClient) { Text(stringResource(R.string.editor_new_client)) }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

// ---- Summary line and invoice details -------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DetailsSection(state: EditorUiState, vm: InvoiceEditorViewModel) {
    val form = state.form
    var expanded by rememberSaveable { mutableStateOf(false) }
    var datePicker by rememberSaveable { mutableStateOf<String?>(null) } // "issue" or "due"
    val errors = state.errors

    // UX_SPEC 3: currency and due date are always visible in words, so a wrong default is noticed.
    val taxValue = form.taxRate.replace(',', '.').toBigDecimalOrNull()
    val taxText = if (taxValue == null || taxValue.signum() == 0) stringResource(R.string.editor_no_tax) else stringResource(R.string.editor_tax_short, form.taxRate)
    val dueText = when (form.dueDays) {
        null -> stringResource(R.string.editor_due_on, DateFormatter.full(form.dueDate))
        0 -> stringResource(R.string.editor_due_on_receipt)
        else -> stringResource(R.string.editor_due_in, pluralStringResource(R.plurals.terms_days, form.dueDays, form.dueDays), DateFormatter.inList(form.dueDate, form.issueDate))
    }
    Surface(
        onClick = { expanded = !expanded },
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(Spacing.m), verticalAlignment = Alignment.CenterVertically) {
            Text("${form.currency} · $dueText · $taxText", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            Text(stringResource(if (expanded) R.string.editor_details_hide else R.string.editor_details_show), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
        }
    }
    val detailErrors = errors.any { it is EditorError.TaxRate || it is EditorError.DiscountValue || it is EditorError.DueBeforeIssue }
    if (!expanded && !detailErrors) return

    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(Spacing.l), verticalArrangement = Arrangement.spacedBy(Spacing.item)) {
            CurrencyField(
                label = stringResource(R.string.editor_currency),
                code = form.currency,
                onChange = { code -> code?.let(vm::onCurrencyChange) },
            )
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.editor_issue_date), modifier = Modifier.weight(1f))
                TextButton(onClick = { datePicker = "issue" }) { Text(DateFormatter.full(form.issueDate)) }
            }
            Text(stringResource(R.string.field_terms), style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                TERMS_CHIPS.forEach { days ->
                    FilterChip(
                        selected = form.dueDays == days,
                        onClick = { vm.onDueDaysChange(days) },
                        label = { Text(if (days == 0) stringResource(R.string.terms_on_receipt) else pluralStringResource(R.plurals.terms_days, days, days)) },
                    )
                }
                FilterChip(
                    selected = form.dueDays == null || form.dueDays !in TERMS_CHIPS,
                    onClick = { datePicker = "due" },
                    label = { Text(stringResource(R.string.editor_custom_date)) },
                )
            }
            Text(stringResource(R.string.editor_due_date_line, DateFormatter.full(form.dueDate)), style = MaterialTheme.typography.bodyMedium)
            if (EditorError.DueBeforeIssue in errors) Text(stringResource(R.string.editor_error_due), color = MaterialTheme.colorScheme.error)
            FormTextField(
                value = form.taxRate,
                onValueChange = vm::onTaxRateChange,
                label = stringResource(R.string.field_tax_rate),
                error = if (EditorError.TaxRate in errors) stringResource(R.string.profile_error_bad_percent) else null,
                keyboardType = KeyboardType.Decimal,
            )
            Text(stringResource(R.string.editor_discount), style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                FilterChip(selected = form.discountType == null, onClick = { vm.onDiscountTypeChange(null) }, label = { Text(stringResource(R.string.editor_discount_none)) })
                FilterChip(selected = form.discountType == DiscountType.PERCENT, onClick = { vm.onDiscountTypeChange(DiscountType.PERCENT) }, label = { Text("%") })
                FilterChip(selected = form.discountType == DiscountType.AMOUNT, onClick = { vm.onDiscountTypeChange(DiscountType.AMOUNT) }, label = { Text(form.currency) })
            }
            if (form.discountType != null) {
                FormTextField(
                    value = form.discountValue,
                    onValueChange = vm::onDiscountValueChange,
                    label = stringResource(if (form.discountType == DiscountType.PERCENT) R.string.editor_discount_percent else R.string.editor_discount_amount),
                    error = if (EditorError.DiscountValue in errors) stringResource(R.string.editor_error_discount) else null,
                    keyboardType = KeyboardType.Decimal,
                )
            }
        }
    }

    datePicker?.let { which ->
        val initial = if (which == "issue") form.issueDate else form.dueDate
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = initial * DAY_MS)
        DatePickerDialog(
            onDismissRequest = { datePicker = null },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        val day = millis / DAY_MS
                        if (which == "issue") vm.onIssueDateChange(day) else vm.onCustomDueDate(day)
                    }
                    datePicker = null
                }) { Text(stringResource(R.string.action_ok)) }
            },
            dismissButton = { TextButton(onClick = { datePicker = null }) { Text(stringResource(R.string.action_cancel)) } },
        ) { DatePicker(state = pickerState) }
    }
}

// ---- Line items -----------------------------------------------------------------------------------

@Composable
private fun LineCard(index: Int, line: LineForm, state: EditorUiState, vm: InvoiceEditorViewModel) {
    var menuOpen by rememberSaveable(line.uuid) { mutableStateOf(false) }
    val errors = state.errors
    val currency = state.form.currency
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(Spacing.m), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.editor_item_n, index + 1), style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                Box {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(painterResource(R.drawable.ic_more_vert), contentDescription = stringResource(R.string.editor_item_menu, index + 1))
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(text = { Text(stringResource(R.string.editor_remove_item)) }, onClick = { menuOpen = false; vm.onRemoveLine(index) })
                        if (index > 0) DropdownMenuItem(text = { Text(stringResource(R.string.editor_move_up)) }, onClick = { menuOpen = false; vm.onMoveLine(index, -1) })
                        if (index < state.form.lines.lastIndex) DropdownMenuItem(text = { Text(stringResource(R.string.editor_move_down)) }, onClick = { menuOpen = false; vm.onMoveLine(index, 1) })
                        DropdownMenuItem(text = { Text(stringResource(R.string.editor_save_preset)) }, onClick = { menuOpen = false; vm.onSaveLineAsPreset(index) })
                    }
                }
            }
            FormTextField(
                value = line.description,
                onValueChange = { v -> vm.onLineChange(index) { it.copy(description = v) } },
                label = stringResource(R.string.editor_description),
                error = if (errors.any { it is EditorError.LineDescription && it.index == index }) stringResource(R.string.editor_error_description) else null,
            )
            state.suggestions?.takeIf { it.lineIndex == index }?.let { s ->
                Column {
                    s.presets.forEach { preset ->
                        val price = preset.unitPriceMinor?.takeIf { preset.currency == currency }?.let { p ->
                            CurrencyCode.fromCode(currency)?.let { MoneyFormatter.format(p, it) }
                        }
                        TextButton(onClick = { vm.onSuggestionPicked(preset) }, modifier = Modifier.fillMaxWidth()) {
                            Text(listOfNotNull(preset.description, price).joinToString(" · "), modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                FormTextField(
                    value = line.quantity,
                    onValueChange = { v -> vm.onLineChange(index) { it.copy(quantity = v) } },
                    label = stringResource(R.string.editor_quantity),
                    error = if (errors.any { it is EditorError.LineQuantity && it.index == index }) stringResource(R.string.editor_error_quantity) else null,
                    keyboardType = KeyboardType.Decimal,
                    modifier = Modifier.weight(1f),
                )
                val priceError = errors.firstOrNull { it is EditorError.LinePrice && it.index == index } as EditorError.LinePrice?
                FormTextField(
                    value = line.price,
                    onValueChange = { v -> vm.onLineChange(index) { it.copy(price = v) } },
                    label = stringResource(R.string.editor_price, currency),
                    error = priceError?.let { e ->
                        when (e.decimals) {
                            null -> stringResource(R.string.editor_error_price)
                            0 -> stringResource(R.string.editor_error_no_decimals, currency)
                            else -> stringResource(R.string.editor_error_decimals, currency, e.decimals)
                        }
                    },
                    keyboardType = KeyboardType.Decimal,
                    modifier = Modifier.weight(1.6f),
                )
            }
            state.totals.lineAmounts.getOrNull(index)?.takeIf { it > 0 }?.let { amount ->
                CurrencyCode.fromCode(currency)?.let {
                    Text(
                        stringResource(R.string.editor_line_total, MoneyFormatter.format(amount, it)),
                        style = MaterialTheme.typography.bodyMedium.tabular,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.End),
                    )
                }
            }
        }
    }
}

// ---- Bottom bar and dialogs -------------------------------------------------------------------------

@Composable
private fun TotalBar(state: EditorUiState, onIssue: () -> Unit, onSaveChanges: () -> Unit) {
    val currency = CurrencyCode.fromCode(state.form.currency) ?: CurrencyCode.USD
    val t = state.totals
    Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(Modifier.fillMaxWidth().padding(horizontal = Spacing.screen, vertical = Spacing.m)) {
            if (t.discountMinor > 0 || t.taxMinor > 0) {
                TotalLine(stringResource(R.string.editor_subtotal), MoneyFormatter.format(t.subtotalMinor, currency))
                if (t.discountMinor > 0) TotalLine(stringResource(R.string.editor_discount), "−" + MoneyFormatter.format(t.discountMinor, currency))
                if (t.taxMinor > 0) TotalLine(stringResource(R.string.editor_tax), MoneyFormatter.format(t.taxMinor, currency))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.editor_total), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        if (t.tooLarge) stringResource(R.string.editor_error_too_large) else MoneyFormatter.format(t.totalMinor, currency),
                        style = MaterialTheme.typography.titleLarge.tabular,
                    )
                }
                if (state.mode == EditorMode.ISSUED) {
                    Button(onClick = onSaveChanges, enabled = !state.busy, modifier = Modifier.heightIn(min = Spacing.buttonHeight)) { Text(stringResource(R.string.editor_save_changes)) }
                } else {
                    Button(onClick = onIssue, enabled = !state.busy, modifier = Modifier.heightIn(min = Spacing.buttonHeight)) { Text(stringResource(R.string.editor_issue)) }
                }
            }
        }
    }
}

@Composable
private fun TotalLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth()) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium.tabular)
    }
}

@Composable
private fun EditorDialogs(state: EditorUiState, vm: InvoiceEditorViewModel) {
    when (val prompt = state.prompt) {
        is EditorPrompt.ConfirmZeroTotal -> AlertDialog(
            onDismissRequest = vm::onPromptDismiss,
            title = { Text(stringResource(R.string.editor_zero_title, prompt.formattedTotal)) },
            text = { Text(stringResource(R.string.editor_zero_body)) },
            confirmButton = { TextButton(onClick = vm::onConfirmZeroTotal) { Text(stringResource(R.string.editor_issue)) } },
            dismissButton = { TextButton(onClick = vm::onPromptDismiss) { Text(stringResource(R.string.action_cancel)) } },
        )
        EditorPrompt.UnsavedChanges -> AlertDialog(
            onDismissRequest = vm::onPromptDismiss,
            title = { Text(stringResource(R.string.editor_unsaved_title)) },
            text = { Text(stringResource(R.string.editor_unsaved_body)) },
            confirmButton = { TextButton(onClick = vm::onSaveChanges) { Text(stringResource(R.string.editor_save_changes)) } },
            dismissButton = {
                Row {
                    TextButton(onClick = vm::onDiscardChanges) { Text(stringResource(R.string.editor_discard)) }
                    TextButton(onClick = vm::onPromptDismiss) { Text(stringResource(R.string.editor_keep_editing)) }
                }
            },
        )
        EditorPrompt.ConfirmDelete -> AlertDialog(
            onDismissRequest = vm::onPromptDismiss,
            title = { Text(stringResource(R.string.editor_delete_title)) },
            confirmButton = { TextButton(onClick = vm::onDeleteConfirm) { Text(stringResource(R.string.editor_delete_draft)) } },
            dismissButton = { TextButton(onClick = vm::onPromptDismiss) { Text(stringResource(R.string.action_cancel)) } },
        )
        null -> Unit
    }
}
