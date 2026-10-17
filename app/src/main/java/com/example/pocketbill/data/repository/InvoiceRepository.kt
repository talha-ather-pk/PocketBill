package com.example.pocketbill.data.repository

import androidx.room.withTransaction
import com.example.pocketbill.data.db.CurrencyTotal
import com.example.pocketbill.data.db.InvoiceEntity
import com.example.pocketbill.data.db.InvoiceWithLines
import com.example.pocketbill.data.db.LineItemEntity
import com.example.pocketbill.data.db.PocketBillDatabase
import com.example.pocketbill.domain.Outcome
import com.example.pocketbill.domain.Problem
import com.example.pocketbill.domain.invoice.DiscountType
import com.example.pocketbill.domain.invoice.InvoiceNumbering
import com.example.pocketbill.domain.invoice.InvoiceStatus
import com.example.pocketbill.domain.money.CalculationResult
import com.example.pocketbill.domain.money.CurrencyCode
import com.example.pocketbill.domain.money.Discount
import com.example.pocketbill.domain.money.InvoiceCalculator
import com.example.pocketbill.domain.money.InvoiceTotals
import com.example.pocketbill.domain.money.LineInput
import com.example.pocketbill.domain.money.MoneyLimits
import com.example.pocketbill.domain.time.Clock
import kotlinx.coroutines.flow.Flow
import java.util.UUID

/** One line as the editor hands it over. [uuid] keeps a line's identity across saves (D-010). */
data class LineDraft(
    val description: String,
    val quantityMilli: Long,
    val unitPriceMinor: Long,
    val uuid: String? = null,
)

/** Everything the editor saves for an invoice (UX_SPEC.md section 3). */
data class InvoiceInput(
    val clientId: Long?,
    val currency: String,
    val issueDate: Long,
    val dueDate: Long,
    val lines: List<LineDraft>,
    val taxRateBp: Int = 0,
    val discount: Discount? = null,
    val notes: String = "",
)

/**
 * The only code that reads or writes invoices (ARCHITECTURE.md section 3). Each operation that
 * touches several rows runs in one transaction (DATA_MODEL.md section 3) and enforces the
 * invariants in DATA_MODEL.md section 2.
 */
