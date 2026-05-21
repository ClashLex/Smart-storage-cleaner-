package com.example

import android.app.Application
import com.example.data.ServiceLocator

class SmartCleanerApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize ServiceLocator helper on startup
        ServiceLocator.initialize(this)
    }
}
