package com.example.pocketbill.data.repository

import androidx.room.withTransaction
import com.example.pocketbill.data.db.ClientCount
import com.example.pocketbill.data.db.ClientEntity
import com.example.pocketbill.data.db.ClientOwed
import com.example.pocketbill.data.db.InvoiceEntity
import com.example.pocketbill.data.db.PocketBillDatabase
import com.example.pocketbill.domain.Outcome
import com.example.pocketbill.domain.Problem
import com.example.pocketbill.domain.money.CurrencyCode
import com.example.pocketbill.domain.text.normalizeForMatching
import com.example.pocketbill.domain.time.Clock
import kotlinx.coroutines.flow.Flow
import java.util.UUID

/** Client fields the client editor saves (UX_SPEC.md section 6, D-015). */
data class ClientInput(
    val name: String,
    val email: String? = null,
    val address: String? = null,
    val country: String? = null,
    val taxId: String? = null,
    val notes: String? = null,
    /** Optional currency for new invoices to this client (D-015). */
    val preferredCurrency: String? = null,
    /** Optional payment terms in days for new invoices to this client (D-015). */
    val preferredTermsDays: Int? = null,
)

/** Longest payment term accepted for a client preference. */
const val MAX_TERMS_DAYS = 365

/**
 * What the client screens need from storage. [ClientRepository] is the real one; tests use a fake
 * (ARCHITECTURE.md section 5: ViewModels tested with fake repositories).
 */
interface ClientStore {
    fun observeActive(): Flow<List<ClientEntity>>
    fun observeArchived(): Flow<List<ClientEntity>>
    fun observeOwedByClient(): Flow<List<ClientOwed>>
    fun observeOverdueByClient(): Flow<List<ClientCount>>
    fun observe(clientId: Long): Flow<ClientEntity?>
    fun observeInvoices(clientId: Long): Flow<List<InvoiceEntity>>

    suspend fun get(clientId: Long): ClientEntity?

    /** Another client whose name matches [name] ignoring case and accents, or null (UX_SPEC 6 "Possible duplicate"). */
    suspend fun findSameName(name: String, excludeId: Long = 0): ClientEntity?

    suspend fun create(input: ClientInput): Outcome<Long>
    suspend fun update(clientId: Long, input: ClientInput, alsoUpdateUnpaid: Boolean): Outcome<Unit>
    suspend fun unpaidIssuedCount(clientId: Long): Int
    suspend fun invoiceCount(clientId: Long): Int
    suspend fun setArchived(clientId: Long, archived: Boolean): Outcome<Unit>
    suspend fun delete(clientId: Long): Outcome<Unit>
}

