package com.example.pocketbill.ui.editor

import com.example.pocketbill.data.db.BusinessProfileEntity
import com.example.pocketbill.data.db.ClientEntity
import com.example.pocketbill.data.db.InvoiceEntity
import com.example.pocketbill.data.db.InvoiceWithLines
import com.example.pocketbill.data.repository.InvoiceInput
import com.example.pocketbill.data.repository.LineDraft
import com.example.pocketbill.domain.invoice.DiscountType
import com.example.pocketbill.domain.money.AmountParser
import com.example.pocketbill.domain.money.CalculationResult
import com.example.pocketbill.domain.money.CurrencyCode
import com.example.pocketbill.domain.money.Discount
import com.example.pocketbill.domain.money.InvoiceCalculator
import com.example.pocketbill.domain.money.LineInput
import com.example.pocketbill.domain.money.MoneyFormatter
import com.example.pocketbill.domain.money.ParseResult
import java.math.BigDecimal
import java.util.UUID

/** One line as typed (UX_SPEC 3). Numbers stay text until parsed (DATA_MODEL 6.3). */
data class LineForm(
    val uuid: String = UUID.randomUUID().toString(),
    val description: String = "",
    val quantity: String = "1",
    val price: String = "",
)

/** The editor's fields as typed. [dueDays] null means a custom [customDueDate]. */
data class EditorForm(
    val clientId: Long? = null,
    val currency: String = "USD",
    val issueDate: Long = 0,
    val dueDays: Int? = 14,
    val customDueDate: Long? = null,
    val taxRate: String = "0",
    val discountType: DiscountType? = null,
    val discountValue: String = "",
    val notes: String = "",
    val lines: List<LineForm> = listOf(LineForm()),
) {
    val dueDate: Long get() = customDueDate ?: (issueDate + (dueDays ?: 0))
}

/** Problems shown at a field when issuing (UX_SPEC 3 "Issuing"). Line problems carry the line's index. */
sealed interface EditorError {
    data object NoClient : EditorError
    data object NoLines : EditorError
    data class LineDescription(val index: Int) : EditorError
    data class LineQuantity(val index: Int) : EditorError
    data class LinePrice(val index: Int, val decimals: Int? = null) : EditorError
    data object TaxRate : EditorError
    data object DiscountValue : EditorError
    data object DueBeforeIssue : EditorError
    data object TooLarge : EditorError
}

/** Live totals for the sticky bar. [lineAmounts] is null for a line whose numbers can't be read yet. */
data class EditorTotals(
    val lineAmounts: List<Long?>,
    val subtotalMinor: Long,
    val discountMinor: Long,
    val taxMinor: Long,
    val totalMinor: Long,
    val tooLarge: Boolean = false,
)

private fun currencyOf(form: EditorForm) = CurrencyCode.fromCode(form.currency) ?: CurrencyCode.USD

/** An empty line: nothing typed apart from the default quantity. Dropped silently on issue. */
fun LineForm.isBlank(): Boolean = description.isBlank() && price.isBlank()

private fun parsedQuantity(line: LineForm): Long? =
    (AmountParser.parseQuantity(line.quantity.ifBlank { "1" }) as? ParseResult.Ok)?.value

private fun parsedPrice(line: LineForm, currency: CurrencyCode): Long? =
    if (line.price.isBlank()) null else (AmountParser.parseAmount(line.price, currency) as? ParseResult.Ok)?.value

private fun parsedTaxBp(form: EditorForm): Int? =
    (AmountParser.parsePercent(form.taxRate.ifBlank { "0" }) as? ParseResult.Ok)?.value?.toInt()

private fun parsedDiscount(form: EditorForm): Discount? {
    if (form.discountType == null || form.discountValue.isBlank()) return null
    return when (form.discountType) {
        DiscountType.PERCENT -> (AmountParser.parsePercent(form.discountValue) as? ParseResult.Ok)?.value
            ?.takeIf { it <= 10_000 }?.let { Discount.Percent(it.toInt()) }
        DiscountType.AMOUNT -> (AmountParser.parseAmount(form.discountValue, currencyOf(form)) as? ParseResult.Ok)?.value
            ?.let { Discount.Amount(it) }
    }
}

/**
 * Totals as the user types (UX_SPEC 3: "Totals update live"). Lines whose quantity or price can't
 * be read yet count as zero until fixed; the money rules themselves are DATA_MODEL 6.4.
 */
fun EditorForm.totals(): EditorTotals {
    val currency = currencyOf(this)
    val readable = lines.map { line ->
        val q = parsedQuantity(line)
        val p = parsedPrice(line, currency)
        if (q != null && p != null) LineInput(q, p) else null
    }
    val result = InvoiceCalculator.calculate(currency, readable.filterNotNull(), parsedTaxBp(this) ?: 0, parsedDiscount(this))
    if (result !is CalculationResult.Ok) return EditorTotals(readable.map { null }, 0, 0, 0, 0, tooLarge = true)
    val amounts = result.totals.lineAmountsMinor.iterator()
    return EditorTotals(
        lineAmounts = readable.map { if (it != null) amounts.next() else null },
        subtotalMinor = result.totals.subtotalMinor,
        discountMinor = result.totals.discountMinor,
        taxMinor = result.totals.taxMinor,
        totalMinor = result.totals.totalMinor,
    )
}

