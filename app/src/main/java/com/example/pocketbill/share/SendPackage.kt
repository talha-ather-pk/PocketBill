package com.example.pocketbill.share

import com.example.pocketbill.domain.invoice.SendKind

/**
 * A generated PDF ready to hand to another app, with the email text that goes with it
 * (UX_SPEC 4). Plain strings, so the code that uses it is unit tested without Android.
 */
data class SendPackage(
    val invoiceId: Long,
    /** "INV-0014", or "Draft". */
    val title: String,
    val filePath: String,
    /** FileProvider content URI; the receiving app gets read access to this file only. */
    val fileUri: String,
    val fileName: String,
    /** Null when the client has no email address: then "Send…" opens the share sheet. */
    val toEmail: String?,
    /** Null for drafts and void invoices, which can't be sent. */
    val kind: SendKind?,
    val subject: String,
    val body: String,
)

/** Makes a [SendPackage]; [InvoiceSharePreparer] is the real one, tests use a fake. */
fun interface SendPreparer {
    /** Null when the invoice no longer exists. Throws if the PDF can't be made. */
    suspend fun prepare(invoiceId: Long): SendPackage?
}
