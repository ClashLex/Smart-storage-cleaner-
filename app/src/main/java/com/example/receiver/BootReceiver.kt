package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.data.ServiceLocator
import com.example.work.AutoCleanWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    private val tag = "BootReceiver"

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d(tag, "Boot completed broadcast received. Re-registering background schedulers if enabled...")
            
            val goAsync = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    // Initialize ServiceLocator first to avoid uninitialized context errors
                    ServiceLocator.initialize(context.applicationContext)
                    
                    val userPreferencesRepository = ServiceLocator.userPreferencesRepository
                    val settings = userPreferencesRepository.cleanerSettings.first()
                    
                    if (settings.scheduledCleanupEnabled) {
                        Log.d(tag, "Scheduled cleanup is enabled in settings. Registering AutoCleanWorker.")
                        AutoCleanWorker.schedule(context.applicationContext)
                    } else {
                        Log.d(tag, "Scheduled cleanup is disabled in settings. Skipping registering.")
                    }
                } catch (e: Exception) {
                    Log.e(tag, "Error re-registering clean schedules upon boot", e)
                } finally {
                    goAsync.finish()
                }
            }
        }
    }
}
