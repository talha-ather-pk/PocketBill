package com.example.pocketbill.ui.invoices

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pocketbill.R
import com.example.pocketbill.data.db.InvoiceWithLines
import com.example.pocketbill.domain.format.DateFormatter
import com.example.pocketbill.domain.money.CurrencyCode
import com.example.pocketbill.domain.money.MoneyFormatter
import com.example.pocketbill.ui.appContainer
import com.example.pocketbill.ui.components.DisplayStatus
import com.example.pocketbill.ui.components.StatusChip
import com.example.pocketbill.ui.screens.EmptyStateScreen
import com.example.pocketbill.ui.send.SendMessage
import com.example.pocketbill.ui.send.SendRequestHandler
import com.example.pocketbill.ui.send.sendMessageText
import com.example.pocketbill.ui.theme.Spacing
import com.example.pocketbill.ui.theme.tabular
import java.math.BigDecimal

private const val DAY_MS = 86_400_000L

/** Invoice detail (UX_SPEC 4–5): sending is the primary action, then status actions and history. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceDetailScreen(
    invoiceId: Long,
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    onDuplicate: (Long) -> Unit,
    onPreview: (Long) -> Unit,
    onOpenClient: (Long) -> Unit,
    onEditClient: (Long) -> Unit,
) {
    val c = appContainer()
    val vm: InvoiceDetailViewModel = viewModel {
        InvoiceDetailViewModel(c.invoices, c.clients, c.clock, invoiceId, sending = c.invoices, preparer = c.sharePreparer)
    }
    val state by vm.state.collectAsStateWithLifecycle()
    val sendState by vm.send.state.collectAsStateWithLifecycle()
    SendRequestHandler(sendState.request, vm.send)
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(state.deleted) { if (state.deleted) onBack() }

    val messageTexts = mapOf(
        DetailMessage.MARKED_PAID to stringResource(R.string.detail_msg_paid),
        DetailMessage.MARKED_UNPAID to stringResource(R.string.detail_msg_unpaid),
        DetailMessage.PAID_DATE_CHANGED to stringResource(R.string.detail_msg_paid_date),
        DetailMessage.VOIDED to stringResource(R.string.detail_msg_voided),
        DetailMessage.FAILED to stringResource(R.string.save_failed),
        DetailMessage.DRAFT_INCOMPLETE to stringResource(R.string.send_draft_incomplete),
    )
    val undoLabel = stringResource(R.string.action_undo)
    LaunchedEffect(state.message) {
        val message = state.message ?: return@LaunchedEffect
        val result = snackbar.showSnackbar(
            message = messageTexts.getValue(message.kind),
            actionLabel = if (message.canUndo) undoLabel else null,
            duration = SnackbarDuration.Short,
        )
        if (result == SnackbarResult.ActionPerformed) vm.onUndo() else vm.onMessageShown()
    }
    val sendMessage = sendState.message
    val sendText = sendMessage?.let { sendMessageText(it) }
    LaunchedEffect(sendMessage) {
        if (sendMessage == null || sendText == null) return@LaunchedEffect
        val result = snackbar.showSnackbar(
            message = sendText,
            actionLabel = if (sendMessage is SendMessage.Sent) undoLabel else null,
            duration = SnackbarDuration.Short,
        )
        if (result == SnackbarResult.ActionPerformed) vm.send.onUndoSent() else vm.send.onMessageShown()
    }

    var menuOpen by rememberSaveable { mutableStateOf(false) }
    val invoice = state.invoice
    val number = invoice?.invoice?.number
    Scaffold(
        contentWindowInsets = WindowInsets(0), // the app's outer Scaffold handles the status bar
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0),
                title = { Text(number ?: stringResource(R.string.status_draft)) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = stringResource(R.string.action_back)) }
                },
                actions = {
                    if (state.invoice != null && state.status != DisplayStatus.DRAFT) {
                        Box {
                            IconButton(onClick = { menuOpen = true }) {
                                Icon(painterResource(R.drawable.ic_more_vert), contentDescription = stringResource(R.string.client_more))
                            }
                            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                DropdownMenuItem(text = { Text(stringResource(R.string.send_save_pdf)) }, onClick = { menuOpen = false; vm.onSavePdf() })
                                if (state.status == DisplayStatus.UNPAID || state.status == DisplayStatus.OVERDUE) {
                                    DropdownMenuItem(text = { Text(stringResource(R.string.detail_void)) }, onClick = { menuOpen = false; vm.onVoidRequest() })
                                }
                            }
                        }
                    }
                },
            )
        },
    ) { padding ->
        if (state.loading) return@Scaffold
        if (invoice == null) {
            // Deleted elsewhere, or replaced by a restore: say so rather than show a blank page.
            EmptyStateScreen(R.string.missing_invoice_title, R.string.missing_body, modifier = Modifier.padding(padding))
            return@Scaffold
        }
        if (sendState.busy) LinearProgressIndicator(Modifier.fillMaxWidth().padding(padding))
        Column(
            verticalArrangement = Arrangement.spacedBy(Spacing.item),
            modifier = Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = Spacing.screen, vertical = Spacing.m),
        ) {
            SummaryCard(state, onOpenClient)
            Actions(
                state, vm, busy = sendState.busy,
                onEdit = { onEdit(invoiceId) }, onDuplicate = { onDuplicate(invoiceId) }, onPreview = { onPreview(invoiceId) },
                onEditClient = onEditClient,
            )
            ItemsBlock(invoice)
            if (invoice.invoice.notes.isNotBlank()) {
                Text(stringResource(R.string.editor_notes), style = MaterialTheme.typography.titleMedium)
                Text(invoice.invoice.notes, style = MaterialTheme.typography.bodyLarge)
            }
            HistoryBlock(state.history)
        }
    }

    when (val prompt = state.prompt) {
        DetailPrompt.ConfirmVoid -> AlertDialog(
            onDismissRequest = vm::onPromptDismiss,
            title = { Text(stringResource(R.string.detail_void_title, number.orEmpty())) },
            text = { Text(stringResource(R.string.detail_void_body)) },
            confirmButton = { TextButton(onClick = vm::onVoidConfirm) { Text(stringResource(R.string.detail_void)) } },
            dismissButton = { TextButton(onClick = vm::onPromptDismiss) { Text(stringResource(R.string.action_cancel)) } },
        )
        DetailPrompt.ConfirmDelete -> AlertDialog(
            onDismissRequest = vm::onPromptDismiss,
            title = { Text(stringResource(R.string.editor_delete_title)) },
            confirmButton = { TextButton(onClick = vm::onDeleteConfirm) { Text(stringResource(R.string.editor_delete_draft)) } },
            dismissButton = { TextButton(onClick = vm::onPromptDismiss) { Text(stringResource(R.string.action_cancel)) } },
        )
        is DetailPrompt.PickPaidDate -> {
            val pickerState = rememberDatePickerState(initialSelectedDateMillis = prompt.current * DAY_MS)
            DatePickerDialog(
                onDismissRequest = vm::onPromptDismiss,
                confirmButton = {
                    TextButton(onClick = { pickerState.selectedDateMillis?.let { vm.onPaidDatePicked(it / DAY_MS) } ?: vm.onPromptDismiss() }) {
                        Text(stringResource(R.string.action_ok))
                    }
                },
                dismissButton = { TextButton(onClick = vm::onPromptDismiss) { Text(stringResource(R.string.action_cancel)) } },
            ) { DatePicker(state = pickerState) }
        }
        is DetailPrompt.ConfirmIssueAndSend -> AlertDialog(
            onDismissRequest = vm::onPromptDismiss,
            title = { Text(stringResource(R.string.send_issue_title)) },
            text = {
                Text(
                    prompt.nextNumber?.let { stringResource(R.string.send_issue_body, it) }
                        ?: stringResource(R.string.send_issue_body_no_number),
                )
            },
            confirmButton = { TextButton(onClick = vm::onIssueAndSendConfirm) { Text(stringResource(R.string.send_issue_and_send)) } },
            dismissButton = { TextButton(onClick = vm::onPromptDismiss) { Text(stringResource(R.string.action_cancel)) } },
        )
        null -> Unit
    }
}

@Composable
private fun SummaryCard(state: InvoiceDetailUiState, onOpenClient: (Long) -> Unit) {
    val e = state.invoice!!.invoice
    val currency = CurrencyCode.fromCode(e.currency) ?: CurrencyCode.USD
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(Spacing.l), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    state.clientName.ifEmpty { stringResource(R.string.invoices_no_client) },
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f).then(if (state.client != null) Modifier.clickable { onOpenClient(state.client.id) } else Modifier),
                )
                StatusChip(state.status)
            }
            Text(MoneyFormatter.format(e.totalMinor, currency), style = MaterialTheme.typography.headlineSmall.tabular)
            val dates = buildList {
                add(stringResource(R.string.detail_issued_on, DateFormatter.full(e.issueDate)))
                add(stringResource(R.string.detail_due_on, DateFormatter.full(e.dueDate)))
            }
            dates.forEach { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}

/** Actions in UX_SPEC 4–5 order: send first, then the status action, then Preview / Edit / Duplicate. */
@Composable
private fun Actions(
    state: InvoiceDetailUiState,
    vm: InvoiceDetailViewModel,
    busy: Boolean,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onPreview: () -> Unit,
    onEditClient: (Long) -> Unit,
) {
    val full = Modifier.fillMaxWidth().heightIn(min = Spacing.buttonHeight)
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
        when (state.status) {
            DisplayStatus.DRAFT -> {
                Button(onClick = vm::onIssueAndSendRequest, enabled = !busy, modifier = full) { Text(stringResource(R.string.send_issue_and_send)) }
                OutlinedButton(onClick = onEdit, modifier = full) { Text(stringResource(R.string.detail_continue_editing)) }
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = onPreview, modifier = Modifier.weight(1f).heightIn(min = Spacing.buttonHeight)) { Text(stringResource(R.string.detail_preview)) }
                    OutlinedButton(onClick = vm::onDeleteRequest, modifier = Modifier.weight(1f).heightIn(min = Spacing.buttonHeight)) { Text(stringResource(R.string.editor_delete_draft)) }
                }
            }
            DisplayStatus.UNPAID, DisplayStatus.OVERDUE -> {
                SendBlock(state, vm, busy, onEditClient)
                FilledTonalButton(onClick = vm::onMarkPaid, modifier = full) { Text(stringResource(R.string.detail_mark_paid)) }
                SecondaryRow(onPreview, onEdit, onDuplicate)
            }
            DisplayStatus.PAID -> {
                SendBlock(state, vm, busy, onEditClient)
                FilledTonalButton(onClick = vm::onMarkUnpaid, modifier = full) { Text(stringResource(R.string.detail_mark_unpaid)) }
                OutlinedButton(onClick = vm::onChangePaidDateRequest, modifier = full) { Text(stringResource(R.string.detail_change_paid_date)) }
                SecondaryRow(onPreview, onEdit = null, onDuplicate = onDuplicate)
            }
            DisplayStatus.VOID -> {
                Text(stringResource(R.string.detail_void_note), color = MaterialTheme.colorScheme.onSurfaceVariant)
                SecondaryRow(onPreview, onEdit = null, onDuplicate = onDuplicate)
            }
        }
    }
}

