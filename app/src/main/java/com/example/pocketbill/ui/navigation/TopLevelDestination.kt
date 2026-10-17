package com.example.pocketbill.ui.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import com.example.pocketbill.R
import kotlin.reflect.KClass

/** The four tabs of the bottom navigation bar, in order, each with the route of its start screen. */
enum class TopLevelDestination(
    val route: Any,
    @StringRes val labelRes: Int,
    @DrawableRes val iconRes: Int,
) {
    HOME(Home, R.string.nav_home, R.drawable.ic_nav_home),
    INVOICES(InvoiceList(), R.string.nav_invoices, R.drawable.ic_nav_invoices),
    CLIENTS(ClientList, R.string.nav_clients, R.drawable.ic_nav_clients),
    SETTINGS(Settings, R.string.nav_settings, R.drawable.ic_nav_settings),
    ;

    val routeClass: KClass<*> get() = route::class

    companion object {
        /** The tab whose start screen [destination] is, or null for any other screen. */
        fun startedAt(destination: NavDestination?): TopLevelDestination? =
            entries.firstOrNull { destination?.hasRoute(it.routeClass) == true }
    }
}

/** Full-screen tasks hide the bottom bar (UX_SPEC.md section 1, "Routes"). */
internal val routesWithoutBottomBar: List<KClass<*>> = listOf(
    Setup::class,
    InvoiceEditor::class,
    PdfPreview::class,
    ClientEditor::class,
)

internal fun NavDestination?.showsBottomBar(): Boolean =
    this == null || routesWithoutBottomBar.none { hasRoute(it) }
