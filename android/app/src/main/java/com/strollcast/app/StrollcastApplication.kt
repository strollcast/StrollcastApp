package com.strollcast.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class StrollcastApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Application initialization
    }
}
