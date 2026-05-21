package com.example

import android.app.Application
import android.util.Log
import com.example.data.ServiceLocator
import com.google.firebase.FirebaseApp

class SmartCleanerApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            FirebaseApp.initializeApp(this)
        } catch (e: Exception) {
            Log.e("SmartCleanerApplication", "Failed to initialize FirebaseApp", e)
        }
        // Initialize ServiceLocator helper on startup
        ServiceLocator.initialize(this)
    }
}
