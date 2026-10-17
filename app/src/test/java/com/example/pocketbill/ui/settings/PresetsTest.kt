package com.example.pocketbill.ui.settings

import com.example.pocketbill.data.db.ItemPresetEntity
import com.example.pocketbill.data.repository.PresetManager
import com.example.pocketbill.ui.MainDispatcherRule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Settings → Saved items (F-18). */
class PresetsTest {
    @get:Rule val main = MainDispatcherRule()

    private class FakePresets : PresetManager {
        val items = MutableStateFlow<List<ItemPresetEntity>>(emptyList())
        private var nextId = 1L
        override fun observeAll(): Flow<List<ItemPresetEntity>> = items
        override suspend fun save(presetId: Long, description: String, unitPriceMinor: Long?, currency: String?): Boolean {
            if (items.value.any { it.description.equals(description.trim(), ignoreCase = true) && it.id != presetId }) return false
            val entity = ItemPresetEntity(
                id = if (presetId == 0L) nextId++ else presetId, uuid = "u", description = description.trim(),
                unitPriceMinor = unitPriceMinor, currency = currency, createdAt = 0, updatedAt = 0,
            )
            items.value = items.value.filterNot { it.id == entity.id } + entity
            return true
        }
        override suspend fun delete(presetId: Long) {
            items.value = items.value.filterNot { it.id == presetId }
        }
    }

    private val store = FakePresets()
    private fun vm() = PresetsViewModel(store) { "EUR" }

    @Test fun `price needs a currency and must suit it`() {
        assertEquals(450_00L to emptySet<PresetError>(), validatePreset(PresetForm(description = "Logo", price = "450", currency = "EUR")))
        assertEquals(null to emptySet<PresetError>(), validatePreset(PresetForm(description = "Logo", price = "", currency = null)))
        assertEquals(setOf(PresetError.PRICE_NEEDS_CURRENCY), validatePreset(PresetForm(description = "Logo", price = "450", currency = null)).second)
        assertEquals(setOf(PresetError.PRICE_DECIMALS), validatePreset(PresetForm(description = "Logo", price = "450.5", currency = "JPY")).second)
        assertEquals(setOf(PresetError.DESCRIPTION_EMPTY), validatePreset(PresetForm(description = "  ")).second)
    }

    @Test fun `add starts with the business currency and saves`() {
        val vm = vm()
        vm.onAdd()
        assertEquals("EUR", vm.state.value.editing!!.currency)
        vm.onFormChange(vm.state.value.editing!!.copy(description = "Logo design", price = "450"))
        vm.onSave()
        assertNull(vm.state.value.editing)
        assertEquals(45_000L, store.items.value.single().unitPriceMinor)
    }

    @Test fun `a second item with the same description is refused`() {
        val vm = vm()
        vm.onAdd(); vm.onFormChange(PresetForm(description = "Logo")); vm.onSave()
        vm.onAdd(); vm.onFormChange(PresetForm(description = "logo")); vm.onSave()
        assertEquals(setOf(PresetError.DUPLICATE), vm.state.value.errors)
        assertEquals(1, store.items.value.size)
    }

    @Test fun `editing shows the price as typed text, and delete asks first`() {
        val vm = vm()
        vm.onAdd(); vm.onFormChange(PresetForm(description = "Logo", price = "1200.50", currency = "EUR")); vm.onSave()
        val preset = vm.state.value.presets.single()
        vm.onEdit(preset)
        assertEquals("1200.50", vm.state.value.editing!!.price)
        vm.onDeleteRequest(preset)
        assertTrue(store.items.value.isNotEmpty())
        vm.onDeleteConfirm()
        assertTrue(store.items.value.isEmpty())
    }
}
