package com.example.pocketbill.app

import android.content.Context
import com.example.pocketbill.data.db.PocketBillDatabase
import com.example.pocketbill.data.repository.ClientRepository
import com.example.pocketbill.data.repository.InvoiceRepository
import com.example.pocketbill.data.repository.PresetRepository
import com.example.pocketbill.data.repository.ProfileRepository
import com.example.pocketbill.domain.time.Clock
import com.example.pocketbill.domain.time.DeviceClock

/**
 * Creates and holds the objects shared across the app (manual dependency injection,
 * ARCHITECTURE.md section 4). ViewModels get what they need from here through a factory.
 * Everything is created on first use, so app start stays fast.
 *
 * Added in later tasks: the PDF renderer (P4-T08), backup (P4-T12).
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
}
