package com.example.pocketbill.ui.settings

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pocketbill.R
import com.example.pocketbill.ui.appContainer
import com.example.pocketbill.ui.theme.Spacing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/** Logo on the business profile (F-14). Saved as soon as it is picked; issued invoices keep theirs. */
class LogoViewModel(
    logoName: Flow<String?>,
    private val import: suspend (String) -> String?,
    private val setLogo: suspend (String?) -> Unit,
) : ViewModel() {
    val logo: StateFlow<String?> = logoName.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _failed = MutableStateFlow(false)
    val failed: StateFlow<Boolean> = _failed.asStateFlow()

    fun onPicked(uri: String) {
        viewModelScope.launch {
            val name = runCatching { import(uri) }.getOrNull()
            _failed.value = name == null
            if (name != null) setLogo(name)
        }
    }

    fun onRemove() {
        _failed.value = false
        viewModelScope.launch { setLogo(null) }
    }
}

@Composable
fun LogoSection() {
    val c = appContainer()
    val vm: LogoViewModel = viewModel {
        LogoViewModel(c.profile.observeLogo(), c.logos::import, c.profile::setLogo)
    }
    val logo by vm.logo.collectAsStateWithLifecycle()
    val failed by vm.failed.collectAsStateWithLifecycle()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) vm.onPicked(uri.toString())
    }
    val image by produceState<ImageBitmap?>(null, logo) {
        value = logo?.let { name ->
            withContext(Dispatchers.IO) { BitmapFactory.decodeFile(File(c.logos.dir, name).path)?.asImageBitmap() }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
        SectionTitle(stringResource(R.string.logo_title))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.m)) {
            image?.let {
                Image(it, contentDescription = stringResource(R.string.logo_current), contentScale = ContentScale.Fit, modifier = Modifier.size(width = 120.dp, height = 48.dp))
            }
            OutlinedButton(
                onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                modifier = Modifier.heightIn(min = Spacing.buttonHeight),
            ) { Text(stringResource(if (logo == null) R.string.logo_choose else R.string.logo_change)) }
            if (logo != null) TextButton(onClick = vm::onRemove) { Text(stringResource(R.string.logo_remove)) }
        }
        Text(
            stringResource(if (failed) R.string.logo_failed else R.string.logo_help),
            style = MaterialTheme.typography.bodySmall,
            color = if (failed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