class InvoiceRepository(
    private val db: PocketBillDatabase,
    private val clock: Clock,
    private val newUuid: () -> String = { UUID.randomUUID().toString() },
) {
    private val invoices = db.invoiceDao()
    private val clients = db.clientDao()
    private val profiles = db.profileDao()

    // ---- Reads -------------------------------------------------------------------------------

    suspend fun get(id: Long): InvoiceWithLines? = invoices.getWithLines(id)

    fun observe(id: Long): Flow<InvoiceWithLines?> = invoices.observeWithLines(id)

    fun observeOutstanding(): Flow<List<CurrencyTotal>> = invoices.observeOutstanding()

    fun observeOverdueCount(): Flow<Int> = invoices.observeOverdueCount(clock.todayEpochDay())

    fun observeRecent(): Flow<List<InvoiceEntity>> = invoices.observeRecent()

    fun observeDrafts(): Flow<List<InvoiceEntity>> = invoices.observeDrafts()

    fun observeIssuedUnsent(): Flow<List<InvoiceEntity>> = invoices.observeIssuedUnsent()

    // ---- Drafts ------------------------------------------------------------------------------

    /**
     * Creates ([invoiceId] = 0) or updates a draft. Drafts may be incomplete (no client, empty
     * descriptions), but stored numbers must be valid and totals are always recalculated (I-7).
     * Returns the draft's ID.
     */
    suspend fun saveDraft(invoiceId: Long, input: InvoiceInput): Outcome<Long> {
        val problems = checkNumbers(input)
        if (problems.isNotEmpty()) return Outcome.ValidationFailed(problems)
        val totals = when (val result = calculate(input)) {
            is CalculationResult.Ok -> result.totals
            CalculationResult.TooLarge -> return Outcome.AmountTooLarge
        }
        val now = clock.nowMillis()
        return db.withTransaction {
            if (input.clientId != null && clients.get(input.clientId) == null) {
                return@withTransaction Outcome.ValidationFailed(setOf(Problem.NO_CLIENT))
            }
            val id = if (invoiceId == 0L) {
                invoices.insert(
                    InvoiceEntity(uuid = newUuid(), clientId = input.clientId, currency = input.currency,
                        issueDate = input.issueDate, dueDate = input.dueDate, createdAt = now, updatedAt = now)
                        .withInput(input, totals, now),
                )
            } else {
                val existing = invoices.get(invoiceId) ?: return@withTransaction Outcome.NotFound
                if (existing.status != InvoiceStatus.DRAFT) {
                    return@withTransaction Outcome.NotAllowed(Problem.NOT_A_DRAFT)
                }
                invoices.update(existing.withInput(input, totals, now))
                invoiceId
            }
            replaceLines(id, input.lines, totals, now)
            Outcome.Success(id)
        }
    }

    /** Deletes a draft (UX_SPEC 3). Issued invoices are never deleted, only voided (N-3). */
    suspend fun deleteDraft(invoiceId: Long): Outcome<Unit> = db.withTransaction {
        val existing = invoices.get(invoiceId) ?: return@withTransaction Outcome.NotFound
        if (existing.status != InvoiceStatus.DRAFT) return@withTransaction Outcome.NotAllowed(Problem.NOT_A_DRAFT)
        invoices.delete(existing)
        Outcome.Success(Unit)
    }

    // ---- Issuing -------------------------------------------------------------------------------

    /**
     * Issues a draft in one transaction (DATA_MODEL.md section 3): checks I-4/I-5/I-11, assigns the
     * next free number (N-1…N-7), freezes the business and client details (5.2), advances the
     * sequence and records when the client was last invoiced. Returns the number.
     */
    suspend fun issue(invoiceId: Long): Outcome<String> = db.withTransaction {
        val draft = invoices.getWithLines(invoiceId) ?: return@withTransaction Outcome.NotFound
        val invoice = draft.invoice
        if (invoice.status != InvoiceStatus.DRAFT) return@withTransaction Outcome.NotAllowed(Problem.NOT_A_DRAFT)
        val profile = profiles.get() ?: return@withTransaction Outcome.NotAllowed(Problem.NO_BUSINESS_PROFILE)

        val problems = checkIssuable(invoice.clientId, invoice.issueDate, invoice.dueDate, draft.lines.map {
            LineDraft(it.description, it.quantityMilli, it.unitPriceMinor)
        })
        val client = invoice.clientId?.let { clients.get(it) }
        if (client == null) problems += Problem.NO_CLIENT
        if (problems.isNotEmpty()) return@withTransaction Outcome.ValidationFailed(problems)

        val totals = recalculate(invoice, draft.lines) ?: return@withTransaction Outcome.AmountTooLarge
        val assigned = InvoiceNumbering.firstFree(profile.invoicePrefix, profile.nextInvoiceNumber) {
            invoices.numberExists(it)
        }
        val now = clock.nowMillis()
        invoices.update(
            invoice.copy(
                number = assigned.number,
                status = InvoiceStatus.ISSUED,
                fromSnapshot = profile.toSnapshotJson(),
                toSnapshot = client!!.toSnapshotJson(),
                subtotalMinor = totals.subtotalMinor,
                discountMinor = totals.discountMinor,
                taxMinor = totals.taxMinor,
                totalMinor = totals.totalMinor,
                updatedAt = now,
            ),
        )
        profiles.upsert(profile.copy(nextInvoiceNumber = assigned.sequence + 1, updatedAt = now))
        clients.update(client.copy(lastInvoicedAt = now, updatedAt = now))
        Outcome.Success(assigned.number)
    }

    /**
     * Edits an issued, unpaid invoice (UX_SPEC 3 "Edit issued"). The number stays. Choosing a
     * different client takes a fresh snapshot of that client (DATA_MODEL 5.4).
     */
    suspend fun updateIssued(invoiceId: Long, input: InvoiceInput): Outcome<Unit> {
        val problems = checkNumbers(input) + checkIssuable(input.clientId, input.issueDate, input.dueDate, input.lines)
        if (problems.isNotEmpty()) return Outcome.ValidationFailed(problems)
        val totals = when (val result = calculate(input)) {
            is CalculationResult.Ok -> result.totals
            CalculationResult.TooLarge -> return Outcome.AmountTooLarge
        }
        val now = clock.nowMillis()
        return db.withTransaction {
            val existing = invoices.get(invoiceId) ?: return@withTransaction Outcome.NotFound
            if (existing.status != InvoiceStatus.ISSUED) return@withTransaction Outcome.NotAllowed(Problem.READ_ONLY)
            val client = clients.get(input.clientId!!)
                ?: return@withTransaction Outcome.ValidationFailed(setOf(Problem.NO_CLIENT))
            val toSnapshot = if (input.clientId != existing.clientId) client.toSnapshotJson() else existing.toSnapshot
            invoices.update(existing.withInput(input, totals, now).copy(toSnapshot = toSnapshot))
            replaceLines(invoiceId, input.lines, totals, now)
            Outcome.Success(Unit)
        }
    }

    // ---- Status changes (UX_SPEC 5) --------------------------------------------------------

    suspend fun markPaid(invoiceId: Long, paidDate: Long = clock.todayEpochDay()): Outcome<Unit> =
        changeStatus(invoiceId, from = InvoiceStatus.ISSUED) { it.copy(status = InvoiceStatus.PAID, paidDate = paidDate) }

    suspend fun markUnpaid(invoiceId: Long): Outcome<Unit> =
        changeStatus(invoiceId, from = InvoiceStatus.PAID) { it.copy(status = InvoiceStatus.ISSUED, paidDate = null) }

    /** Voids an unpaid invoice. Its number stays reserved forever (N-3). */
    suspend fun void(invoiceId: Long): Outcome<Unit> =
        changeStatus(invoiceId, from = InvoiceStatus.ISSUED) { it.copy(status = InvoiceStatus.VOID) }

    /** Records that the invoice was handed to an app for sending (UX_SPEC 4). Status is unchanged. */
    suspend fun recordSent(invoiceId: Long, via: String, at: Long = clock.nowMillis()): Outcome<Unit> =
        db.withTransaction {
            val existing = invoices.get(invoiceId) ?: return@withTransaction Outcome.NotFound
            if (existing.status != InvoiceStatus.ISSUED && existing.status != InvoiceStatus.PAID) {
                return@withTransaction Outcome.NotAllowed(Problem.READ_ONLY)
            }
            invoices.update(existing.copy(lastSentAt = at, lastSentVia = via, updatedAt = clock.nowMillis()))
            Outcome.Success(Unit)
        }

    /** Undo for "Marked as sent" (UX_SPEC 4): restores the previous values. */
    suspend fun restoreSent(invoiceId: Long, lastSentAt: Long?, lastSentVia: String?): Outcome<Unit> =
        db.withTransaction {
            val existing = invoices.get(invoiceId) ?: return@withTransaction Outcome.NotFound
            invoices.update(existing.copy(lastSentAt = lastSentAt, lastSentVia = lastSentVia, updatedAt = clock.nowMillis()))
            Outcome.Success(Unit)
        }

    // ---- Helpers -------------------------------------------------------------------------------

    private suspend fun changeStatus(
        invoiceId: Long,
        from: InvoiceStatus,
        change: (InvoiceEntity) -> InvoiceEntity,
    ): Outcome<Unit> = db.withTransaction {
        val existing = invoices.get(invoiceId) ?: return@withTransaction Outcome.NotFound
        if (existing.status != from) return@withTransaction Outcome.NotAllowed(Problem.READ_ONLY)
        invoices.update(change(existing).copy(updatedAt = clock.nowMillis()))
        Outcome.Success(Unit)
    }

    private suspend fun replaceLines(invoiceId: Long, lines: List<LineDraft>, totals: InvoiceTotals, now: Long) {
        invoices.deleteLines(invoiceId)
        invoices.insertLines(
            lines.mapIndexed { index, line ->
                LineItemEntity(
                    uuid = line.uuid ?: newUuid(),
                    invoiceId = invoiceId,
                    position = index,
                    description = line.description.trim(),
                    quantityMilli = line.quantityMilli,
                    unitPriceMinor = line.unitPriceMinor,
                    amountMinor = totals.lineAmountsMinor[index],
                    createdAt = now,
                    updatedAt = now,
                )
            },
        )
    }

    private fun InvoiceEntity.withInput(input: InvoiceInput, totals: InvoiceTotals, now: Long) = copy(
        clientId = input.clientId,
        currency = input.currency,
        issueDate = input.issueDate,
        dueDate = input.dueDate,
        taxRateBp = input.taxRateBp,
        discountType = when (input.discount) {
            null -> null
            is Discount.Percent -> DiscountType.PERCENT
            is Discount.Amount -> DiscountType.AMOUNT
        },
        discountValue = when (val d = input.discount) {
            null -> null
            is Discount.Percent -> d.bp.toLong()
            is Discount.Amount -> d.minor
        },
        notes = input.notes,
        subtotalMinor = totals.subtotalMinor,
        discountMinor = totals.discountMinor,
        taxMinor = totals.taxMinor,
        totalMinor = totals.totalMinor,
        updatedAt = now,
    )

    private fun recalculate(invoice: InvoiceEntity, lines: List<LineItemEntity>): InvoiceTotals? {
        val discount = when (invoice.discountType) {
            null -> null
            DiscountType.PERCENT -> Discount.Percent(invoice.discountValue!!.toInt())
            DiscountType.AMOUNT -> Discount.Amount(invoice.discountValue!!)
        }
        val result = InvoiceCalculator.calculate(
            CurrencyCode.fromCode(invoice.currency)!!,
            lines.map { LineInput(it.quantityMilli, it.unitPriceMinor) },
            invoice.taxRateBp,
            discount,
        )
        return (result as? CalculationResult.Ok)?.totals
    }

    private fun calculate(input: InvoiceInput): CalculationResult = InvoiceCalculator.calculate(
        CurrencyCode.fromCode(input.currency)!!,
        input.lines.map { LineInput(it.quantityMilli, it.unitPriceMinor) },
        input.taxRateBp,
        input.discount,
    )

    /** Checks stored numbers are in range (I-6, I-8). Applies to drafts too. */
    private fun checkNumbers(input: InvoiceInput): MutableSet<Problem> {
        val problems = mutableSetOf<Problem>()
        if (CurrencyCode.fromCode(input.currency) == null) problems += Problem.UNKNOWN_CURRENCY
        if (input.lines.any { it.quantityMilli !in 1..MoneyLimits.MAX_QUANTITY_MILLI }) problems += Problem.BAD_QUANTITY
        if (input.lines.any { it.unitPriceMinor !in 0..MoneyLimits.MAX_AMOUNT_MINOR }) problems += Problem.BAD_PRICE
        if (input.taxRateBp !in 0..MoneyLimits.MAX_TAX_BP) problems += Problem.BAD_TAX_RATE
        when (val d = input.discount) {
            is Discount.Percent -> if (d.bp !in 0..MoneyLimits.MAX_DISCOUNT_BP) problems += Problem.BAD_DISCOUNT
            is Discount.Amount -> if (d.minor !in 0..MoneyLimits.MAX_AMOUNT_MINOR) problems += Problem.BAD_DISCOUNT
            null -> Unit
        }
        return problems
    }

    /** What issuing needs on top of valid numbers (I-4, I-5, I-11, UX_SPEC 3 "Issuing"). */
    private fun checkIssuable(clientId: Long?, issueDate: Long, dueDate: Long, lines: List<LineDraft>): MutableSet<Problem> {
        val problems = mutableSetOf<Problem>()
        if (clientId == null) problems += Problem.NO_CLIENT
        if (lines.isEmpty()) problems += Problem.NO_LINES
        if (lines.any { it.description.isBlank() }) problems += Problem.LINE_WITHOUT_DESCRIPTION
        if (dueDate < issueDate) problems += Problem.DUE_BEFORE_ISSUE
        return problems
    }
}
