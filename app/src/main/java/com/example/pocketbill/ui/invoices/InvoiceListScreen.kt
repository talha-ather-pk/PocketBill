package com.example.pocketbill.ui.invoices

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pocketbill.R
import com.example.pocketbill.domain.format.DateFormatter
import com.example.pocketbill.ui.appContainer
import com.example.pocketbill.ui.clients.formatOwed
import com.example.pocketbill.ui.clients.Owed
import com.example.pocketbill.ui.components.StatusChip
import com.example.pocketbill.ui.navigation.InvoiceFilter
import com.example.pocketbill.ui.screens.EmptyStateScreen
import com.example.pocketbill.ui.theme.Spacing
import com.example.pocketbill.ui.theme.tabular
import java.time.Month
import java.time.format.TextStyle
import java.util.Locale

/** Invoices tab (UX_SPEC 5). */
@Composable
fun InvoiceListScreen(initialFilter: InvoiceFilter, onOpenInvoice: (Long) -> Unit, onNewInvoice: () -> Unit) {
    val c = appContainer()
    val vm: InvoiceListViewModel = viewModel { InvoiceListViewModel(c.invoices, c.clients, c.clock, initialFilter) }
    val state by vm.state.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(start = Spacing.screen, end = Spacing.xs, top = Spacing.l),
        ) {
            Text(stringResource(R.string.nav_invoices), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f).semantics { heading() })
            IconButton(onClick = onNewInvoice) {
                Icon(painterResource(R.drawable.ic_add), contentDescription = stringResource(R.string.invoices_new))
            }
        }
        if (state.loading) return@Column
        if (state.noInvoices) {
            EmptyStateScreen(R.string.invoices_empty_title, R.string.invoices_empty_body, modifier = Modifier.weight(1f))
            Button(onClick = onNewInvoice, modifier = Modifier.fillMaxWidth().padding(Spacing.screen).heightIn(min = Spacing.buttonHeight)) {
                Text(stringResource(R.string.home_create_invoice))
            }
            return@Column
        }

        OutlinedTextField(
            value = state.query,
            onValueChange = vm::onQueryChange,
            label = { Text(stringResource(R.string.invoices_search)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.screen, vertical = Spacing.s),
        )
        val result = state.result
        if (result.searching) {
            Text(stringResource(R.string.invoices_searching_all), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = Spacing.screen))
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s), modifier = Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = Spacing.screen)) {
                InvoiceFilter.entries.forEach { f ->
                    val count = result.counts[f] ?: 0
                    FilterChip(
                        selected = state.filter == f,
                        onClick = { vm.onFilterChange(f) },
                        label = { Text(if (f == InvoiceFilter.ALL) filterLabel(f) else "${filterLabel(f)} $count") },
                    )
                }
            }
            if (result.totals.isNotEmpty()) {
                Text(
                    stringResource(R.string.invoices_total_line, filterLabel(state.filter), result.totals.joinToString(" · ") { formatOwed(it) }),
                    style = MaterialTheme.typography.bodyMedium.tabular,
                    modifier = Modifier.padding(horizontal = Spacing.screen, vertical = Spacing.s),
                )
            }
        }

        LazyColumn(Modifier.weight(1f)) {
            if (result.groups.isEmpty()) {
                item {
                    Text(
                        if (result.searching) stringResource(R.string.invoices_no_match, state.query) else emptyFilterText(state.filter),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(Spacing.screen),
                    )
                }
            }
            result.groups.forEach { group ->
                group.yearMonth?.let { (year, month) ->
                    item(key = "h$year-$month") {
                        Text(
                            "${Month.of(month).getDisplayName(TextStyle.FULL, Locale.ENGLISH)} $year",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = Spacing.screen, vertical = Spacing.s).semantics { heading() },
                        )
                    }
                }
                items(group.rows, key = { it.id }) { row -> InvoiceRowItem(row, state.today, onClick = { onOpenInvoice(row.id) }) }
            }
        }
    }
}

/** UX_SPEC 5 row: client and amount on the first line; number, date and status on the second. */
@Composable
fun InvoiceRowItem(row: InvoiceRow, today: Long, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val (kind, day) = row.datePart()
    val date = DateFormatter.inList(day, today)
    val dateText = when (kind) {
        DateKind.DUE -> stringResource(R.string.invoice_row_due, date)
        DateKind.OVERDUE -> stringResource(R.string.invoice_row_overdue, date)
        DateKind.PAID -> stringResource(R.string.invoice_row_paid, date)
        DateKind.EDITED -> stringResource(R.string.invoice_row_edited, date)
        DateKind.ISSUED -> stringResource(R.string.invoice_row_issued, date)
    }
    val sent = if (row.lastSentAt == null && kind == DateKind.DUE) stringResource(R.string.invoice_row_not_sent) else null
    val numberText = if (row.isDraft) stringResource(R.string.status_draft) else row.number.orEmpty()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.m),
        modifier = modifier.fillMaxWidth().heightIn(min = Spacing.rowHeight).clickable(onClick = onClick).padding(horizontal = Spacing.screen, vertical = Spacing.s),
    ) {
        Column(Modifier.weight(1f)) {
            Text(row.clientName.ifEmpty { stringResource(R.string.invoices_no_client) }, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(listOfNotNull(numberText, dateText, sent).joinToString(" · "), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Text(formatOwed(Owed(row.currency, row.totalMinor)), style = MaterialTheme.typography.bodyLarge.tabular)
            StatusChip(row.status)
        }
    }
}

@Composable
private fun filterLabel(f: InvoiceFilter): String = stringResource(
    when (f) {
        InvoiceFilter.ALL -> R.string.filter_all
        InvoiceFilter.DRAFTS -> R.string.filter_drafts
        InvoiceFilter.UNPAID -> R.string.status_unpaid
        InvoiceFilter.OVERDUE -> R.string.status_overdue
        InvoiceFilter.PAID -> R.string.status_paid
        InvoiceFilter.VOID -> R.string.status_void
    },
)

@Composable
private fun emptyFilterText(f: InvoiceFilter): String = stringResource(
    when (f) {
        InvoiceFilter.ALL -> R.string.invoices_empty_body
        InvoiceFilter.DRAFTS -> R.string.invoices_empty_drafts
        InvoiceFilter.UNPAID -> R.string.invoices_empty_unpaid
        InvoiceFilter.OVERDUE -> R.string.invoices_empty_overdue
        InvoiceFilter.PAID -> R.string.invoices_empty_paid
        InvoiceFilter.VOID -> R.string.invoices_empty_void
    },
)
