package com.example.pocketbill.ui.send

import com.example.pocketbill.data.db.InvoiceEntity
import com.example.pocketbill.data.db.LineItemEntity
import com.example.pocketbill.domain.invoice.InvoiceStatus
import com.example.pocketbill.domain.invoice.SendKind
import com.example.pocketbill.domain.time.FixedClock
import com.example.pocketbill.share.SendPackage
import com.example.pocketbill.ui.MainDispatcherRule
import com.example.pocketbill.ui.clients.FakeClientStore
import com.example.pocketbill.ui.invoices.DetailMessage
import com.example.pocketbill.ui.invoices.DetailPrompt
import com.example.pocketbill.ui.invoices.FakeInvoiceBrowser
import com.example.pocketbill.ui.invoices.InvoiceDetailViewModel
import com.example.pocketbill.ui.invoices.PendingMessage
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Sending (P4-T09, UX_SPEC 4): which app opens, recording "Sent" with Undo, Save, Issue and send. */
@OptIn(ExperimentalCoroutinesApi::class)
class InvoiceSendFlowTest {
    @get:Rule val main = MainDispatcherRule()

    private val today = 20_000L
    private val clock = FixedClock(today, millis = 1_000_000L)
    private val browser = FakeInvoiceBrowser()
    private val clients = FakeClientStore()

    private fun invoice(status: InvoiceStatus, clientId: Long? = 1, sentAt: Long? = null, sentVia: String? = null) = InvoiceEntity(
        id = 7, uuid = "i7", number = if (status == InvoiceStatus.DRAFT) null else "INV-0007", status = status, clientId = clientId,
        currency = "EUR", issueDate = today, dueDate = today + 14, lastSentAt = sentAt, lastSentVia = sentVia, createdAt = 0, updatedAt = 0,
    )

    private val line = LineItemEntity(uuid = "l", invoiceId = 7, position = 0, description = "Logo", quantityMilli = 1000, unitPriceMinor = 100, amountMinor = 100, createdAt = 0, updatedAt = 0)

    private fun pkg(email: String?) = SendPackage(
        invoiceId = 7, title = "INV-0007", filePath = "/cache/pdf/x.pdf", fileUri = "content://x", fileName = "x.pdf",
        toEmail = email, kind = SendKind.INVOICE, subject = "s", body = "b",
    )

    /** A flow wired to the fake browser, observing it the way a ViewModel does. */
    private fun TestScope.flow(email: String?, prepared: () -> SendPackage? = { pkg(email) }): InvoiceSendFlow {
        val flow = InvoiceSendFlow(backgroundScope, browser, { prepared() }, clock, invoiceId = 7)
        backgroundScope.launch { browser.observe(7).collect { flow.observe(it?.invoice) } }
        return flow
    }

    @Test fun `client with email gets the email app, otherwise the share sheet`() = runTest(UnconfinedTestDispatcher()) {
        browser.put(invoice(InvoiceStatus.ISSUED), line)
        val withEmail = flow("a@b.example")
        withEmail.send(toEmail = true)
        assertTrue(withEmail.state.value.request is SendRequest.Email)

        val withoutEmail = flow(null)
        withoutEmail.send(toEmail = true)
        assertTrue(withoutEmail.state.value.request is SendRequest.Other)
    }

    @Test fun `opening the email app records sent, and Undo restores the previous values`() = runTest(UnconfinedTestDispatcher()) {
        browser.put(invoice(InvoiceStatus.ISSUED, sentAt = 5L, sentVia = "WhatsApp"), line)
        val flow = flow("a@b.example")
        flow.send(toEmail = true)
        flow.onRequestHandled()
        flow.onEmailOpened("Gmail")

        assertEquals(1_000_000L, browser.status(7).lastSentAt)
        assertEquals("Gmail", browser.status(7).lastSentVia)
        assertEquals(SendMessage.Sent("Gmail"), flow.state.value.message)

        flow.onUndoSent()
        assertEquals(5L, browser.status(7).lastSentAt)
        assertEquals("WhatsApp", browser.status(7).lastSentVia)
        assertNull(flow.state.value.message)
    }

