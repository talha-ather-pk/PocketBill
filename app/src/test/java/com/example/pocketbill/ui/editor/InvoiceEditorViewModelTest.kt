package com.example.pocketbill.ui.editor

import com.example.pocketbill.data.db.BusinessProfileEntity
import com.example.pocketbill.data.db.InvoiceEntity
import com.example.pocketbill.data.db.ItemPresetEntity
import com.example.pocketbill.data.db.LineItemEntity
import com.example.pocketbill.domain.invoice.InvoiceStatus
import com.example.pocketbill.domain.time.FixedClock
import com.example.pocketbill.ui.MainDispatcherRule
import com.example.pocketbill.ui.clients.FakeClientStore
import com.example.pocketbill.ui.components.SaveStatus
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** The editor's states (P4-T06): autosave (D-013), client defaults (D-015), issuing, editing issued invoices. */
@OptIn(ExperimentalCoroutinesApi::class)
class InvoiceEditorViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    private val today = 20_000L
    private val invoiceStore = FakeInvoiceStore()
    private val clientStore = FakeClientStore()
    private val presetStore = FakePresetStore()
    private val profileStore = FakeProfileStore(
        BusinessProfileEntity(uuid = "p", name = "Your Studio", defaultCurrency = "USD", defaultTermsDays = 30, defaultTaxRateBp = 0, createdAt = 0, updatedAt = 0),
    )

    private fun editor(args: EditorArgs = EditorArgs()) =
        InvoiceEditorViewModel(invoiceStore, clientStore, profileStore, presetStore, FixedClock(today), args)

    /** Lets the 500 ms autosave delay pass. */
    private fun waitForAutosave() {
        main.dispatcher.scheduler.advanceTimeBy(InvoiceEditorViewModel.AUTOSAVE_DELAY_MS + 1)
        main.dispatcher.scheduler.runCurrent()
    }

    private fun InvoiceEditorViewModel.fillValidInvoice(clientId: Long) {
        onClientPicked(clientId)
        onLineChange(0) { it.copy(description = "Logo design", price = "450") }
    }

    // ---- New invoice and autosave ------------------------------------------------------------------

    @Test fun `new invoice starts from the business defaults and shows the next number`() {
        val s = editor().state.value
        assertEquals(EditorMode.NEW, s.mode)
        assertEquals("USD", s.form.currency)
        assertEquals(30, s.form.dueDays)
        assertEquals(today, s.form.issueDate)
        assertEquals("INV-0001", s.nextNumberPreview)
        assertEquals("nothing saved until something is entered", 0, invoiceStore.saveDraftCalls)
    }

    @Test fun `typing shows Saving, then one save 500 ms after the last change`() {
        val vm = editor()
        vm.onLineChange(0) { it.copy(description = "L") }
        vm.onLineChange(0) { it.copy(description = "Lo") }
        vm.onLineChange(0) { it.copy(description = "Logo") }
        assertEquals(SaveStatus.SAVING, vm.state.value.saveStatus)
        assertEquals(0, invoiceStore.saveDraftCalls)

        waitForAutosave()
        assertEquals("debounced into one save", 1, invoiceStore.saveDraftCalls)
        assertEquals(SaveStatus.SAVED, vm.state.value.saveStatus)
        assertEquals(EditorMode.DRAFT, vm.state.value.mode)
        assertEquals("Logo", invoiceStore.invoices[vm.state.value.invoiceId]!!.lines.single().description)
    }

    @Test fun `going to the background saves at once`() {
        val vm = editor()
        vm.onLineChange(0) { it.copy(description = "Logo") }
        vm.flush()
        assertEquals(1, invoiceStore.saveDraftCalls)
        assertEquals(SaveStatus.SAVED, vm.state.value.saveStatus)
    }

    @Test fun `totals recalculate live`() {
        val vm = editor()
        vm.onLineChange(0) { it.copy(description = "Logo design", price = "450") }
        vm.onAddLine()
        vm.onLineChange(1) { it.copy(description = "Extra revisions", quantity = "2", price = "60") }
        assertEquals(57_000, vm.state.value.totals.totalMinor)
        vm.onTaxRateChange("10")
        assertEquals(62_700, vm.state.value.totals.totalMinor)
        vm.onRemoveLine(1)
        assertEquals(49_500, vm.state.value.totals.totalMinor)
    }

    // ---- Client defaults (D-015) ----------------------------------------------------------------

    @Test fun `picking a client applies their preferred currency and terms`() {
        val client = clientStore.add("Müller Design GmbH", preferredCurrency = "EUR", preferredTermsDays = 14)
        val vm = editor()
        vm.onClientPicked(client.id)
        val f = vm.state.value.form
        assertEquals(client.id, f.clientId)
        assertEquals("EUR", f.currency)
        assertEquals(14, f.dueDays)
        assertEquals(client.id, vm.state.value.selectedClient!!.id)
    }

    @Test fun `without preferences the client's last invoice is used`() {
        val client = clientStore.add("Acme Studio")
        invoiceStore.put(InvoiceEntity(id = 1, uuid = "x", clientId = client.id, currency = "GBP", issueDate = 10, dueDate = 17, taxRateBp = 2_000, createdAt = 0, updatedAt = 0))
        val vm = editor()
        vm.onClientPicked(client.id)
        assertEquals("GBP", vm.state.value.form.currency)
        assertEquals(7, vm.state.value.form.dueDays)
        assertEquals("20", vm.state.value.form.taxRate)
    }

    @Test fun `a currency changed by hand is not overwritten by the client`() {
        val client = clientStore.add("Müller Design GmbH", preferredCurrency = "EUR")
        val vm = editor()
        vm.onCurrencyChange("JPY")
        vm.onClientPicked(client.id)
        assertEquals("JPY", vm.state.value.form.currency)
    }

    @Test fun `opening the editor for a client fills it in without creating a draft`() {
        val client = clientStore.add("Acme Studio", preferredCurrency = "CAD")
        val vm = editor(EditorArgs(clientId = client.id))
        assertEquals("CAD", vm.state.value.form.currency)
        waitForAutosave()
        assertEquals(0, invoiceStore.saveDraftCalls)
    }

    // ---- Issuing ----------------------------------------------------------------------------------

    @Test fun `issue with problems shows them and issues nothing`() {
        val vm = editor()
        vm.onIssue()
        val errors = vm.state.value.errors
        assertTrue(EditorError.NoClient in errors)
        assertTrue(EditorError.NoLines in errors)
        assertEquals(0, invoiceStore.issueCalls)
        // Errors update as fields are fixed.
        vm.onClientPicked(clientStore.add("Acme").id)
        assertTrue(EditorError.NoClient !in vm.state.value.errors)
    }

    @Test fun `issue saves the latest form first and returns the number`() {
        val vm = editor()
        vm.fillValidInvoice(clientStore.add("Acme").id)
        vm.onIssue()
        val result = vm.state.value.result as EditorResult.Issued
        assertEquals("INV-0001", result.number)
        val issued = invoiceStore.invoices[result.invoiceId]!!
        assertEquals(InvoiceStatus.ISSUED, issued.invoice.status)
        assertEquals("Logo design", issued.lines.single().description)
    }

    @Test fun `numbers advance with each issued invoice`() {
        val client = clientStore.add("Acme").id
        val first = editor().apply { fillValidInvoice(client); onIssue() }.state.value.result as EditorResult.Issued
        val nextEditor = editor()
        assertEquals("INV-0002", nextEditor.state.value.nextNumberPreview)
        val second = nextEditor.apply { fillValidInvoice(client); onIssue() }.state.value.result as EditorResult.Issued
        assertEquals(listOf("INV-0001", "INV-0002"), listOf(first.number, second.number))
    }

    @Test fun `a zero total asks before issuing`() {
        val vm = editor()
        vm.onClientPicked(clientStore.add("Acme").id)
        vm.onLineChange(0) { it.copy(description = "Free consultation", price = "0") }
        vm.onIssue()
        assertTrue(vm.state.value.prompt is EditorPrompt.ConfirmZeroTotal)
        assertEquals(0, invoiceStore.issueCalls)
        vm.onConfirmZeroTotal()
        assertTrue(vm.state.value.result is EditorResult.Issued)
    }

    // ---- Leaving, deleting ------------------------------------------------------------------------

    @Test fun `back from an untouched new invoice leaves no draft`() {
        val vm = editor()
        vm.onBack()
        assertEquals(EditorResult.Closed(draftSaved = false), vm.state.value.result)
        assertEquals(0, invoiceStore.saveDraftCalls)
    }

    @Test fun `back from a changed draft saves it without asking`() {
        val vm = editor()
        vm.onLineChange(0) { it.copy(description = "Logo") }
        vm.onBack()
        assertNull(vm.state.value.prompt)
        assertEquals(EditorResult.Closed(draftSaved = true), vm.state.value.result)
        assertEquals(1, invoiceStore.invoices.size)
    }

    @Test fun `deleting a draft removes it`() {
        val vm = editor()
        vm.onLineChange(0) { it.copy(description = "Logo") }
        waitForAutosave()
        vm.onDeleteRequest()
        vm.onDeleteConfirm()
        assertEquals(EditorResult.DraftDeleted, vm.state.value.result)
        assertTrue(invoiceStore.invoices.isEmpty())
    }

    // ---- Editing an issued invoice ---------------------------------------------------------------

    private fun putIssued(clientId: Long): Long {
        invoiceStore.put(
            InvoiceEntity(id = 7, uuid = "i7", number = "INV-0007", status = InvoiceStatus.ISSUED, clientId = clientId, currency = "EUR", issueDate = today, dueDate = today + 14, createdAt = 0, updatedAt = 0),
            LineItemEntity(id = 1, uuid = "l1", invoiceId = 7, position = 0, description = "Logo design", quantityMilli = 1_000, unitPriceMinor = 45_000, amountMinor = 45_000, createdAt = 0, updatedAt = 0),
        )
        return 7
    }

    @Test fun `issued invoice is not autosaved and asks before discarding changes`() {
        val id = putIssued(clientStore.add("Acme").id)
        val vm = editor(EditorArgs(invoiceId = id))
        assertEquals(EditorMode.ISSUED, vm.state.value.mode)
        assertEquals("INV-0007", vm.state.value.issuedNumber)
        vm.onLineChange(0) { it.copy(price = "500") }
        waitForAutosave()
        assertEquals(0, invoiceStore.saveDraftCalls)
        vm.onBack()
        assertEquals(EditorPrompt.UnsavedChanges, vm.state.value.prompt)
    }

    @Test fun `save changes on an issued invoice keeps it issued`() {
        val id = putIssued(clientStore.add("Acme").id)
        val vm = editor(EditorArgs(invoiceId = id))
        vm.onLineChange(0) { it.copy(price = "500") }
        vm.onSaveChanges()
        assertEquals(EditorResult.SavedChanges(id), vm.state.value.result)
        assertEquals(1, invoiceStore.updateIssuedCalls)
        assertEquals(50_000L, invoiceStore.invoices[id]!!.lines.single().unitPriceMinor)
        assertEquals("INV-0007", invoiceStore.invoices[id]!!.invoice.number)
    }

    @Test fun `paid invoices can't be opened in the editor`() {
        invoiceStore.put(InvoiceEntity(id = 8, uuid = "i8", number = "INV-0008", status = InvoiceStatus.PAID, clientId = 1, currency = "EUR", issueDate = 1, dueDate = 2, createdAt = 0, updatedAt = 0))
        assertEquals(EditorResult.NotEditable, editor(EditorArgs(invoiceId = 8)).state.value.result)
    }

    // ---- Duplicate and saved items -----------------------------------------------------------------

    @Test fun `duplicate becomes a new draft with today's dates and no number`() {
        val id = putIssued(clientStore.add("Acme").id)
        val vm = editor(EditorArgs(duplicateOf = id))
        waitForAutosave()
        val s = vm.state.value
        assertEquals(EditorMode.DRAFT, s.mode)
        val copy = invoiceStore.invoices[s.invoiceId]!!
        assertEquals(InvoiceStatus.DRAFT, copy.invoice.status)
        assertNull(copy.invoice.number)
        assertEquals(today, copy.invoice.issueDate)
        assertEquals("Logo design", copy.lines.single().description)
    }

    @Test fun `saved items are suggested and fill the price in the same currency`() {
        presetStore.presets += ItemPresetEntity(id = 3, uuid = "p3", description = "Logo design", unitPriceMinor = 45_000, currency = "USD", createdAt = 0, updatedAt = 0)
        val vm = editor()
        vm.onLineChange(0) { it.copy(description = "Lo") }
        val suggestions = vm.state.value.suggestions!!
        assertEquals(0, suggestions.lineIndex)
        vm.onSuggestionPicked(suggestions.presets.single())
        val line = vm.state.value.form.lines.single()
        assertEquals("Logo design", line.description)
        assertEquals("450.00", line.price)
        assertEquals(listOf(3L), presetStore.used)
        assertNull(vm.state.value.suggestions)
    }

    @Test fun `a line can be saved as a saved item`() {
        val vm = editor()
        vm.onLineChange(0) { it.copy(description = "Brand guide", price = "1,200") }
        vm.onSaveLineAsPreset(0)
        assertEquals(Triple("Brand guide", 120_000L, "USD"), presetStore.saved.single())
    }
}