/** The only code that reads or writes clients (ARCHITECTURE.md section 3). */
class ClientRepository(
    private val db: PocketBillDatabase,
    private val clock: Clock,
    private val newUuid: () -> String = { UUID.randomUUID().toString() },
) : ClientStore {
    private val clients = db.clientDao()
    private val invoices = db.invoiceDao()

    override suspend fun get(clientId: Long): ClientEntity? = clients.get(clientId)

    override fun observe(clientId: Long): Flow<ClientEntity?> = clients.observe(clientId)

    override fun observeActive(): Flow<List<ClientEntity>> = clients.observeActive()

    override fun observeArchived(): Flow<List<ClientEntity>> = clients.observeArchived()

    fun observeRecent(): Flow<List<ClientEntity>> = clients.observeRecent()

    override fun observeOwedByClient(): Flow<List<ClientOwed>> = invoices.observeOwedByClient()

    override fun observeOverdueByClient(): Flow<List<ClientCount>> = invoices.observeOverdueByClient(clock.todayEpochDay())

    override fun observeInvoices(clientId: Long): Flow<List<InvoiceEntity>> = invoices.observeForClient(clientId)

    override suspend fun findSameName(name: String, excludeId: Long): ClientEntity? {
        val wanted = normalizeForMatching(name)
        if (wanted.isEmpty()) return null
        return clients.all().firstOrNull { it.id != excludeId && normalizeForMatching(it.name) == wanted }
    }

    /** True if a client with the same name exists, ignoring case and accents. */
    suspend fun hasClientNamed(name: String): Boolean = findSameName(name) != null

    override suspend fun create(input: ClientInput): Outcome<Long> {
        val problems = validateClient(input)
        if (problems.isNotEmpty()) return Outcome.ValidationFailed(problems)
        val now = clock.nowMillis()
        val id = clients.insert(
            ClientEntity(uuid = newUuid(), name = input.name.trim(), createdAt = now, updatedAt = now).withInput(input, now),
        )
        return Outcome.Success(id)
    }

    /** How many issued, unpaid invoices would be affected by "Also update unpaid invoices" (DATA_MODEL 5.4). */
    override suspend fun unpaidIssuedCount(clientId: Long): Int = invoices.unpaidIssuedForClient(clientId).size

    override suspend fun invoiceCount(clientId: Long): Int = clients.countInvoices(clientId)

    /**
     * Saves changes to a client. With [alsoUpdateUnpaid], issued unpaid invoices take the new
     * details too; paid and void invoices never change (DATA_MODEL 5.4). Drafts always show live details.
     */
    override suspend fun update(clientId: Long, input: ClientInput, alsoUpdateUnpaid: Boolean): Outcome<Unit> {
        val problems = validateClient(input)
        if (problems.isNotEmpty()) return Outcome.ValidationFailed(problems)
        val now = clock.nowMillis()
        return db.withTransaction {
            val existing = clients.get(clientId) ?: return@withTransaction Outcome.NotFound
            val updated = existing.withInput(input, now)
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

    override suspend fun setArchived(clientId: Long, archived: Boolean): Outcome<Unit> = db.withTransaction {
        val existing = clients.get(clientId) ?: return@withTransaction Outcome.NotFound
        clients.update(existing.copy(archived = archived, updatedAt = clock.nowMillis()))
        Outcome.Success(Unit)
    }

    /** Deletes a client with no invoices at all. Otherwise only archiving is allowed (UX_SPEC 6). */
    override suspend fun delete(clientId: Long): Outcome<Unit> = db.withTransaction {
        val existing = clients.get(clientId) ?: return@withTransaction Outcome.NotFound
        if (clients.countInvoices(clientId) > 0) return@withTransaction Outcome.NotAllowed(Problem.CLIENT_HAS_INVOICES)
        clients.delete(existing)
        Outcome.Success(Unit)
    }

    private fun ClientEntity.withInput(input: ClientInput, now: Long) = copy(
        name = input.name.trim(),
        email = input.email.clean(),
        address = input.address.clean(),
        country = input.country.clean(),
        taxId = input.taxId.clean(),
        notes = input.notes.clean(),
        preferredCurrency = input.preferredCurrency.clean(),
        preferredTermsDays = input.preferredTermsDays,
        updatedAt = now,
    )

    private fun String?.clean(): String? = this?.trim()?.ifEmpty { null }
}

/** Rules for client fields (UX_SPEC 6). Shared by the repository and the client editor. */
fun validateClient(input: ClientInput): Set<Problem> {
    val problems = mutableSetOf<Problem>()
    if (input.name.isBlank()) problems += Problem.CLIENT_NAME_EMPTY
    val email = input.email?.trim().orEmpty()
    if (email.isNotEmpty() && !isPlausibleEmail(email)) problems += Problem.BAD_EMAIL
    val currency = input.preferredCurrency?.trim().orEmpty()
    if (currency.isNotEmpty() && CurrencyCode.fromCode(currency) == null) problems += Problem.UNKNOWN_CURRENCY
    if (input.preferredTermsDays != null && input.preferredTermsDays !in 0..MAX_TERMS_DAYS) problems += Problem.BAD_TERMS
    return problems
}

/** Something@something.something, with no spaces. Strict enough to catch typos, loose enough for real addresses. */
fun isPlausibleEmail(email: String): Boolean = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$").matches(email)
