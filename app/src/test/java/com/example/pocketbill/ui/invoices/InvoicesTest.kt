package com.example.pocketbill.ui.invoices

import com.example.pocketbill.data.db.InvoiceEntity
import com.example.pocketbill.data.db.LineDescription
import com.example.pocketbill.data.db.LineItemEntity
import com.example.pocketbill.domain.invoice.InvoiceStatus
import com.example.pocketbill.domain.invoice.SnapshotJson
import com.example.pocketbill.domain.invoice.ToSnapshot
import com.example.pocketbill.domain.time.FixedClock
import com.example.pocketbill.ui.MainDispatcherRule
import com.example.pocketbill.ui.clients.FakeClientStore
import com.example.pocketbill.ui.clients.Owed
import com.example.pocketbill.ui.components.DisplayStatus
import com.example.pocketbill.ui.navigation.InvoiceFilter
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

/** Invoices tab and invoice detail (P4-T10/T11, UX_SPEC 5): filtering, search, totals, status transitions. */
@OptIn(ExperimentalCoroutinesApi::class)
class InvoicesTest {
    @get:Rule val main = MainDispatcherRule()

    private val today = LocalDate.of(2026, 10, 17).toEpochDay()
    private val clients = FakeClientStore()
    private val browser = FakeInvoiceBrowser()

    private fun invoice(
        id: Long,
        status: InvoiceStatus,
        clientId: Long?,
        currency: String = "EUR",
        total: Long = 10_000,
        issue: Long = today - 5,
        due: Long = today + 9,
        paid: Long? = null,
        number: String? = if (status == InvoiceStatus.DRAFT) null else "INV-%04d".format(id),
        frozenName: String? = null,
        updatedAt: Long = 0,
    ) = InvoiceEntity(
        id = id, uuid = "i$id", number = number, status = status, clientId = clientId, currency = currency,
        issueDate = issue, dueDate = due, paidDate = paid, totalMinor = total,
        toSnapshot = frozenName?.let { SnapshotJson.encode(ToSnapshot(name = it)) }, createdAt = 0, updatedAt = updatedAt,
    )

    /** A typical book: one of each status, two currencies, one overdue. */
    private fun sampleBook(): List<InvoiceEntity> {
        val acme = clients.add("Acme Studio").id
        val muller = clients.add("Müller Design GmbH").id
        return listOf(
            invoice(1, InvoiceStatus.DRAFT, null, updatedAt = 5),
            invoice(2, InvoiceStatus.ISSUED, muller, total = 57_000, frozenName = "Müller Design GmbH"),
            invoice(3, InvoiceStatus.ISSUED, acme, currency = "USD", total = 120_000, issue = today - 40, due = today - 10, frozenName = "Acme Studio"),
            invoice(4, InvoiceStatus.PAID, acme, currency = "USD", total = 80_000, issue = today - 70, paid = today - 50, frozenName = "Acme Studio"),
            invoice(5, InvoiceStatus.VOID, muller, issue = today - 20, frozenName = "Müller Design GmbH"),
        )
    }

    private fun list(book: List<InvoiceEntity>, filter: InvoiceFilter, query: String = "", lines: List<LineDescription> = emptyList()) =
        buildInvoiceList(book, clients.clients.value, lines, filter, query, today)

    private fun ids(r: InvoiceListResult) = r.groups.flatMap { g -> g.rows.map { it.id } }

    // ---- Filtering and counts ---------------------------------------------------------------------

    @Test fun `each filter shows the right invoices and the chips count them`() {
        val book = sampleBook()
        assertEquals(listOf(1L), ids(list(book, InvoiceFilter.DRAFTS)))
        assertEquals("unpaid includes overdue, soonest due first", listOf(3L, 2L), ids(list(book, InvoiceFilter.UNPAID)))
        assertEquals(listOf(3L), ids(list(book, InvoiceFilter.OVERDUE)))
        assertEquals(listOf(4L), ids(list(book, InvoiceFilter.PAID)))
        assertEquals(listOf(5L), ids(list(book, InvoiceFilter.VOID)))
        assertEquals(5, ids(list(book, InvoiceFilter.ALL)).size)
        val counts = list(book, InvoiceFilter.ALL).counts
        assertEquals(mapOf(InvoiceFilter.ALL to 5, InvoiceFilter.DRAFTS to 1, InvoiceFilter.UNPAID to 2, InvoiceFilter.OVERDUE to 1, InvoiceFilter.PAID to 1, InvoiceFilter.VOID to 1), counts)
    }

