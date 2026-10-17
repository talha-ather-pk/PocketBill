package com.example.pocketbill.pdf

import com.example.pocketbill.data.db.BusinessProfileEntity
import com.example.pocketbill.data.db.ClientEntity
import com.example.pocketbill.data.db.InvoiceWithLines
import com.example.pocketbill.domain.format.DateFormatter
import com.example.pocketbill.domain.format.PdfFileName
import com.example.pocketbill.domain.invoice.DiscountType
import com.example.pocketbill.domain.invoice.FromSnapshot
import com.example.pocketbill.domain.invoice.InvoiceStatus
import com.example.pocketbill.domain.invoice.PaperSize
import com.example.pocketbill.domain.invoice.SnapshotJson
import com.example.pocketbill.domain.invoice.ToSnapshot
import com.example.pocketbill.domain.money.CurrencyCode
import com.example.pocketbill.domain.money.MoneyFormatter
import java.math.BigDecimal

/** The status mark drawn on the PDF (MVP_SPEC 6). */
enum class PdfMark { NONE, DRAFT, PAID, VOID }

data class PdfLine(val description: String, val quantity: String, val unitPrice: String, val amount: String)

data class PdfRow(val label: String, val value: String)

/**
 * Everything a PDF shows, already formatted as text, so the renderer only draws (ARCHITECTURE 3:
 * the pdf layer never reads the database). Build it with [InvoiceDocument.from].
 */
data class InvoiceDocument(
    val paperSize: PaperSize,
    /** Null for drafts. */
    val number: String?,
    val mark: PdfMark,
    /** Under the PAID stamp: the paid date. */
    val markDetail: String?,
    val fromName: String,
    val fromLines: List<String>,
    /** Logo file name (frozen for issued invoices); drawn if the file exists (F-14). */
    val logoFile: String? = null,
    /** Null when a draft has no client yet. */
    val toName: String?,
    val toLines: List<String>,
    /** The client's email as shown on the invoice (frozen once issued). */
    val toEmail: String?,
    /** Issue date, due date, currency, and the paid date when paid. */
    val facts: List<PdfRow>,
    val lines: List<PdfLine>,
    /** Subtotal, discount and tax; the last row is the bold total. */
    val totals: List<PdfRow>,
    val paymentInstructions: String,
    val notes: String,
    val fileName: String,
) {
    companion object {
        /**
         * Issued, paid and void invoices use their frozen details; drafts (or an unreadable
         * snapshot) use the live [profile] and [client] (DATA_MODEL 5.2). The paper size is a
         * setting, so it always comes from the live profile.
         */
        fun from(invoice: InvoiceWithLines, profile: BusinessProfileEntity?, client: ClientEntity?): InvoiceDocument {
            val e = invoice.invoice
            val currency = CurrencyCode.fromCode(e.currency) ?: CurrencyCode.USD
            fun money(minor: Long) = MoneyFormatter.format(minor, currency)
            val frozen = e.status != InvoiceStatus.DRAFT
            val from = (if (frozen) e.fromSnapshot?.let(SnapshotJson::decodeFrom) else null)
                ?: profile?.let {
                    FromSnapshot(
                        name = it.name,
                        contactLines = it.contactLines,
                        address = it.address,
                        taxId = it.taxId,
                        paymentInstructions = it.paymentInstructions,
                        logoFile = it.logoFile,
                    )
                }
                ?: FromSnapshot(name = "")
            val to = (if (frozen) e.toSnapshot?.let(SnapshotJson::decodeTo) else null)
                ?: client?.let { ToSnapshot(name = it.name, email = it.email, address = it.address, country = it.country, taxId = it.taxId) }

            val facts = buildList {
                add(PdfRow("Issue date", DateFormatter.full(e.issueDate)))
                add(PdfRow("Due date", DateFormatter.full(e.dueDate)))
                add(PdfRow("Currency", currency.name))
                if (e.status == InvoiceStatus.PAID && e.paidDate != null) add(PdfRow("Paid on", DateFormatter.full(e.paidDate)))
            }
            val totals = buildList {
                add(PdfRow("Subtotal", money(e.subtotalMinor)))
                if (e.discountMinor > 0) {
                    val label = if (e.discountType == DiscountType.PERCENT && e.discountValue != null) {
                        "Discount (${percent(e.discountValue)}%)"
                    } else {
                        "Discount"
                    }
                    add(PdfRow(label, "−" + money(e.discountMinor)))
                }
                if (e.taxRateBp > 0) add(PdfRow("Tax (${percent(e.taxRateBp.toLong())}%)", money(e.taxMinor)))
                val totalLabel = when (e.status) {
                    InvoiceStatus.PAID -> "Total paid"
                    InvoiceStatus.VOID -> "Total"
                    else -> "Total due"
                }
                add(PdfRow(totalLabel, money(e.totalMinor)))
            }
            val mark = when (e.status) {
                InvoiceStatus.DRAFT -> PdfMark.DRAFT
                InvoiceStatus.ISSUED -> PdfMark.NONE
                InvoiceStatus.PAID -> PdfMark.PAID
                InvoiceStatus.VOID -> PdfMark.VOID
            }
            return InvoiceDocument(
                paperSize = profile?.paperSize ?: PaperSize.A4,
                number = e.number,
                mark = mark,
                markDetail = e.paidDate?.takeIf { mark == PdfMark.PAID }?.let(DateFormatter::full),
                fromName = from.name,
                logoFile = from.logoFile,
                fromLines = textLines(from.contactLines) + textLines(from.address) +
                    listOfNotNull(from.taxId?.takeIf { it.isNotBlank() }?.let { "Tax ID: ${it.trim()}" }),
                toName = to?.name,
                toLines = if (to == null) {
                    emptyList()
                } else {
                    textLines(to.address.orEmpty()) + textLines(to.country.orEmpty()) +
                        listOfNotNull(to.email?.takeIf { it.isNotBlank() }?.trim(), to.taxId?.takeIf { it.isNotBlank() }?.let { "Tax ID: ${it.trim()}" })
                },
                toEmail = to?.email?.takeIf { it.isNotBlank() }?.trim(),
                facts = facts,
                lines = invoice.lines.map {
                    PdfLine(
                        description = it.description.trim(),
                        quantity = BigDecimal.valueOf(it.quantityMilli, 3).stripTrailingZeros().toPlainString(),
                        unitPrice = money(it.unitPriceMinor),
                        amount = money(it.amountMinor),
                    )
                },
                totals = totals,
                paymentInstructions = from.paymentInstructions.trim(),
                notes = e.notes.trim(),
                fileName = PdfFileName.forInvoice(e.number, to?.name),
            )
        }

        /** Basis points as a percentage: 750 → "7.5". */
        private fun percent(bp: Long): String = BigDecimal.valueOf(bp, 2).stripTrailingZeros().toPlainString()

        private fun textLines(text: String): List<String> = text.lines().map { it.trim() }.filter { it.isNotEmpty() }
    }
}
