package com.example.pocketbill.domain

/**
 * Result of a repository operation (ARCHITECTURE.md section 4, "Errors"). ViewModels turn each
 * failure into the plain message written in UX_SPEC.md.
 */
sealed interface Outcome<out T> {
    data class Success<T>(val value: T) : Outcome<T>

    sealed interface Failure : Outcome<Nothing>

    /** The input breaks a rule; [reasons] say which (e.g. NO_LINES, DUE_BEFORE_ISSUE). */
    data class ValidationFailed(val reasons: Set<Problem>) : Failure

    /** The record doesn't exist (any more). */
    data object NotFound : Failure

    /** The operation isn't allowed in the record's current state (e.g. editing a paid invoice, I-9). */
    data class NotAllowed(val reason: Problem) : Failure

    /** An amount would exceed the money limit (DATA_MODEL M-6). */
    data object AmountTooLarge : Failure
}

/** Why an operation was refused. Each maps to a message in the UI. */
enum class Problem {
    NO_CLIENT,
    NO_LINES,
    LINE_WITHOUT_DESCRIPTION,
    BAD_QUANTITY,
    BAD_PRICE,
    BAD_TAX_RATE,
    BAD_DISCOUNT,
    DUE_BEFORE_ISSUE,
    UNKNOWN_CURRENCY,
    NO_BUSINESS_PROFILE,
    NOT_A_DRAFT,
    READ_ONLY,
    CLIENT_HAS_INVOICES,
    CLIENT_NAME_EMPTY,
    BAD_EMAIL,
    BAD_TERMS,
}
