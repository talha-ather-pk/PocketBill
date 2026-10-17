package com.example.pocketbill.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.example.pocketbill.app.AppContainer
import com.example.pocketbill.app.PocketBillApplication

/**
 * The app's [AppContainer], for creating ViewModels in composables (ARCHITECTURE.md section 4):
 * `viewModel { SetupViewModel(appContainer().profile) }`.
 */
@Composable
fun appContainer(): AppContainer =
    (LocalContext.current.applicationContext as PocketBillApplication).container
