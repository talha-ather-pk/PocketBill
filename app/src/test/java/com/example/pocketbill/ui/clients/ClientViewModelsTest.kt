package com.example.pocketbill.ui.clients

import com.example.pocketbill.data.db.ClientCount
import com.example.pocketbill.data.db.ClientOwed
import com.example.pocketbill.data.db.InvoiceEntity
import com.example.pocketbill.domain.Problem
import com.example.pocketbill.domain.invoice.InvoiceStatus
import com.example.pocketbill.ui.MainDispatcherRule
import com.example.pocketbill.ui.navigation.NO_ID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** ViewModel states for the client screens (P4-T05), with a fake store instead of the database. */
class ClientViewModelsTest {
    @get:Rule val mainDispatcher = MainDispatcherRule()

    private val store = FakeClientStore()

    // ---- Editor: new client ---------------------------------------------------------------------

    @Test fun `new client is saved and opens as new`() {
        val vm = ClientEditorViewModel(store, NO_ID)
        vm.onFormChange { it.copy(name = "Müller Design GmbH", email = "billing@mueller.example") }
        vm.onSave()
        val result = vm.state.value.result as ClientEditorResult.Saved
        assertTrue(result.isNew)
        assertEquals("Müller Design GmbH", store.clients.value.single().name)
    }

    @Test fun `empty name and bad email are shown as field errors, nothing saved`() {
        val vm = ClientEditorViewModel(store, NO_ID)
        vm.onFormChange { it.copy(name = " ", email = "not-an-email") }
        vm.onSave()
        val errors = vm.state.value.errors
        assertEquals(Problem.CLIENT_NAME_EMPTY, errors[ClientField.NAME])
        assertEquals(Problem.BAD_EMAIL, errors[ClientField.EMAIL])
        assertTrue(store.clients.value.isEmpty())
        assertNull(vm.state.value.result)
    }

    @Test fun `fixing a field clears its error`() {
        val vm = ClientEditorViewModel(store, NO_ID)
        vm.onSave()
        assertTrue(ClientField.NAME in vm.state.value.errors)
        vm.onFormChange { it.copy(name = "Acme") }
        assertFalse(ClientField.NAME in vm.state.value.errors)
    }

    @Test fun `duplicate name asks first, and Use existing returns the existing client`() {
        val existing = store.add("Müller Design GmbH")
        val vm = ClientEditorViewModel(store, NO_ID)
        vm.onFormChange { it.copy(name = "muller design gmbh") }
        vm.onSave()
        val prompt = vm.state.value.prompt as ClientPrompt.Duplicate
        assertEquals(existing.id, prompt.existingId)
        assertTrue(prompt.isNew)
        vm.onUseExisting()
        assertEquals(ClientEditorResult.UseExisting(existing.id), vm.state.value.result)
        assertEquals("no second client created", 1, store.clients.value.size)
    }

    @Test fun `duplicate name can be created anyway`() {
        store.add("Acme Studio")
        val vm = ClientEditorViewModel(store, NO_ID)
        vm.onFormChange { it.copy(name = "Acme Studio") }
        vm.onSave()
        vm.onSaveAnyway()
        assertTrue(vm.state.value.result is ClientEditorResult.Saved)
        assertEquals(2, store.clients.value.size)
    }

    @Test fun `leaving asks only after an edit, and not once saved`() {
        val client = store.add("Acme Studio")
        val vm = ClientEditorViewModel(store, client.id)
        assertFalse("just opened", vm.state.value.hasChanges)
        vm.onFormChange { it.copy(email = "pay@acme.example") }
        assertTrue("edited", vm.state.value.hasChanges)
        vm.onFormChange { it.copy(email = "") }
        assertFalse("edit undone by hand", vm.state.value.hasChanges)
        vm.onFormChange { it.copy(notes = "Net 30") }
        vm.onSave()
        assertFalse("saved", vm.state.value.hasChanges)
    }

    // ---- Editor: editing ----------------------------------------------------------------------

    @Test fun `editing without renaming never asks about duplicates`() {
        val client = store.add("Acme Studio")
        store.add("acme studio") // an older duplicate already exists
        val vm = ClientEditorViewModel(store, client.id)
        vm.onFormChange { it.copy(notes = "Pays late") }
        vm.onSave()
        assertNull(vm.state.value.prompt)
        assertEquals(ClientEditorResult.Saved(client.id, isNew = false), vm.state.value.result)
    }

    @Test fun `changing printed details with unpaid invoices asks, defaulting to only new invoices`() {
        val client = store.add("Müller Design GmbH", email = "old@mueller.example")
        store.unpaidIssued = mapOf(client.id to 2)
        val vm = ClientEditorViewModel(store, client.id)
        vm.onFormChange { it.copy(email = "new@mueller.example") }
        vm.onSave()
        val prompt = vm.state.value.prompt as ClientPrompt.UpdateUnpaid
        assertEquals(2, prompt.unpaidCount)
        assertFalse(prompt.alsoUpdate)

        vm.onUpdateChoice(true)
        vm.onUpdateConfirm()
        assertTrue("alsoUpdateUnpaid passed to the store", store.updates.single().third)
        assertTrue(vm.state.value.result is ClientEditorResult.Saved)
    }

