package com.example.pocketbill.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.pocketbill.R
import com.example.pocketbill.ui.screens.EmptyStateScreen
import com.example.pocketbill.ui.screens.HomeScreen
import com.example.pocketbill.ui.screens.PlaceholderScreen

/**
 * App shell: bottom bar plus the navigation graph from UX_SPEC.md section 1.
 * Each tab keeps its own back stack; full-screen tasks hide the bottom bar.
 */
@Composable
fun PocketBillApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    // The highlighted tab follows the last tab start screen visited, so it stays correct on
    // screens opened from a tab (e.g. an invoice opened from the Invoices tab).
    var selectedTab by rememberSaveable { mutableStateOf(TopLevelDestination.HOME) }
    LaunchedEffect(currentDestination) {
        TopLevelDestination.startedAt(currentDestination)?.let { selectedTab = it }
    }

    Scaffold(
        bottomBar = {
            if (currentDestination.showsBottomBar()) {
                NavigationBar {
                    TopLevelDestination.entries.forEach { tab ->
                        NavigationBarItem(
                            selected = tab == selectedTab,
                            onClick = {
                                if (tab == selectedTab) {
                                    // Re-selecting the current tab returns to its start screen.
                                    navController.popBackStack(tab.route, inclusive = false)
                                } else {
                                    selectedTab = tab
                                    navController.navigateToTab(tab)
                                }
                            },
                            icon = { Icon(painterResource(tab.iconRes), contentDescription = null) },
                            label = { Text(stringResource(tab.labelRes)) },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Home,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable<Setup> { PlaceholderScreen(R.string.title_setup) }

            composable<Home> {
                HomeScreen(onCreateInvoice = { navController.navigate(InvoiceEditor()) })
            }
            composable<InvoiceList> {
                EmptyStateScreen(R.string.invoices_empty_title, R.string.invoices_empty_body)
            }
            composable<ClientList> {
                EmptyStateScreen(R.string.clients_empty_title, R.string.clients_empty_body)
            }
            composable<Settings> {
                EmptyStateScreen(R.string.settings_placeholder_title, R.string.settings_placeholder_body)
            }

            composable<InvoiceDetail> { PlaceholderScreen(R.string.title_invoice) }
            composable<InvoiceEditor> { PlaceholderScreen(R.string.title_new_invoice) }
            composable<PdfPreview> { PlaceholderScreen(R.string.title_pdf_preview) }
            composable<ClientDetail> { PlaceholderScreen(R.string.title_client) }
            composable<ClientEditor> { PlaceholderScreen(R.string.title_client_editor) }
            composable<BusinessProfile> { PlaceholderScreen(R.string.title_business_profile) }
            composable<Presets> { PlaceholderScreen(R.string.title_presets) }
            composable<Backup> { PlaceholderScreen(R.string.title_backup) }
            composable<RestoreConfirm> { PlaceholderScreen(R.string.title_restore) }
        }
    }
}

/** Standard bottom-bar navigation: one copy of each tab, tab state saved and restored, Back returns to Home. */
private fun NavHostController.navigateToTab(tab: TopLevelDestination) {
    navigate(tab.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
