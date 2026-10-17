package com.example.pocketbill.ui.settings

import com.example.pocketbill.data.db.BusinessProfileEntity
import com.example.pocketbill.domain.invoice.PaperSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileFormTest {

    private fun valid(form: ProfileForm) = (form.validate() as ProfileFormResult.Valid).input
    private fun errors(form: ProfileForm) = (form.validate() as ProfileFormResult.Invalid).errors

    @Test fun `setup needs only a name`() {
        val input = valid(ProfileForm(name = "  Your Studio ", currency = "EUR"))
        assertEquals("Your Studio", input.name)
        assertEquals("EUR", input.defaultCurrency)
        assertEquals(14, input.defaultTermsDays)
        assertEquals(0, input.defaultTaxRateBp)
        assertEquals("INV-", input.invoicePrefix)
    }

    @Test fun `name is required`() = assertEquals(ProfileError.REQUIRED, errors(ProfileForm(name = " "))[ProfileField.NAME])

    @Test fun `tax rate is read like a typed percentage`() {
        assertEquals(750, valid(ProfileForm(name = "A", taxRate = "7.5")).defaultTaxRateBp)
        assertEquals(750, valid(ProfileForm(name = "A", taxRate = "7,5")).defaultTaxRateBp)
        assertEquals(2_000, valid(ProfileForm(name = "A", taxRate = "20%")).defaultTaxRateBp)
        assertEquals(0, valid(ProfileForm(name = "A", taxRate = "")).defaultTaxRateBp)
        assertEquals(ProfileError.BAD_PERCENT, errors(ProfileForm(name = "A", taxRate = "abc"))[ProfileField.TAX_RATE])
        assertEquals(ProfileError.BAD_PERCENT, errors(ProfileForm(name = "A", taxRate = "7.125"))[ProfileField.TAX_RATE])
    }

    @Test fun `next number must be a whole number not below the current one`() {
        assertEquals(58L, valid(ProfileForm(name = "A", nextNumber = "58")).nextInvoiceNumber)
        assertEquals(ProfileError.BAD_NUMBER, errors(ProfileForm(name = "A", nextNumber = "x"))[ProfileField.NEXT_NUMBER])
        assertEquals(ProfileError.BAD_NUMBER, errors(ProfileForm(name = "A", nextNumber = "0"))[ProfileField.NEXT_NUMBER])
        assertEquals(
            ProfileError.NUMBER_TOO_LOW,
            errors(ProfileForm(name = "A", nextNumber = "3", minNextNumber = 5))[ProfileField.NEXT_NUMBER],
        )
    }

    @Test fun `prefix length is limited`() =
        assertEquals(ProfileError.TOO_LONG, errors(ProfileForm(name = "A", prefix = "ABCDEFGHIJK"))[ProfileField.PREFIX])

    @Test fun `existing profile round-trips through the form`() {
        val entity = profile(paymentInstructions = "IBAN 1", taxBp = 750)
        val form = entity.toForm()
        assertEquals("7.5", form.taxRate)
        assertEquals(entity.nextInvoiceNumber, form.minNextNumber)
        val input = valid(form)
        assertFalse(entity.frozenDetailsDiffer(input))
        assertFalse(entity.paymentDetailsDiffer(input))
    }

    @Test fun `changes to frozen details and payment details are detected`() {
        val entity = profile(paymentInstructions = "IBAN 1", taxBp = 0)
        assertTrue(entity.frozenDetailsDiffer(valid(entity.toForm().copy(address = "New street 1"))))
        assertFalse("tax rate isn't frozen on invoices", entity.frozenDetailsDiffer(valid(entity.toForm().copy(taxRate = "20"))))
        assertTrue(entity.paymentDetailsDiffer(valid(entity.toForm().copy(paymentInstructions = "IBAN 2"))))
    }

    @Test fun `suggested currency follows the phone region when supported`() {
        assertEquals("PKR", suggestedCurrency("PKR"))
        assertEquals("EUR", suggestedCurrency("EUR"))
        assertEquals("USD", suggestedCurrency("XAF")) // not in the MVP list
        assertEquals("USD", suggestedCurrency(null))
    }

    private fun profile(paymentInstructions: String, taxBp: Int) = BusinessProfileEntity(
        uuid = "u", name = "Your Studio", defaultCurrency = "EUR", paymentInstructions = paymentInstructions,
        defaultTaxRateBp = taxBp, nextInvoiceNumber = 7, paperSize = PaperSize.LETTER, createdAt = 0, updatedAt = 0,
    )
}
