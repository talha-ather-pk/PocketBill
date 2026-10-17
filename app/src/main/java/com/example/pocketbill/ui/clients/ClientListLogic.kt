package com.example.pocketbill.ui.clients

import com.example.pocketbill.data.db.ClientCount
import com.example.pocketbill.data.db.ClientEntity
import com.example.pocketbill.data.db.ClientOwed
import com.example.pocketbill.domain.text.normalizeForMatching

/** Client list order (UX_SPEC 6): A–Z, or most owed first. */
enum class ClientSort { NAME, MOST_OWED }

/** An amount in one currency. Amounts in different currencies are never added together. */
data class Owed(val currency: String, val totalMinor: Long)

/** One row of the client list. */
data class ClientRow(
    val id: Long,
    val name: String,
    val country: String?,
    val unpaidCount: Int,
    val overdueCount: Int,
    /** Largest amount first. Empty when nothing is owed. */
    val owed: List<Owed>,
    val archived: Boolean,
)

/**
 * Builds the rows the client list shows: search matches name, email and country, ignoring case
 * and accents (UX_SPEC 6). "Most owed" sorts by the number of unpaid invoices, then by name, since
 * amounts in different currencies can't be compared.
 */
fun buildClientRows(
    clients: List<ClientEntity>,
    owed: List<ClientOwed>,
    overdue: List<ClientCount>,
    query: String,
    sort: ClientSort,
): List<ClientRow> {
    val owedByClient = owed.groupBy { it.clientId }
    val overdueByClient = overdue.associate { it.clientId to it.count }
    val wanted = normalizeForMatching(query)
    return clients
        .filter { client ->
            wanted.isEmpty() || listOfNotNull(client.name, client.email, client.country)
                .any { normalizeForMatching(it).contains(wanted) }
        }
        .map { client ->
            val mine = owedByClient[client.id].orEmpty()
            ClientRow(
                id = client.id,
                name = client.name,
                country = client.country,
                unpaidCount = mine.sumOf { it.invoiceCount },
                overdueCount = overdueByClient[client.id] ?: 0,
                owed = mine.sortedByDescending { it.totalMinor }.map { Owed(it.currency, it.totalMinor) },
                archived = client.archived,
            )
        }
        .let { rows ->
            val byName = compareBy<ClientRow> { normalizeForMatching(it.name) }
            when (sort) {
                ClientSort.NAME -> rows.sortedWith(byName)
                ClientSort.MOST_OWED -> rows.sortedWith(
                    compareByDescending<ClientRow> { it.overdueCount }
                        .thenByDescending { it.unpaidCount }
                        .then(byName),
                )
            }
        }
}
