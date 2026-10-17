package com.example.pocketbill.data.repository

import com.example.pocketbill.data.db.ItemPresetEntity
import com.example.pocketbill.data.db.PocketBillDatabase
import com.example.pocketbill.domain.time.Clock
import kotlinx.coroutines.flow.Flow
import java.util.UUID

/** Saved line items offered while typing a description (F-18, UX_SPEC 3). */
interface PresetStore {
    /** Up to 3 presets starting with [prefix], most used first. */
    suspend fun suggestions(prefix: String): List<ItemPresetEntity>

    /** Saves a line as a preset, or updates the price of an existing preset with the same description. */
    suspend fun saveFromLine(description: String, unitPriceMinor: Long?, currency: String)

    /** Counts a use so frequent items come first. */
    suspend fun markUsed(presetId: Long)
}

/** Settings → Saved items (F-18): list, add, edit and delete. Deleting never changes invoices. */
interface PresetManager {
    fun observeAll(): Flow<List<ItemPresetEntity>>

    /** [presetId] 0 adds a new one. False if another saved item already has this description. */
    suspend fun save(presetId: Long, description: String, unitPriceMinor: Long?, currency: String?): Boolean
    suspend fun delete(presetId: Long)
}

/** The only code that reads or writes presets (ARCHITECTURE.md section 3). */
class PresetRepository(
    db: PocketBillDatabase,
    private val clock: Clock,
    private val newUuid: () -> String = { UUID.randomUUID().toString() },
) : PresetStore, PresetManager {
    private val presets = db.presetDao()

    override suspend fun suggestions(prefix: String): List<ItemPresetEntity> {
        val wanted = prefix.trim()
        if (wanted.length < 2) return emptyList()
        return presets.suggestions(wanted)
    }

    override suspend fun saveFromLine(description: String, unitPriceMinor: Long?, currency: String) {
        val text = description.trim()
        if (text.isEmpty()) return
        val now = clock.nowMillis()
        val existing = presets.findByDescription(text)
        if (existing == null) {
            presets.insert(
                ItemPresetEntity(uuid = newUuid(), description = text, unitPriceMinor = unitPriceMinor, currency = currency, createdAt = now, updatedAt = now),
            )
        } else {
            presets.update(existing.copy(unitPriceMinor = unitPriceMinor, currency = currency, updatedAt = now))
        }
    }

    override suspend fun markUsed(presetId: Long) {
        val existing = presets.get(presetId) ?: return
        val now = clock.nowMillis()
        presets.update(existing.copy(useCount = existing.useCount + 1, lastUsedAt = now, updatedAt = now))
    }

    override fun observeAll(): Flow<List<ItemPresetEntity>> = presets.observeAll()

    override suspend fun save(presetId: Long, description: String, unitPriceMinor: Long?, currency: String?): Boolean {
        val text = description.trim()
        val clash = presets.findByDescription(text)
        if (clash != null && clash.id != presetId) return false
        val now = clock.nowMillis()
        val existing = if (presetId == 0L) null else presets.get(presetId)
        if (existing == null) {
            presets.insert(ItemPresetEntity(uuid = newUuid(), description = text, unitPriceMinor = unitPriceMinor, currency = currency, createdAt = now, updatedAt = now))
        } else {
            presets.update(existing.copy(description = text, unitPriceMinor = unitPriceMinor, currency = currency, updatedAt = now))
        }
        return true
    }

    override suspend fun delete(presetId: Long) = presets.delete(presetId)
}
