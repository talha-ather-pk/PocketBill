package com.example.pocketbill.app

import android.content.Context
import com.example.pocketbill.data.backup.BackupRepository
import com.example.pocketbill.data.db.PocketBillDatabase
import com.example.pocketbill.data.repository.ClientRepository
import com.example.pocketbill.data.repository.InvoiceRepository
import com.example.pocketbill.data.repository.PresetRepository
import com.example.pocketbill.data.repository.ProfileRepository
import com.example.pocketbill.domain.time.Clock
import com.example.pocketbill.domain.time.DeviceClock
import com.example.pocketbill.pdf.InvoicePdfRenderer
import com.example.pocketbill.share.InvoiceSharePreparer
import java.io.File

/**
 * Creates and holds the objects shared across the app (manual dependency injection,
 * ARCHITECTURE.md section 4). ViewModels get what they need from here through a factory.
 * Everything is created on first use, so app start stays fast.
 *
 */
class AppContainer(
    private val appContext: Context,
    val clock: Clock = DeviceClock(),
) {
    val database: PocketBillDatabase by lazy { PocketBillDatabase.open(appContext) }

    val invoices: InvoiceRepository by lazy { InvoiceRepository(database, clock) }
    val clients: ClientRepository by lazy { ClientRepository(database, clock) }
    val profile: ProfileRepository by lazy { ProfileRepository(database, clock) }
    val presets: PresetRepository by lazy { PresetRepository(database, clock) }

    /** Writes invoice PDFs to the app's cache (MVP_SPEC 6), shared through FileProvider. */
    val pdfRenderer: InvoicePdfRenderer by lazy { InvoicePdfRenderer(File(appContext.cacheDir, "pdf")) }

    /** Backup file and restore (MVP_SPEC 7); the pre-restore safety copy goes to private storage. */
    val backup: BackupRepository by lazy {
        BackupRepository(database, clock, File(appContext.filesDir, "backups"), appVersion = appVersion())
    }

    /** Hides Home's backup card for 7 days after ✕ (a per-phone preference, not invoice data). */
    val homePrefs: HomePrefs by lazy { HomePrefs(appContext) }

    private fun appVersion(): String = try {
        appContext.packageManager.getPackageInfo(appContext.packageName, 0).versionName.orEmpty()
    } catch (e: android.content.pm.PackageManager.NameNotFoundException) {
        ""
    }

    /** Generates the PDF and its email text for sending (UX_SPEC 4). */
    val sharePreparer: InvoiceSharePreparer by lazy { InvoiceSharePreparer(appContext, invoices, profile, clients, pdfRenderer, clock) }
}
