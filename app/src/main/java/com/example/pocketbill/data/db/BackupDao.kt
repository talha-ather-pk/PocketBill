package com.example.pocketbill.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

/** Whole-table reads and writes for backup and restore (MVP_SPEC 7). Queries only: no schema change. */
@Dao
interface BackupDao {
    @Query("SELECT * FROM business_profile WHERE id = 1")
    suspend fun profile(): BusinessProfileEntity?

    @Query("SELECT * FROM client")
    suspend fun clients(): List<ClientEntity>

    @Query("SELECT * FROM invoice")
    suspend fun invoices(): List<InvoiceEntity>

    @Query("SELECT * FROM line_item")
    suspend fun lines(): List<LineItemEntity>

    @Query("SELECT * FROM item_preset")
    suspend fun presets(): List<ItemPresetEntity>

    // Children first, so foreign keys are never broken mid-transaction.
    @Query("DELETE FROM line_item")
    suspend fun deleteLines()

    @Query("DELETE FROM invoice")
    suspend fun deleteInvoices()

    @Query("DELETE FROM client")
    suspend fun deleteClients()

    @Query("DELETE FROM item_preset")
    suspend fun deletePresets()

    @Query("DELETE FROM business_profile")
    suspend fun deleteProfile()

    @Insert
    suspend fun insertProfile(profile: BusinessProfileEntity)

    @Insert
    suspend fun insertClients(clients: List<ClientEntity>)

    @Insert
    suspend fun insertInvoices(invoices: List<InvoiceEntity>)

    @Insert
    suspend fun insertLines(lines: List<LineItemEntity>)

    @Insert
    suspend fun insertPresets(presets: List<ItemPresetEntity>)

    /** For the Home reminder: invoices created after [since] (MVP_SPEC 7). */
    @Query("SELECT COUNT(*) FROM invoice WHERE createdAt > :since")
    suspend fun countCreatedAfter(since: Long): Int

    @Query("SELECT MIN(createdAt) FROM invoice")
    suspend fun firstInvoiceCreatedAt(): Long?
}
