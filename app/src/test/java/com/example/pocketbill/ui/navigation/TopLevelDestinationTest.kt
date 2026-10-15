package com.example.pocketbill.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

class TopLevelDestinationTest {

    @Test
    fun appHasFourTopLevelSectionsStartingWithHome() {
        assertEquals(
            listOf("home", "invoices", "clients", "settings"),
            TopLevelDestination.entries.map { it.route },
        )
    }

    @Test
    fun routesAreUnique() {
        val routes = TopLevelDestination.entries.map { it.route }
        assertEquals(routes.size, routes.toSet().size)
    }
}
