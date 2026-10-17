package com.example.pocketbill.data.export

import com.example.pocketbill.data.db.ClientEntity
import com.example.pocketbill.data.db.InvoiceEntity
import com.example.pocketbill.domain.invoice.InvoiceStatus
import com.example.pocketbill.domain.invoice.SnapshotJson
import com.example.pocketbill.domain.invoice.ToSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** F-19: the accountant's CSV. */
class InvoiceCsvTest {
    private val issue = LocalDate.of(2026, 10, 16).toEpochDay()
    private val client = ClientEntity(id = 1, uuid = "c", name = "Live name", email = "live@x.example", createdAt = 0, updatedAt = 0)

    private fun inv(id: Long, status: InvoiceStatus, name: String = "Müller Design GmbH", currency: String = "EUR", total: Long = 57_000) = InvoiceEntity(
        id = id, uuid = "i$id", number = if (status == InvoiceStatus.DRAFT) null else "INV-%04d".format(id), status = status,
        clientId = 1, currency = currency, issueDate = issue + id, dueDate = issue + 14, paidDate = if (status == InvoiceStatus.PAID) issue + 5 else null,
        subtotalMinor = total, totalMinor = total, toSnapshot = SnapshotJson.encode(ToSnapshot(name = name, email = "billing@mueller.example")),
        createdAt = 0, updatedAt = 0,
    )

    private fun lines(csv: String) = csv.removePrefix("\uFEFF").trimEnd().split("\r\n")

    @Test fun `header and one row per non-draft invoice, oldest first`() {
        val csv = InvoiceCsv.build(listOf(inv(2, InvoiceStatus.PAID), inv(1, InvoiceStatus.ISSUED), inv(3, InvoiceStatus.DRAFT), inv(4, InvoiceStatus.VOID)), listOf(client))
        val rows = lines(csv)
        assertEquals(InvoiceCsv.HEADER.joinToString(","), rows[0])
        assertEquals(listOf("INV-0001", "INV-0002", "INV-0004"), rows.drop(1).map { it.substringBefore(',') })
        assertEquals("INV-0002,Paid,Müller Design GmbH,billing@mueller.example,2026-10-18,2026-10-30,2026-10-21,EUR,570.00,0.00,0.00,570.00,", rows[2])
        assertTrue(rows[3].startsWith("INV-0004,Void,"))
    }

    @Test fun `starts with a byte-order mark so Excel reads accents`() =
        assertTrue(InvoiceCsv.build(emptyList(), emptyList()).startsWith("\uFEFFNumber,"))

    @Test fun `zero-decimal currencies have no decimals`() {
        val row = lines(InvoiceCsv.build(listOf(inv(1, InvoiceStatus.ISSUED, currency = "JPY", total = 4_500)), listOf(client)))[1]
        assertTrue(row, row.endsWith(",JPY,4500,0,0,4500,"))
    }

    @Test fun `commas and quotes are quoted`() {
        val row = lines(InvoiceCsv.build(listOf(inv(1, InvoiceStatus.ISSUED, name = "Smith, \"Jones\" & Co")), listOf(client)))[1]
        assertTrue(row, row.contains(",\"Smith, \"\"Jones\"\" & Co\","))
    }

    @Test fun `names that look like formulas can't run in a spreadsheet`() {
        val row = lines(InvoiceCsv.build(listOf(inv(1, InvoiceStatus.ISSUED, name = "=HYPERLINK(\"x\")")), listOf(client)))[1]
        assertTrue(row, row.contains(",\"'=HYPERLINK(\"\"x\"\")\","))
    }

    @Test fun `file name has the date`() =
        assertEquals("PocketBill-invoices-2026-10-17.csv", InvoiceCsv.fileName(LocalDate.of(2026, 10, 17)))
}
