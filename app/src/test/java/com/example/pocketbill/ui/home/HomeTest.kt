package com.example.pocketbill.ui.home

import com.example.pocketbill.app.HomeCardPrefs
import com.example.pocketbill.data.db.ClientEntity
import com.example.pocketbill.data.db.InvoiceEntity
import com.example.pocketbill.domain.invoice.InvoiceStatus
import com.example.pocketbill.domain.time.FixedClock
import com.example.pocketbill.share.SendPackage
import com.example.pocketbill.ui.MainDispatcherRule
import com.example.pocketbill.ui.clients.FakeClientStore
import com.example.pocketbill.ui.clients.Owed
import com.example.pocketbill.ui.invoices.FakeInvoiceBrowser
import com.example.pocketbill.ui.navigation.InvoiceFilter
import com.example.pocketbill.ui.send.SendRequest
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Home (UX_SPEC 2): outstanding per currency, overdue row, Needs attention, Recent. */
class HomeTest {
    @get:Rule val main = MainDispatcherRule()

    private val today = 20_000L
    private val acme = ClientEntity(id = 1, uuid = "c1", name = "Acme", email = "a@acme.example", createdAt = 0, updatedAt = 0)
    private val lumen = ClientEntity(id = 2, uuid = "c2", name = "Lumen", createdAt = 0, updatedAt = 0)

    private fun inv(
        id: Long,
        status: InvoiceStatus,
        currency: String = "USD",
        total: Long = 1_000,
        due: Long = today + 5,
        issue: Long = today - 5,
        sentAt: Long? = 1L,
        client: Long = 1,
        updatedAt: Long = id,
    ) = InvoiceEntity(
        id = id, uuid = "i$id", number = if (status == InvoiceStatus.DRAFT) null else "INV-%04d".format(id), status = status,
        clientId = client, currency = currency, issueDate = issue, dueDate = due, totalMinor = total, lastSentAt = sentAt,
        createdAt = 0, updatedAt = updatedAt,
    )

    @Test fun `first use shows the welcome`() {
        val home = buildHome(emptyList(), emptyList(), today)
        assertTrue(home.noInvoices)
        assertTrue(home.outstanding.isEmpty())
    }

    @Test fun `outstanding is per currency, most unpaid invoices first, then by code, never mixed`() {
        val home = buildHome(
            listOf(
                inv(1, InvoiceStatus.ISSUED, "USD", 120_000),
                inv(2, InvoiceStatus.ISSUED, "EUR", 30_000),
                inv(3, InvoiceStatus.ISSUED, "EUR", 27_000),
                inv(4, InvoiceStatus.ISSUED, "GBP", 80_000),
                inv(5, InvoiceStatus.PAID, "EUR", 99_999),
                inv(6, InvoiceStatus.DRAFT, "EUR", 99_999),
                inv(7, InvoiceStatus.VOID, "EUR", 99_999),
            ),
            listOf(acme), today,
        )
        assertEquals(
            listOf(OutstandingLine("EUR", 57_000, 2), OutstandingLine("GBP", 80_000, 1), OutstandingLine("USD", 120_000, 1)),
            home.outstanding,
        )
        assertEquals(4, home.unpaidCount)
    }

    @Test fun `overdue row counts and totals only overdue invoices`() {
        val home = buildHome(
            listOf(inv(1, InvoiceStatus.ISSUED, "USD", 120_000, due = today - 8), inv(2, InvoiceStatus.ISSUED, "USD", 5_000)),
            listOf(acme), today,
        )
        assertEquals(1, home.overdueCount)
        assertEquals(listOf(Owed("USD", 120_000)), home.overdueTotals)
    }

