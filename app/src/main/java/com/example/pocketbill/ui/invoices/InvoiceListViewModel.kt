package com.example.pocketbill.ui.invoices

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pocketbill.data.repository.ClientStore
import com.example.pocketbill.data.repository.InvoiceBrowser
import com.example.pocketbill.domain.time.Clock
import com.example.pocketbill.ui.navigation.InvoiceFilter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class InvoiceListUiState(
    val loading: Boolean = true,
    val filter: InvoiceFilter = InvoiceFilter.ALL,
    val query: String = "",
    val today: Long = 0,
    val result: InvoiceListResult = InvoiceListResult(emptyList(), emptyMap(), emptyList(), searching = false),
    /** No invoices at all, so the first-use empty state shows. */
    val noInvoices: Boolean = false,
)

/** Invoices tab (UX_SPEC 5): filters with counts, search, totals per currency. */
class InvoiceListViewModel(
    browser: InvoiceBrowser,
    clients: ClientStore,
    private val clock: Clock,
    initialFilter: InvoiceFilter,
) : ViewModel() {
    private val today = clock.todayEpochDay()
    private val filter = MutableStateFlow(initialFilter)
    private val query = MutableStateFlow("")
    private val _state = MutableStateFlow(InvoiceListUiState(filter = initialFilter, today = today))
    val state: StateFlow<InvoiceListUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            // Archived clients' invoices still show their names.
            val allClients = combine(clients.observeActive(), clients.observeArchived()) { a, b -> a + b }
            combine(browser.observeAll(), allClients, browser.observeLineDescriptions(), filter, query) { invoices, cs, lines, f, q ->
                InvoiceListUiState(
                    loading = false, filter = f, query = q, today = today,
                    result = buildInvoiceList(invoices, cs, lines, f, q, today, clock::dayOf),
                    noInvoices = invoices.isEmpty(),
                )
            }.collect { _state.value = it }
        }
    }

    fun onFilterChange(value: InvoiceFilter) {
        filter.value = value
    }

    fun onQueryChange(value: String) {
        query.value = value
        _state.update { it.copy(query = value) }
    }
}
