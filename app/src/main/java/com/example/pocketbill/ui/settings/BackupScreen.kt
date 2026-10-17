package com.example.pocketbill.ui.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pocketbill.app.SafeLog
import com.example.pocketbill.R
import com.example.pocketbill.data.backup.BackupProblem
import com.example.pocketbill.data.backup.RestorePreview
import com.example.pocketbill.domain.format.DateFormatter
import com.example.pocketbill.ui.appContainer
import com.example.pocketbill.ui.theme.Spacing
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Backups are small (text only); anything far larger isn't a PocketBill backup. */
private const val MAX_BACKUP_BYTES = 50L * 1024 * 1024

/** Backup and restore (MVP_SPEC 7): save a JSON file anywhere, or replace all data from one. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(onBack: () -> Unit) {
    val c = appContainer()
    val vm: BackupViewModel = viewModel { BackupViewModel(c.backup, c.clock) }
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    val saveLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult // backed out
        scope.launch {
            val ok = try {
                val text = vm.backupText()
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri, "wt")?.use { it.write(text.toByteArray(Charsets.UTF_8)) } != null
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                SafeLog.failure("Backup", "Writing the backup failed", e)
                false
            }
            vm.onBackupWritten(ok)
        }
    }
    val csvLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val ok = try {
                val text = vm.csvText()
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri, "wt")?.use { it.write(text.toByteArray(Charsets.UTF_8)) } != null
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                SafeLog.failure("Backup", "Writing the CSV failed", e)
                false
            }
            vm.onCsvWritten(ok)
        }
    }
    val openLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val text = withContext(Dispatchers.IO) {
                try {
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        val bytes = input.readNBytesCompat(MAX_BACKUP_BYTES + 1)
                        if (bytes.size > MAX_BACKUP_BYTES) null else bytes.toString(Charsets.UTF_8)
                    }
                } catch (e: Exception) {
                    SafeLog.failure("Backup", "Reading the backup failed", e)
                    null
                }
            }
            vm.onRestoreFileRead(text)
        }
    }

    val messages = mapOf(
        BackupMessage.BACKED_UP to stringResource(R.string.backup_msg_saved),
        BackupMessage.BACKUP_FAILED to stringResource(R.string.backup_msg_save_failed),
        BackupMessage.RESTORED to stringResource(R.string.backup_msg_restored),
        BackupMessage.RESTORE_FAILED to stringResource(R.string.backup_msg_restore_failed),
        BackupMessage.FILE_UNREADABLE to stringResource(R.string.backup_unreadable),
        BackupMessage.CSV_SAVED to stringResource(R.string.csv_saved),
        BackupMessage.CSV_FAILED to stringResource(R.string.csv_failed),
    )
    LaunchedEffect(state.message) {
        val message = state.message ?: return@LaunchedEffect
        snackbar.showSnackbar(messages.getValue(message))
        vm.onMessageShown()
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0), // the app's outer Scaffold handles the status bar
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0),
                title = { Text(stringResource(R.string.title_backup)) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = stringResource(R.string.action_back)) }
                },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            Column(
                verticalArrangement = Arrangement.spacedBy(Spacing.item),
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Spacing.screen),
            ) {
                Text(stringResource(R.string.backup_intro), style = MaterialTheme.typography.bodyLarge)
                if (!state.loading) {
                    Text(lastBackupText(state.lastBackupAt), style = MaterialTheme.typography.titleMedium)
                }
                Button(
                    onClick = { saveLauncher.launch(vm.backupFileName()) },
                    enabled = !state.busy,
                    modifier = Modifier.fillMaxWidth().heightIn(min = Spacing.buttonHeight),
                ) { Text(stringResource(R.string.backup_now)) }
                Text(stringResource(R.string.backup_where), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

                Text(
                    stringResource(R.string.backup_restore_title),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = Spacing.l).semantics { heading() },
                )
                Text(stringResource(R.string.backup_restore_body), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedButton(
                    onClick = { openLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) },
                    enabled = !state.busy,
                    modifier = Modifier.fillMaxWidth().heightIn(min = Spacing.buttonHeight),
                ) { Text(stringResource(R.string.backup_restore_button)) }

                Text(
                    stringResource(R.string.csv_title),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = Spacing.l).semantics { heading() },
                )
                Text(stringResource(R.string.csv_body), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedButton(
                    onClick = { csvLauncher.launch(vm.csvFileName()) },
                    enabled = !state.busy,
                    modifier = Modifier.fillMaxWidth().heightIn(min = Spacing.buttonHeight),
                ) { Text(stringResource(R.string.csv_button)) }
            }
        }
    }

    state.preview?.let { preview -> RestoreDialog(preview, onConfirm = vm::onRestoreConfirm, onDismiss = vm::onPreviewDismiss) }
}

/** Shows what the file holds and asks before replacing anything, or says why it can't be used. */
@Composable
private fun RestoreDialog(preview: RestorePreview, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    when (preview) {
        is RestorePreview.Ready -> {
            val counts = preview.pending.counts
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text(stringResource(R.string.backup_confirm_title)) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
                        if (counts.createdAt > 0) {
                            Text(stringResource(R.string.backup_confirm_made, DateFormatter.full(counts.createdAt / 86_400_000L)))
                        }
                        Text(
                            listOf(
                                pluralStringResource(R.plurals.backup_invoices, counts.invoices, counts.invoices),
                                pluralStringResource(R.plurals.backup_clients, counts.clients, counts.clients),
                                pluralStringResource(R.plurals.backup_presets, counts.presets, counts.presets),
                            ).joinToString(" · "),
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Text(stringResource(R.string.backup_confirm_body))
                    }
                },
                confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(R.string.backup_confirm_replace)) } },
                dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
            )
        }
        is RestorePreview.Newer -> InfoDialog(stringResource(R.string.backup_newer), onDismiss)
        RestorePreview.Unreadable -> InfoDialog(stringResource(R.string.backup_unreadable), onDismiss)
        is RestorePreview.Invalid -> InfoDialog(
            stringResource(R.string.backup_invalid) + "\n\n" + preview.problems.sorted().map { problemText(it) }.joinToString("\n") { "• $it" },
            onDismiss,
        )
    }
}

