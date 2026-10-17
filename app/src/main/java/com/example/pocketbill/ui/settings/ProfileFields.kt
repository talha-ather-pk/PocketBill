package com.example.pocketbill.ui.settings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import com.example.pocketbill.R

/** The message for a profile form error (UX_SPEC 3: errors say what to do). */
@Composable
fun profileErrorText(error: ProfileError?, minNextNumber: Long = 1): String? = when (error) {
    null -> null
    ProfileError.REQUIRED -> stringResource(R.string.profile_error_name_required)
    ProfileError.BAD_PERCENT -> stringResource(R.string.profile_error_bad_percent)
    ProfileError.TOO_LONG -> stringResource(R.string.profile_error_prefix_too_long, MAX_PREFIX_LENGTH)
    ProfileError.BAD_NUMBER -> stringResource(R.string.profile_error_bad_number)
    ProfileError.NUMBER_TOO_LOW -> stringResource(R.string.profile_error_number_too_low, minNextNumber)
}

/** A labelled text field with an optional helper line, replaced by the error when there is one. */
@Composable
fun FormTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    helper: String? = null,
    error: String? = null,
    singleLine: Boolean = true,
    minLines: Int = 1,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.Sentences,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        isError = error != null,
        supportingText = (error ?: helper)?.let { text -> { Text(text) } },
        singleLine = singleLine,
        minLines = minLines,
        keyboardOptions = KeyboardOptions(
            capitalization = capitalization,
            keyboardType = keyboardType,
            imeAction = if (singleLine) ImeAction.Next else ImeAction.Default,
        ),
        modifier = modifier.fillMaxWidth(),
    )
}
