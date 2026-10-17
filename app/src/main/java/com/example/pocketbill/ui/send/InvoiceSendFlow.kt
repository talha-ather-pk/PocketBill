package com.example.pocketbill.ui.send

import com.example.pocketbill.data.db.InvoiceEntity
import com.example.pocketbill.data.repository.InvoiceSending
import com.example.pocketbill.domain.time.Clock
import com.example.pocketbill.share.SendPackage
import com.example.pocketbill.share.SendPreparer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Something the screen must start with an Activity (the ViewModel can't). Cleared once handled. */
sealed interface SendRequest {
    val pkg: SendPackage

    /** Email app directly (UX_SPEC 4 "Email to client"). */
    data class Email(override val pkg: SendPackage) : SendRequest

    /** The share sheet ("Send another way", or "Send…" when the client has no email). */
    data class Other(override val pkg: SendPackage) : SendRequest

    /** The system "save as" picker; does not count as sent. */
    data class Save(override val pkg: SendPackage) : SendRequest
}

sealed interface SendMessage {
    /** "Marked as sent via Gmail" with Undo. */
    data class Sent(val via: String?) : SendMessage

    /** "No email app found. Choose another way to send." */
    data object NoEmailApp : SendMessage

    data object Saved : SendMessage
    data object SaveFailed : SendMessage

    /** "Couldn't create the PDF. Your invoice is saved." */
    data object PdfFailed : SendMessage
}

data class SendUiState(
    /** A PDF is being made; the screen shows progress only after 300 ms (UX_SPEC 4). */
    val busy: Boolean = false,
    val request: SendRequest? = null,
    val message: SendMessage? = null,
)

/**
 * Sending, shared by invoice detail and the PDF preview (UX_SPEC 4): makes the PDF, asks the
 * screen to open the email app / share sheet / save picker, and notices when the invoice gets
 * marked as sent so it can offer Undo. Owned by a ViewModel, which passes its scope.
 */
class InvoiceSendFlow(
    private val scope: CoroutineScope,
    private val sending: InvoiceSending,
    private val preparer: SendPreparer,
    private val clock: Clock,
    private val invoiceId: Long,
    private val onError: (Throwable) -> Unit = {},
) {
    private val _state = MutableStateFlow(SendUiState())
    val state: StateFlow<SendUiState> = _state.asStateFlow()

    private var latest: InvoiceEntity? = null

    /** The "sent" values before the latest send started; a change from these means it was recorded. */
    private var awaiting: Pair<Long?, String?>? = null
    private var undoTo: Pair<Long?, String?>? = null

    /** The owning ViewModel passes every version of the invoice it observes. */
    fun observe(invoice: InvoiceEntity?) {
        latest = invoice
        val before = awaiting ?: return
        if (invoice != null && invoice.lastSentAt != null && invoice.lastSentAt != before.first) {
            awaiting = null
            undoTo = before
            _state.update { it.copy(message = SendMessage.Sent(invoice.lastSentVia)) }
        }
    }

    /** [toEmail] true → email app; false → share sheet. */
    fun send(toEmail: Boolean) = prepare { pkg ->
        if (toEmail && pkg.toEmail != null) SendRequest.Email(pkg) else SendRequest.Other(pkg)
    }

    fun save() = prepare { SendRequest.Save(it) }

    private fun prepare(request: (SendPackage) -> SendRequest) {
        if (_state.value.busy) return
        _state.update { it.copy(busy = true, message = null) }
        scope.launch {
            val pkg = try {
                preparer.prepare(invoiceId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                onError(e)
                null
            }
            if (pkg == null) {
                _state.update { it.copy(busy = false, message = SendMessage.PdfFailed) }
                return@launch
            }
            val next = request(pkg)
            if (next !is SendRequest.Save) awaiting = latest?.let { it.lastSentAt to it.lastSentVia } ?: (null to null)
            _state.update { it.copy(busy = false, request = next) }
        }
    }

    // ---- Results from the screen ---------------------------------------------------------------------

    fun onRequestHandled() = _state.update { it.copy(request = null) }

    /** The email app opened directly; [via] is its name, or null if Android offered several. */
    fun onEmailOpened(via: String?) {
        scope.launch { sending.recordSent(invoiceId, via ?: EMAIL_LABEL, clock.nowMillis()) }
    }

    /** No email app: say so and fall back to the share sheet (UX_SPEC 4 step 4). */
    fun onNoEmailApp(pkg: SendPackage) =
        _state.update { it.copy(message = SendMessage.NoEmailApp, request = SendRequest.Other(pkg)) }

    fun onSaveResult(ok: Boolean) =
        _state.update { it.copy(message = if (ok) SendMessage.Saved else SendMessage.SaveFailed) }

    fun onUndoSent() {
        val previous = undoTo ?: return
        undoTo = null
        _state.update { it.copy(message = null) }
        scope.launch { sending.restoreSent(invoiceId, previous.first, previous.second) }
    }

    fun onMessageShown() {
        undoTo = null
        _state.update { it.copy(message = null) }
    }

    companion object {
        /** Recorded when Android let the user choose between several email apps. */
        const val EMAIL_LABEL = "email"
    }
}
