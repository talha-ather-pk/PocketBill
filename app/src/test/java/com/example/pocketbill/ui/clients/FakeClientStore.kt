package com.example.pocketbill.ui.clients

import com.example.pocketbill.data.db.ClientCount
import com.example.pocketbill.data.db.ClientEntity
import com.example.pocketbill.data.db.ClientOwed
import com.example.pocketbill.data.db.InvoiceEntity
import com.example.pocketbill.data.repository.ClientInput
import com.example.pocketbill.data.repository.ClientStore
import com.example.pocketbill.data.repository.validateClient
import com.example.pocketbill.domain.Outcome
import com.example.pocketbill.domain.Problem
import com.example.pocketbill.domain.text.normalizeForMatching
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** In-memory [ClientStore] for ViewModel tests (ARCHITECTURE.md section 5). Applies the same validation as the real one. */
class FakeClientStore : ClientStore {
    val clients = MutableStateFlow<List<ClientEntity>>(emptyList())
    val owed = MutableStateFlow<List<ClientOwed>>(emptyList())
    val overdue = MutableStateFlow<List<ClientCount>>(emptyList())
    val invoices = MutableStateFlow<List<InvoiceEntity>>(emptyList())
    var unpaidIssued: Map<Long, Int> = emptyMap()

    /** Records of update() calls: (clientId, input, alsoUpdateUnpaid). */
    val updates = mutableListOf<Triple<Long, ClientInput, Boolean>>()
    private var nextId = 1L

    fun add(
        name: String,
        email: String? = null,
        country: String? = null,
        archived: Boolean = false,
        preferredCurrency: String? = null,
        preferredTermsDays: Int? = null,
    ): ClientEntity {
        val client = ClientEntity(
            id = nextId++, uuid = "u$nextId", name = name, email = email, country = country,
            preferredCurrency = preferredCurrency, preferredTermsDays = preferredTermsDays,
            archived = archived, createdAt = 0, updatedAt = 0,
        )
        clients.value = clients.value + client
        return client
    }

    override fun observeActive(): Flow<List<ClientEntity>> = clients.map { list -> list.filter { !it.archived } }
    override fun observeRecent(): Flow<List<ClientEntity>> =
        clients.map { list -> list.filter { !it.archived && it.lastInvoicedAt != null }.sortedByDescending { it.lastInvoicedAt }.take(3) }
    override fun observeArchived(): Flow<List<ClientEntity>> = clients.map { list -> list.filter { it.archived } }
    override fun observeOwedByClient(): Flow<List<ClientOwed>> = owed
    override fun observeOverdueByClient(): Flow<List<ClientCount>> = overdue
    override fun observe(clientId: Long): Flow<ClientEntity?> = clients.map { list -> list.firstOrNull { it.id == clientId } }
    override fun observeInvoices(clientId: Long): Flow<List<InvoiceEntity>> = invoices.map { list -> list.filter { it.clientId == clientId } }

    override suspend fun get(clientId: Long): ClientEntity? = clients.value.firstOrNull { it.id == clientId }

    override suspend fun findSameName(name: String, excludeId: Long): ClientEntity? =
        clients.value.firstOrNull { it.id != excludeId && normalizeForMatching(it.name) == normalizeForMatching(name) }

    override suspend fun create(input: ClientInput): Outcome<Long> {
        val problems = validateClient(input)
        if (problems.isNotEmpty()) return Outcome.ValidationFailed(problems)
        return Outcome.Success(add(input.name.trim(), input.email, input.country).id)
    }

    override suspend fun update(clientId: Long, input: ClientInput, alsoUpdateUnpaid: Boolean): Outcome<Unit> {
        val problems = validateClient(input)
        if (problems.isNotEmpty()) return Outcome.ValidationFailed(problems)
        updates += Triple(clientId, input, alsoUpdateUnpaid)
        clients.value = clients.value.map { if (it.id == clientId) it.copy(name = input.name.trim(), email = input.email, notes = input.notes) else it }
        return Outcome.Success(Unit)
    }

    override suspend fun unpaidIssuedCount(clientId: Long): Int = unpaidIssued[clientId] ?: 0
    override suspend fun invoiceCount(clientId: Long): Int = invoices.value.count { it.clientId == clientId }

    override suspend fun setArchived(clientId: Long, archived: Boolean): Outcome<Unit> {
        clients.value = clients.value.map { if (it.id == clientId) it.copy(archived = archived) else it }
        return Outcome.Success(Unit)
    }

    override suspend fun delete(clientId: Long): Outcome<Unit> {
        if (invoiceCount(clientId) > 0) return Outcome.NotAllowed(Problem.CLIENT_HAS_INVOICES)
        clients.value = clients.value.filter { it.id != clientId }
        return Outcome.Success(Unit)
    }
}
