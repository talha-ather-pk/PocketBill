package com.example.pocketbill.ui.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.example.pocketbill.R

/** The four sections reachable from the bottom navigation bar. */
enum class TopLevelDestination(
    val route: String,
    @StringRes val labelRes: Int,
    @DrawableRes val iconRes: Int,
) {
    HOME("home", R.string.nav_home, R.drawable.ic_nav_home),
    INVOICES("invoices", R.string.nav_invoices, R.drawable.ic_nav_invoices),
    CLIENTS("clients", R.string.nav_clients, R.drawable.ic_nav_clients),
    SETTINGS("settings", R.string.nav_settings, R.drawable.ic_nav_settings),
}
