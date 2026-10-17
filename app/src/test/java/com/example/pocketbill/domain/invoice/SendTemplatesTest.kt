package com.example.pocketbill.domain.invoice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

/** UX_SPEC 4 email templates and which one applies. */
class SendTemplatesTest {
    private fun compose(kind: SendKind, payment: String = "IBAN DE00 1234", client: String = "Müller Design GmbH", business: String = "Your Studio") =
        SendTemplates.compose(kind, "INV-0014", business, client, "€570.00", "30 Oct 2026", "23 Oct 2026", payment)

    @Test fun `invoice email matches the spec`() {
        val text = compose(SendKind.INVOICE)
        assertEquals("Invoice INV-0014 from Your Studio", text.subject)
        assertEquals(
            "Hi Müller Design GmbH,\n\nPlease find attached invoice INV-0014 for €570.00, due on 30 Oct 2026.\n\n" +
                "Payment details:\nIBAN DE00 1234\n\nThank you,\nYour Studio",
            text.body,
        )
    }

    @Test fun `receipt thanks for the payment with its date`() {
        val text = compose(SendKind.RECEIPT)
        assertEquals("Receipt for invoice INV-0014 from Your Studio", text.subject)
        assertEquals(
            "Hi Müller Design GmbH,\n\nThank you for your payment of €570.00, received on 23 Oct 2026. " +
                "The paid invoice INV-0014 is attached for your records.\n\nThank you,\nYour Studio",
            text.body,
        )
    }

    @Test fun `reminder names the due date`() {
        val text = compose(SendKind.REMINDER)
        assertEquals("Reminder: invoice INV-0014 was due on 30 Oct 2026", text.subject)
        assertEquals(
            "Hi Müller Design GmbH,\n\nThis is a friendly reminder that invoice INV-0014 for €570.00 was due on 30 Oct 2026. " +
                "I've attached it again for convenience.\n\nPayment details:\nIBAN DE00 1234\n\nThank you,\nYour Studio",
            text.body,
        )
    }

    @Test fun `empty payment details and names leave no gaps`() {
        val text = compose(SendKind.INVOICE, payment = "  ", client = "", business = "")
        assertEquals("Invoice INV-0014", text.subject)
        assertEquals("Hi,\n\nPlease find attached invoice INV-0014 for €570.00, due on 30 Oct 2026.\n\nThank you", text.body)
        assertFalse(text.body.contains("Payment details"))
    }

    @Test fun `kind follows status and due date`() {
        val today = 100L
        assertEquals(SendKind.INVOICE, sendKindFor(InvoiceStatus.ISSUED, dueDateEpochDay = 100, todayEpochDay = today))
        assertEquals(SendKind.REMINDER, sendKindFor(InvoiceStatus.ISSUED, dueDateEpochDay = 99, todayEpochDay = today))
        assertEquals(SendKind.RECEIPT, sendKindFor(InvoiceStatus.PAID, dueDateEpochDay = 50, todayEpochDay = today))
        assertNull(sendKindFor(InvoiceStatus.DRAFT, 100, today))
        assertNull(sendKindFor(InvoiceStatus.VOID, 100, today))
    }
}
