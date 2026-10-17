package com.example.pocketbill.ui.clients

import com.example.pocketbill.data.db.ClientEntity
import com.example.pocketbill.data.repository.isPlausibleEmail
import com.example.pocketbill.domain.Problem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ClientFormTest {

    @Test fun `only the name is required`() {
        assertTrue(ClientForm(name = "Acme").errors().isEmpty())
        assertEquals(Problem.CLIENT_NAME_EMPTY, ClientForm(name = "").errors()[ClientField.NAME])
    }

    @Test fun `email is checked only when given`() {
        assertTrue(ClientForm(name = "A", email = "").errors().isEmpty())
        assertTrue(ClientForm(name = "A", email = " billing@acme.example ").errors().isEmpty())
        assertEquals(Problem.BAD_EMAIL, ClientForm(name = "A", email = "billing@acme").errors()[ClientField.EMAIL])
        assertTrue(isPlausibleEmail("jose.muller+invoices@studio.co.uk"))
        assertFalse(isPlausibleEmail("two words@acme.example"))
    }

    @Test fun `preferred currency must be supported, terms within a year (D-015)`() {
        assertTrue(ClientForm(name = "A", preferredCurrency = "EUR", preferredTermsDays = 30).errors().isEmpty())
        assertEquals(Problem.UNKNOWN_CURRENCY, ClientForm(name = "A", preferredCurrency = "XYZ").errors()[ClientField.CURRENCY])
        assertEquals(Problem.BAD_TERMS, ClientForm(name = "A", preferredTermsDays = 400).errors()[ClientField.TERMS])
    }

    @Test fun `only printed fields count as frozen-detail changes`() {
        val client = ClientEntity(uuid = "u", name = "Acme", email = "a@acme.example", createdAt = 0, updatedAt = 0)
        val form = client.toForm()
        assertFalse(client.frozenDetailsDiffer(form))
        assertFalse(client.frozenDetailsDiffer(form.copy(notes = "Pays late", preferredCurrency = "EUR", preferredTermsDays = 7)))
        assertFalse("trailing spaces don't count", client.frozenDetailsDiffer(form.copy(name = "Acme ")))
        assertTrue(client.frozenDetailsDiffer(form.copy(country = "Germany")))
        assertTrue(client.frozenDetailsDiffer(form.copy(email = "new@acme.example")))
    }
}
