package com.example.pocketbill.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pocketbill.data.backup.BackupFormat
import com.example.pocketbill.data.backup.BackupStore
import com.example.pocketbill.data.backup.RestorePreview
import com.example.pocketbill.domain.time.Clock
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

enum class BackupMessage { BACKED_UP, BACKUP_FAILED, RESTORED, RESTORE_FAILED, FILE_UNREADABLE }

data class BackupUiState(
    val loading: Boolean = true,
    val lastBackupAt: Long? = null,
    val busy: Boolean = false,
    /** A checked file waiting for confirmation, or why it can't be restored. */
    val preview: RestorePreview? = null,
    val message: BackupMessage? = null,
)

/**
 * Backup and restore (MVP_SPEC 7). The screen does the file I/O through the system pickers;
 * this decides what to write, checks what was read, and runs the restore only after "Replace".
 */
class BackupViewModel(private val store: BackupStore, private val clock: Clock) : ViewModel() {
    private val _state = MutableStateFlow(BackupUiState())
    val state: StateFlow<BackupUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch { _state.update { it.copy(loading = false, lastBackupAt = store.lastBackupAt()) } }
    }

    /** "PocketBill-backup-2026-10-17.json" for the save picker. */
    fun backupFileName(): String = BackupFormat.fileName(LocalDate.ofEpochDay(clock.todayEpochDay()))

    /** The file's text; the screen writes it to the place the user picked. */
    suspend fun backupText(): String {
        _state.update { it.copy(busy = true, message = null) }
        return try {
            store.export()
        } finally {
            _state.update { it.copy(busy = false) }
        }
    }

    fun onBackupWritten(ok: Boolean) {
        viewModelScope.launch {
            if (ok) store.recordBackup()
            _state.update {
                it.copy(message = if (ok) BackupMessage.BACKED_UP else BackupMessage.BACKUP_FAILED, lastBackupAt = store.lastBackupAt())
            }
        }
    }

    /** [text] is null when the picked file couldn't be read. */
    fun onRestoreFileRead(text: String?) {
        if (text == null) {
            _state.update { it.copy(message = BackupMessage.FILE_UNREADABLE) }
            return
        }
        _state.update { it.copy(preview = store.preview(text), message = null) }
    }

    fun onRestoreConfirm() {
        val ready = _state.value.preview as? RestorePreview.Ready ?: return
        _state.update { it.copy(preview = null, busy = true) }
        viewModelScope.launch {
            val ok = try {
                store.restore(ready.pending)
                true
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // The transaction rolled back: existing data is untouched (MVP_SPEC 7).
                false
            }
            _state.update {
                it.copy(busy = false, message = if (ok) BackupMessage.RESTORED else BackupMessage.RESTORE_FAILED, lastBackupAt = store.lastBackupAt())
            }
        }
    }

    fun onPreviewDismiss() = _state.update { it.copy(preview = null) }

    fun onMessageShown() = _state.update { it.copy(message = null) }
}
