package com.example.pocketbill.ui.clients

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pocketbill.R
import com.example.pocketbill.data.db.ClientEntity
import com.example.pocketbill.data.db.InvoiceEntity
import com.example.pocketbill.domain.format.DateFormatter
import com.example.pocketbill.domain.invoice.InvoiceStatus
import com.example.pocketbill.ui.appContainer
import com.example.pocketbill.ui.components.DisplayStatus
import com.example.pocketbill.ui.components.StatusChip
import com.example.pocketbill.ui.components.currencyLabel
import com.example.pocketbill.ui.components.displayStatus
import com.example.pocketbill.ui.screens.EmptyStateScreen
import com.example.pocketbill.ui.theme.Spacing
import com.example.pocketbill.ui.theme.tabular

/** Client detail (UX_SPEC 6). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientDetailScreen(
    clientId: Long,
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    onNewInvoice: (Long) -> Unit,
    onOpenInvoice: (Long) -> Unit,
) {
    val container = appContainer()
    val vm: ClientDetailViewModel = viewModel { ClientDetailViewModel(container.clients, clientId, container.clock.todayEpochDay()) }
    val state by vm.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.deleted) { if (state.deleted) onBack() }
    var menuOpen by rememberSaveable { mutableStateOf(false) }

    val client = state.client
    // The app's outer Scaffold already handles the status bar (see BusinessProfileScreen).
    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0),
                title = { Text(client?.name.orEmpty(), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = stringResource(R.string.action_back))
                    }
                },
                actions = {
                    if (client != null) {
                        Box {
                            IconButton(onClick = { menuOpen = true }) {
                                Icon(painterResource(R.drawable.ic_more_vert), contentDescription = stringResource(R.string.client_more))
                            }
                            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(if (client.archived) R.string.client_unarchive else R.string.client_archive)) },
                                    onClick = { menuOpen = false; vm.onArchive(!client.archived) },
                                )
                                if (state.canDelete) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.client_delete)) },
                                        onClick = { menuOpen = false; vm.onDeleteRequest() },
                                    )
                                }
                            }
                        }
                    }
                },
            )
        },
    ) { padding ->
        if (state.loading) return@Scaffold
        if (client == null) {
            EmptyStateScreen(R.string.missing_client_title, R.string.missing_body, modifier = Modifier.padding(padding))
            return@Scaffold
        }
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(Spacing.item),
            modifier = Modifier.padding(padding).fillMaxSize().padding(horizontal = Spacing.screen),
        ) {
            if (client.archived) {
                item { Text(stringResource(R.string.client_archived_note), color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            item { ClientDetailsBlock(client) }
            item { OwedCard(state) }
            item {
                Button(
                    onClick = { onNewInvoice(client.id) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = Spacing.buttonHeight),
                ) { Text(stringResource(R.string.client_new_invoice)) }
            }
            item {
                OutlinedButton(
                    onClick = { onEdit(client.id) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = Spacing.buttonHeight),
                ) { Text(stringResource(R.string.client_edit)) }
            }
            item {
                Text(
                    stringResource(R.string.client_invoices_title),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = Spacing.s).semantics { heading() },
                )
            }
            if (state.invoices.isEmpty()) {
                item { Text(stringResource(R.string.client_no_invoices), color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            items(state.invoices, key = { it.id }) { invoice ->
                InvoiceRowItem(invoice, state.today, onClick = { onOpenInvoice(invoice.id) })
            }
        }
    }

    if (state.confirmDelete && client != null) {
        AlertDialog(
            onDismissRequest = vm::onDeleteDismiss,
            title = { Text(stringResource(R.string.client_delete_title, client.name)) },
            text = { Text(stringResource(R.string.client_delete_body)) },
            confirmButton = { TextButton(onClick = vm::onDeleteConfirm) { Text(stringResource(R.string.client_delete)) } },
            dismissButton = { TextButton(onClick = vm::onDeleteDismiss) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

@Composable
private fun ClientDetailsBlock(client: ClientEntity) {
    // Selectable so the email can be copied (UX_SPEC 6).
    SelectionContainer {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            listOfNotNull(client.email, client.country, client.address, client.taxId).forEach {
                Text(it, style = MaterialTheme.typography.bodyLarge)
            }
            preferencesLine(client)?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/** "New invoices: EUR · Euro · 14 days" (D-015), or null with no preferences. */
@Composable
private fun preferencesLine(client: ClientEntity): String? {
    val parts = listOfNotNull(
        client.preferredCurrency?.let(::currencyLabel),
        client.preferredTermsDays?.let {
            if (it == 0) stringResource(R.string.terms_on_receipt) else pluralStringResource(R.plurals.terms_days, it, it)
        },
    )
    return if (parts.isEmpty()) null else stringResource(R.string.client_prefs_line, parts.joinToString(" · "))
}

@Composable
private fun OwedCard(state: ClientDetailUiState) {
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(Spacing.l), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Text(stringResource(R.string.client_owed_label), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (state.owed.isEmpty()) {
                Text(stringResource(R.string.client_nothing_owed), style = MaterialTheme.typography.titleLarge)
            } else {
                state.owed.forEach { Text(formatOwed(it), style = MaterialTheme.typography.titleLarge.tabular) }
                val parts = listOfNotNull(
                    pluralStringResource(R.plurals.client_unpaid, state.unpaidCount, state.unpaidCount),
                    state.overdueCount.takeIf { it > 0 }?.let { pluralStringResource(R.plurals.client_overdue, it, it) },
                )
                Text(parts.joinToString(" · "), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/** Invoice row (UX_SPEC 5 design): number and date on the left, amount and status on the right. */
@Composable
private fun InvoiceRowItem(invoice: InvoiceEntity, today: Long, onClick: () -> Unit) {
    val status = displayStatus(invoice.status, invoice.dueDate, today)
    val dateLine = when (status) {
        DisplayStatus.PAID -> invoice.paidDate?.let { stringResource(R.string.invoice_row_paid, DateFormatter.inList(it, today)) }
        DisplayStatus.OVERDUE -> stringResource(R.string.invoice_row_overdue, DateFormatter.inList(invoice.dueDate, today))
        DisplayStatus.DRAFT -> null
        else -> stringResource(R.string.invoice_row_due, DateFormatter.inList(invoice.dueDate, today))
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.m),
        modifier = Modifier.fillMaxWidth().heightIn(min = Spacing.rowHeight).clickable(onClick = onClick),
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                if (invoice.status == InvoiceStatus.DRAFT) stringResource(R.string.status_draft) else invoice.number.orEmpty(),
                style = MaterialTheme.typography.titleMedium,
            )
            dateLine?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Text(formatOwed(Owed(invoice.currency, invoice.totalMinor)), style = MaterialTheme.typography.bodyLarge.tabular)
            StatusChip(status)
        }
    }
}
