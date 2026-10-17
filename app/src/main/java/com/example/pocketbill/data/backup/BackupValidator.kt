package com.example.pocketbill.data.backup

import com.example.pocketbill.domain.invoice.DiscountType
import com.example.pocketbill.domain.invoice.InvoiceStatus
import com.example.pocketbill.domain.invoice.PaperSize
import com.example.pocketbill.domain.money.CurrencyCode
import com.example.pocketbill.domain.money.MoneyLimits

/** How much a backup holds, shown before the user confirms a restore (MVP_SPEC 7). */
data class BackupCounts(val clients: Int, val invoices: Int, val presets: Int, val createdAt: Long)

/** Why a file can't be restored; each becomes one line of the error message. */
enum class BackupProblem {
    NOT_POCKETBILL,
    NO_PROFILE,
    BAD_PROFILE,
    DUPLICATE_ID,
    DUPLICATE_UUID,
    DUPLICATE_NUMBER,
    MISSING_CLIENT,
    MISSING_INVOICE,
    BAD_STATUS,
    BAD_CURRENCY,
    BAD_AMOUNT,
    BAD_DATES,
    BAD_TEXT,
}

sealed interface BackupCheck {
    data class Valid(val counts: BackupCounts) : BackupCheck
    data class Invalid(val problems: Set<BackupProblem>) : BackupCheck
}

/**
 * Checks everything before anything is changed (MVP_SPEC 7): required fields, links between
 * tables, unique IDs and numbers, money within range, and the invoice invariants (DATA_MODEL 2).
 * A restore only runs on a [BackupCheck.Valid] file.
 */
