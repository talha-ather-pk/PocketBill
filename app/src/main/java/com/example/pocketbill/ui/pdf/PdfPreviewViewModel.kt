package com.example.pocketbill.ui.pdf

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pocketbill.data.repository.ClientStore
import com.example.pocketbill.data.repository.InvoiceStore
import com.example.pocketbill.data.repository.ProfileStore
import com.example.pocketbill.pdf.InvoiceDocument
import com.example.pocketbill.pdf.InvoicePdfRenderer
import com.example.pocketbill.pdf.PdfMark
import com.example.pocketbill.pdf.PdfPageImages
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

/** PDF preview (MVP_SPEC 6): generates the PDF from the saved invoice, then shows its pages. */
class PdfPreviewViewModel(
    private val invoices: InvoiceStore,
    private val profile: ProfileStore,
    private val clients: ClientStore,
    private val renderer: InvoicePdfRenderer,
    private val invoiceId: Long,
    private val pageWidthPx: Int,
) : ViewModel() {
    private val _state = MutableStateFlow<PdfPreviewUiState>(PdfPreviewUiState.Loading)
    val state: StateFlow<PdfPreviewUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun onRetry() = load()

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
                Log.e(TAG, "PDF generation failed", e)
                PdfPreviewUiState.Failed
            }
        }
    }

    private companion object {
        const val TAG = "PdfPreview"
    }
}
