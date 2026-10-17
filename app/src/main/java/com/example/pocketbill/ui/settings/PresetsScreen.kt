package com.example.pocketbill.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pocketbill.R
import com.example.pocketbill.data.db.ItemPresetEntity
import com.example.pocketbill.domain.money.CurrencyCode
import com.example.pocketbill.domain.money.MoneyFormatter
import com.example.pocketbill.ui.appContainer
import com.example.pocketbill.ui.components.CurrencyField
import com.example.pocketbill.ui.screens.EmptyStateScreen
import com.example.pocketbill.ui.theme.Spacing

/** Settings → Saved items (F-18): services with a usual price, suggested while typing a line. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PresetsScreen(onBack: () -> Unit) {
    val c = appContainer()
    val vm: PresetsViewModel = viewModel { PresetsViewModel(c.presets) { c.profile.get()?.defaultCurrency } }
    val state by vm.state.collectAsStateWithLifecycle()

    Scaffold(
        contentWindowInsets = WindowInsets(0), // the app's outer Scaffold handles the status bar
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0),
                title = { Text(stringResource(R.string.title_presets)) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = stringResource(R.string.action_back)) }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = vm::onAdd,
                icon = { Icon(painterResource(R.drawable.ic_add), contentDescription = null) },
                text = { Text(stringResource(R.string.presets_add)) },
            )
        },
    ) { padding ->
        if (state.loading) return@Scaffold
        if (state.presets.isEmpty()) {
            EmptyStateScreen(titleRes = R.string.presets_empty_title, bodyRes = R.string.presets_empty_body, modifier = Modifier.padding(padding))
        } else {
            LazyColumn(Modifier.padding(padding).fillMaxSize()) {
                item {
                    Text(
                        stringResource(R.string.presets_intro),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = Spacing.screen, vertical = Spacing.s),
                    )
                }
                items(state.presets, key = { it.id }) { preset -> PresetRow(preset, onClick = { vm.onEdit(preset) }) }
            }
        }
    }

    state.editing?.let { form -> PresetDialog(form, state.errors, vm) }
    state.confirmDelete?.let { preset ->
        AlertDialog(
            onDismissRequest = vm::onDeleteDismiss,
            title = { Text(stringResource(R.string.presets_delete_title, preset.description)) },
            text = { Text(stringResource(R.string.presets_delete_body)) },
            confirmButton = { TextButton(onClick = vm::onDeleteConfirm) { Text(stringResource(R.string.presets_delete)) } },
            dismissButton = { TextButton(onClick = vm::onDeleteDismiss) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

@Composable
private fun PresetRow(preset: ItemPresetEntity, onClick: () -> Unit) {
    val currency = preset.currency?.let(CurrencyCode::fromCode)
    val price = if (preset.unitPriceMinor != null && currency != null) {
        MoneyFormatter.format(preset.unitPriceMinor, currency)
    } else {
        stringResource(R.string.presets_no_price)
    }
    ListItem(
        headlineContent = { Text(preset.description) },
        supportingContent = { Text(price + " · " + pluralStringResource(R.plurals.presets_used, preset.useCount, preset.useCount)) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.clickable(onClick = onClick),
    )
}

@Composable
private fun PresetDialog(form: PresetForm, errors: Set<PresetError>, vm: PresetsViewModel) {
    val descriptionError = when {
        PresetError.DESCRIPTION_EMPTY in errors -> stringResource(R.string.editor_error_description)
        PresetError.DESCRIPTION_TOO_LONG in errors -> stringResource(R.string.presets_error_too_long, MAX_PRESET_DESCRIPTION)
        PresetError.DUPLICATE in errors -> stringResource(R.string.presets_error_duplicate)
        else -> null
    }
    val priceError = when {
        PresetError.PRICE_NEEDS_CURRENCY in errors -> stringResource(R.string.presets_error_currency)
        PresetError.PRICE_DECIMALS in errors -> stringResource(R.string.presets_error_decimals)
        PresetError.PRICE_TOO_LARGE in errors -> stringResource(R.string.editor_error_too_large)
        PresetError.PRICE_INVALID in errors -> stringResource(R.string.editor_error_price)
        else -> null
    }
    AlertDialog(
        onDismissRequest = vm::onDismissDialog,
        title = { Text(stringResource(if (form.presetId == 0L) R.string.presets_add else R.string.presets_edit)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.m)) {
                OutlinedTextField(
                    value = form.description,
                    onValueChange = { vm.onFormChange(form.copy(description = it)) },
                    label = { Text(stringResource(R.string.presets_description)) },
                    isError = descriptionError != null,
                    supportingText = descriptionError?.let { { Text(it) } },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = form.price,
                    onValueChange = { vm.onFormChange(form.copy(price = it)) },
                    label = { Text(stringResource(R.string.presets_price)) },
                    isError = priceError != null,
                    supportingText = { Text(priceError ?: stringResource(R.string.presets_price_hint)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                CurrencyField(
                    label = stringResource(R.string.presets_currency),
                    code = form.currency,
                    onChange = { vm.onFormChange(form.copy(currency = it)) },
                )
                if (form.presetId != 0L) {
                    TextButton(onClick = {
                        vm.state.value.presets.firstOrNull { it.id == form.presetId }?.let(vm::onDeleteRequest)
                    }) { Text(stringResource(R.string.presets_delete)) }
                }
            }
        },
        confirmButton = { TextButton(onClick = vm::onSave) { Text(stringResource(R.string.action_save)) } },
        dismissButton = { TextButton(onClick = vm::onDismissDialog) { Text(stringResource(R.string.action_cancel)) } },
    )
}
