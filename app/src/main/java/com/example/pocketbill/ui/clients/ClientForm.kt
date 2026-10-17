package com.example.pocketbill.ui.clients

import com.example.pocketbill.data.db.ClientEntity
import com.example.pocketbill.data.repository.ClientInput
import com.example.pocketbill.data.repository.validateClient
import com.example.pocketbill.domain.Problem

/** Client fields as typed (UX_SPEC 6, D-015). */
data class ClientForm(
    val name: String = "",
    val email: String = "",
    val address: String = "",
    val country: String = "",
    val taxId: String = "",
    val notes: String = "",
    /** Null = no preferred currency. */
    val preferredCurrency: String? = null,
    /** Null = no preferred terms. */
    val preferredTermsDays: Int? = null,
)

enum class ClientField { NAME, EMAIL, CURRENCY, TERMS }

/** Terms offered on the client form, after "No preference" (UX_SPEC 3 chips). */
val CLIENT_TERMS_CHOICES = listOf(0, 7, 14, 30)

fun ClientForm.toInput() = ClientInput(
    name = name,
    email = email,
    address = address,
    country = country,
    taxId = taxId,
    notes = notes,
    preferredCurrency = preferredCurrency,
    preferredTermsDays = preferredTermsDays,
)

/** Field errors, using the same rules the repository enforces (validateClient). */
fun ClientForm.errors(): Map<ClientField, Problem> = validateClient(toInput()).associateBy {
    when (it) {
        Problem.CLIENT_NAME_EMPTY -> ClientField.NAME
        Problem.BAD_EMAIL -> ClientField.EMAIL
        Problem.UNKNOWN_CURRENCY -> ClientField.CURRENCY
        else -> ClientField.TERMS
    }
}

fun ClientEntity.toForm() = ClientForm(
    name = name,
    email = email.orEmpty(),
    address = address.orEmpty(),
    country = country.orEmpty(),
    taxId = taxId.orEmpty(),
    notes = notes.orEmpty(),
    preferredCurrency = preferredCurrency,
    preferredTermsDays = preferredTermsDays,
)

/**
 * True if saving [form] changes details that issued invoices freeze (the ToSnapshot fields,
 * DATA_MODEL 5.2), so the "Update unpaid invoices too?" question applies (5.4).
 * Notes and preferences are never printed on invoices.
 */
fun ClientEntity.frozenDetailsDiffer(form: ClientForm): Boolean {
    fun String?.norm() = this?.trim().orEmpty()
    return name.norm() != form.name.norm() || email.norm() != form.email.norm() ||
        address.norm() != form.address.norm() || country.norm() != form.country.norm() ||
        taxId.norm() != form.taxId.norm()
}
