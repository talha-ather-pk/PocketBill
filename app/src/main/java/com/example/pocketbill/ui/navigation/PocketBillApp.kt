package com.example.pocketbill.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.pocketbill.R
import com.example.pocketbill.ui.appContainer
import com.example.pocketbill.ui.clients.ClientDetailScreen
import com.example.pocketbill.ui.clients.ClientEditorResult
import com.example.pocketbill.ui.clients.ClientEditorScreen
import com.example.pocketbill.ui.clients.ClientListScreen
import com.example.pocketbill.ui.editor.EditorArgs
import com.example.pocketbill.ui.editor.EditorResult
import com.example.pocketbill.ui.editor.InvoiceEditorScreen
import com.example.pocketbill.ui.screens.EmptyStateScreen
import com.example.pocketbill.ui.screens.HomeScreen
import com.example.pocketbill.ui.screens.PlaceholderScreen
import com.example.pocketbill.ui.settings.BusinessProfileScreen
import com.example.pocketbill.ui.settings.SettingsScreen
import com.example.pocketbill.ui.setup.SetupScreen

/**
 * App shell: bottom bar plus the navigation graph from UX_SPEC.md section 1.
 * Each tab keeps its own back stack; full-screen tasks hide the bottom bar.
 */
@Composable
fun PocketBillApp() {
    val container = appContainer()
    val startVm: StartViewModel = viewModel { StartViewModel(container.profile) }
    val start by startVm.start.collectAsStateWithLifecycle()
    if (start == StartState.LOADING) {
        // The database answers in well under a second; show the plain background meanwhile (UX_SPEC 2: no spinner).
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
    } else {
        // One call site, so the graph survives NEEDS_SETUP changing to READY when Setup saves.
        PocketBillNavigation(startDestination = if (start == StartState.NEEDS_SETUP) Setup else Home)
    }
}

/**
 * [startDestination] is fixed for the life of the nav graph: once Setup finishes it navigates
 * to Home itself, so the change from NEEDS_SETUP to READY must not rebuild the graph.
 */
@Composable
private fun PocketBillNavigation(startDestination: Any) {
    val graphStart = rememberSaveable(saver = startDestinationSaver) { startDestination }
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
            startDestination = graphStart,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable<Setup> {
                SetupScreen(onDone = {
                    navController.navigate(Home) { popUpTo<Setup> { inclusive = true } }
                })
            }

            composable<Home> {
                HomeScreen(onCreateInvoice = { navController.navigate(InvoiceEditor()) })
            }
            composable<InvoiceList> {
                EmptyStateScreen(R.string.invoices_empty_title, R.string.invoices_empty_body)
            }
            composable<ClientList> {
                ClientListScreen(
                    onOpenClient = { navController.navigate(ClientDetail(it)) },
                    onAddClient = { navController.navigate(ClientEditor()) },
                )
            }
            composable<Settings> {
                SettingsScreen(
                    onOpenProfile = { navController.navigate(BusinessProfile) },
                    onOpenPresets = { navController.navigate(Presets) },
                    onOpenBackup = { navController.navigate(Backup) },
                )
            }

            composable<InvoiceDetail> { PlaceholderScreen(R.string.title_invoice) }
            composable<InvoiceEditor> { entry ->
                val route = entry.toRoute<InvoiceEditor>()
                // A client created via "+ New client" comes back through this entry's saved state.
                val picked by entry.savedStateHandle.getStateFlow(PICKED_CLIENT_KEY, NO_ID).collectAsStateWithLifecycle()
                InvoiceEditorScreen(
                    args = EditorArgs(route.invoiceId, route.duplicateOf, route.clientId),
                    pickedClientId = picked.takeIf { it != NO_ID },
                    onPickedClientConsumed = { entry.savedStateHandle[PICKED_CLIENT_KEY] = NO_ID },
                    onNewClient = { navController.navigate(ClientEditor(returnToEditor = true)) },
                    onDone = { result ->
                        when (result) {
                            // UX_SPEC 1: after issuing, Back from the preview goes to the invoice, not the editor.
                            is EditorResult.Issued -> {
                                navController.navigate(InvoiceDetail(result.invoiceId)) { popUpTo<InvoiceEditor> { inclusive = true } }
                                navController.navigate(PdfPreview(result.invoiceId))
                            }
                            else -> navController.popBackStack()
                        }
                    },
                )
            }
            composable<PdfPreview> { PlaceholderScreen(R.string.title_pdf_preview) }
            composable<ClientDetail> { entry ->
                ClientDetailScreen(
                    clientId = entry.toRoute<ClientDetail>().clientId,
                    onBack = { navController.popBackStack() },
                    onEdit = { navController.navigate(ClientEditor(clientId = it)) },
                    onNewInvoice = { navController.navigate(InvoiceEditor(clientId = it)) },
                    onOpenInvoice = { navController.navigate(InvoiceDetail(it)) },
                )
            }
            composable<ClientEditor> { entry ->
                val route = entry.toRoute<ClientEditor>()
                ClientEditorScreen(
                    clientId = route.clientId,
                    onCancel = { navController.popBackStack() },
                    onDone = { result ->
                        val chosenId = when (result) {
                            is ClientEditorResult.Saved -> result.clientId
                            is ClientEditorResult.UseExisting -> result.clientId
                        }
                        if (route.returnToEditor) {
                            // Opened from the invoice editor: hand the client back and return there (UX_SPEC 1).
                            navController.previousBackStackEntry?.savedStateHandle?.set(PICKED_CLIENT_KEY, chosenId)
                            navController.popBackStack()
                            return@ClientEditorScreen
                        }
                        when (result) {
                            // A new client opens on its own page; an edit returns to where it started.
                            is ClientEditorResult.Saved -> if (result.isNew) {
                                navController.navigate(ClientDetail(result.clientId)) { popUpTo<ClientEditor> { inclusive = true } }
                            } else {
                                navController.popBackStack()
                            }
                            is ClientEditorResult.UseExisting ->
                                navController.navigate(ClientDetail(result.clientId)) { popUpTo<ClientEditor> { inclusive = true } }
                        }
                    },
                )
            }
            composable<BusinessProfile> { BusinessProfileScreen(onBack = { navController.popBackStack() }) }
            composable<Presets> { PlaceholderScreen(R.string.title_presets) }
            composable<Backup> { PlaceholderScreen(R.string.title_backup) }
            composable<RestoreConfirm> { PlaceholderScreen(R.string.title_restore) }
        }
    }
}

/** Saved-state key the client editor uses to hand a new client back to the invoice editor. */
private const val PICKED_CLIENT_KEY = "pickedClientId"

/** Remembers whether the graph started at Setup or Home across rotation and process death. */
private val startDestinationSaver = Saver<Any, Boolean>(
    save = { it == Setup },
    restore = { startedAtSetup -> if (startedAtSetup) Setup else Home },
)

/** Standard bottom-bar navigation: one copy of each tab, tab state saved and restored, Back returns to Home. */
private fun NavHostController.navigateToTab(tab: TopLevelDestination) {
    navigate(tab.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
