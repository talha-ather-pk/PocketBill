package com.example.pocketbill.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pocketbill.data.repository.ProfileRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** Which screen the app opens on (UX_SPEC 1): Setup until a business profile exists, then Home. */
enum class StartState { LOADING, NEEDS_SETUP, READY }

class StartViewModel(profile: ProfileRepository) : ViewModel() {
    val start: StateFlow<StartState> = profile.observe()
        .map { if (it == null) StartState.NEEDS_SETUP else StartState.READY }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StartState.LOADING)
}
