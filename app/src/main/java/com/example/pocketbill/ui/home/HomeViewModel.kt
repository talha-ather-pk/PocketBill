package com.example.pocketbill.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pocketbill.data.repository.ClientStore
import com.example.pocketbill.data.repository.InvoiceBrowser
import com.example.pocketbill.data.repository.InvoiceSending
import com.example.pocketbill.domain.time.Clock
import com.example.pocketbill.share.SendPreparer
import com.example.pocketbill.ui.send.InvoiceSendFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class HomeUiState(
    val loading: Boolean = true,
    val businessName: String = "",
    val today: Long = 0,
    val summary: HomeSummary = HomeSummary(),
)

/** Home (UX_SPEC 2): who owes me, what's overdue, what's unfinished; one-tap Remind and Email. */
class HomeViewModel(
    private val browser: InvoiceBrowser,
    clients: ClientStore,
    businessName: Flow<String?>,
    private val sending: InvoiceSending,
    private val preparer: SendPreparer,
    private val clock: Clock,
) : ViewModel() {
    private val today = clock.todayEpochDay()
    private val _state = MutableStateFlow(HomeUiState(today = today))
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    /** The send started from a Needs attention row, if any. */
    private val _send = MutableStateFlow<InvoiceSendFlow?>(null)
    val send: StateFlow<InvoiceSendFlow?> = _send.asStateFlow()
    private var sendJob: Job? = null

    init {
        viewModelScope.launch {
            val allClients = combine(clients.observeActive(), clients.observeArchived()) { a, b -> a + b }
            combine(browser.observeAll(), allClients, businessName) { invoices, cs, name ->
                HomeUiState(loading = false, businessName = name.orEmpty(), today = today, summary = buildHome(invoices, cs, today))
            }.collect { _state.value = it }
        }
    }

    /** Remind (overdue) or Email (issued, not sent): the email app with the right template, or Send… without an email. */
    fun onSendFromRow(item: AttentionItem) {
        sendJob?.cancel()
        val flow = InvoiceSendFlow(viewModelScope, sending, preparer, clock, item.row.id)
        _send.value = flow
        sendJob = viewModelScope.launch {
            var started = false
            browser.observe(item.row.id).collect { invoice ->
                flow.observe(invoice?.invoice)
                // Start only once the flow knows the current "sent" values, so Undo restores the right ones.
                if (!started) {
                    started = true
                    flow.send(toEmail = item.hasEmail)
                }
            }
        }
    }
}
