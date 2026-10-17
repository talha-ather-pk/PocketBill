package com.example.pocketbill.ui.pdf

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pocketbill.app.SafeLog
import com.example.pocketbill.data.repository.ClientStore
import com.example.pocketbill.data.repository.InvoiceBrowser
import com.example.pocketbill.data.repository.InvoiceSending
import com.example.pocketbill.data.repository.InvoiceStore
import com.example.pocketbill.data.repository.ProfileStore
import com.example.pocketbill.domain.invoice.InvoiceStatus
import com.example.pocketbill.domain.invoice.SendKind
import com.example.pocketbill.domain.invoice.SnapshotJson
import com.example.pocketbill.domain.invoice.sendKindFor
import com.example.pocketbill.domain.time.Clock
import com.example.pocketbill.pdf.InvoiceDocument
import com.example.pocketbill.pdf.InvoicePdfRenderer
import com.example.pocketbill.pdf.PdfMark
import com.example.pocketbill.pdf.PdfPageImages
import com.example.pocketbill.share.SendPreparer
import com.example.pocketbill.ui.send.InvoiceSendFlow
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

sealed interface PdfPreviewUiState {
    data object Loading : PdfPreviewUiState

    data class Ready(val title: String, val isDraft: Boolean, val file: File, val pages: List<Bitmap>) : PdfPreviewUiState

    /** UX_SPEC 4: "Couldn't create the PDF. Your invoice is saved." with Try again. */
    data object Failed : PdfPreviewUiState

    /** The invoice was deleted (a draft) while the preview was open. */
    data object Missing : PdfPreviewUiState
}

/** What the preview's bottom bar offers (UX_SPEC 4): Send for issued and paid invoices, Save for all but drafts. */
data class PreviewActions(
    val kind: SendKind? = null,
    val hasEmail: Boolean = false,
    val sentBefore: Boolean = false,
    val canSave: Boolean = false,
)

/** PDF preview (MVP_SPEC 6): generates the PDF from the saved invoice, shows its pages, and sends it. */
class PdfPreviewViewModel(
    private val invoices: InvoiceStore,
    browser: InvoiceBrowser,
    sending: InvoiceSending,
    private val profile: ProfileStore,
    private val clients: ClientStore,
    private val renderer: InvoicePdfRenderer,
    preparer: SendPreparer,
    private val clock: Clock,
    private val invoiceId: Long,
    private val pageWidthPx: Int,
) : ViewModel() {
    private val _state = MutableStateFlow<PdfPreviewUiState>(PdfPreviewUiState.Loading)
    val state: StateFlow<PdfPreviewUiState> = _state.asStateFlow()

    private val _actions = MutableStateFlow(PreviewActions())
    val actions: StateFlow<PreviewActions> = _actions.asStateFlow()

    val send = InvoiceSendFlow(viewModelScope, sending, preparer, clock, invoiceId)

    init {
        load()
        viewModelScope.launch {
            browser.observe(invoiceId).collect { invoice ->
                val e = invoice?.invoice
                send.observe(e)
                if (e == null) return@collect
                val email = e.clientId?.let { clients.get(it) }?.email?.takeIf { it.isNotBlank() }
                    ?: e.toSnapshot?.let(SnapshotJson::decodeTo)?.email?.takeIf { it.isNotBlank() }
                _actions.value = PreviewActions(
                    kind = sendKindFor(e.status, e.dueDate, clock.todayEpochDay()),
                    hasEmail = email != null,
                    sentBefore = e.lastSentAt != null,
                    canSave = e.status != InvoiceStatus.DRAFT,
                )
            }
        }
    }

    fun onRetry() = load()

    fun onSend() = send.send(toEmail = _actions.value.hasEmail)

    fun onSave() = send.save()

    private fun load() {
        _state.value = PdfPreviewUiState.Loading
        viewModelScope.launch {
            _state.value = try {
                val invoice = invoices.get(invoiceId)
                if (invoice == null) {
                    PdfPreviewUiState.Missing
                } else {
                    val client = invoice.invoice.clientId?.let { clients.get(it) }
                    val doc = InvoiceDocument.from(invoice, profile.get(), client)
                    val file = renderer.render(doc)
                    PdfPreviewUiState.Ready(
                        title = doc.number ?: "Draft",
                        isDraft = doc.mark == PdfMark.DRAFT,
                        file = file,
                        pages = PdfPageImages.render(file, pageWidthPx),
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Error details go to the local log only (UX_SPEC 4).
                SafeLog.failure(TAG, "PDF generation failed", e)
                PdfPreviewUiState.Failed
            }
        }
    }

    private companion object {
        const val TAG = "PdfPreview"
    }
}