    @Test fun `totals are per currency and never mixed, none for All`() {
        val book = sampleBook()
        assertEquals(listOf(Owed("USD", 120_000), Owed("EUR", 57_000)), list(book, InvoiceFilter.UNPAID).totals)
        assertEquals(listOf(Owed("USD", 80_000)), list(book, InvoiceFilter.PAID).totals)
        assertTrue(list(book, InvoiceFilter.ALL).totals.isEmpty())
    }

    @Test fun `All and Paid are grouped by month, newest first`() {
        val groups = list(sampleBook(), InvoiceFilter.ALL).groups
        assertEquals(listOf(2026 to 10, 2026 to 9, 2026 to 8), groups.map { it.yearMonth })
        assertNull(list(sampleBook(), InvoiceFilter.UNPAID).groups.single().yearMonth)
    }

    @Test fun `rows show the frozen client name for issued invoices and the live name for drafts`() {
        val acme = clients.add("Acme Studio")
        val book = listOf(
            invoice(1, InvoiceStatus.ISSUED, acme.id, frozenName = "Acme Studio Ltd (old name)"),
            invoice(2, InvoiceStatus.DRAFT, acme.id),
        )
        val rows = list(book, InvoiceFilter.ALL).groups.flatMap { it.rows }.associateBy { it.id }
        assertEquals("Acme Studio Ltd (old name)", rows.getValue(1).clientName)
        assertEquals("Acme Studio", rows.getValue(2).clientName)
        assertEquals(DisplayStatus.DRAFT, rows.getValue(2).status)
    }

    // ---- Search ------------------------------------------------------------------------------------

    @Test fun `search covers number, current and frozen names and item descriptions, across all statuses`() {
        val book = sampleBook()
        val lines = listOf(LineDescription(4, "Logo design"), LineDescription(2, "Brand guidelines"))
        assertEquals(listOf(3L), ids(list(book, InvoiceFilter.PAID, query = "INV-0003")))
        assertEquals("accents ignored; voided too", setOf(2L, 5L), ids(list(book, InvoiceFilter.UNPAID, query = "muller")).toSet())
        assertEquals(listOf(4L), ids(list(book, InvoiceFilter.UNPAID, query = "logo", lines = lines)))
        assertTrue(list(book, InvoiceFilter.ALL, query = "nothing like this").groups.isEmpty())
        assertTrue(list(book, InvoiceFilter.ALL, query = "x").searching)
    }

    @Test fun `search finds invoices issued before a client was renamed`() {
        val client = clients.add("Müller Studio AG")
        val book = listOf(invoice(1, InvoiceStatus.ISSUED, client.id, frozenName = "Müller Design GmbH"))
        assertEquals(listOf(1L), ids(list(book, InvoiceFilter.ALL, query = "Design GmbH")))
        assertEquals(listOf(1L), ids(list(book, InvoiceFilter.ALL, query = "studio ag")))
    }

    // ---- List ViewModel ------------------------------------------------------------------------------

    @Test fun `list view model follows filter and search changes`() {
        sampleBook().forEach { browser.put(it) }
        val vm = InvoiceListViewModel(browser, clients, FixedClock(today), InvoiceFilter.UNPAID)
        assertEquals(listOf(3L, 2L), ids(vm.state.value.result))
        vm.onFilterChange(InvoiceFilter.PAID)
        assertEquals(listOf(4L), ids(vm.state.value.result))
        vm.onQueryChange("acme")
        assertEquals(setOf(3L, 4L), ids(vm.state.value.result).toSet())
        assertFalse(vm.state.value.noInvoices)
    }

    @Test fun `list updates when an invoice changes status`() {
        sampleBook().forEach { browser.put(it) }
        val vm = InvoiceListViewModel(browser, clients, FixedClock(today), InvoiceFilter.UNPAID)
        browser.put(browser.status(2).copy(status = InvoiceStatus.PAID, paidDate = today))
        assertEquals(listOf(3L), ids(vm.state.value.result))
        assertEquals(2, vm.state.value.result.counts[InvoiceFilter.PAID])
    }

    // ---- Detail: status transitions ------------------------------------------------------------------

    private fun detailFor(inv: InvoiceEntity, vararg lines: LineItemEntity): InvoiceDetailViewModel {
        browser.put(inv, *lines)
        return InvoiceDetailViewModel(browser, clients, FixedClock(today), inv.id)
    }

    @Test fun `mark paid records today, and Undo makes it unpaid again`() {
        val vm = detailFor(invoice(2, InvoiceStatus.ISSUED, clients.add("Acme").id))
        vm.onMarkPaid()
        assertEquals(InvoiceStatus.PAID, browser.status(2).status)
        assertEquals(today, browser.status(2).paidDate)
        assertEquals(PendingMessage(DetailMessage.MARKED_PAID, canUndo = true), vm.state.value.message)
        assertEquals(DisplayStatus.PAID, vm.state.value.status)
        vm.onUndo()
        assertEquals(InvoiceStatus.ISSUED, browser.status(2).status)
        assertNull(browser.status(2).paidDate)
        assertNull(vm.state.value.message)
    }