    @Test fun `several email apps are recorded as email`() = runTest(UnconfinedTestDispatcher()) {
        browser.put(invoice(InvoiceStatus.ISSUED), line)
        val flow = flow("a@b.example")
        flow.send(toEmail = true)
        flow.onEmailOpened(null)
        assertEquals(InvoiceSendFlow.EMAIL_LABEL, browser.status(7).lastSentVia)
    }

    @Test fun `a share-sheet choice recorded elsewhere still shows Sent with Undo`() = runTest(UnconfinedTestDispatcher()) {
        browser.put(invoice(InvoiceStatus.ISSUED), line)
        val flow = flow(null)
        flow.send(toEmail = false)
        flow.onRequestHandled()
        // What SentReceiver does when the user picks WhatsApp:
        browser.recordSent(7, "WhatsApp", 42L)
        assertEquals(SendMessage.Sent("WhatsApp"), flow.state.value.message)
        flow.onUndoSent()
        assertNull(browser.status(7).lastSentAt)
    }

    @Test fun `no email app falls back to the share sheet and says so`() = runTest(UnconfinedTestDispatcher()) {
        browser.put(invoice(InvoiceStatus.ISSUED), line)
        val flow = flow("a@b.example")
        flow.send(toEmail = true)
        flow.onNoEmailApp(pkg("a@b.example"))
        assertEquals(SendMessage.NoEmailApp, flow.state.value.message)
        assertTrue(flow.state.value.request is SendRequest.Other)
    }

    @Test fun `saving does not count as sent`() = runTest(UnconfinedTestDispatcher()) {
        browser.put(invoice(InvoiceStatus.ISSUED), line)
        val flow = flow("a@b.example")
        flow.save()
        assertTrue(flow.state.value.request is SendRequest.Save)
        flow.onRequestHandled()
        flow.onSaveResult(ok = true)
        assertEquals(SendMessage.Saved, flow.state.value.message)
        assertNull(browser.status(7).lastSentAt)
    }

    @Test fun `PDF failure shows the message and no request`() = runTest(UnconfinedTestDispatcher()) {
        browser.put(invoice(InvoiceStatus.ISSUED), line)
        val flow = flow("a@b.example") { error("disk full") }
        flow.send(toEmail = true)
        assertEquals(SendMessage.PdfFailed, flow.state.value.message)
        assertNull(flow.state.value.request)
        assertTrue(!flow.state.value.busy)
    }

    // ---- Invoice detail: Issue and send ----------------------------------------------------------------

    private fun detail(inv: InvoiceEntity, vararg lines: LineItemEntity): InvoiceDetailViewModel {
        browser.put(inv, *lines)
        return InvoiceDetailViewModel(browser, clients, clock, inv.id, sending = browser, preparer = { pkg("a@b.example") })
    }

    @Test fun `issue and send asks first with the number, then issues and opens email`() = runTest(UnconfinedTestDispatcher()) {
        val client = clients.add("Acme", email = "billing@acme.example")
        val vm = detail(invoice(InvoiceStatus.DRAFT, clientId = client.id), line)
        vm.state.first { !it.loading }
        vm.onIssueAndSendRequest()
        assertEquals(DetailPrompt.ConfirmIssueAndSend("INV-0015"), vm.state.value.prompt)

        vm.onIssueAndSendConfirm()
        assertEquals(InvoiceStatus.ISSUED, browser.status(7).status)
        assertEquals("INV-0015", browser.status(7).number)
        assertTrue(vm.send.state.value.request is SendRequest.Email)
    }

    @Test fun `an incomplete draft is not issued`() = runTest(UnconfinedTestDispatcher()) {
        val vm = detail(invoice(InvoiceStatus.DRAFT, clientId = null))
        vm.onIssueAndSendConfirm()
        assertEquals(InvoiceStatus.DRAFT, browser.status(7).status)
        assertEquals(PendingMessage(DetailMessage.DRAFT_INCOMPLETE, canUndo = false), vm.state.value.message)
        assertNull(vm.send.state.value.request)
    }

    @Test fun `recipient is the client's current email, else the one frozen on the invoice (D-017)`() = runTest(UnconfinedTestDispatcher()) {
        val client = clients.add("Acme", email = "new@acme.example")
        val vm = detail(invoice(InvoiceStatus.ISSUED, clientId = client.id), line)
        assertEquals("new@acme.example", vm.state.first { !it.loading }.recipientEmail)
    }
}
