package com.example.pocketbill.ui.invoices

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pocketbill.data.db.ClientEntity
import com.example.pocketbill.data.db.InvoiceEntity
import com.example.pocketbill.data.db.InvoiceWithLines
import com.example.pocketbill.data.repository.ClientStore
import com.example.pocketbill.data.repository.InvoiceBrowser
import com.example.pocketbill.data.repository.InvoiceSending
import com.example.pocketbill.domain.Outcome
import com.example.pocketbill.domain.invoice.InvoiceStatus
import com.example.pocketbill.domain.invoice.SnapshotJson
import com.example.pocketbill.domain.time.Clock
import com.example.pocketbill.share.SendPreparer
import com.example.pocketbill.ui.send.InvoiceSendFlow
import com.example.pocketbill.ui.components.DisplayStatus
import com.example.pocketbill.ui.components.displayStatus
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** One line of the invoice's short history (UX_SPEC 5; D-011: existing columns only). */
sealed interface HistoryEntry {
    data class Created(val day: Long) : HistoryEntry
    data class Issued(val day: Long) : HistoryEntry
    data class LastSent(val day: Long, val via: String?) : HistoryEntry
    data class Paid(val day: Long) : HistoryEntry
}

/** A message with an optional Undo (UX_SPEC 5 status actions). */
enum class DetailMessage { MARKED_PAID, MARKED_UNPAID, PAID_DATE_CHANGED, VOIDED, FAILED, DRAFT_INCOMPLETE }

data class PendingMessage(val kind: DetailMessage, val canUndo: Boolean)

sealed interface DetailPrompt {
    data object ConfirmVoid : DetailPrompt
    data object ConfirmDelete : DetailPrompt

    /** "Change date" for a payment recorded late. */
    data class PickPaidDate(val current: Long) : DetailPrompt

    /** UX_SPEC 4: "This gives the invoice its number, INV-0015, and opens your email app." */
    data class ConfirmIssueAndSend(val nextNumber: String?) : DetailPrompt
}

data class InvoiceDetailUiState(
    val loading: Boolean = true,
    val today: Long = 0,
    /** Null once loaded means the invoice no longer exists (e.g. a deleted draft). */
    val invoice: InvoiceWithLines? = null,
    val clientName: String = "",
    val client: ClientEntity? = null,
    val status: DisplayStatus = DisplayStatus.DRAFT,
    val history: List<HistoryEntry> = emptyList(),
    val message: PendingMessage? = null,
    val prompt: DetailPrompt? = null,
    val deleted: Boolean = false,
    /** Where "Email to client" goes (D-017); null shows "Send…" instead. */
    val recipientEmail: String? = null,
) {
    /** Drafts and issued (unpaid) invoices can be edited; paid and void are read-only (I-9). */
    val canEdit: Boolean get() = invoice?.invoice?.status.let { it == InvoiceStatus.DRAFT || it == InvoiceStatus.ISSUED }
}

