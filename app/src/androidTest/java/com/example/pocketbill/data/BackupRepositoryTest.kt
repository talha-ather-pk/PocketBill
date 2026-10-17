package com.example.pocketbill.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.pocketbill.data.backup.BackupFormat
import com.example.pocketbill.data.backup.BackupRepository
import com.example.pocketbill.data.backup.PendingRestore
import com.example.pocketbill.data.backup.RestorePreview
import com.example.pocketbill.data.db.PocketBillDatabase
import com.example.pocketbill.data.repository.ClientInput
import com.example.pocketbill.data.repository.ClientRepository
import com.example.pocketbill.data.repository.InvoiceInput
import com.example.pocketbill.data.repository.InvoiceRepository
import com.example.pocketbill.data.repository.LineDraft
import com.example.pocketbill.data.repository.ProfileInput
import com.example.pocketbill.data.repository.ProfileRepository
import com.example.pocketbill.domain.Outcome
import com.example.pocketbill.domain.time.FixedClock
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.time.LocalDate

/**
 * MVP_SPEC 7 restore tests on a real (in-memory) database: full round trip field by field, a
 * damaged file, a newer version, a duplicate number, and a restore that fails half way. The
 * founder's data is never touched: everything runs in memory and in a test folder.
 */
@RunWith(AndroidJUnit4::class)
class BackupRepositoryTest {
    private lateinit var db: PocketBillDatabase
    private lateinit var backup: BackupRepository
    private lateinit var invoices: InvoiceRepository
    private lateinit var clients: ClientRepository
    private val today = LocalDate.of(2026, 10, 17).toEpochDay()
    private val clock = FixedClock(today)
    private val safetyDir = File(ApplicationProvider.getApplicationContext<android.content.Context>().cacheDir, "backup-test")

    @Before fun setUp() = runBlocking {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), PocketBillDatabase::class.java).build()
        backup = BackupRepository(db, clock, safetyDir, appVersion = "test")
        invoices = InvoiceRepository(db, clock)
        clients = ClientRepository(db, clock)
        ProfileRepository(db, clock).save(ProfileInput(name = "Øresund Design ApS", defaultCurrency = "EUR", paymentInstructions = "IBAN DK00"))
        Unit
    }

    @After fun tearDown() {
        db.close()
        safetyDir.deleteRecursively()
    }

    private fun <T> Outcome<T>.ok(): T = (this as? Outcome.Success)?.value ?: error("Expected success, got $this")

    /** Two clients, an issued invoice paid and sent, a draft, a void one. */
    private suspend fun fillBook() {
        val müller = clients.create(ClientInput(name = "Müller Design GmbH", email = "billing@mueller.example")).ok()
        val josé = clients.create(ClientInput(name = "José Ltda", country = "Brazil")).ok()
        fun input(client: Long?) = InvoiceInput(
            clientId = client, currency = "EUR", issueDate = today, dueDate = today + 14,
            lines = listOf(LineDraft("Logo — design", 1_500, 30_000), LineDraft("Revisions", 2_000, 6_000)),
        )
        val paid = invoices.saveDraft(0, input(müller)).ok()
        invoices.issue(paid).ok()
        invoices.recordSent(paid, "Gmail", 123L).ok()
        invoices.markPaid(paid, today + 2).ok()
        val void = invoices.saveDraft(0, input(josé)).ok()
        invoices.issue(void).ok()
        invoices.void(void).ok()
        invoices.saveDraft(0, input(null)).ok()
    }

    private suspend fun snapshot() = BackupFormat.toEntities((BackupFormat.decode(backup.export()) as com.example.pocketbill.data.backup.BackupRead.Ok).file)

    private fun ready(text: String): PendingRestore = (backup.preview(text) as RestorePreview.Ready).pending

    @Test fun roundTripRestoresIdenticalData() = runBlocking {
        fillBook()
        val before = snapshot()
        val text = backup.export()

        // Replace everything with a different book, then restore the original.
        backup.restore(ready(text.replace("\"name\": \"Øresund Design ApS\"", "\"name\": \"Temporary\"")))
        assertEquals("Temporary", db.backupDao().profile()!!.name)
        backup.restore(ready(text))

        val after = snapshot()
        assertEquals(before.profile!!.copy(logoFile = null), after.profile)
        assertEquals(before.clients, after.clients)
        assertEquals(before.invoices, after.invoices)
        assertEquals(before.lines, after.lines)
        assertEquals(before.presets, after.presets)
    }

    @Test fun restoreKeepsASafetyCopyOfTheCurrentData() = runBlocking {
        fillBook()
        val current = backup.export()
        backup.restore(ready(current.replace("\"name\": \"Øresund Design ApS\"", "\"name\": \"Other\"")))
        val safety = File(safetyDir, BackupRepository.SAFETY_FILE).readText()
        assertTrue(safety.contains("Øresund Design ApS"))
    }

    @Test fun badFilesChangeNothing() = runBlocking {
        fillBook()
        val before = snapshot()
        val text = backup.export()
        assertEquals(RestorePreview.Unreadable, backup.preview(text.dropLast(40)))
        assertEquals(RestorePreview.Newer(2), backup.preview(text.replace("\"formatVersion\": 1", "\"formatVersion\": 2")))
        val duplicate = text.replace("\"number\": \"INV-0002\"", "\"number\": \"INV-0001\"")
        assertTrue(backup.preview(duplicate) is RestorePreview.Invalid)
        assertEquals(before, snapshot())
    }

    @Test fun aRestoreThatFailsHalfWayRollsBack() = runBlocking {
        fillBook()
        val before = snapshot()
        val good = ready(backup.export())
        // Skips the checks on purpose: a repeated line uuid makes the last insert fail inside the transaction.
        val broken = good.copy(file = good.file.copy(lineItems = good.file.lineItems + good.file.lineItems.first().copy(id = 999)))
        val failed = runCatching { backup.restore(broken) }
        assertTrue("restore should fail", failed.isFailure)
        assertEquals(before, snapshot())
    }

    @Test fun backupReminderAfterTenInvoicesOrThirtyDays() = runBlocking {
        assertEquals(false, backup.reminderDue())
        val client = clients.create(ClientInput(name = "Acme")).ok()
        repeat(9) {
            invoices.saveDraft(0, InvoiceInput(clientId = client, currency = "EUR", issueDate = today, dueDate = today + 14, lines = listOf(LineDraft("x", 1_000, 100)))).ok()
        }
        assertEquals(false, backup.reminderDue())
        invoices.saveDraft(0, InvoiceInput(clientId = client, currency = "EUR", issueDate = today, dueDate = today + 14, lines = listOf(LineDraft("x", 1_000, 100)))).ok()
        assertEquals(true, backup.reminderDue())
        backup.recordBackup()
        assertEquals(false, backup.reminderDue())
    }
}
