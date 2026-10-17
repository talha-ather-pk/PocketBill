package com.example.pocketbill.ui.settings

import com.example.pocketbill.data.db.BusinessProfileEntity
import com.example.pocketbill.data.repository.ProfileInput
import com.example.pocketbill.domain.invoice.PaperSize
import com.example.pocketbill.domain.money.AmountParser
import com.example.pocketbill.domain.money.CurrencyCode
import com.example.pocketbill.domain.money.ParseResult
import java.math.BigDecimal

/** Fields of the business profile as typed (MVP_SPEC F-01). Numbers stay text until saved (DATA_MODEL 6.3). */
data class ProfileForm(
    val name: String = "",
    val currency: String = "USD",
    val paymentInstructions: String = "",
    val contactLines: String = "",
    val address: String = "",
    val taxId: String = "",
    val termsDays: Int = 14,
    val taxRate: String = "0",
    val prefix: String = "INV-",
    val nextNumber: String = "1",
    val paperSize: PaperSize = PaperSize.A4,
    /** Lowest allowed "next invoice number": numbers are never reused (N-4). */
    val minNextNumber: Long = 1,
)

enum class ProfileField { NAME, TAX_RATE, PREFIX, NEXT_NUMBER }

enum class ProfileError { REQUIRED, BAD_PERCENT, TOO_LONG, BAD_NUMBER, NUMBER_TOO_LOW }

sealed interface ProfileFormResult {
    data class Valid(val input: ProfileInput) : ProfileFormResult
    data class Invalid(val errors: Map<ProfileField, ProfileError>) : ProfileFormResult
}

/** Payment terms offered as chips (UX_SPEC 3): on receipt, 7, 14, 30 days. */
val TERMS_CHOICES = listOf(0, 7, 14, 30)

const val MAX_PREFIX_LENGTH = 10

/** Checks the form and turns it into what the repository saves. */
fun ProfileForm.validate(): ProfileFormResult {
    val errors = mutableMapOf<ProfileField, ProfileError>()
    if (name.isBlank()) errors[ProfileField.NAME] = ProfileError.REQUIRED

    val taxBp = when (val parsed = AmountParser.parsePercent(taxRate.ifBlank { "0" })) {
        is ParseResult.Ok -> parsed.value.toInt()
        else -> {
            errors[ProfileField.TAX_RATE] = ProfileError.BAD_PERCENT
            0
        }
    }

    if (prefix.length > MAX_PREFIX_LENGTH) errors[ProfileField.PREFIX] = ProfileError.TOO_LONG

    val next = nextNumber.trim().toLongOrNull()
    when {
        next == null || next < 1 -> errors[ProfileField.NEXT_NUMBER] = ProfileError.BAD_NUMBER
        next < minNextNumber -> errors[ProfileField.NEXT_NUMBER] = ProfileError.NUMBER_TOO_LOW
    }

    if (errors.isNotEmpty()) return ProfileFormResult.Invalid(errors)
    return ProfileFormResult.Valid(
        ProfileInput(
            name = name.trim(),
            defaultCurrency = currency,
            contactLines = contactLines.trim(),
            address = address.trim(),
            taxId = taxId.trim().ifEmpty { null },
            paymentInstructions = paymentInstructions.trim(),
            defaultTermsDays = termsDays,
            defaultTaxRateBp = taxBp,
            invoicePrefix = prefix,
            nextInvoiceNumber = next!!,
            paperSize = paperSize,
        ),
    )
}

/** The form for an existing profile. */
fun BusinessProfileEntity.toForm(): ProfileForm = ProfileForm(
    name = name,
    currency = defaultCurrency,
    paymentInstructions = paymentInstructions,
    contactLines = contactLines,
    address = address,
    taxId = taxId.orEmpty(),
    termsDays = defaultTermsDays,
    taxRate = BigDecimal.valueOf(defaultTaxRateBp.toLong(), 2).stripTrailingZeros().toPlainString(),
    prefix = invoicePrefix,
    nextNumber = nextInvoiceNumber.toString(),
    paperSize = paperSize,
    minNextNumber = nextInvoiceNumber,
)

/** True if saving [input] would change details that issued invoices freeze (DATA_MODEL 5.2). */
fun BusinessProfileEntity.frozenDetailsDiffer(input: ProfileInput): Boolean =
    name != input.name || contactLines != input.contactLines || address != input.address ||
        taxId != input.taxId || paymentInstructions != input.paymentInstructions

/** DATA_MODEL 5.5: a change of payment details pre-selects "Also update unpaid invoices". */
fun BusinessProfileEntity.paymentDetailsDiffer(input: ProfileInput): Boolean =
    paymentInstructions != input.paymentInstructions

/** First-launch default currency: the phone's region's currency if PocketBill supports it, else USD. */
fun suggestedCurrency(regionCurrencyCode: String?): String =
    regionCurrencyCode?.let { CurrencyCode.fromCode(it) }?.name ?: CurrencyCode.USD.name
