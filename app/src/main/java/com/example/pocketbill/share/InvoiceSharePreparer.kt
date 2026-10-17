package com.example.pocketbill.share

import android.content.Context
import androidx.core.content.FileProvider
import com.example.pocketbill.data.repository.ClientStore
import com.example.pocketbill.data.repository.InvoiceStore
import com.example.pocketbill.data.repository.ProfileStore
import com.example.pocketbill.domain.format.DateFormatter
import com.example.pocketbill.domain.invoice.SendTemplates
import com.example.pocketbill.domain.invoice.sendKindFor
import com.example.pocketbill.domain.time.Clock
import com.example.pocketbill.pdf.InvoiceDocument
import com.example.pocketbill.pdf.InvoicePdfRenderer

/**
 * Generates the PDF from saved data and wraps it for sending (UX_SPEC 4 steps 1–2): the file goes
 * to the app's cache and is exposed through FileProvider, so no storage permission is needed.
 */
class InvoiceSharePreparer(
    private val appContext: Context,
    private val invoices: InvoiceStore,
    private val profile: ProfileStore,
    private val clients: ClientStore,
    private val renderer: InvoicePdfRenderer,
    private val clock: Clock,
) : SendPreparer {

    override suspend fun prepare(invoiceId: Long): SendPackage? {
        val invoice = invoices.get(invoiceId) ?: return null
        val e = invoice.invoice
        val client = e.clientId?.let { clients.get(it) }
        val doc = InvoiceDocument.from(invoice, profile.get(), client)
        val file = renderer.render(doc)
        val kind = sendKindFor(e.status, e.dueDate, clock.todayEpochDay())
        val title = doc.number ?: "Draft"
        val text = kind?.let {
            SendTemplates.compose(
                kind = it,
                number = title,
                businessName = doc.fromName,
                clientName = doc.toName.orEmpty(),
                total = doc.totals.last().value,
                dueDate = DateFormatter.full(e.dueDate),
                paidDate = e.paidDate?.let(DateFormatter::full),
                paymentInstructions = doc.paymentInstructions,
            )
        }
        return SendPackage(
            invoiceId = invoiceId,
            title = title,
            filePath = file.absolutePath,
            fileUri = FileProvider.getUriForFile(appContext, authority(appContext), file).toString(),
            fileName = doc.fileName,
            // D-017: the client's current address, else the one frozen on the invoice.
            toEmail = client?.email?.takeIf { it.isNotBlank() }?.trim() ?: doc.toEmail,
            kind = kind,
            subject = text?.subject.orEmpty(),
            body = text?.body.orEmpty(),
        )
    }

    companion object {
        /** Must match the provider in AndroidManifest.xml. */
        fun authority(context: Context) = "${context.packageName}.fileprovider"
    }
}
