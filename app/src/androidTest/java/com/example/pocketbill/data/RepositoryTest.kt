package com.example.pocketbill.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.pocketbill.data.db.PocketBillDatabase
import com.example.pocketbill.data.repository.ClientInput
import com.example.pocketbill.data.repository.ClientRepository
import com.example.pocketbill.data.repository.InvoiceInput
import com.example.pocketbill.data.repository.InvoiceRepository
import com.example.pocketbill.data.repository.LineDraft
import com.example.pocketbill.data.repository.ProfileInput
import com.example.pocketbill.data.repository.ProfileRepository
import com.example.pocketbill.domain.Outcome
import com.example.pocketbill.domain.Problem
import com.example.pocketbill.domain.invoice.InvoiceStatus
import com.example.pocketbill.domain.invoice.SnapshotJson
import com.example.pocketbill.domain.money.Discount
import com.example.pocketbill.domain.time.FixedClock
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/**
 * Repository rules on a real SQLite database on the phone (in memory, so the app's data is never
 * touched): invariants (DATA_MODEL 2), atomic operations (3), queries (4), numbering and frozen
 * details (5.7).
 */
@RunWith(AndroidJUnit4::class)
class RepositoryTest {
    private lateinit var db: PocketBillDatabase
    private lateinit var invoices: InvoiceRepository
    private lateinit var clients: ClientRepository
    private lateinit var profile: ProfileRepository

    private val today = LocalDate.of(2026, 10, 17).toEpochDay()
    private val clock = FixedClock(today)

