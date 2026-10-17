package com.example.pocketbill.domain.invoice

/**
 * Stored invoice status (DATA_MODEL.md section 1). OVERDUE is never stored: it is calculated
 * from ISSUED + due date by [isOverdue]. The names are stored in the database, so renaming one
 * needs a migration (DATA_MODEL G-7).
 */
enum class InvoiceStatus { DRAFT, ISSUED, PAID, VOID }

/** How an invoice-level discount is expressed (F-15). */
enum class DiscountType { PERCENT, AMOUNT }

/** PDF page size (MVP_SPEC.md section 6). */
enum class PaperSize { A4, LETTER }

/** Overdue = issued, not paid, and today is after the due date (DATA_MODEL.md section 6.4 rule 7). */
fun isOverdue(status: InvoiceStatus, dueDateEpochDay: Long, todayEpochDay: Long): Boolean =
    status == InvoiceStatus.ISSUED && todayEpochDay > dueDateEpochDay
