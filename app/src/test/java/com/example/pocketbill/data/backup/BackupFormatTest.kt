package com.example.pocketbill.data.backup

import com.example.pocketbill.data.db.BusinessProfileEntity
import com.example.pocketbill.data.db.ClientEntity
import com.example.pocketbill.data.db.InvoiceEntity
import com.example.pocketbill.data.db.ItemPresetEntity
import com.example.pocketbill.data.db.LineItemEntity
import com.example.pocketbill.domain.invoice.DiscountType
import com.example.pocketbill.domain.invoice.InvoiceStatus
import com.example.pocketbill.domain.invoice.PaperSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** MVP_SPEC 7 / DATA_MODEL 7.4: the backup file, its versions, and the checks before a restore. */
class BackupFormatTest {
    private val profile = BusinessProfileEntity(
        uuid = "p", name = "Øresund Design ApS", contactLines = "hej@oresund.example", paymentInstructions = "IBAN DK00",
        defaultCurrency = "DKK", paperSize = PaperSize.LETTER, nextInvoiceNumber = 15, lastBackupAt = 5, createdAt = 1, updatedAt = 2,
    )
    private val client = ClientEntity(id = 3, uuid = "c3", name = "Müller Design GmbH", email = "billing@mueller.example", preferredCurrency = "EUR", createdAt = 1, updatedAt = 2)
    private val invoices = listOf(
        InvoiceEntity(
            id = 10, uuid = "i10", number = "INV-0014", status = InvoiceStatus.PAID, clientId = 3, currency = "EUR", issueDate = 20_000,
            dueDate = 20_014, paidDate = 20_010, taxRateBp = 750, discountType = DiscountType.PERCENT, discountValue = 1_000,
            subtotalMinor = 57_000, discountMinor = 5_700, taxMinor = 3_848, totalMinor = 55_148, fromSnapshot = "{\"v\":1}",
            toSnapshot = "{\"v\":1,\"name\":\"Müller\"}", lastSentAt = 99, lastSentVia = "Gmail", createdAt = 1, updatedAt = 2,
        ),
        InvoiceEntity(id = 11, uuid = "i11", currency = "EUR", issueDate = 20_000, dueDate = 19_990, createdAt = 3, updatedAt = 4),
    )
    private val lines = listOf(
        LineItemEntity(id = 20, uuid = "l20", invoiceId = 10, position = 0, description = "Logo — José", quantityMilli = 1_500, unitPriceMinor = 30_000, amountMinor = 45_000, createdAt = 1, updatedAt = 2),
    )
    private val presets = listOf(ItemPresetEntity(id = 30, uuid = "s30", description = "Logo", unitPriceMinor = 30_000, currency = "EUR", useCount = 2, createdAt = 1, updatedAt = 2))

    private fun file() = BackupFormat.from(profile, listOf(client), invoices, lines, presets, appVersion = "0.1.0", databaseVersion = 2, madeAt = 1234)

    private fun decoded(text: String) = (BackupFormat.decode(text) as BackupRead.Ok).file

    @Test fun `round trip keeps every field, accents included`() {
        val text = BackupFormat.encode(file())
        val back = BackupFormat.toEntities(decoded(text))
        // lastBackupAt becomes the file's own date (1234), later than the stored 5.
        assertEquals(profile.copy(logoFile = null, lastBackupAt = 1234), back.profile)
        assertEquals(listOf(client), back.clients)
        assertEquals(invoices, back.invoices)
        assertEquals(lines, back.lines)
        assertEquals(presets, back.presets)
    }

    @Test fun `file is readable JSON with its version first`() {
        val text = BackupFormat.encode(file())
        assertTrue(text.startsWith("{\n    \"formatVersion\": 1,"))
        assertTrue(text.contains("\"name\": \"Müller Design GmbH\""))
    }

    @Test fun `a valid file reports its counts`() {
        assertEquals(BackupCheck.Valid(BackupCounts(clients = 1, invoices = 2, presets = 1, createdAt = 1234)), BackupValidator.check(file()))
    }

    @Test fun `newer versions are refused before anything else is read`() {
        assertEquals(BackupRead.Newer(2), BackupFormat.decode("""{"formatVersion": 2, "somethingNew": true}"""))
    }

    @Test fun `damaged or foreign files are unreadable`() {
        assertEquals(BackupRead.Unreadable, BackupFormat.decode("not json"))
        assertEquals(BackupRead.Unreadable, BackupFormat.decode("""{"hello": 1}"""))
        assertEquals(BackupRead.Unreadable, BackupFormat.decode(BackupFormat.encode(file()).dropLast(20)))
        assertEquals(BackupRead.Unreadable, BackupFormat.decode("""{"formatVersion": 0}"""))
    }

    @Test fun `the sample version 1 file in test resources still restores`() {
        // DATA_MODEL 7.4: one sample per formatVersion, never edited.
        val text = javaClass.getResource("/backup/backup-v1.json")!!.readText()
        assertTrue(BackupValidator.check(decoded(text)) is BackupCheck.Valid)
    }

    private fun problems(change: (BackupFile) -> BackupFile) = (BackupValidator.check(change(file())) as BackupCheck.Invalid).problems

    @Test fun `duplicate invoice numbers are refused`() {
        val f = file()
        assertEquals(setOf(BackupProblem.DUPLICATE_NUMBER), problems { it.copy(invoices = f.invoices + f.invoices[0].copy(id = 12, uuid = "i12")) })
    }

    @Test fun `links must point to records in the file`() {
        assertEquals(setOf(BackupProblem.MISSING_CLIENT), problems { it.copy(invoices = it.invoices.map { i -> i.copy(clientId = if (i.clientId == null) null else 99) }) })
        assertEquals(setOf(BackupProblem.MISSING_INVOICE), problems { it.copy(lineItems = it.lineItems.map { l -> l.copy(invoiceId = 99) }) })
    }

    @Test fun `money and statuses must be possible`() {
        assertEquals(setOf(BackupProblem.BAD_AMOUNT), problems { it.copy(invoices = it.invoices.map { i -> i.copy(totalMinor = -1) }) })
        assertEquals(setOf(BackupProblem.BAD_STATUS), problems { it.copy(invoices = it.invoices.map { i -> i.copy(status = "LOST") }) })
        assertEquals(setOf(BackupProblem.BAD_STATUS), problems { it.copy(invoices = it.invoices.map { i -> if (i.id == 10L) i.copy(paidDate = null) else i }) })
        assertEquals(setOf(BackupProblem.BAD_CURRENCY), problems { it.copy(invoices = it.invoices.map { i -> i.copy(currency = "XXX") }) })
    }

    @Test fun `issued invoices due before issue are refused, half-finished drafts are fine`() {
        assertEquals(setOf(BackupProblem.BAD_DATES), problems { it.copy(invoices = it.invoices.map { i -> if (i.id == 10L) i.copy(dueDate = 1) else i }) })
    }

    @Test fun `a backup without a business profile is refused`() {
        assertEquals(setOf(BackupProblem.NO_PROFILE), problems { it.copy(businessProfile = null) })
    }

    @Test fun `file name has the date`() =
        assertEquals("PocketBill-backup-2026-10-15.json", BackupFormat.fileName(LocalDate.of(2026, 10, 15)))
}