/** Everything that would stop this invoice being issued, in screen order. */
fun EditorForm.issueErrors(): List<EditorError> {
    val currency = currencyOf(this)
    val errors = mutableListOf<EditorError>()
    if (clientId == null) errors += EditorError.NoClient
    val kept = lines.withIndex().filterNot { it.value.isBlank() }
    if (kept.isEmpty()) errors += EditorError.NoLines
    kept.forEach { (i, line) ->
        if (line.description.isBlank()) errors += EditorError.LineDescription(i)
        if (parsedQuantity(line) == null) errors += EditorError.LineQuantity(i)
        if (line.price.isBlank()) {
            errors += EditorError.LinePrice(i)
        } else when (val p = AmountParser.parseAmount(line.price, currency)) {
            is ParseResult.TooManyDecimals -> errors += EditorError.LinePrice(i, p.allowed)
            is ParseResult.Ok -> Unit
            else -> errors += EditorError.LinePrice(i)
        }
    }
    if (parsedTaxBp(this) == null) errors += EditorError.TaxRate
    if (discountType != null && discountValue.isNotBlank() && parsedDiscount(this) == null) errors += EditorError.DiscountValue
    if (dueDate < issueDate) errors += EditorError.DueBeforeIssue
    if (errors.isEmpty() && totals().tooLarge) errors += EditorError.TooLarge
    return errors
}

/**
 * What the repository saves. Blank lines are dropped. For autosaved drafts, numbers that can't be
 * read yet are saved as quantity 1 / price 0 so the draft is never refused; issuing checks them
 * properly first ([issueErrors]).
 */
fun EditorForm.toInput(): InvoiceInput {
    val currency = currencyOf(this)
    return InvoiceInput(
        clientId = clientId,
        currency = currency.name,
        issueDate = issueDate,
        dueDate = maxOf(dueDate, issueDate),
        lines = lines.filterNot { it.isBlank() }.map { line ->
            LineDraft(
                description = line.description.trim(),
                quantityMilli = parsedQuantity(line) ?: 1_000,
                unitPriceMinor = parsedPrice(line, currency) ?: 0,
                uuid = line.uuid,
            )
        },
        taxRateBp = parsedTaxBp(this) ?: 0,
        discount = parsedDiscount(this),
        notes = notes.trim(),
    )
}

/** The form for a saved invoice (editing a draft or an issued invoice). */
fun InvoiceWithLines.toForm(): EditorForm {
    val inv = invoice
    val currency = CurrencyCode.fromCode(inv.currency) ?: CurrencyCode.USD
    val days = (inv.dueDate - inv.issueDate).toInt()
    return EditorForm(
        clientId = inv.clientId,
        currency = currency.name,
        issueDate = inv.issueDate,
        dueDays = days.takeIf { it in TERMS_CHIPS },
        customDueDate = inv.dueDate.takeUnless { days in TERMS_CHIPS },
        taxRate = percentText(inv.taxRateBp),
        discountType = inv.discountType,
        discountValue = when (inv.discountType) {
            DiscountType.PERCENT -> percentText(inv.discountValue!!.toInt())
            DiscountType.AMOUNT -> MoneyFormatter.formatPlain(inv.discountValue!!, currency)
            null -> ""
        },
        notes = inv.notes,
        lines = lines.map {
            LineForm(
                uuid = it.uuid,
                description = it.description,
                quantity = BigDecimal.valueOf(it.quantityMilli, 3).stripTrailingZeros().toPlainString(),
                price = MoneyFormatter.formatPlain(it.unitPriceMinor, currency),
            )
        }.ifEmpty { listOf(LineForm()) },
    )
}

/**
 * A new draft copied from [source] (Duplicate, F-13): same client, currency, items, tax, discount
 * and notes; new dates by the same terms; new line identities; no number (N-9).
 */
fun InvoiceWithLines.toDuplicateForm(today: Long): EditorForm {
    val copy = toForm()
    val days = (invoice.dueDate - invoice.issueDate).toInt()
    return copy.copy(
        issueDate = today,
        dueDays = days,
        customDueDate = null,
        lines = copy.lines.map { it.copy(uuid = UUID.randomUUID().toString()) },
    )
}

/** Payment-term chips (UX_SPEC 3): on receipt, 7, 14, 30 days. Anything else is a custom date. */
val TERMS_CHIPS = listOf(0, 7, 14, 30)

/** What picking a client fills in (D-015). */
data class ClientDefaults(val currency: String, val dueDays: Int, val taxBp: Int)

/**
 * D-015 order: the client's preference, then the client's last invoice, then the business defaults.
 * Tax has no client preference, so it comes from the last invoice, then the business default.
 */
fun resolveDefaults(client: ClientEntity?, lastInvoice: InvoiceEntity?, profile: BusinessProfileEntity?): ClientDefaults {
    val fallbackCurrency = profile?.defaultCurrency ?: CurrencyCode.USD.name
    return ClientDefaults(
        currency = client?.preferredCurrency ?: lastInvoice?.currency ?: fallbackCurrency,
        dueDays = client?.preferredTermsDays ?: lastInvoice?.let { (it.dueDate - it.issueDate).toInt() } ?: profile?.defaultTermsDays ?: 14,
        taxBp = lastInvoice?.taxRateBp ?: profile?.defaultTaxRateBp ?: 0,
    )
}

/** 750 → "7.5", 0 → "0". */
fun percentText(bp: Int): String = BigDecimal.valueOf(bp.toLong(), 2).stripTrailingZeros().toPlainString()
