package com.example.pocketbill.ui.clients

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pocketbill.data.db.ClientEntity
import com.example.pocketbill.data.repository.ClientStore
import com.example.pocketbill.domain.Outcome
import com.example.pocketbill.domain.Problem
import com.example.pocketbill.ui.navigation.NO_ID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** A question the editor is asking before it saves. */
sealed interface ClientPrompt {
    /** UX_SPEC 6: another client has the same name. */
    data class Duplicate(val existingId: Long, val existingName: String, val isNew: Boolean) : ClientPrompt

    /** DATA_MODEL 5.4: issued unpaid invoices show the old details. "Only new invoices" is the default. */
    data class UpdateUnpaid(val unpaidCount: Int, val alsoUpdate: Boolean = false) : ClientPrompt
}

/** Where the editor goes when it's finished. */
sealed interface ClientEditorResult {
    data class Saved(val clientId: Long, val isNew: Boolean) : ClientEditorResult

    /** The user chose an existing client instead of creating a duplicate. */
    data class UseExisting(val clientId: Long) : ClientEditorResult
}

data class ClientEditorUiState(
    val loading: Boolean = true,
    val isNew: Boolean = true,
    val form: ClientForm = ClientForm(),
    /** The form as last loaded, to tell whether leaving would drop edits. */
    val savedForm: ClientForm = ClientForm(),
    val errors: Map<ClientField, Problem> = emptyMap(),
    val prompt: ClientPrompt? = null,
    val saving: Boolean = false,
    val failed: Boolean = false,
    val result: ClientEditorResult? = null,
) {
    val hasChanges: Boolean get() = !loading && result == null && form != savedForm
}

/** New client or edit (UX_SPEC 6), with the duplicate-name and update-unpaid questions. */
class ClientEditorViewModel(private val store: ClientStore, private val clientId: Long) : ViewModel() {
    private val _state = MutableStateFlow(ClientEditorUiState(isNew = clientId == NO_ID, loading = clientId != NO_ID))
    val state: StateFlow<ClientEditorUiState> = _state.asStateFlow()

    private var original: ClientEntity? = null
    private var duplicateConfirmed = false

    init {
        if (clientId != NO_ID) {
            viewModelScope.launch {
                val existing = store.get(clientId)
                original = existing
                val form = existing?.toForm() ?: ClientForm()
                _state.update { it.copy(loading = false, form = form, savedForm = form) }
            }
        }
    }

    fun onFormChange(change: (ClientForm) -> ClientForm) = _state.update {
        val form = change(it.form)
        if (form.name != it.form.name) duplicateConfirmed = false
        // Keep only errors that still apply, so fixing a field clears its message.
        it.copy(form = form, errors = it.errors.filterKeys { field -> field in form.errors().keys })
    }

    fun onSave() {
        val s = _state.value
        if (s.saving) return
        val errors = s.form.errors()
        if (errors.isNotEmpty()) {
            _state.update { it.copy(errors = errors) }
            return
        }
        viewModelScope.launch {
            // 1. Possible duplicate (UX_SPEC 6). Skipped when the name of an existing client didn't change.
            val nameChanged = original?.name?.trim() != s.form.name.trim()
            if (!duplicateConfirmed && nameChanged) {
                val same = store.findSameName(s.form.name, excludeId = clientId)
                if (same != null) {
                    _state.update { it.copy(prompt = ClientPrompt.Duplicate(same.id, same.name, it.isNew)) }
                    return@launch
                }
            }
            // 2. Update unpaid invoices? (DATA_MODEL 5.4)
            val before = original
            if (before != null && before.frozenDetailsDiffer(s.form)) {
                val unpaid = store.unpaidIssuedCount(clientId)
                if (unpaid > 0) {
                    _state.update { it.copy(prompt = ClientPrompt.UpdateUnpaid(unpaid)) }
                    return@launch
                }
            }
            save(alsoUpdateUnpaid = false)
        }
    }

    /** Duplicate prompt: keep going and save under this name anyway. */
    fun onSaveAnyway() {
        duplicateConfirmed = true
        _state.update { it.copy(prompt = null) }
        onSave()
    }

    /** Duplicate prompt on a new client: use the existing client instead. */
    fun onUseExisting() {
        val prompt = _state.value.prompt as? ClientPrompt.Duplicate ?: return
        _state.update { it.copy(prompt = null, result = ClientEditorResult.UseExisting(prompt.existingId)) }
    }

    fun onUpdateChoice(alsoUpdate: Boolean) = _state.update { s ->
        val prompt = s.prompt as? ClientPrompt.UpdateUnpaid ?: return@update s
        s.copy(prompt = prompt.copy(alsoUpdate = alsoUpdate))
    }

    fun onUpdateConfirm() {
        val prompt = _state.value.prompt as? ClientPrompt.UpdateUnpaid ?: return
        _state.update { it.copy(prompt = null) }
        viewModelScope.launch { save(prompt.alsoUpdate) }
    }

    fun onPromptDismiss() = _state.update { it.copy(prompt = null) }

    private suspend fun save(alsoUpdateUnpaid: Boolean) {
        _state.update { it.copy(saving = true, failed = false) }
        val input = _state.value.form.toInput()
        val result: ClientEditorResult? = if (clientId == NO_ID) {
            (store.create(input) as? Outcome.Success)?.let { ClientEditorResult.Saved(it.value, isNew = true) }
        } else {
            (store.update(clientId, input, alsoUpdateUnpaid) as? Outcome.Success)?.let { ClientEditorResult.Saved(clientId, isNew = false) }
        }
        _state.update { it.copy(saving = false, failed = result == null, result = result) }
    }
}