    @Test fun `needs attention is overdue oldest first, then unsent, then drafts newest first, at most 3`() {
        val home = buildHome(
            listOf(
                inv(1, InvoiceStatus.ISSUED, due = today - 2),
                inv(2, InvoiceStatus.ISSUED, due = today - 8, client = 2),
                inv(3, InvoiceStatus.ISSUED, sentAt = null),
                inv(4, InvoiceStatus.DRAFT, updatedAt = 10),
                inv(5, InvoiceStatus.DRAFT, updatedAt = 20),
                inv(6, InvoiceStatus.ISSUED), // sent, not overdue: nothing to do
            ),
            listOf(acme, lumen), today,
        )
        assertEquals(listOf(2L, 1L, 3L), home.attention.map { it.row.id })
        assertEquals(listOf(AttentionKind.OVERDUE, AttentionKind.OVERDUE, AttentionKind.UNSENT), home.attention.map { it.kind })
        assertEquals(8L, home.attention[0].daysOverdue)
        assertFalse("Lumen has no email: Send…", home.attention[0].hasEmail)
        assertTrue(home.attention[1].hasEmail)
        assertEquals(5, home.attentionTotal)
        assertEquals(InvoiceFilter.OVERDUE, home.attentionFilter)
    }

    @Test fun `an overdue unsent invoice appears once, as overdue`() {
        val home = buildHome(listOf(inv(1, InvoiceStatus.ISSUED, due = today - 1, sentAt = null)), listOf(acme), today)
        assertEquals(listOf(AttentionKind.OVERDUE), home.attention.map { it.kind })
    }

    @Test fun `only drafts shows nothing outstanding and the drafts`() {
        val home = buildHome(listOf(inv(1, InvoiceStatus.DRAFT)), listOf(acme), today)
        assertTrue(home.outstanding.isEmpty())
        assertFalse(home.allPaid)
        assertEquals(InvoiceFilter.DRAFTS, home.attentionFilter)
    }

    @Test fun `everything paid`() {
        val home = buildHome(listOf(inv(1, InvoiceStatus.PAID)), listOf(acme), today)
        assertTrue(home.allPaid)
        assertTrue(home.attention.isEmpty())
    }

    @Test fun `recent is the last 5 by last change`() {
        val home = buildHome((1L..7L).map { inv(it, InvoiceStatus.PAID, updatedAt = 100 - it) }, listOf(acme), today)
        assertEquals(listOf(1L, 2L, 3L, 4L, 5L), home.recent.map { it.id })
    }

    @Test fun `after payment details change, unpaid invoices sent before it are offered for resending`() {
        val changedAt = 5_000L
        val home = buildHome(
            listOf(
                inv(1, InvoiceStatus.ISSUED, sentAt = 1_000L), // sent with the old details: resend
                inv(2, InvoiceStatus.ISSUED, sentAt = 9_000L), // already re-sent after the change
                inv(3, InvoiceStatus.ISSUED, sentAt = null), // never sent: "not sent yet" covers it
                inv(4, InvoiceStatus.PAID, sentAt = 1_000L), // paid: nothing to do
            ),
            listOf(acme), today, paymentChangedAt = changedAt,
        )
        assertEquals(listOf(1L), home.resend.map { it.row.id })
        assertEquals(AttentionKind.RESEND, home.resend.single().kind)
        assertTrue(buildHome(listOf(inv(1, InvoiceStatus.ISSUED, sentAt = 1_000L)), listOf(acme), today).resend.isEmpty())
    }

    @Test fun `Remind opens the email app for that invoice`() {
        val browser = FakeInvoiceBrowser()
        val clients = FakeClientStore()
        browser.put(inv(9, InvoiceStatus.ISSUED, due = today - 3))
        val pkg = SendPackage(9, "INV-0009", "/f", "content://f", "f.pdf", "a@acme.example", null, "s", "b")
        val prefs = object : HomeCardPrefs {
            override var backupCardHiddenUntil = 0L
            override var paymentDetailsChangedAt = 0L
        }
        var due = true
        val vm = HomeViewModel(browser, clients, flowOf("Studio"), browser, { pkg }, FixedClock(today), backupDue = { due }, prefs = prefs)
        val item = vm.state.value.summary.attention.single()
        vm.onSendFromRow(item.copy(hasEmail = true))
        assertTrue(vm.send.value!!.state.value.request is SendRequest.Email)
        assertEquals("Studio", vm.state.value.businessName)

        // Backup card (MVP_SPEC 7): shown when due, ✕ hides it for 7 days.
        assertTrue(vm.state.value.showBackupCard)
        vm.onDismissBackupCard()
        assertFalse(vm.state.value.showBackupCard)
        assertEquals(FixedClock(today).nowMillis() + HomeViewModel.BACKUP_CARD_HIDE_MS, prefs.backupCardHiddenUntil)
    }
}
