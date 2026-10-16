package com.example.pocketbill.ui.navigation

import kotlinx.serialization.serializer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import kotlin.reflect.KClass

class TopLevelDestinationTest {

    @Test
    fun appHasFourTopLevelSectionsStartingWithHome() {
        assertEquals(
            listOf(Home::class, InvoiceList::class, ClientList::class, Settings::class),
            TopLevelDestination.entries.map { it.routeClass },
        )
    }

    @Test
    fun routesAreUnique() {
        val routes = TopLevelDestination.entries.map { it.routeClass }
        assertEquals(routes.size, routes.toSet().size)
    }

    @Test
    fun invoicesTabOpensTheUnfilteredList() {
        assertEquals(InvoiceList(InvoiceFilter.ALL), TopLevelDestination.INVOICES.route)
    }

    @Test
    fun everyRouteCanBeSerialized() {
        // Type-safe navigation needs a serializer for each route; a missing @Serializable fails here, not at runtime.
        val routes: List<KClass<*>> = listOf(
            Setup::class, Home::class, InvoiceList::class, ClientList::class, Settings::class,
            InvoiceDetail::class, InvoiceEditor::class, PdfPreview::class, ClientDetail::class,
            ClientEditor::class, BusinessProfile::class, Presets::class, Backup::class, RestoreConfirm::class,
        )
        for (route in routes) {
            assertNotNull(route.simpleName, serializer(route.java))
        }
    }

    @Test
    fun fullScreenTasksHideTheBottomBar() {
        assertEquals(
            setOf(Setup::class, InvoiceEditor::class, PdfPreview::class, ClientEditor::class, RestoreConfirm::class),
            routesWithoutBottomBar.toSet(),
        )
    }

    @Test
    fun newInvoiceHasNoIds() {
        val editor = InvoiceEditor()
        assertEquals(listOf(NO_ID, NO_ID, NO_ID), listOf(editor.invoiceId, editor.duplicateOf, editor.clientId))
    }
}