/**
 * UX_SPEC 4: the primary send button (its label depends on status and whether it was sent),
 * where it goes, "Send another way", and when it was last sent.
 */
@Composable
private fun SendBlock(state: InvoiceDetailUiState, vm: InvoiceDetailViewModel, busy: Boolean, onEditClient: (Long) -> Unit) {
    val e = state.invoice!!.invoice
    val email = state.recipientEmail
    val label = when {
        email == null -> R.string.send_generic
        state.status == DisplayStatus.PAID -> R.string.send_receipt
        state.status == DisplayStatus.OVERDUE -> R.string.send_reminder
        e.lastSentAt != null -> R.string.send_email_again
        else -> R.string.send_email_client
    }
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    Button(onClick = vm::onSend, enabled = !busy, modifier = Modifier.fillMaxWidth().heightIn(min = Spacing.buttonHeight)) {
        Text(stringResource(label))
    }
    if (email != null) {
        Text(stringResource(R.string.send_to, email), style = MaterialTheme.typography.bodyMedium, color = muted)
        TextButton(onClick = vm::onSendAnotherWay, enabled = !busy, modifier = Modifier.heightIn(min = Spacing.touchTarget)) {
            Text(stringResource(R.string.send_another_way))
        }
    } else if (state.client != null) {
        val clientId = state.client.id
        TextButton(onClick = { onEditClient(clientId) }, modifier = Modifier.heightIn(min = Spacing.touchTarget)) {
            Text(stringResource(R.string.send_add_email))
        }
    }
    e.lastSentAt?.let { at ->
        val day = DateFormatter.inList(at / DAY_MS, state.today)
        val text = e.lastSentVia?.let { stringResource(R.string.send_last_sent_via, day, it) } ?: stringResource(R.string.send_last_sent, day)
        Text(text, style = MaterialTheme.typography.bodyMedium, color = muted)
    }
}

