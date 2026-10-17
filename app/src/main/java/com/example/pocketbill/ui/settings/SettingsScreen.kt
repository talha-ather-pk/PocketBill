package com.example.pocketbill.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import com.example.pocketbill.R
import com.example.pocketbill.app.SafeLog
import com.example.pocketbill.domain.format.DateFormatter
import com.example.pocketbill.data.repository.ProfileRepository
import com.example.pocketbill.ui.appContainer
import com.example.pocketbill.ui.theme.Spacing
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class SettingsViewModel(profile: ProfileRepository) : ViewModel() {
    /** Shown under "Business profile". */
    val businessName: StateFlow<String> = profile.observe()
        .map { it?.name.orEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    /** Shown under "Backup and restore": null = never backed up (MVP_SPEC 7). */
    val lastBackupAt: StateFlow<Long?> = profile.observe()
        .map { it?.lastBackupAt }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

/** Settings tab (UX_SPEC 1): business profile, saved items, backup, and what happens to the user's data. */
@Composable
fun SettingsScreen(
    onOpenProfile: () -> Unit,
    onOpenPresets: () -> Unit,
    onOpenBackup: () -> Unit,
) {
    val container = appContainer()
    val vm: SettingsViewModel = viewModel { SettingsViewModel(container.profile) }
    val businessName by vm.businessName.collectAsStateWithLifecycle()
    val lastBackupAt by vm.lastBackupAt.collectAsStateWithLifecycle()
    // Rows sit on the page background, not on a lighter surface band.
    val rowColors = ListItemDefaults.colors(containerColor = Color.Transparent)

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(vertical = Spacing.l)) {
        Text(
            stringResource(R.string.nav_settings),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(horizontal = Spacing.screen).semantics { heading() },
        )
        ListItem(
            headlineContent = { Text(stringResource(R.string.title_business_profile)) },
            supportingContent = { Text(businessName) },
            colors = rowColors,
            modifier = Modifier.clickable(onClick = onOpenProfile),
        )
        ListItem(
            headlineContent = { Text(stringResource(R.string.title_presets)) },
            supportingContent = { Text(stringResource(R.string.settings_presets_hint)) },
            colors = rowColors,
            modifier = Modifier.clickable(onClick = onOpenPresets),
        )
        ListItem(
            headlineContent = { Text(stringResource(R.string.title_backup)) },
            supportingContent = { Text(lastBackupText(lastBackupAt)) },
            colors = rowColors,
            modifier = Modifier.clickable(onClick = onOpenBackup),
        )
        // Shown once a support address is set for release (P8-T01); only versions are prefilled (ANALYTICS_PLAN).
        val supportEmail = stringResource(R.string.support_email)
        if (supportEmail.isNotBlank()) {
            val context = LocalContext.current
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_feedback)) },
                supportingContent = { Text(stringResource(R.string.settings_feedback_hint)) },
                colors = rowColors,
                modifier = Modifier.clickable { sendFeedback(context, supportEmail) },
            )
        }
        HorizontalDivider(Modifier.padding(vertical = Spacing.s))
        Text(
            stringResource(R.string.settings_privacy_title),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = Spacing.screen, vertical = Spacing.s).semantics { heading() },
        )
        Text(
            stringResource(R.string.settings_privacy_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = Spacing.screen),
        )
    }
}

/** "Last backup 17 Oct 2026" or "Never backed up". */
@Composable
internal fun lastBackupText(at: Long?): String =
    at?.let { stringResource(R.string.backup_last, DateFormatter.full(it / 86_400_000L)) } ?: stringResource(R.string.backup_never)

/** Opens the email app to the support address with the app and Android versions; nothing else is attached. */
private fun sendFeedback(context: Context, to: String) {
    val version = runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull().orEmpty()
    val intent = Intent(Intent.ACTION_SENDTO, "mailto:".toUri()).apply {
        putExtra(Intent.EXTRA_EMAIL, arrayOf(to))
        putExtra(Intent.EXTRA_SUBJECT, "PocketBill feedback (app $version, Android ${Build.VERSION.RELEASE})")
    }
    try {
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        SafeLog.failure("Feedback", "No email app", e)
    }
}
