package com.example.receiver

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.*
import com.example.data.ServiceLocator
import com.example.ui.screens.NotificationHelper
import com.example.work.AutoCleanWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class NotificationActionReceiver : BroadcastReceiver() {
    private val tag = "NotificationReceiver"

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_SNOOZE) {
            Log.d(tag, "Action snooze broadcast received! Processing 1 day snooze...")

            // Dismiss the critical notification
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(NotificationHelper.NOTIFICATION_ID_CRITICAL)

            // Update preferences and WorkManager inside IO coroutine scope
            val goAsync = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val userPreferencesRepository = ServiceLocator.userPreferencesRepository
                    
                    // 1. Save snooze timestamp in DataStore
                    val snoozeTimestamp = System.currentTimeMillis() + 24L * 60L * 60L * 1000L
                    userPreferencesRepository.saveSnoozeUntil(snoozeTimestamp)
                    Log.d(tag, "Stored snooze timestamp successfully in preferences")

                    // 2. Snooze the WorkManager task (Cancel and reschedule with 1 day initial delay)
                    val constraints = Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.UNMETERED)
                        .setRequiresBatteryNotLow(true)
                        .build()

                    val workRequest = PeriodicWorkRequestBuilder<AutoCleanWorker>(
                        24, TimeUnit.HOURS,
                        1, TimeUnit.HOURS
                    )
                    .setConstraints(constraints)
                    .setInitialDelay(24, TimeUnit.HOURS)
                    .build()

                    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                        AutoCleanWorker.WORK_NAME,
                        ExistingPeriodicWorkPolicy.REPLACE,
                        workRequest
                    )
                    Log.d(tag, "WorkManager periodic clean rescheduled with 24 hours initial delay snooze")

                } catch (e: Exception) {
                    Log.e(tag, "Failed to completely snooze notification parameters", e)
                } finally {
                    goAsync.finish()
                }
            }
        }
    }

    companion object {
        const val ACTION_SNOOZE = "com.example.action.SNOOZE"
    }
}