@Composable
private fun SecondaryRow(onPreview: () -> Unit, onEdit: (() -> Unit)?, onDuplicate: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s), modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(onClick = onPreview, modifier = Modifier.weight(1f).heightIn(min = Spacing.buttonHeight)) { Text(stringResource(R.string.detail_preview)) }
        if (onEdit != null) {
            OutlinedButton(onClick = onEdit, modifier = Modifier.weight(1f).heightIn(min = Spacing.buttonHeight)) { Text(stringResource(R.string.client_edit_short)) }
        }
        OutlinedButton(onClick = onDuplicate, modifier = Modifier.weight(1f).heightIn(min = Spacing.buttonHeight)) { Text(stringResource(R.string.detail_duplicate)) }
    }
}

@Composable
private fun ItemsBlock(invoice: InvoiceWithLines) {
    val e = invoice.invoice
    val currency = CurrencyCode.fromCode(e.currency) ?: CurrencyCode.USD
    Text(stringResource(R.string.editor_items), style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
    invoice.lines.forEach { line ->
        Row(Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f)) {
                Text(line.description.ifBlank { "—" }, style = MaterialTheme.typography.bodyLarge)
                val qty = BigDecimal.valueOf(line.quantityMilli, 3).stripTrailingZeros().toPlainString()
                Text("$qty × ${MoneyFormatter.format(line.unitPriceMinor, currency)}", style = MaterialTheme.typography.bodyMedium.tabular, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(MoneyFormatter.format(line.amountMinor, currency), style = MaterialTheme.typography.bodyLarge.tabular)
        }
    }
    HorizontalDivider()
    Amount(stringResource(R.string.editor_subtotal), MoneyFormatter.format(e.subtotalMinor, currency))
    if (e.discountMinor > 0) Amount(stringResource(R.string.editor_discount), "−" + MoneyFormatter.format(e.discountMinor, currency))
    if (e.taxMinor > 0) Amount(stringResource(R.string.editor_tax), MoneyFormatter.format(e.taxMinor, currency))
    Amount(stringResource(R.string.editor_total), MoneyFormatter.format(e.totalMinor, currency), bold = true)
}

@Composable
private fun Amount(label: String, value: String, bold: Boolean = false) {
    val style = if (bold) MaterialTheme.typography.titleMedium.tabular else MaterialTheme.typography.bodyMedium.tabular
    Row(Modifier.fillMaxWidth()) {
        Text(label, style = style, modifier = Modifier.weight(1f))
        Text(value, style = style)
    }
}

/** UX_SPEC 5 history block (D-011: issued, last sent, paid; no full log). */
@Composable
private fun HistoryBlock(history: List<HistoryEntry>) {
    Text(stringResource(R.string.detail_history), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = Spacing.s).semantics { heading() })
    history.forEach { entry ->
        val (label, value) = when (entry) {
            is HistoryEntry.Created -> stringResource(R.string.detail_history_created) to DateFormatter.full(entry.day)
            is HistoryEntry.Issued -> stringResource(R.string.detail_history_issued) to DateFormatter.full(entry.day)
            is HistoryEntry.LastSent -> stringResource(R.string.detail_history_sent) to
                (DateFormatter.full(entry.day) + (entry.via?.let { " · $it" } ?: ""))
            is HistoryEntry.Paid -> stringResource(R.string.detail_history_paid) to DateFormatter.full(entry.day)
        }
        Amount(label, value)
    }
}
