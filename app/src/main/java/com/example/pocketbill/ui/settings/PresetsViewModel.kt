package com.example.pocketbill.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pocketbill.data.db.ItemPresetEntity
import com.example.pocketbill.data.repository.PresetManager
import com.example.pocketbill.domain.money.AmountParser
import com.example.pocketbill.domain.money.CurrencyCode
import com.example.pocketbill.domain.money.MoneyFormatter
import com.example.pocketbill.domain.money.ParseResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Longest description kept for a saved item. */
const val MAX_PRESET_DESCRIPTION = 200

enum class PresetError { DESCRIPTION_EMPTY, DESCRIPTION_TOO_LONG, DUPLICATE, PRICE_INVALID, PRICE_DECIMALS, PRICE_TOO_LARGE, PRICE_NEEDS_CURRENCY }

/** The add/edit dialog. [presetId] 0 = new. Price is typed text until saved (DATA_MODEL 6.3). */
data class PresetForm(
    val presetId: Long = 0,
    val description: String = "",
    val price: String = "",
    val currency: String? = null,
)

data class PresetsUiState(
    val loading: Boolean = true,
    val presets: List<ItemPresetEntity> = emptyList(),
    val editing: PresetForm? = null,
    val errors: Set<PresetError> = emptySet(),
    val confirmDelete: ItemPresetEntity? = null,
)

/** Checks the dialog; returns the price in minor units (or null for no price), or the problems. */
fun validatePreset(form: PresetForm): Pair<Long?, Set<PresetError>> {
    val errors = mutableSetOf<PresetError>()
    val description = form.description.trim()
    if (description.isEmpty()) errors += PresetError.DESCRIPTION_EMPTY
    if (description.length > MAX_PRESET_DESCRIPTION) errors += PresetError.DESCRIPTION_TOO_LONG
    var price: Long? = null
    if (form.price.isNotBlank()) {
        val currency = form.currency?.let(CurrencyCode::fromCode)
        if (currency == null) {
            errors += PresetError.PRICE_NEEDS_CURRENCY
        } else {
            when (val parsed = AmountParser.parseAmount(form.price, currency)) {
                is ParseResult.Ok -> price = parsed.value
                is ParseResult.TooManyDecimals -> errors += PresetError.PRICE_DECIMALS
                ParseResult.TooLarge -> errors += PresetError.PRICE_TOO_LARGE
                else -> errors += PresetError.PRICE_INVALID
            }
        }
    }
    return price to errors
}

/** Settings → Saved items (F-18): the presets offered while typing a line description. */
class PresetsViewModel(private val store: PresetManager, private val defaultCurrency: suspend () -> String?) : ViewModel() {
    private val _state = MutableStateFlow(PresetsUiState())
    val state: StateFlow<PresetsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            store.observeAll().collect { list -> _state.update { it.copy(loading = false, presets = list) } }
        }
    }

    fun onAdd() {
        viewModelScope.launch { _state.update { it.copy(editing = PresetForm(currency = defaultCurrency()), errors = emptySet()) } }
    }

    fun onEdit(preset: ItemPresetEntity) = _state.update {
        val price = preset.unitPriceMinor?.let { minor -> preset.currency?.let(CurrencyCode::fromCode)?.let { MoneyFormatter.formatPlain(minor, it) } }
        it.copy(editing = PresetForm(preset.id, preset.description, price.orEmpty(), preset.currency), errors = emptySet())
    }

    fun onFormChange(form: PresetForm) = _state.update { it.copy(editing = form, errors = emptySet()) }

    fun onSave() {
        val form = _state.value.editing ?: return
        val (price, errors) = validatePreset(form)
        if (errors.isNotEmpty()) {
            _state.update { it.copy(errors = errors) }
            return
        }
        viewModelScope.launch {
            val saved = store.save(form.presetId, form.description, price, form.currency)
            _state.update { if (saved) it.copy(editing = null) else it.copy(errors = setOf(PresetError.DUPLICATE)) }
        }
    }

    fun onDismissDialog() = _state.update { it.copy(editing = null, errors = emptySet()) }

    fun onDeleteRequest(preset: ItemPresetEntity) = _state.update { it.copy(confirmDelete = preset, editing = null) }

    fun onDeleteConfirm() {
        val preset = _state.value.confirmDelete ?: return
        _state.update { it.copy(confirmDelete = null) }
        viewModelScope.launch { store.delete(preset.id) }
    }

    fun onDeleteDismiss() = _state.update { it.copy(confirmDelete = null) }
}
