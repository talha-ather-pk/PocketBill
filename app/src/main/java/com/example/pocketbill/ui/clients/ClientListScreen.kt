package com.example.pocketbill.ui.clients

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.pocketbill.domain.money.CurrencyCode
import com.example.pocketbill.domain.money.MoneyFormatter
import com.example.pocketbill.ui.appContainer
import com.example.pocketbill.ui.screens.EmptyStateScreen
import com.example.pocketbill.ui.theme.Spacing
import com.example.pocketbill.ui.theme.tabular

/** Clients tab (UX_SPEC 6). */
@Composable
fun ClientListScreen(onOpenClient: (Long) -> Unit, onAddClient: () -> Unit) {
    val container = appContainer()
    val vm: ClientListViewModel = viewModel { ClientListViewModel(container.clients) }
    val state by vm.state.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(start = Spacing.screen, end = Spacing.xs, top = Spacing.l),
        ) {
            Text(
                stringResource(R.string.nav_clients),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.weight(1f).semantics { heading() },
            )
            IconButton(onClick = onAddClient) {
                Icon(painterResource(R.drawable.ic_add), contentDescription = stringResource(R.string.clients_add))
            }
        }
        if (state.loading) return@Column
        if (state.noClients) {
            EmptyStateScreen(R.string.clients_empty_title, R.string.clients_empty_body, modifier = Modifier.weight(1f))
            Button(
                onClick = onAddClient,
                modifier = Modifier.fillMaxWidth().padding(Spacing.screen).heightIn(min = Spacing.buttonHeight),
            ) { Text(stringResource(R.string.clients_add)) }
            return@Column
        }

        OutlinedTextField(
            value = state.query,
            onValueChange = vm::onQueryChange,
            label = { Text(stringResource(R.string.clients_search)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.screen, vertical = Spacing.s),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s), modifier = Modifier.padding(horizontal = Spacing.screen)) {
            FilterChip(
                selected = state.sort == ClientSort.NAME,
                onClick = { vm.onSortChange(ClientSort.NAME) },
                label = { Text(stringResource(R.string.clients_sort_name)) },
            )
            FilterChip(
                selected = state.sort == ClientSort.MOST_OWED,
                onClick = { vm.onSortChange(ClientSort.MOST_OWED) },
                label = { Text(stringResource(R.string.clients_sort_owed)) },
            )
        }

        LazyColumn(Modifier.weight(1f)) {
            if (state.rows.isEmpty() && state.query.isNotBlank()) {
                item {
                    Text(
                        stringResource(R.string.clients_no_match, state.query),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(Spacing.screen),
                    )
                }
            }
            items(state.rows, key = { it.id }) { row -> ClientRowItem(row, onClick = { onOpenClient(row.id) }) }
            if (state.archivedRows.isNotEmpty()) {
                item {
                    TextButton(onClick = vm::onToggleArchived, modifier = Modifier.padding(horizontal = Spacing.s)) {
                        Text(stringResource(R.string.clients_archived_toggle, state.archivedRows.size))
                    }
                }
                if (state.showArchived) {
                    items(state.archivedRows, key = { "a${it.id}" }) { row -> ClientRowItem(row, onClick = { onOpenClient(row.id) }) }
                }
            }
        }
    }
}

@Composable
private fun ClientRowItem(row: ClientRow, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.m),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = Spacing.rowHeight)
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.screen, vertical = Spacing.s),
    ) {
        Column(Modifier.weight(1f)) {
            Text(row.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(clientSubtitle(row), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (row.owed.isNotEmpty()) {
            // Amounts never shorten (UX_SPEC 7.1); one line per currency, never added together.
            Column(horizontalAlignment = Alignment.End) {
                row.owed.forEach { Text(formatOwed(it), style = MaterialTheme.typography.bodyLarge.tabular) }
                Text(stringResource(R.string.client_owed_label), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/** "Germany · 2 unpaid · 1 overdue", or "Nothing owed". */
@Composable
internal fun clientSubtitle(row: ClientRow): String {
    val parts = mutableListOf<String>()
    row.country?.let(parts::add)
    if (row.archived) parts += stringResource(R.string.client_archived_label)
    if (row.unpaidCount > 0) parts += pluralStringResource(R.plurals.client_unpaid, row.unpaidCount, row.unpaidCount)
    if (row.overdueCount > 0) parts += pluralStringResource(R.plurals.client_overdue, row.overdueCount, row.overdueCount)
    if (row.unpaidCount == 0) parts += stringResource(R.string.client_nothing_owed)
    return parts.joinToString(" · ")
}

/** Formats an amount; an unknown currency (never expected, validated on save) falls back to code + number. */
internal fun formatOwed(owed: Owed): String =
    CurrencyCode.fromCode(owed.currency)?.let { MoneyFormatter.format(owed.totalMinor, it) } ?: "${owed.currency} ${owed.totalMinor}"
