package com.example.pocketbill.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import com.example.pocketbill.domain.invoice.InvoiceStatus
import kotlinx.coroutines.flow.Flow

// Queries from DATA_MODEL.md section 4. Reads that screens watch return Flow, so screens update
// after any write. Only repositories use these DAOs (ARCHITECTURE.md section 3).

@Dao
interface ProfileDao {
    @Query("SELECT * FROM business_profile WHERE id = 1")
    fun observe(): Flow<BusinessProfileEntity?>

    @Query("SELECT * FROM business_profile WHERE id = 1")
    suspend fun get(): BusinessProfileEntity?

    @Upsert
    suspend fun upsert(profile: BusinessProfileEntity)
}

@Dao
interface ClientDao {
    @Insert
    suspend fun insert(client: ClientEntity): Long

    @Update
    suspend fun update(client: ClientEntity)

    @Delete
    suspend fun delete(client: ClientEntity)

    @Query("SELECT * FROM client WHERE id = :id")
    suspend fun get(id: Long): ClientEntity?

    @Query("SELECT * FROM client WHERE id = :id")
    fun observe(id: Long): Flow<ClientEntity?>

    @Query("SELECT * FROM client")
    suspend fun all(): List<ClientEntity>

    @Query("SELECT * FROM client WHERE archived = 0 ORDER BY name COLLATE NOCASE")
    fun observeActive(): Flow<List<ClientEntity>>

    @Query("SELECT * FROM client WHERE archived = 1 ORDER BY name COLLATE NOCASE")
    fun observeArchived(): Flow<List<ClientEntity>>

    /** Recent-client chips in the editor (UX_SPEC 3). */
    @Query(
        "SELECT * FROM client WHERE archived = 0 AND lastInvoicedAt IS NOT NULL " +
            "ORDER BY lastInvoicedAt DESC LIMIT :limit",
    )
    fun observeRecent(limit: Int = 3): Flow<List<ClientEntity>>

    @Query("SELECT COUNT(*) FROM invoice WHERE clientId = :clientId")
    suspend fun countInvoices(clientId: Long): Int
}

@Dao
interface InvoiceDao {
    @Insert
    suspend fun insert(invoice: InvoiceEntity): Long

    @Update
    suspend fun update(invoice: InvoiceEntity)

    @Delete
    suspend fun delete(invoice: InvoiceEntity)

    @Query("SELECT * FROM invoice WHERE id = :id")
    suspend fun get(id: Long): InvoiceEntity?

    @Transaction
    @Query("SELECT * FROM invoice WHERE id = :id")
    suspend fun getWithLines(id: Long): InvoiceWithLines?

    @Transaction
    @Query("SELECT * FROM invoice WHERE id = :id")
    fun observeWithLines(id: Long): Flow<InvoiceWithLines?>

    /** The client's most recent invoice, any status: source of their defaults (D-015). */
    @Query("SELECT * FROM invoice WHERE clientId = :clientId ORDER BY issueDate DESC, id DESC LIMIT 1")
    suspend fun lastForClient(clientId: Long): InvoiceEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM invoice WHERE number = :number)")
    suspend fun numberExists(number: String): Boolean

    // Line items
    @Insert
    suspend fun insertLines(lines: List<LineItemEntity>)

    @Query("DELETE FROM line_item WHERE invoiceId = :invoiceId")
    suspend fun deleteLines(invoiceId: Long)

    // Home (UX_SPEC 2)
    @Query(
        "SELECT currency, SUM(totalMinor) AS totalMinor FROM invoice " +
            "WHERE status = 'ISSUED' GROUP BY currency ORDER BY COUNT(*) DESC, currency",
    )
    fun observeOutstanding(): Flow<List<CurrencyTotal>>

    @Query("SELECT COUNT(*) FROM invoice WHERE status = 'ISSUED' AND dueDate < :today")
    fun observeOverdueCount(today: Long): Flow<Int>

    @Query("SELECT * FROM invoice WHERE status = 'ISSUED' AND lastSentAt IS NULL ORDER BY issueDate, id")
    fun observeIssuedUnsent(): Flow<List<InvoiceEntity>>

    @Query("SELECT * FROM invoice ORDER BY updatedAt DESC, id DESC LIMIT :limit")
    fun observeRecent(limit: Int = 5): Flow<List<InvoiceEntity>>

    // Invoices list (UX_SPEC 5)
    @Query("SELECT * FROM invoice ORDER BY issueDate DESC, id DESC")
    fun observeAll(): Flow<List<InvoiceEntity>>

    @Query("SELECT * FROM invoice WHERE status = :status ORDER BY dueDate, id")
    fun observeByStatusDueSoonest(status: InvoiceStatus): Flow<List<InvoiceEntity>>

    @Query("SELECT * FROM invoice WHERE status = 'ISSUED' AND dueDate < :today ORDER BY dueDate, id")
    fun observeOverdue(today: Long): Flow<List<InvoiceEntity>>

    @Query("SELECT * FROM invoice WHERE status = 'PAID' ORDER BY paidDate DESC, id DESC")
    fun observePaid(): Flow<List<InvoiceEntity>>

    @Query("SELECT * FROM invoice WHERE status = 'DRAFT' ORDER BY updatedAt DESC, id DESC")
    fun observeDrafts(): Flow<List<InvoiceEntity>>

    @Query("SELECT * FROM invoice WHERE clientId = :clientId ORDER BY issueDate DESC, id DESC")
    fun observeForClient(clientId: Long): Flow<List<InvoiceEntity>>

    // Clients list and detail (UX_SPEC 6): owed per client per currency, never added across currencies
    @Query(
        "SELECT clientId, currency, SUM(totalMinor) AS totalMinor, COUNT(*) AS invoiceCount FROM invoice " +
            "WHERE status = 'ISSUED' AND clientId IS NOT NULL GROUP BY clientId, currency",
    )
    fun observeOwedByClient(): Flow<List<ClientOwed>>

    @Query(
        "SELECT clientId, COUNT(*) AS count FROM invoice " +
            "WHERE status = 'ISSUED' AND dueDate < :today AND clientId IS NOT NULL GROUP BY clientId",
    )
    fun observeOverdueByClient(today: Long): Flow<List<ClientCount>>

    // Snapshot updates (DATA_MODEL 5.4–5.5)
    @Query("SELECT * FROM invoice WHERE status = 'ISSUED' AND clientId = :clientId")
    suspend fun unpaidIssuedForClient(clientId: Long): List<InvoiceEntity>

    @Query("SELECT * FROM invoice WHERE status = 'ISSUED'")
    suspend fun allUnpaidIssued(): List<InvoiceEntity>
}

@Dao
interface PresetDao {
    @Insert
    suspend fun insert(preset: ItemPresetEntity): Long

    @Update
    suspend fun update(preset: ItemPresetEntity)

    @Query("SELECT * FROM item_preset WHERE id = :id")
    suspend fun get(id: Long): ItemPresetEntity?

    @Query("SELECT * FROM item_preset WHERE description = :description COLLATE NOCASE LIMIT 1")
    suspend fun findByDescription(description: String): ItemPresetEntity?

    /** Editor suggestions after 2 typed characters (UX_SPEC 3). */
    @Query(
        "SELECT * FROM item_preset WHERE description LIKE :prefix || '%' " +
            "ORDER BY useCount DESC, lastUsedAt DESC LIMIT :limit",
    )
    suspend fun suggestions(prefix: String, limit: Int = 3): List<ItemPresetEntity>
}
