package com.example.pocketbill.data.repository

import androidx.room.withTransaction
import com.example.pocketbill.data.db.BusinessProfileEntity
import com.example.pocketbill.data.db.PocketBillDatabase
import com.example.pocketbill.domain.Outcome
import com.example.pocketbill.domain.Problem
import com.example.pocketbill.domain.invoice.PaperSize
import com.example.pocketbill.domain.money.CurrencyCode
import com.example.pocketbill.domain.money.MoneyLimits
import com.example.pocketbill.domain.time.Clock
import kotlinx.coroutines.flow.Flow
import java.util.UUID

/** Business profile fields set in Setup and Settings (MVP_SPEC.md F-01). */
data class ProfileInput(
    val name: String,
    val defaultCurrency: String,
    val contactLines: String = "",
    val address: String = "",
    val taxId: String? = null,
    val paymentInstructions: String = "",
    val defaultTermsDays: Int = 14,
    val defaultTaxRateBp: Int = 0,
    val invoicePrefix: String = "INV-",
    val nextInvoiceNumber: Long = 1,
    val paperSize: PaperSize = PaperSize.A4,
)

/** The only code that reads or writes the business profile (ARCHITECTURE.md section 3). */
class ProfileRepository(
    private val db: PocketBillDatabase,
    private val clock: Clock,
    private val newUuid: () -> String = { UUID.randomUUID().toString() },
) {
    private val profiles = db.profileDao()
    private val invoices = db.invoiceDao()

    fun observe(): Flow<BusinessProfileEntity?> = profiles.observe()

    suspend fun get(): BusinessProfileEntity? = profiles.get()

    /** How many issued, unpaid invoices would take new details (DATA_MODEL 5.5 dialog). */
    suspend fun unpaidIssuedCount(): Int = invoices.allUnpaidIssued().size

    /**
     * Creates the profile (Setup) or saves changes (Settings). With [alsoUpdateUnpaid], issued
     * unpaid invoices take the new business details; the UI pre-selects this when the payment
     * instructions changed (DATA_MODEL 5.5). Paid and void invoices never change.
     */
    suspend fun save(input: ProfileInput, alsoUpdateUnpaid: Boolean = false): Outcome<Unit> {
        val problems = mutableSetOf<Problem>()
        if (input.name.isBlank()) problems += Problem.NO_BUSINESS_PROFILE
        if (CurrencyCode.fromCode(input.defaultCurrency) == null) problems += Problem.UNKNOWN_CURRENCY
        if (input.defaultTaxRateBp !in 0..MoneyLimits.MAX_TAX_BP) problems += Problem.BAD_TAX_RATE
        if (problems.isNotEmpty()) return Outcome.ValidationFailed(problems)

        val now = clock.nowMillis()
        return db.withTransaction {
            val existing = profiles.get()
            val profile = (existing ?: BusinessProfileEntity(uuid = newUuid(), name = "", defaultCurrency = "", createdAt = now, updatedAt = now))
                .copy(
                    name = input.name.trim(),
                    defaultCurrency = input.defaultCurrency,
                    contactLines = input.contactLines.trim(),
                    address = input.address.trim(),
                    taxId = input.taxId?.trim()?.ifEmpty { null },
                    paymentInstructions = input.paymentInstructions.trim(),
                    defaultTermsDays = input.defaultTermsDays,
                    defaultTaxRateBp = input.defaultTaxRateBp,
                    invoicePrefix = input.invoicePrefix,
                    // N-4: the sequence only increases; asking for a lower starting number is ignored here
                    // and issuing still skips taken numbers (N-7).
                    nextInvoiceNumber = maxOf(input.nextInvoiceNumber, existing?.nextInvoiceNumber ?: 1),
                    paperSize = input.paperSize,
                    updatedAt = now,
                )
            profiles.upsert(profile)
            if (alsoUpdateUnpaid) {
                val snapshot = profile.toSnapshotJson()
                invoices.allUnpaidIssued().forEach { invoices.update(it.copy(fromSnapshot = snapshot, updatedAt = now)) }
            }
            Outcome.Success(Unit)
        }
    }

    suspend fun recordBackup(at: Long = clock.nowMillis()) {
        profiles.get()?.let { profiles.upsert(it.copy(lastBackupAt = at, updatedAt = clock.nowMillis())) }
    }
}
