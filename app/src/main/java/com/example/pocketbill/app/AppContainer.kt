package com.example.pocketbill.app

import android.content.Context
import com.example.pocketbill.domain.time.Clock
import com.example.pocketbill.domain.time.DeviceClock

/**
 * Creates and holds the objects shared across the app (manual dependency injection,
 * ARCHITECTURE.md section 4). ViewModels get what they need from here through a factory.
 *
 * Added in later tasks: the Room database and repositories (P4-T03), the PDF renderer (P4-T08).
 */
class AppContainer(
    @Suppress("unused") private val appContext: Context,
    val clock: Clock = DeviceClock(),
)
