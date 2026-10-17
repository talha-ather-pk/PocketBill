package com.example.pocketbill.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.pocketbill.R
import com.example.pocketbill.domain.money.CurrencyCode
import com.example.pocketbill.domain.text.normalizeForMatching
import com.example.pocketbill.ui.theme.Spacing
import java.util.Currency
import java.util.Locale

/** "EUR · Euro". English names, whatever the phone's language (UX_SPEC 7.2). */
fun currencyLabel(code: String): String {
    val name = runCatching { Currency.getInstance(code).getDisplayName(Locale.US) }.getOrNull()
    return if (name == null || name == code) code else "$code · $name"
}

/** A field showing the chosen currency; tapping it opens a searchable list of the supported currencies. */
@Composable
fun CurrencyField(
    label: String,
    code: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var open by rememberSaveable { mutableStateOf(false) }
    Surface(
        onClick = { open = true },
        shape = MaterialTheme.shapes.small,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = modifier.fillMaxWidth().heightIn(min = 64.dp),
    ) {
        Column(Modifier.padding(horizontal = Spacing.l, vertical = Spacing.s)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(currencyLabel(code), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                Text(stringResource(R.string.action_change), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
    if (open) {
        CurrencyPickerDialog(
            selected = code,
            onPick = { onChange(it); open = false },
            onDismiss = { open = false },
        )
    }
}

@Composable
private fun CurrencyPickerDialog(selected: String, onPick: (String) -> Unit, onDismiss: () -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    val wanted = normalizeForMatching(query)
    val matches = CurrencyCode.entries.map { it.name }.filter { normalizeForMatching(currencyLabel(it)).contains(wanted) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.currency_picker_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text(stringResource(R.string.action_search)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                LazyColumn(Modifier.heightIn(max = 360.dp)) {
                    items(matches, key = { it }) { code ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = Spacing.touchTarget)
                                .semantics { this.selected = code == selected }
                                .clickable(role = Role.RadioButton) { onPick(code) }
                                .padding(horizontal = Spacing.xs),
                        ) {
                            Text(currencyLabel(code), modifier = Modifier.weight(1f))
                            if (code == selected) {
                                Icon(painterResource(R.drawable.ic_check), contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}