/** Invoice detail (UX_SPEC 4–5): the invoice, sending, its status actions and its short history. */
class InvoiceDetailViewModel(
    private val browser: InvoiceBrowser,
    clients: ClientStore,
    private val clock: Clock,
    private val invoiceId: Long,
    private val sending: InvoiceSending,
    preparer: SendPreparer,
) : ViewModel() {
    /** Email to client, Send another way, Save PDF (UX_SPEC 4). */
    val send = InvoiceSendFlow(viewModelScope, sending, preparer, clock, invoiceId)

    private val _state = MutableStateFlow(InvoiceDetailUiState(today = clock.todayEpochDay()))
    val state: StateFlow<InvoiceDetailUiState> = _state.asStateFlow()

    /** What Undo should do for the message on screen. */
    private var undo: (suspend () -> Unit)? = null
    private var undoExpiry: Job? = null

    init {
        viewModelScope.launch {
            val allClients = combine(clients.observeActive(), clients.observeArchived()) { a, b -> a + b }
            combine(browser.observe(invoiceId), allClients) { invoice, cs -> invoice to cs }.collect { (invoice, cs) ->
                val entity = invoice?.invoice
                val byId = cs.associateBy { it.id }
                send.observe(entity)
                val client = entity?.clientId?.let(byId::get)
                _state.update {
                    it.copy(
                        loading = false,
                        invoice = invoice,
                        client = client,
                        recipientEmail = client?.email?.takeIf { e -> e.isNotBlank() }
                            ?: entity?.toSnapshot?.let(SnapshotJson::decodeTo)?.email?.takeIf { e -> e.isNotBlank() },
                        clientName = entity?.let { e -> clientNameFor(e, byId) }.orEmpty(),
                        status = entity?.let { e -> displayStatus(e.status, e.dueDate, it.today) } ?: DisplayStatus.DRAFT,
                        history = entity?.let(::historyFor).orEmpty(),
                    )
                }
            }
        }
    }

    // ---- Status actions ----------------------------------------------------------------------------

    /** Marks paid today, in two taps (UX_SPEC 5); "Change date" and Undo follow in the message. */
    fun onMarkPaid() = act(DetailMessage.MARKED_PAID, undoable = true, undoAction = { browser.markUnpaid(invoiceId) }) {
        browser.markPaid(invoiceId, clock.todayEpochDay())
    }

    fun onMarkUnpaid() {
        val previousDate = _state.value.invoice?.invoice?.paidDate ?: clock.todayEpochDay()
        act(DetailMessage.MARKED_UNPAID, undoable = true, undoAction = { browser.markPaid(invoiceId, previousDate) }) {
            browser.markUnpaid(invoiceId)
        }
    }

    fun onChangePaidDateRequest() {
        val current = _state.value.invoice?.invoice?.paidDate ?: clock.todayEpochDay()
        _state.update { it.copy(prompt = DetailPrompt.PickPaidDate(current), message = null) }
    }

    fun onPaidDatePicked(day: Long) {
        _state.update { it.copy(prompt = null) }
        act(DetailMessage.PAID_DATE_CHANGED, undoable = false, undoAction = null) { browser.changePaidDate(invoiceId, day) }
    }

    // ---- Sending (UX_SPEC 4) ------------------------------------------------------------------------

    /** Primary button: email if the client has an address, else the share sheet. */
    fun onSend() = send.send(toEmail = _state.value.recipientEmail != null)

    fun onSendAnotherWay() = send.send(toEmail = false)

    fun onSavePdf() = send.save()

    /** Drafts: "Issue and send" asks first, showing the number it will get. */
    fun onIssueAndSendRequest() {
        viewModelScope.launch {
            _state.update { it.copy(prompt = DetailPrompt.ConfirmIssueAndSend(sending.previewNextNumber()), message = null) }
        }
    }

    fun onIssueAndSendConfirm() {
        _state.update { it.copy(prompt = null) }
        viewModelScope.launch {
            if (sending.issue(invoiceId) is Outcome.Success) {
                onSend()
            } else {
                _state.update { it.copy(message = PendingMessage(DetailMessage.DRAFT_INCOMPLETE, canUndo = false)) }
            }
        }
    }

    fun onVoidRequest() = _state.update { it.copy(prompt = DetailPrompt.ConfirmVoid) }

    /** Void keeps the number reserved; Undo is offered for 6 seconds (UX_SPEC 5). */
    fun onVoidConfirm() {
        _state.update { it.copy(prompt = null) }
        act(DetailMessage.VOIDED, undoable = true, undoAction = { browser.undoVoid(invoiceId) }, undoWindowMs = VOID_UNDO_MS) {
            browser.void(invoiceId)
        }
    }

    fun onDeleteRequest() = _state.update { it.copy(prompt = DetailPrompt.ConfirmDelete) }

    fun onDeleteConfirm() {
        _state.update { it.copy(prompt = null) }
        viewModelScope.launch {
            if (browser.deleteDraft(invoiceId) is Outcome.Success) _state.update { it.copy(deleted = true) }
        }
    }

    fun onPromptDismiss() = _state.update { it.copy(prompt = null) }

    fun onUndo() {
        val action = undo ?: return
        undo = null
        undoExpiry?.cancel()
        _state.update { it.copy(message = null) }
        viewModelScope.launch { action() }
    }

    fun onMessageShown() {
        undoExpiry?.cancel()
        undo = null
        _state.update { it.copy(message = null) }
    }

    private fun act(
        kind: DetailMessage,
        undoable: Boolean,
        undoAction: (suspend () -> Unit)?,
        undoWindowMs: Long? = null,
        action: suspend () -> Outcome<Unit>,
    ) {
        viewModelScope.launch {
            if (action() is Outcome.Success) {
                undo = if (undoable) undoAction else null
                _state.update { it.copy(message = PendingMessage(kind, canUndo = undoable)) }
                undoExpiry?.cancel()
                if (undoWindowMs != null) {
                    undoExpiry = launch {
                        delay(undoWindowMs)
                        undo = null
                        _state.update { s -> if (s.message?.kind == kind) s.copy(message = null) else s }
                    }
                }
            } else {
                _state.update { it.copy(message = PendingMessage(DetailMessage.FAILED, canUndo = false)) }
            }
        }
    }

    private fun historyFor(e: InvoiceEntity): List<HistoryEntry> = buildList {
        add(HistoryEntry.Created(e.createdAt / DAY_MS))
        if (e.status != InvoiceStatus.DRAFT) add(HistoryEntry.Issued(e.issueDate))
        e.lastSentAt?.let { add(HistoryEntry.LastSent(it / DAY_MS, e.lastSentVia)) }
        e.paidDate?.let { add(HistoryEntry.Paid(it)) }
    }

    companion object {
        /** UX_SPEC 5: Void can be undone for 6 seconds. */
        const val VOID_UNDO_MS = 6_000L
        private const val DAY_MS = 86_400_000L
    }
}