    @Test fun `changing only notes does not ask about unpaid invoices`() {
        val client = store.add("Acme Studio")
        store.unpaidIssued = mapOf(client.id to 3)
        val vm = ClientEditorViewModel(store, client.id)
        vm.onFormChange { it.copy(notes = "Prefers WhatsApp") }
        vm.onSave()
        assertNull(vm.state.value.prompt)
        assertFalse(store.updates.single().third)
    }

    // ---- List -------------------------------------------------------------------------------------

    @Test fun `list searches by name, email and country ignoring accents`() {
        store.add("Müller Design GmbH", country = "Germany")
        store.add("Acme Studio", email = "billing@acme.example", country = "United States")
        val vm = ClientListViewModel(store)
        vm.onQueryChange("muller")
        assertEquals(listOf("Müller Design GmbH"), vm.state.value.rows.map { it.name })
        vm.onQueryChange("acme.example")
        assertEquals(listOf("Acme Studio"), vm.state.value.rows.map { it.name })
        vm.onQueryChange("germ")
        assertEquals(listOf("Müller Design GmbH"), vm.state.value.rows.map { it.name })
    }

    @Test fun `list sorts A to Z, or overdue and unpaid first`() {
        val a = store.add("Acme Studio")
        val b = store.add("Bright Ltd")
        val c = store.add("Cobalt")
        store.owed.value = listOf(ClientOwed(b.id, "EUR", 10_000, 1), ClientOwed(c.id, "USD", 5_000, 2))
        store.overdue.value = listOf(ClientCount(b.id, 1))
        val vm = ClientListViewModel(store)
        assertEquals(listOf(a.id, b.id, c.id), vm.state.value.rows.map { it.id })
        vm.onSortChange(ClientSort.MOST_OWED)
        assertEquals("overdue first, then most unpaid", listOf(b.id, c.id, a.id), vm.state.value.rows.map { it.id })
    }

    @Test fun `amounts owed stay per currency`() {
        val client = store.add("Acme Studio")
        store.owed.value = listOf(ClientOwed(client.id, "USD", 120_000, 1), ClientOwed(client.id, "EUR", 57_000, 1))
        val row = ClientListViewModel(store).state.value.rows.single()
        assertEquals(listOf(Owed("USD", 120_000), Owed("EUR", 57_000)), row.owed)
        assertEquals(2, row.unpaidCount)
    }

    @Test fun `archived clients are listed separately and empty state is detected`() {
        val vm = ClientListViewModel(store)
        assertTrue(vm.state.value.noClients)
        store.add("Old Client", archived = true)
        store.add("Current Client")
        assertFalse(vm.state.value.noClients)
        assertEquals(listOf("Current Client"), vm.state.value.rows.map { it.name })
        assertEquals(listOf("Old Client"), vm.state.value.archivedRows.map { it.name })
    }

    // ---- Detail -----------------------------------------------------------------------------------

    @Test fun `detail shows what the client owes and blocks deleting clients with invoices`() {
        val client = store.add("Acme Studio")
        store.owed.value = listOf(ClientOwed(client.id, "USD", 120_000, 1))
        store.overdue.value = listOf(ClientCount(client.id, 1))
        store.invoices.value = listOf(invoice(client.id))
        val vm = ClientDetailViewModel(store, client.id, today = 100)
        val s = vm.state.value
        assertEquals(listOf(Owed("USD", 120_000)), s.owed)
        assertEquals(1, s.overdueCount)
        assertFalse(s.canDelete)
    }

    @Test fun `client without invoices can be deleted after confirming`() {
        val client = store.add("Unused Client")
        val vm = ClientDetailViewModel(store, client.id, today = 100)
        assertTrue(vm.state.value.canDelete)
        vm.onDeleteRequest()
        assertTrue(vm.state.value.confirmDelete)
        vm.onDeleteConfirm()
        assertTrue(vm.state.value.deleted)
        assertTrue(store.clients.value.isEmpty())
    }

    @Test fun `archiving updates the detail`() {
        val client = store.add("Acme Studio")
        val vm = ClientDetailViewModel(store, client.id, today = 100)
        vm.onArchive(true)
        assertTrue(vm.state.value.client!!.archived)
    }

    private fun invoice(clientId: Long) = InvoiceEntity(
        id = 1, uuid = "i1", clientId = clientId, currency = "USD", issueDate = 50, dueDate = 80,
        status = InvoiceStatus.ISSUED, number = "INV-0001", createdAt = 0, updatedAt = 0,
    )
}