    @Test fun `paid date can be changed for payments recorded late`() {
        val vm = detailFor(invoice(2, InvoiceStatus.PAID, 1, paid = today))
        vm.onChangePaidDateRequest()
        assertEquals(DetailPrompt.PickPaidDate(today), vm.state.value.prompt)
        vm.onPaidDatePicked(today - 3)
        assertEquals(today - 3, browser.status(2).paidDate)
        assertEquals(InvoiceStatus.PAID, browser.status(2).status)
    }

    @Test fun `mark unpaid, and Undo restores the original paid date`() {
        val vm = detailFor(invoice(2, InvoiceStatus.PAID, 1, paid = today - 4))
        vm.onMarkUnpaid()
        assertEquals(InvoiceStatus.ISSUED, browser.status(2).status)
        vm.onUndo()
        assertEquals(today - 4, browser.status(2).paidDate)
    }

    @Test fun `void asks first, keeps the number, and can be undone within 6 seconds`() {
        val vm = detailFor(invoice(2, InvoiceStatus.ISSUED, 1))
        vm.onVoidRequest()
        assertEquals(DetailPrompt.ConfirmVoid, vm.state.value.prompt)
        vm.onVoidConfirm()
        assertEquals(InvoiceStatus.VOID, browser.status(2).status)
        assertEquals("INV-0002", browser.status(2).number)
        vm.onUndo()
        assertEquals(InvoiceStatus.ISSUED, browser.status(2).status)
    }

    @Test fun `void can't be undone after 6 seconds`() {
        val vm = detailFor(invoice(2, InvoiceStatus.ISSUED, 1))
        vm.onVoidConfirm()
        main.dispatcher.scheduler.advanceTimeBy(InvoiceDetailViewModel.VOID_UNDO_MS + 1)
        main.dispatcher.scheduler.runCurrent()
        assertNull(vm.state.value.message)
        vm.onUndo()
        assertEquals(InvoiceStatus.VOID, browser.status(2).status)
    }

    @Test fun `paid invoices can't be voided and drafts can't be marked paid`() {
        val paid = detailFor(invoice(2, InvoiceStatus.PAID, 1, paid = today))
        paid.onVoidConfirm()
        assertEquals(InvoiceStatus.PAID, browser.status(2).status)
        assertEquals(DetailMessage.FAILED, paid.state.value.message!!.kind)
        val draft = detailFor(invoice(3, InvoiceStatus.DRAFT, null))
        draft.onMarkPaid()
        assertEquals(InvoiceStatus.DRAFT, browser.status(3).status)
    }

    @Test fun `deleting a draft closes the page`() {
        val vm = detailFor(invoice(3, InvoiceStatus.DRAFT, null))
        vm.onDeleteRequest()
        vm.onDeleteConfirm()
        assertTrue(vm.state.value.deleted)
        assertTrue(browser.invoices.value.isEmpty())
    }

    @Test fun `editing is offered only for drafts and unpaid invoices`() {
        assertTrue(detailFor(invoice(1, InvoiceStatus.DRAFT, null)).state.value.canEdit)
        assertTrue(detailFor(invoice(2, InvoiceStatus.ISSUED, 1)).state.value.canEdit)
        assertFalse(detailFor(invoice(3, InvoiceStatus.PAID, 1, paid = today)).state.value.canEdit)
        assertFalse(detailFor(invoice(4, InvoiceStatus.VOID, 1)).state.value.canEdit)
    }

    @Test fun `history shows issued, last sent and paid (D-011)`() {
        val vm = detailFor(
            invoice(2, InvoiceStatus.PAID, 1, issue = today - 10, paid = today - 1).copy(lastSentAt = (today - 9) * 86_400_000L, lastSentVia = "Gmail"),
        )
        val history = vm.state.value.history
        assertTrue(HistoryEntry.Issued(today - 10) in history)
        assertTrue(HistoryEntry.LastSent(today - 9, "Gmail") in history)
        assertTrue(HistoryEntry.Paid(today - 1) in history)
    }

    @Test fun `overdue status is shown for issued invoices past their due date`() {
        val vm = detailFor(invoice(3, InvoiceStatus.ISSUED, 1, due = today - 1))
        assertEquals(DisplayStatus.OVERDUE, vm.state.value.status)
    }
}
