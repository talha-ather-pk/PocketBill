package com.example.pocketbill.ui.setup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pocketbill.R
import com.example.pocketbill.ui.appContainer
import com.example.pocketbill.ui.components.CurrencyField
import com.example.pocketbill.ui.settings.FormTextField
import com.example.pocketbill.ui.settings.ProfileField
import com.example.pocketbill.ui.settings.ProfileForm
import com.example.pocketbill.ui.settings.profileErrorText
import com.example.pocketbill.ui.theme.PocketBillTheme
import com.example.pocketbill.ui.theme.Spacing
import java.util.Currency
import java.util.Locale

/** First launch (UX_SPEC 1): only the business name is required; the rest can be changed later. */
@Composable
fun SetupScreen(onDone: () -> Unit) {
    val container = appContainer()
    val vm: SetupViewModel = viewModel { SetupViewModel(container.profile, regionCurrencyCode()) }
    val state by vm.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.done) { if (state.done) onDone() }
    SetupContent(
        state = state,
        onNameChange = vm::onNameChange,
        onCurrencyChange = vm::onCurrencyChange,
        onPaymentChange = vm::onPaymentChange,
        onSave = vm::onSave,
    )
}

@Composable
private fun SetupContent(
    state: SetupUiState,
    onNameChange: (String) -> Unit,
    onCurrencyChange: (String) -> Unit,
    onPaymentChange: (String) -> Unit,
    onSave: () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.item),
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.screen, vertical = Spacing.section),
    ) {
        Text(
            stringResource(R.string.title_setup),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            stringResource(R.string.setup_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FormTextField(
            value = state.form.name,
            onValueChange = onNameChange,
            label = stringResource(R.string.field_business_name),
            error = profileErrorText(state.errors[ProfileField.NAME]),
            capitalization = KeyboardCapitalization.Words,
            modifier = Modifier.padding(top = Spacing.s),
        )
        CurrencyField(
            label = stringResource(R.string.field_currency),
            code = state.form.currency,
            onChange = onCurrencyChange,
        )
        FormTextField(
            value = state.form.paymentInstructions,
            onValueChange = onPaymentChange,
            label = stringResource(R.string.field_payment),
            helper = stringResource(R.string.field_payment_help),
            singleLine = false,
            minLines = 3,
        )
        if (state.failed) {
            Text(stringResource(R.string.save_failed), color = MaterialTheme.colorScheme.error)
        }
        Button(
            onClick = onSave,
            enabled = !state.saving,
            modifier = Modifier.fillMaxWidth().heightIn(min = Spacing.buttonHeight).padding(top = Spacing.s),
        ) {
            Text(stringResource(R.string.setup_start))
        }
    }
}

/** The phone's region currency, used only as the suggested default. */
private fun regionCurrencyCode(): String? = runCatching { Currency.getInstance(Locale.getDefault()).currencyCode }.getOrNull()

@Preview(showBackground = true)
@Composable
private fun SetupPreview() {
    PocketBillTheme {
        SetupContent(SetupUiState(ProfileForm(currency = "EUR")), {}, {}, {}, {})
    }
}
