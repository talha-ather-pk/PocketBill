package com.example.pocketbill.app

import android.app.Application

/** Starts once per app process and creates the [AppContainer]. Registered in AndroidManifest.xml. */
class PocketBillApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