    @Before fun setUp() = runBlocking {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), PocketBillDatabase::class.java).build()
        invoices = InvoiceRepository(db, clock)
        clients = ClientRepository(db, clock)
        profile = ProfileRepository(db, clock)
        profile.save(ProfileInput(name = "Your Studio", defaultCurrency = "USD", paymentInstructions = "IBAN OLD 1111"))
        Unit
    }

    @After fun tearDown() = db.close()

    // ---- helpers --------------------------------------------------------------------------------

    private fun <T> Outcome<T>.ok(): T = (this as? Outcome.Success)?.value ?: error("Expected success, got $this")

    private suspend fun newClient(name: String = "Müller Design GmbH", email: String? = "billing@mueller.example") =
        clients.create(ClientInput(name = name, email = email, country = "Germany")).ok()

    private fun input(clientId: Long?, vararg lines: LineDraft = arrayOf(LineDraft("Logo design", 1_000, 45_000)), currency: String = "EUR") =
        InvoiceInput(clientId = clientId, currency = currency, issueDate = today, dueDate = today + 14, lines = lines.toList())

    private suspend fun issuedInvoice(clientId: Long): Pair<Long, String> {
        val id = invoices.saveDraft(0, input(clientId)).ok()
        return id to invoices.issue(id).ok()
    }

    // ---- drafts and issuing ----------------------------------------------------------------

    @Test fun usabilityTaskT2TotalIsStoredAs570Euros() = runBlocking {
        val client = newClient()
        val id = invoices.saveDraft(0, input(client, LineDraft("Logo design", 1_000, 45_000), LineDraft("Extra revisions", 2_000, 6_000))).ok()
        val saved = invoices.get(id)!!
        assertEquals(57_000, saved.invoice.totalMinor)
        assertEquals(listOf(45_000L, 12_000L), saved.lines.map { it.amountMinor })
        assertEquals(listOf("Logo design", "Extra revisions"), saved.lines.map { it.description })
    }

    @Test fun draftCanBeSavedWithoutAClientButNotIssued() = runBlocking {
        val id = invoices.saveDraft(0, input(clientId = null)).ok()
        assertNull(invoices.get(id)!!.invoice.clientId)
        val result = invoices.issue(id)
        assertTrue(result is Outcome.ValidationFailed && Problem.NO_CLIENT in result.reasons)
        assertEquals(InvoiceStatus.DRAFT, invoices.get(id)!!.invoice.status)
    }

    @Test fun issuingAssignsSequentialNumbersAndFreezesDetails() = runBlocking {
        val client = newClient()
        val (firstId, first) = issuedInvoice(client)
        val (_, second) = issuedInvoice(client)
        assertEquals("INV-0001", first)
        assertEquals("INV-0002", second)

        val issued = invoices.get(firstId)!!.invoice
        assertEquals(InvoiceStatus.ISSUED, issued.status)
        assertEquals("Müller Design GmbH", SnapshotJson.decodeTo(issued.toSnapshot!!)!!.name)
        assertEquals("IBAN OLD 1111", SnapshotJson.decodeFrom(issued.fromSnapshot!!)!!.paymentInstructions)
        assertEquals(3L, profile.get()!!.nextInvoiceNumber)
        assertEquals(clock.nowMillis(), clients.get(client)!!.lastInvoicedAt)
    }

    @Test fun deletedDraftsLeaveNoGapInNumbering() = runBlocking {
        val client = newClient()
        repeat(3) { invoices.deleteDraft(invoices.saveDraft(0, input(client)).ok()).ok() }
        assertEquals("INV-0001", issuedInvoice(client).second)
    }

    @Test fun voidedNumbersAreNeverReused() = runBlocking {
        val client = newClient()
        val (id, number) = issuedInvoice(client)
        invoices.void(id).ok()
        assertEquals(InvoiceStatus.VOID, invoices.get(id)!!.invoice.status)
        assertEquals(number, invoices.get(id)!!.invoice.number)
        assertEquals("INV-0002", issuedInvoice(client).second)
    }

    @Test fun prefixChangeAffectsOnlyNewInvoices() = runBlocking {
        // N-5. Skipping taken numbers (N-7) is covered in InvoiceRulesTest: the sequence only
        // increases (N-4), so changing the prefix back can't collide here.
        val client = newClient()
        val (firstId, first) = issuedInvoice(client)
        profile.save(ProfileInput(name = "Your Studio", defaultCurrency = "USD", invoicePrefix = "A-")).ok()
        assertEquals("A-0002", issuedInvoice(client).second)
        assertEquals("INV-0001", first)
        assertEquals("existing number unchanged", first, invoices.get(firstId)!!.invoice.number)
    }

    @Test fun startingNumberCannotGoBackwards() = runBlocking {
        issuedInvoice(newClient()) // INV-0001, next is 2
        profile.save(ProfileInput(name = "Your Studio", defaultCurrency = "USD", nextInvoiceNumber = 1)).ok()
        assertEquals(2L, profile.get()!!.nextInvoiceNumber)
    }

    @Test fun startingNumberContinuesFromAnotherTool() = runBlocking {
        profile.save(ProfileInput(name = "Your Studio", defaultCurrency = "USD", nextInvoiceNumber = 58)).ok()
        assertEquals("INV-0058", issuedInvoice(newClient()).second)
    }

    @Test fun issuingChecksTheRules() = runBlocking {
        val client = newClient()
        val noLines = invoices.saveDraft(0, input(client, *emptyArray())).ok()
        val noDescription = invoices.saveDraft(0, input(client, LineDraft("  ", 1_000, 100))).ok()
        val dueBeforeIssue = invoices.saveDraft(0, input(client).copy(dueDate = today - 1)).ok()
        assertTrue(Problem.NO_LINES in (invoices.issue(noLines) as Outcome.ValidationFailed).reasons)
        assertTrue(Problem.LINE_WITHOUT_DESCRIPTION in (invoices.issue(noDescription) as Outcome.ValidationFailed).reasons)
        assertTrue(Problem.DUE_BEFORE_ISSUE in (invoices.issue(dueBeforeIssue) as Outcome.ValidationFailed).reasons)
        assertEquals("a failed issue uses no number", 1L, profile.get()!!.nextInvoiceNumber)
    }

    @Test fun badNumbersAreRefusedEvenForDrafts() = runBlocking {
        val result = invoices.saveDraft(0, input(null, LineDraft("x", 0, 100), currency = "XYZ"))
        assertTrue(result is Outcome.ValidationFailed)
        val reasons = (result as Outcome.ValidationFailed).reasons
        assertTrue(Problem.BAD_QUANTITY in reasons && Problem.UNKNOWN_CURRENCY in reasons)
    }

    @Test fun issuedInvoiceCannotBeEditedAsADraftOrDeleted() = runBlocking {
        val (id, _) = issuedInvoice(newClient())
        assertEquals(Outcome.NotAllowed(Problem.NOT_A_DRAFT), invoices.saveDraft(id, input(null)))
        assertEquals(Outcome.NotAllowed(Problem.NOT_A_DRAFT), invoices.deleteDraft(id))
    }

    @Test fun editingAnIssuedInvoiceKeepsItsNumberAndRecalculates() = runBlocking {
        val client = newClient()
        val (id, number) = issuedInvoice(client)
        invoices.updateIssued(id, input(client, LineDraft("Logo design", 1_000, 45_000), LineDraft("Extra revisions", 2_000, 6_000)).copy(taxRateBp = 2_000, discount = Discount.Percent(1_000))).ok()
        val edited = invoices.get(id)!!.invoice
        assertEquals(number, edited.number)
        // 570.00 − 10% = 513.00, + 20% tax 102.60 = 615.60
        assertEquals(5_700, edited.discountMinor)
        assertEquals(10_260, edited.taxMinor)
        assertEquals(61_560, edited.totalMinor)
    }

    @Test fun paidInvoicesAreReadOnlyUntilMarkedUnpaid() = runBlocking {
        val client = newClient()
        val (id, _) = issuedInvoice(client)
        invoices.markPaid(id, paidDate = today).ok()
        assertEquals(today, invoices.get(id)!!.invoice.paidDate)
        assertEquals(Outcome.NotAllowed(Problem.READ_ONLY), invoices.updateIssued(id, input(client)))
        assertEquals(Outcome.NotAllowed(Problem.READ_ONLY), invoices.void(id))
        invoices.markUnpaid(id).ok()
        assertNull(invoices.get(id)!!.invoice.paidDate)
        assertEquals(InvoiceStatus.ISSUED, invoices.get(id)!!.invoice.status)
    }

    @Test fun recordingASendDoesNotChangeTheStatus() = runBlocking {
        val (id, _) = issuedInvoice(newClient())
        invoices.recordSent(id, "Gmail", at = 123L).ok()
        val sent = invoices.get(id)!!.invoice
        assertEquals(123L, sent.lastSentAt)
        assertEquals("Gmail", sent.lastSentVia)
        assertEquals(InvoiceStatus.ISSUED, sent.status)
        invoices.restoreSent(id, null, null).ok()
        assertNull(invoices.get(id)!!.invoice.lastSentAt)
    }

    // ---- frozen details (DATA_MODEL 5.4–5.5) ------------------------------------------------

    @Test fun editingAClientLeavesIssuedInvoicesUnchangedByDefault() = runBlocking {
        val client = newClient()
        val (issuedId, _) = issuedInvoice(client)
        val draftId = invoices.saveDraft(0, input(client)).ok()
        clients.update(client, ClientInput(name = "Müller Studio AG", email = "new@mueller.example"), alsoUpdateUnpaid = false).ok()
        assertEquals("Müller Design GmbH", SnapshotJson.decodeTo(invoices.get(issuedId)!!.invoice.toSnapshot!!)!!.name)
        assertNull("drafts have no snapshot; they show live details", invoices.get(draftId)!!.invoice.toSnapshot)
    }

    @Test fun alsoUpdateChangesUnpaidButNeverPaidInvoices() = runBlocking {
        val client = newClient()
        val (unpaidId, _) = issuedInvoice(client)
        val (paidId, _) = issuedInvoice(client)
        invoices.markPaid(paidId).ok()
        val paidBefore = invoices.get(paidId)!!.invoice.toSnapshot
        assertEquals(1, clients.unpaidIssuedCount(client))

        clients.update(client, ClientInput(name = "Müller Studio AG"), alsoUpdateUnpaid = true).ok()

        assertEquals("Müller Studio AG", SnapshotJson.decodeTo(invoices.get(unpaidId)!!.invoice.toSnapshot!!)!!.name)
        assertEquals("paid snapshot identical", paidBefore, invoices.get(paidId)!!.invoice.toSnapshot)
    }

    @Test fun newPaymentDetailsCanBeAppliedToUnpaidInvoices() = runBlocking {
        val client = newClient()
        val (unpaidId, _) = issuedInvoice(client)
        val (paidId, _) = issuedInvoice(client)
        invoices.markPaid(paidId).ok()
        profile.save(ProfileInput(name = "Your Studio", defaultCurrency = "USD", paymentInstructions = "IBAN NEW 2222"), alsoUpdateUnpaid = true).ok()
        assertEquals("IBAN NEW 2222", SnapshotJson.decodeFrom(invoices.get(unpaidId)!!.invoice.fromSnapshot!!)!!.paymentInstructions)
        assertEquals("IBAN OLD 1111", SnapshotJson.decodeFrom(invoices.get(paidId)!!.invoice.fromSnapshot!!)!!.paymentInstructions)
    }

    // ---- clients ------------------------------------------------------------------------------

    @Test fun clientsWithInvoicesCanOnlyBeArchived() = runBlocking {
        val withInvoice = newClient()
        invoices.saveDraft(0, input(withInvoice)).ok()
        assertEquals(Outcome.NotAllowed(Problem.CLIENT_HAS_INVOICES), clients.delete(withInvoice))
        clients.setArchived(withInvoice, true).ok()
        assertTrue(clients.observeActive().first().none { it.id == withInvoice })

        val unused = newClient("Acme Studio", null)
        clients.delete(unused).ok()
        assertNull(clients.get(unused))
    }

    @Test fun preferredCurrencyAndTermsAreStoredAndCleared() = runBlocking {
        val id = clients.create(ClientInput(name = "Acme Studio", preferredCurrency = "EUR", preferredTermsDays = 30)).ok()
        assertEquals("EUR", clients.get(id)!!.preferredCurrency)
        assertEquals(30, clients.get(id)!!.preferredTermsDays)
        clients.update(id, ClientInput(name = "Acme Studio"), alsoUpdateUnpaid = false).ok()
        assertNull(clients.get(id)!!.preferredCurrency)
        assertNull(clients.get(id)!!.preferredTermsDays)
    }

    @Test fun badClientFieldsAreRefused() = runBlocking {
        val result = clients.create(ClientInput(name = "Acme", email = "acme@", preferredCurrency = "XYZ", preferredTermsDays = 999))
        val reasons = (result as Outcome.ValidationFailed).reasons
        assertEquals(setOf(Problem.BAD_EMAIL, Problem.UNKNOWN_CURRENCY, Problem.BAD_TERMS), reasons)
    }

    @Test fun owedAndOverdueAreGroupedPerClientAndCurrency() = runBlocking {
        val acme = newClient("Acme Studio", null)
        val muller = newClient()
        invoices.issue(invoices.saveDraft(0, input(acme, LineDraft("Brand guide", 1_000, 120_000), currency = "USD").copy(issueDate = today - 40, dueDate = today - 10)).ok()).ok()
        invoices.issue(invoices.saveDraft(0, input(acme, currency = "EUR")).ok()).ok()
        invoices.issue(invoices.saveDraft(0, input(muller, currency = "EUR")).ok()).ok()
        invoices.issue(invoices.saveDraft(0, input(muller, currency = "EUR")).ok()).ok()

        val owed = clients.observeOwedByClient().first().associate { (it.clientId to it.currency) to (it.totalMinor to it.invoiceCount) }
        assertEquals(
            mapOf((acme to "USD") to (120_000L to 1), (acme to "EUR") to (45_000L to 1), (muller to "EUR") to (90_000L to 2)),
            owed,
        )
        assertEquals(mapOf(acme to 1), clients.observeOverdueByClient().first().associate { it.clientId to it.count })
    }

    @Test fun sameNameCheckIgnoresTheClientBeingEdited() = runBlocking {
        val id = newClient("Müller Design GmbH")
        assertNull(clients.findSameName("muller design gmbh", excludeId = id))
        assertEquals(id, clients.findSameName("muller design gmbh")!!.id)
    }

    // ---- Invoice editor support (P4-T06) ------------------------------------------------------

    @Test fun nextNumberPreviewMatchesWhatIssuingAssigns() = runBlocking {
        val client = newClient()
        assertEquals("INV-0001", invoices.previewNextNumber())
        assertEquals(invoices.previewNextNumber(), issuedInvoice(client).second)
        assertEquals("INV-0002", invoices.previewNextNumber())
        profile.save(ProfileInput(name = "Your Studio", defaultCurrency = "USD", invoicePrefix = "2026-", nextInvoiceNumber = 40)).ok()
        assertEquals("2026-0040", invoices.previewNextNumber())
        assertEquals("2026-0040", issuedInvoice(client).second)
        assertEquals("2026-0041", invoices.previewNextNumber())
    }

    @Test fun lastInvoiceForClientIsTheMostRecentOne() = runBlocking {
        val client = newClient()
        assertNull(invoices.lastInvoiceForClient(client))
        invoices.saveDraft(0, input(client, currency = "EUR").copy(issueDate = today - 30, dueDate = today - 16)).ok()
        val newer = invoices.saveDraft(0, input(client, currency = "GBP")).ok()
        assertEquals(newer, invoices.lastInvoiceForClient(client)!!.id)
    }

    @Test fun savedItemsAreSuggestedMostUsedFirstAndUpdatedNotDuplicated() = runBlocking {
        val presets = com.example.pocketbill.data.repository.PresetRepository(db, clock)
        presets.saveFromLine("Logo design", 45_000, "EUR")
        presets.saveFromLine("Logo animation", 30_000, "EUR")
        presets.saveFromLine("logo design", 50_000, "EUR") // same item, new price
        val found = presets.suggestions("lo")
        assertEquals(2, found.size)
        val design = found.first { it.description.equals("Logo design", ignoreCase = true) }
        assertEquals(50_000L, design.unitPriceMinor)
        presets.markUsed(design.id)
        presets.markUsed(design.id)
        assertEquals("Logo design", presets.suggestions("Lo").first().description)
        assertTrue("one character isn't enough", presets.suggestions("L").isEmpty())
    }

    @Test fun duplicateNamesAreFoundIgnoringCaseAndAccents() = runBlocking {
        newClient("Müller Design GmbH")
        assertTrue(clients.hasClientNamed("muller design gmbh"))
        assertTrue(!clients.hasClientNamed("Acme Studio"))
    }

    // ---- Home queries (UX_SPEC 2) --------------------------------------------------------------

    @Test fun outstandingIsPerCurrencyAndOverdueIsCounted() = runBlocking {
        val client = newClient()
        val eur = invoices.saveDraft(0, input(client)).ok().also { invoices.issue(it).ok() }       // €450, due in 14 days
        val usdOverdue = invoices.saveDraft(0, input(client, LineDraft("Brand guide", 1_000, 120_000), currency = "USD").copy(issueDate = today - 40, dueDate = today - 10)).ok()
        invoices.issue(usdOverdue).ok()
        val paid = invoices.saveDraft(0, input(client, currency = "USD")).ok().also { invoices.issue(it).ok() }
        invoices.markPaid(paid).ok()
        invoices.saveDraft(0, input(client, currency = "GBP")).ok() // drafts never count

        val outstanding = invoices.observeOutstanding().first().associate { it.currency to it.totalMinor }
        assertEquals(mapOf("EUR" to 45_000L, "USD" to 120_000L), outstanding)
        assertEquals(1, invoices.observeOverdueCount().first())
        assertEquals(listOf(eur, usdOverdue), invoices.observeIssuedUnsent().first().map { it.id }.sorted())
    }
}
