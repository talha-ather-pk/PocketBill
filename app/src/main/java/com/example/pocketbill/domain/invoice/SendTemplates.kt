package com.example.pocketbill.domain.invoice

/** Which email goes with the PDF (UX_SPEC 4). */
enum class SendKind { INVOICE, RECEIPT, REMINDER }

/**
 * Paid → receipt, overdue → reminder, issued → invoice. Drafts must be issued first and void
 * invoices can't be sent, so both give null (UX_SPEC 4 variations).
 */
fun sendKindFor(status: InvoiceStatus, dueDateEpochDay: Long, todayEpochDay: Long): SendKind? = when (status) {
    InvoiceStatus.PAID -> SendKind.RECEIPT
    InvoiceStatus.ISSUED -> if (isOverdue(status, dueDateEpochDay, todayEpochDay)) SendKind.REMINDER else SendKind.INVOICE
    InvoiceStatus.DRAFT, InvoiceStatus.VOID -> null
}

data class EmailText(val subject: String, val body: String)

/**
 * The fixed MVP email templates (UX_SPEC 4). All inputs are already formatted text from the
 * invoice's frozen details, so the email matches the PDF. Messaging apps ignore the subject and
 * use the body as the caption.
 */
object SendTemplates {
    fun compose(
        kind: SendKind,
        number: String,
        businessName: String,
        clientName: String,
        total: String,
        dueDate: String,
        paidDate: String?,
        paymentInstructions: String,
    ): EmailText {
        val from = businessName.trim()
        val greeting = clientName.trim().let { if (it.isEmpty()) "Hi," else "Hi $it," }
        val payment = paymentInstructions.trim().let { if (it.isEmpty()) "" else "\n\nPayment details:\n$it" }
        val signOff = if (from.isEmpty()) "\n\nThank you" else "\n\nThank you,\n$from"
        val byFrom = if (from.isEmpty()) "" else " from $from"
        return when (kind) {
            SendKind.INVOICE -> EmailText(
                subject = "Invoice $number$byFrom",
                body = "$greeting\n\nPlease find attached invoice $number for $total, due on $dueDate.$payment$signOff",
            )
            SendKind.RECEIPT -> EmailText(
                subject = "Receipt for invoice $number$byFrom",
                body = "$greeting\n\nThank you for your payment of $total" +
                    (paidDate?.let { ", received on $it" } ?: "") +
                    ". The paid invoice $number is attached for your records.$signOff",
            )
            SendKind.REMINDER -> EmailText(
                subject = "Reminder: invoice $number was due on $dueDate",
                body = "$greeting\n\nThis is a friendly reminder that invoice $number for $total was due on $dueDate. " +
                    "I've attached it again for convenience.$payment$signOff",
            )
        }
    }
}
