package com.example.pocketbill.data.backup

import androidx.room.withTransaction
import com.example.pocketbill.data.LogoFiles
import com.example.pocketbill.data.db.PocketBillDatabase
import com.example.pocketbill.data.export.InvoiceCsv
import com.example.pocketbill.domain.time.Clock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Base64

/** A checked backup waiting for the user's "Replace" (MVP_SPEC 7: show counts → confirm). */
data class PendingRestore(val file: BackupFile, val counts: BackupCounts)

sealed interface RestorePreview {
    data class Ready(val pending: PendingRestore) : RestorePreview
    data class Newer(val formatVersion: Int) : RestorePreview
    data object Unreadable : RestorePreview
    data class Invalid(val problems: Set<BackupProblem>) : RestorePreview
}

/** What the Backup screen needs; [BackupRepository] is the real one, tests use a fake. */
interface BackupStore {
    /** The full backup as JSON text; also records the backup date once the caller has saved it. */
    suspend fun export(): String
    suspend fun recordBackup()
    suspend fun lastBackupAt(): Long?

    /** Invoices as a spreadsheet for an accountant (F-19). */
    suspend fun exportCsv(): String

    /** Reads and checks a file. Changes nothing. */
    fun preview(text: String): RestorePreview

    /** Writes a safety copy of the current data, then replaces everything in one transaction. */
    suspend fun restore(pending: PendingRestore)
}

/**
 * Backup and restore (MVP_SPEC 7, DATA_MODEL 7.4). Restore validates the whole file first and
 * replaces all data in **one** transaction, so any failure leaves the existing data untouched.
 * Before replacing anything it keeps a pre-restore copy of the current data in private storage.
 */
class BackupRepository(
    private val db: PocketBillDatabase,
    private val clock: Clock,
    private val safetyDir: File,
    private val appVersion: String,
    private val logos: LogoFiles? = null,
) : BackupStore {
    private val dao = db.backupDao()

    override suspend fun export(): String = db.withTransaction {
        // One transaction so the file is a consistent snapshot.
        val profile = dao.profile()
        val logo = profile?.logoFile?.let { logos?.read(it) }?.let { Base64.getEncoder().encodeToString(it) }
        BackupFormat.encode(
            BackupFormat.from(
                profile, dao.clients(), dao.invoices(), dao.lines(), dao.presets(),
                appVersion = appVersion, databaseVersion = DATABASE_VERSION, madeAt = clock.nowMillis(),
            ).copy(logo = logo),
        )
    }

    override suspend fun exportCsv(): String = db.withTransaction { InvoiceCsv.build(dao.invoices(), dao.clients(), clock::dayOf) }

    override suspend fun recordBackup() {
        db.withTransaction {
            val profile = dao.profile() ?: return@withTransaction
            db.profileDao().upsert(profile.copy(lastBackupAt = clock.nowMillis()))
        }
    }

    override suspend fun lastBackupAt(): Long? = dao.profile()?.lastBackupAt

    override fun preview(text: String): RestorePreview = when (val read = BackupFormat.decode(text)) {
        is BackupRead.Newer -> RestorePreview.Newer(read.formatVersion)
        BackupRead.Unreadable -> RestorePreview.Unreadable
        is BackupRead.Ok -> when (val check = BackupValidator.check(read.file)) {
            is BackupCheck.Valid -> RestorePreview.Ready(PendingRestore(read.file, check.counts))
            is BackupCheck.Invalid -> RestorePreview.Invalid(check.problems)
        }
    }

    override suspend fun restore(pending: PendingRestore) {
        val restored = BackupFormat.toEntities(pending.file)
        // The logo file is written before the transaction; an unused file is harmless if it fails.
        val logoName = pending.file.logo?.let { encoded -> logos?.write(Base64.getDecoder().decode(encoded)) }
        val entities = restored.copy(profile = restored.profile?.copy(logoFile = logoName))
        val safetyCopy = export()
        withContext(Dispatchers.IO) {
            safetyDir.mkdirs()
            // The last one is kept (MVP_SPEC 7).
            File(safetyDir, SAFETY_FILE).writeText(safetyCopy)
        }
        db.withTransaction {
            dao.deleteLines()
            dao.deleteInvoices()
            dao.deleteClients()
            dao.deletePresets()
            dao.deleteProfile()
            entities.profile?.let { dao.insertProfile(it) }
            dao.insertClients(entities.clients)
            dao.insertInvoices(entities.invoices)
            dao.insertLines(entities.lines)
            dao.insertPresets(entities.presets)
        }
    }

    /** Home's "Back up your invoices" card (MVP_SPEC 7): 10+ invoices since the last backup, or 30 days. */
    suspend fun reminderDue(): Boolean {
        val last = lastBackupAt()
        val now = clock.nowMillis()
        val newInvoices = dao.countCreatedAfter(last ?: 0)
        if (newInvoices == 0) return false
        if (newInvoices >= REMIND_AFTER_INVOICES) return true
        val since = last ?: dao.firstInvoiceCreatedAt() ?: return false
        return now - since >= REMIND_AFTER_DAYS * DAY_MS
    }

    companion object {
        const val SAFETY_FILE = "pre-restore-backup.json"
        const val REMIND_AFTER_INVOICES = 10
        const val REMIND_AFTER_DAYS = 30L
        private const val DAY_MS = 86_400_000L

        /** Matches @Database(version) in PocketBillDatabase; recorded in the file for support. */
        const val DATABASE_VERSION = 2
    }
}
