package com.example.pocketbill.data

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.pocketbill.data.db.PocketBillDatabase
import com.example.pocketbill.domain.invoice.InvoiceStatus
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * DATA_MODEL 7.3: real rows written in the old schema must read back correctly after upgrading.
 * Uses the exported schema JSON in app/schemas (copied into the test assets by the Room plugin).
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {
    private val dbName = "migration-test.db"

    @get:Rule val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), PocketBillDatabase::class.java)

    @After fun cleanUp() {
        ApplicationProvider.getApplicationContext<android.content.Context>().deleteDatabase(dbName)
    }

    @Test fun version1RowsSurviveTheUpgradeToVersion2() {
        helper.createDatabase(dbName, 1).use { db ->
            db.execSQL(
                "INSERT INTO business_profile (id, uuid, name, contactLines, address, taxId, paymentInstructions, logoFile, " +
                    "defaultCurrency, defaultTermsDays, defaultTaxRateBp, invoicePrefix, nextInvoiceNumber, paperSize, lastBackupAt, createdAt, updatedAt) " +
                    "VALUES (1, 'p1', 'Your Studio', '', 'Karachi', NULL, 'IBAN 1', NULL, 'USD', 14, 0, 'INV-', 3, 'A4', NULL, 1, 1)",
            )
            db.execSQL(
                "INSERT INTO client (id, uuid, name, email, address, country, taxId, notes, archived, lastInvoicedAt, createdAt, updatedAt) " +
                    "VALUES (1, 'c1', 'Müller Design GmbH', 'billing@mueller.example', NULL, 'Germany', NULL, NULL, 0, 5, 1, 1)",
            )
            db.execSQL(
                "INSERT INTO invoice (id, uuid, number, status, clientId, currency, issueDate, dueDate, paidDate, taxRateBp, discountType, " +
                    "discountValue, notes, subtotalMinor, discountMinor, taxMinor, totalMinor, fromSnapshot, toSnapshot, lastSentAt, lastSentVia, createdAt, updatedAt) " +
                    "VALUES (1, 'i1', 'INV-0002', 'ISSUED', 1, 'EUR', 100, 114, NULL, 0, NULL, NULL, '', 57000, 0, 0, 57000, '{}', '{}', NULL, NULL, 1, 1)",
            )
            db.execSQL(
                "INSERT INTO line_item (id, uuid, invoiceId, position, description, quantityMilli, unitPriceMinor, amountMinor, createdAt, updatedAt) " +
                    "VALUES (1, 'l1', 1, 0, 'Logo design', 1000, 45000, 45000, 1, 1)",
            )
        }

        // Validates the migrated schema against version 2's exported JSON.
        helper.runMigrationsAndValidate(dbName, 2, true).close()

        // And the app's own Room code reads everything back.
        val room = Room.databaseBuilder(ApplicationProvider.getApplicationContext(), PocketBillDatabase::class.java, dbName).build()
        try {
            runBlocking {
                val profile = room.profileDao().get()!!
                assertEquals("Your Studio", profile.name)
                assertEquals(3L, profile.nextInvoiceNumber)
                val client = room.clientDao().get(1)!!
                assertEquals("Müller Design GmbH", client.name)
                assertNull("new column starts empty", client.preferredCurrency)
                assertNull(client.preferredTermsDays)
                val invoice = room.invoiceDao().getWithLines(1)!!
                assertEquals(InvoiceStatus.ISSUED, invoice.invoice.status)
                assertEquals("INV-0002", invoice.invoice.number)
                assertEquals(57_000L, invoice.invoice.totalMinor)
                assertEquals("Logo design", invoice.lines.single().description)
            }
        } finally {
            room.close()
        }
    }
}
