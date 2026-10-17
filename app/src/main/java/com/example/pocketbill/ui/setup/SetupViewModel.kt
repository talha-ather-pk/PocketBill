package com.example.pocketbill.ui.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pocketbill.data.repository.ProfileRepository
import com.example.pocketbill.domain.Outcome
import com.example.pocketbill.ui.settings.ProfileError
import com.example.pocketbill.ui.settings.ProfileField
import com.example.pocketbill.ui.settings.ProfileForm
import com.example.pocketbill.ui.settings.ProfileFormResult
import com.example.pocketbill.ui.settings.suggestedCurrency
import com.example.pocketbill.ui.settings.validate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SetupUiState(
    val form: ProfileForm,
    val errors: Map<ProfileField, ProfileError> = emptyMap(),
    val saving: Boolean = false,
    val failed: Boolean = false,
    /** Set once the profile is saved; the screen then opens Home. */
    val done: Boolean = false,
)

/** First launch: business name, currency and payment details (UX_SPEC 1, MVP_SPEC F-01). */
class SetupViewModel(
    private val profile: ProfileRepository,
    regionCurrencyCode: String?,
) : ViewModel() {
    private val _state = MutableStateFlow(SetupUiState(ProfileForm(currency = suggestedCurrency(regionCurrencyCode))))
    val state: StateFlow<SetupUiState> = _state.asStateFlow()

    fun onNameChange(value: String) = _state.update { it.copy(form = it.form.copy(name = value), errors = it.errors - ProfileField.NAME) }

    fun onCurrencyChange(code: String) = _state.update { it.copy(form = it.form.copy(currency = code)) }

    fun onPaymentChange(value: String) = _state.update { it.copy(form = it.form.copy(paymentInstructions = value)) }

    fun onSave() {
        val current = _state.value
        if (current.saving) return
        when (val result = current.form.validate()) {
            is ProfileFormResult.Invalid -> _state.update { it.copy(errors = result.errors) }
            is ProfileFormResult.Valid -> {
                _state.update { it.copy(saving = true, failed = false) }
                viewModelScope.launch {
                    val outcome = profile.save(result.input)
                    _state.update { it.copy(saving = false, done = outcome is Outcome.Success, failed = outcome !is Outcome.Success) }
                }
            }
        }
    }
}
