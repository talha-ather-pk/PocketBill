package com.example.pocketbill.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pocketbill.app.HomeCardPrefs
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
    /** "Back up your invoices" (UX_SPEC 2 cards, MVP_SPEC 7). */
    val showBackupCard: Boolean = false,
)

/** Home (UX_SPEC 2): who owes me, what's overdue, what's unfinished; one-tap Remind and Email. */
class HomeViewModel(
    private val browser: InvoiceBrowser,
    clients: ClientStore,
    businessName: Flow<String?>,
    private val sending: InvoiceSending,
    private val preparer: SendPreparer,
    private val clock: Clock,
    private val backupDue: suspend () -> Boolean,
    private val prefs: HomeCardPrefs,
) : ViewModel() {
    private val backupCardHidden = MutableStateFlow(prefs.backupCardHiddenUntil > clock.nowMillis())
    private val paymentChangedAt = MutableStateFlow(prefs.paymentDetailsChangedAt)

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
            // Recomputed when invoices change or the profile changes (it holds the last backup date).
            combine(browser.observeAll(), allClients, businessName, backupCardHidden, paymentChangedAt) { invoices, cs, name, hidden, changedAt ->
                HomeUiState(
                    loading = false, businessName = name.orEmpty(), today = today, summary = buildHome(invoices, cs, today, changedAt, clock::dayOf),
                    showBackupCard = !hidden && invoices.isNotEmpty() && backupDue(),
                )
            }.collect { _state.value = it }
        }
    }

    /** ✕ on the backup card hides it for 7 days (UX_SPEC 2). */
    fun onDismissBackupCard() {
        prefs.backupCardHiddenUntil = clock.nowMillis() + BACKUP_CARD_HIDE_MS
        backupCardHidden.value = true
    }

    /**
     * Home stays alive while other tabs are open, so preferences changed there (Business profile
     * records when payment details changed) are read again each time Home is shown.
     */
    fun onShown() {
        paymentChangedAt.value = prefs.paymentDetailsChangedAt
        backupCardHidden.value = prefs.backupCardHiddenUntil > clock.nowMillis()
    }

    /** ✕ on the payment-details card: the invoices stay as they are, the card goes away. */
    fun onDismissPaymentCard() {
        prefs.paymentDetailsChangedAt = 0
        paymentChangedAt.value = 0
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

    companion object {
        const val BACKUP_CARD_HIDE_MS = 7 * 86_400_000L
    }
}