@Composable
private fun InfoDialog(text: String, onDismiss: () -> Unit) = AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(stringResource(R.string.backup_cant_restore)) },
    text = { Text(text) },
    confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_ok)) } },
)

@Composable
private fun problemText(problem: BackupProblem): String = stringResource(
    when (problem) {
        BackupProblem.NOT_POCKETBILL -> R.string.backup_problem_not_pocketbill
        BackupProblem.NO_PROFILE, BackupProblem.BAD_PROFILE -> R.string.backup_problem_profile
        BackupProblem.DUPLICATE_ID, BackupProblem.DUPLICATE_UUID -> R.string.backup_problem_duplicates
        BackupProblem.DUPLICATE_NUMBER -> R.string.backup_problem_numbers
        BackupProblem.MISSING_CLIENT, BackupProblem.MISSING_INVOICE -> R.string.backup_problem_links
        BackupProblem.BAD_STATUS -> R.string.backup_problem_status
        BackupProblem.BAD_CURRENCY -> R.string.backup_problem_currency
        BackupProblem.BAD_AMOUNT -> R.string.backup_problem_amounts
        BackupProblem.BAD_DATES -> R.string.backup_problem_dates
        BackupProblem.BAD_TEXT -> R.string.backup_problem_text
    },
)

/** Reads at most [limit] bytes (InputStream.readNBytes needs Android 13). */
private fun java.io.InputStream.readNBytesCompat(limit: Long): ByteArray {
    val out = java.io.ByteArrayOutputStream()
    val buffer = ByteArray(64 * 1024)
    var total = 0L
    while (total < limit) {
        val n = read(buffer, 0, minOf(buffer.size.toLong(), limit - total).toInt())
        if (n < 0) break
        out.write(buffer, 0, n)
        total += n
    }
    return out.toByteArray()
}
