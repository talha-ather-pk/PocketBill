package com.example.pocketbill.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pocketbill.data.db.BusinessProfileEntity
import com.example.pocketbill.data.repository.ProfileInput
import com.example.pocketbill.data.repository.ProfileRepository
import com.example.pocketbill.domain.Outcome
import com.example.pocketbill.domain.invoice.PaperSize
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** "Update unpaid invoices too?" (DATA_MODEL 5.5). */
data class UpdateUnpaidPrompt(
    val unpaidCount: Int,
    val paymentDetailsChanged: Boolean,
    /** Pre-selected when payment details changed. */
    val alsoUpdate: Boolean = paymentDetailsChanged,
)

data class BusinessProfileUiState(
    val loading: Boolean = true,
    val form: ProfileForm = ProfileForm(),
    /** The form as last loaded, to tell whether leaving would drop edits. */
    val savedForm: ProfileForm = ProfileForm(),
    val errors: Map<ProfileField, ProfileError> = emptyMap(),
    val prompt: UpdateUnpaidPrompt? = null,
    val saving: Boolean = false,
    val failed: Boolean = false,
    /** Set once saved; the screen then goes back to Settings. */
    val done: Boolean = false,
) {
    val hasChanges: Boolean get() = !loading && !done && form != savedForm
}

/** Settings → Business profile: every profile field (MVP_SPEC F-01). */
class BusinessProfileViewModel(
    private val profile: ProfileRepository,
    /** Called with the time when updated unpaid invoices carry new payment details (Home card, UX_SPEC 2). */
    private val onPaymentDetailsUpdated: () -> Unit = {},
) : ViewModel() {
    private val _state = MutableStateFlow(BusinessProfileUiState())
    val state: StateFlow<BusinessProfileUiState> = _state.asStateFlow()

    private var original: BusinessProfileEntity? = null
    private var pendingInput: ProfileInput? = null

    init {
        viewModelScope.launch {
            val existing = profile.get()
            original = existing
            val form = existing?.toForm() ?: ProfileForm()
            _state.update { it.copy(loading = false, form = form, savedForm = form) }
        }
    }

    fun onFormChange(change: (ProfileForm) -> ProfileForm) = _state.update {
        val form = change(it.form)
        // Clear an error as soon as its field is edited.
        val cleared = it.errors.filterKeys { field -> fieldValue(field, form) == fieldValue(field, it.form) }
        it.copy(form = form, errors = cleared)
    }

    fun onPaperSize(size: PaperSize) = onFormChange { it.copy(paperSize = size) }

    fun onSave() {
        if (_state.value.saving) return
        when (val result = _state.value.form.validate()) {
            is ProfileFormResult.Invalid -> _state.update { it.copy(errors = result.errors) }
            is ProfileFormResult.Valid -> viewModelScope.launch {
                val before = original
                val unpaid = if (before != null && before.frozenDetailsDiffer(result.input)) profile.unpaidIssuedCount() else 0
                if (unpaid > 0) {
                    pendingInput = result.input
                    _state.update { it.copy(prompt = UpdateUnpaidPrompt(unpaid, before!!.paymentDetailsDiffer(result.input))) }
                } else {
                    save(result.input, alsoUpdateUnpaid = false)
                }
            }
        }
    }

    fun onPromptChoice(alsoUpdate: Boolean) = _state.update { s -> s.copy(prompt = s.prompt?.copy(alsoUpdate = alsoUpdate)) }

    fun onPromptConfirm() {
        val input = pendingInput ?: return
        val alsoUpdate = _state.value.prompt?.alsoUpdate ?: false
        _state.update { it.copy(prompt = null) }
        viewModelScope.launch { save(input, alsoUpdate) }
    }

    fun onPromptDismiss() {
        pendingInput = null
        _state.update { it.copy(prompt = null) }
    }

    private suspend fun save(input: ProfileInput, alsoUpdateUnpaid: Boolean) {
        _state.update { it.copy(saving = true, failed = false) }
        val paymentChanged = original?.paymentDetailsDiffer(input) == true
        val outcome = profile.save(input, alsoUpdateUnpaid)
        if (outcome is Outcome.Success && alsoUpdateUnpaid && paymentChanged) onPaymentDetailsUpdated()
        _state.update { it.copy(saving = false, done = outcome is Outcome.Success, failed = outcome !is Outcome.Success) }
    }

    private fun fieldValue(field: ProfileField, form: ProfileForm): Any = when (field) {
        ProfileField.NAME -> form.name
        ProfileField.TAX_RATE -> form.taxRate
        ProfileField.PREFIX -> form.prefix
        ProfileField.NEXT_NUMBER -> form.nextNumber
    }
}
