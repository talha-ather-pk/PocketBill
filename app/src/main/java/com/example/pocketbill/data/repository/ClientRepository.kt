package com.example.pocketbill.data.repository

import androidx.room.withTransaction
import com.example.pocketbill.data.db.ClientEntity
import com.example.pocketbill.data.db.PocketBillDatabase
import com.example.pocketbill.domain.Outcome
import com.example.pocketbill.domain.Problem
import com.example.pocketbill.domain.text.normalizeForMatching
import com.example.pocketbill.domain.time.Clock
import kotlinx.coroutines.flow.Flow
import java.util.UUID

/** Client fields the client editor saves (UX_SPEC.md section 6). */
data class ClientInput(
    val name: String,
    val email: String? = null,
    val address: String? = null,
    val country: String? = null,
    val taxId: String? = null,
    val notes: String? = null,
)

/** The only code that reads or writes clients (ARCHITECTURE.md section 3). */
class ClientRepository(
    private val db: PocketBillDatabase,
    private val clock: Clock,
    private val newUuid: () -> String = { UUID.randomUUID().toString() },
) {
    private val clients = db.clientDao()
    private val invoices = db.invoiceDao()

    suspend fun get(id: Long): ClientEntity? = clients.get(id)

    fun observeActive(): Flow<List<ClientEntity>> = clients.observeActive()

    fun observeArchived(): Flow<List<ClientEntity>> = clients.observeArchived()

    fun observeRecent(): Flow<List<ClientEntity>> = clients.observeRecent()

    /** True if a client with the same name exists, ignoring case and accents (UX_SPEC 6 "Possible duplicate"). */
    suspend fun hasClientNamed(name: String): Boolean {
        val wanted = normalizeForMatching(name)
        return clients.allNames().any { normalizeForMatching(it) == wanted }
    }

    suspend fun create(input: ClientInput): Outcome<Long> {
        if (input.name.isBlank()) return Outcome.ValidationFailed(setOf(Problem.CLIENT_NAME_EMPTY))
        val now = clock.nowMillis()
        val id = clients.insert(
            ClientEntity(
                uuid = newUuid(), name = input.name.trim(), email = input.email.clean(), address = input.address.clean(),
                country = input.country.clean(), taxId = input.taxId.clean(), notes = input.notes.clean(),
                createdAt = now, updatedAt = now,
            ),
        )
        return Outcome.Success(id)
    }

    /** How many issued, unpaid invoices would be affected by "Also update unpaid invoices" (DATA_MODEL 5.4). */
    suspend fun unpaidIssuedCount(clientId: Long): Int = invoices.unpaidIssuedForClient(clientId).size

    /**
     * Saves changes to a client. With [alsoUpdateUnpaid], issued unpaid invoices take the new
     * details too; paid and void invoices never change (DATA_MODEL 5.4). Drafts always show live details.
     */
    suspend fun update(clientId: Long, input: ClientInput, alsoUpdateUnpaid: Boolean): Outcome<Unit> {
        if (input.name.isBlank()) return Outcome.ValidationFailed(setOf(Problem.CLIENT_NAME_EMPTY))
        val now = clock.nowMillis()
        return db.withTransaction {
            val existing = clients.get(clientId) ?: return@withTransaction Outcome.NotFound
            val updated = existing.copy(
                name = input.name.trim(), email = input.email.clean(), address = input.address.clean(),
                country = input.country.clean(), taxId = input.taxId.clean(), notes = input.notes.clean(),
                updatedAt = now,
            )
            clients.update(updated)
            if (alsoUpdateUnpaid) {
                val snapshot = updated.toSnapshotJson()
                invoices.unpaidIssuedForClient(clientId).forEach {
                    invoices.update(it.copy(toSnapshot = snapshot, updatedAt = now))
                }
            }
            Outcome.Success(Unit)
        }
    }

    suspend fun setArchived(clientId: Long, archived: Boolean): Outcome<Unit> = db.withTransaction {
        val existing = clients.get(clientId) ?: return@withTransaction Outcome.NotFound
        clients.update(existing.copy(archived = archived, updatedAt = clock.nowMillis()))
        Outcome.Success(Unit)
    }

    /** Deletes a client with no invoices at all. Otherwise only archiving is allowed (UX_SPEC 6). */
    suspend fun delete(clientId: Long): Outcome<Unit> = db.withTransaction {
        val existing = clients.get(clientId) ?: return@withTransaction Outcome.NotFound
        if (clients.countInvoices(clientId) > 0) return@withTransaction Outcome.NotAllowed(Problem.CLIENT_HAS_INVOICES)
        clients.delete(existing)
        Outcome.Success(Unit)
    }

    private fun String?.clean(): String? = this?.trim()?.ifEmpty { null }
}