object BackupValidator {
    fun check(file: BackupFile): BackupCheck {
        val problems = mutableSetOf<BackupProblem>()
        if (file.app != BackupFormat.APP) problems += BackupProblem.NOT_POCKETBILL

        val profile = file.businessProfile
        if (profile == null) {
            problems += BackupProblem.NO_PROFILE
        } else {
            if (profile.name.isBlank() || profile.uuid.isBlank()) problems += BackupProblem.BAD_PROFILE
            if (CurrencyCode.fromCode(profile.defaultCurrency) == null) problems += BackupProblem.BAD_CURRENCY
            if (PaperSize.entries.none { it.name == profile.paperSize }) problems += BackupProblem.BAD_PROFILE
            if (profile.nextInvoiceNumber < 1 || profile.defaultTermsDays < 0) problems += BackupProblem.BAD_PROFILE
            if (profile.defaultTaxRateBp !in 0..MoneyLimits.MAX_TAX_BP) problems += BackupProblem.BAD_AMOUNT
        }

        file.logo?.let { encoded ->
            if (runCatching { java.util.Base64.getDecoder().decode(encoded) }.getOrNull()?.isNotEmpty() != true) problems += BackupProblem.BAD_PROFILE
        }

        fun <T> unique(items: List<T>, key: (T) -> Any, problem: BackupProblem) {
            if (items.map(key).toSet().size != items.size) problems += problem
        }
        unique(file.clients, { it.id }, BackupProblem.DUPLICATE_ID)
        unique(file.invoices, { it.id }, BackupProblem.DUPLICATE_ID)
        unique(file.lineItems, { it.id }, BackupProblem.DUPLICATE_ID)
        unique(file.itemPresets, { it.id }, BackupProblem.DUPLICATE_ID)
        val uuids = file.clients.map { it.uuid } + file.invoices.map { it.uuid } + file.lineItems.map { it.uuid } + file.itemPresets.map { it.uuid }
        if (uuids.any { it.isBlank() }) problems += BackupProblem.BAD_TEXT
        // Each table's uuids are unique (unique index); checked per table as the database would.
        unique(file.clients, { it.uuid }, BackupProblem.DUPLICATE_UUID)
        unique(file.invoices, { it.uuid }, BackupProblem.DUPLICATE_UUID)
        unique(file.lineItems, { it.uuid }, BackupProblem.DUPLICATE_UUID)
        unique(file.itemPresets, { it.uuid }, BackupProblem.DUPLICATE_UUID)
        val numbers = file.invoices.mapNotNull { it.number }
        if (numbers.toSet().size != numbers.size) problems += BackupProblem.DUPLICATE_NUMBER

        val ids = listOf(file.clients.map { it.id }, file.invoices.map { it.id }, file.lineItems.map { it.id }, file.itemPresets.map { it.id })
        if (ids.flatten().any { it < 1 }) problems += BackupProblem.DUPLICATE_ID

        file.clients.forEach { c ->
            if (c.name.isBlank()) problems += BackupProblem.BAD_TEXT
            if (c.preferredCurrency != null && CurrencyCode.fromCode(c.preferredCurrency) == null) problems += BackupProblem.BAD_CURRENCY
        }

        val clientIds = file.clients.map { it.id }.toSet()
        file.invoices.forEach { inv ->
            val status = InvoiceStatus.entries.firstOrNull { it.name == inv.status }
            if (status == null) {
                problems += BackupProblem.BAD_STATUS
            } else {
                // Issued, paid and void invoices have a number and a client (DATA_MODEL I-11); paid ones a paid date.
                if (status != InvoiceStatus.DRAFT && (inv.number.isNullOrBlank() || inv.clientId == null)) problems += BackupProblem.BAD_STATUS
                if (status == InvoiceStatus.DRAFT && inv.number != null) problems += BackupProblem.BAD_STATUS
                if (status == InvoiceStatus.PAID && inv.paidDate == null) problems += BackupProblem.BAD_STATUS
            }
            if (inv.clientId != null && inv.clientId !in clientIds) problems += BackupProblem.MISSING_CLIENT
            if (CurrencyCode.fromCode(inv.currency) == null) problems += BackupProblem.BAD_CURRENCY
            // Drafts are autosaved half-finished, so only issued invoices must have sensible dates.
            if (status != null && status != InvoiceStatus.DRAFT && inv.dueDate < inv.issueDate) problems += BackupProblem.BAD_DATES
            if (inv.discountType != null && DiscountType.entries.none { it.name == inv.discountType }) problems += BackupProblem.BAD_AMOUNT
            val amounts = listOf(inv.subtotalMinor, inv.discountMinor, inv.taxMinor, inv.totalMinor) + listOfNotNull(inv.discountValue)
            if (amounts.any { it !in 0..MoneyLimits.MAX_AMOUNT_MINOR }) problems += BackupProblem.BAD_AMOUNT
            if (inv.taxRateBp !in 0..MoneyLimits.MAX_TAX_BP) problems += BackupProblem.BAD_AMOUNT
        }

        val invoiceIds = file.invoices.map { it.id }.toSet()
        file.lineItems.forEach { line ->
            if (line.invoiceId !in invoiceIds) problems += BackupProblem.MISSING_INVOICE
            if (line.quantityMilli !in 1..MoneyLimits.MAX_QUANTITY_MILLI) problems += BackupProblem.BAD_AMOUNT
            if (line.unitPriceMinor !in 0..MoneyLimits.MAX_AMOUNT_MINOR || line.amountMinor !in 0..MoneyLimits.MAX_AMOUNT_MINOR) {
                problems += BackupProblem.BAD_AMOUNT
            }
        }
        file.itemPresets.forEach { preset ->
            if (preset.description.isBlank()) problems += BackupProblem.BAD_TEXT
            if (preset.currency != null && CurrencyCode.fromCode(preset.currency) == null) problems += BackupProblem.BAD_CURRENCY
            if (preset.unitPriceMinor != null && preset.unitPriceMinor !in 0..MoneyLimits.MAX_AMOUNT_MINOR) problems += BackupProblem.BAD_AMOUNT
        }

        return if (problems.isEmpty()) {
            BackupCheck.Valid(BackupCounts(file.clients.size, file.invoices.size, file.itemPresets.size, file.createdAt))
        } else {
            BackupCheck.Invalid(problems)
        }
    }
}
