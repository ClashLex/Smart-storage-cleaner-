package com.example.ui.screens

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.data.ServiceLocator
import com.example.receiver.NotificationActionReceiver
import com.example.work.AutoCleanWorker
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.util.Calendar

object NotificationHelper {
    private const val TAG = "NotificationHelper"

    const val CHANNEL_CLEANUP_REMINDERS = "cleanup_reminders"
    const val CHANNEL_CRITICAL_STORAGE = "critical_storage"
    const val CHANNEL_BACKGROUND_ACTIVITY = "background_activity"

    const val NOTIFICATION_ID_CRITICAL = 1001

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val channelReminders = NotificationChannel(
                CHANNEL_CLEANUP_REMINDERS,
                "Cleanup Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Timely cleaners and junk reminders"
            }

            val channelCritical = NotificationChannel(
                CHANNEL_CRITICAL_STORAGE,
                "Critical Storage Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Warns when device space is critically low"
            }

            val channelBackground = NotificationChannel(
                CHANNEL_BACKGROUND_ACTIVITY,
                "Background Engine Activity",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Information on scheduled scans and background updates"
            }

            notificationManager.createNotificationChannel(channelReminders)
            notificationManager.createNotificationChannel(channelCritical)
            notificationManager.createNotificationChannel(channelBackground)
            Log.d(TAG, "Notification channels initialized successfully")
        }
    }

    fun canNotifyNow(context: Context): Boolean = runBlocking {
        val userPreferencesRepository = ServiceLocator.userPreferencesRepository
        val settings = userPreferencesRepository.cleanerSettings.first()

        if (!settings.notificationsEnabled) {
            Log.d(TAG, "Notification skipped: disabled in settings")
            return@runBlocking false
        }

        val snoozeUntil = userPreferencesRepository.snoozeUntil.first()
        if (System.currentTimeMillis() < snoozeUntil) {
            Log.d(TAG, "Notification skipped: actively snoozed")
            return@runBlocking false
        }

        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val start = settings.quietHoursStart
        val end = settings.quietHoursEnd

        if (start == end) {
            return@runBlocking true
        }

        val isQuiet = if (start < end) {
            currentHour in start until end
        } else {
            currentHour >= start || currentHour < end
        }

        Log.d(TAG, "Checking quiet hours suppression: currentHour=$currentHour, start=$start, end=$end, isQuiet=$isQuiet")
        return@runBlocking !isQuiet
    }

    suspend fun checkAndNotifyCriticalStorage(context: Context) {
        val userPreferencesRepository = ServiceLocator.userPreferencesRepository
        val settings = userPreferencesRepository.cleanerSettings.first()

        val storageStats = ServiceLocator.storageStatsRepository.getStorageStats()
        val usedPercentage = storageStats.usedPercentage

        Log.d(TAG, "Evaluating critical storage logic: usedPercentage=$usedPercentage%, threshold=${settings.criticalStorageThreshold}%")
        
        if (usedPercentage >= settings.criticalStorageThreshold.toFloat()) {
            if (canNotifyNow(context)) {
                showCriticalStorageNotification(context, storageStats.availableBytes)
            }
        }
    }

    fun showCriticalStorageNotification(context: Context, availableBytes: Long) {
        createNotificationChannels(context)

        Log.d(TAG, "Displaying high importance critical storage alert...")

        val cleanNowIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            action = "com.example.action.CLEAN_NOW"
        }
        val cleanNowPendingIntent = PendingIntent.getActivity(
            context,
            201,
            cleanNowIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_SNOOZE
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            202,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val formattedAvailable = formatBytes(availableBytes)
        val notification = NotificationCompat.Builder(context, CHANNEL_CRITICAL_STORAGE)
            .setContentTitle("Critical Storage Alert")
            .setContentText("Device starts to suffocate. Only $formattedAvailable free left on internal storage.")
            .setSmallIcon(android.R.drawable.stat_notify_sdcard)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(cleanNowPendingIntent)
            .addAction(android.R.drawable.ic_menu_delete, "Clean Now", cleanNowPendingIntent)
            .addAction(android.R.drawable.ic_lock_power_off, "Snooze 1 day", snoozePendingIntent)
            .build()

        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(NOTIFICATION_ID_CRITICAL, notification)
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission denied for POST_NOTIFICATIONS", e)
        }
    }
}
