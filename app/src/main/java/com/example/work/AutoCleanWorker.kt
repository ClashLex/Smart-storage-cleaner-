package com.example.work

import android.content.Context
import android.os.Environment
import android.util.Log
import androidx.work.*
import com.example.data.ServiceLocator
import com.example.ui.screens.NotificationHelper
import kotlinx.coroutines.flow.first
import java.io.File
import java.util.concurrent.TimeUnit

class AutoCleanWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        Log.d("AutoCleanWorker", "Auto cleanup worker started running...")
        
        val userPreferencesRepository = ServiceLocator.userPreferencesRepository
        val cleanupLogRepository = ServiceLocator.cleanupLogRepository
        
        // Respect snooze
        try {
            val timestamp = userPreferencesRepository.snoozeUntil.first()
            if (System.currentTimeMillis() < timestamp) {
                Log.d("AutoCleanWorker", "Cleanup worker skipped: currently inside snooze period.")
                return Result.success()
            }
        } catch (e: Exception) {
            Log.e("AutoCleanWorker", "Could not check snooze status", e)
        }

        // Check if scheduled cleanup is even enabled (in case of double check)
        var settings: com.example.data.UserPreferencesRepository.CleanerSettings? = null
        try {
            settings = userPreferencesRepository.cleanerSettings.first()
        } catch (e: Exception) {
            Log.e("AutoCleanWorker", "Could not load settings", e)
        }

        // If not enabled, return success directly
        if (settings != null && !settings.scheduledCleanupEnabled) {
            Log.d("AutoCleanWorker", "Scheduled cleanup is disabled in setting preferences")
            return Result.success()
        }

        var totalBytesFreed = 0L

        // 1. Clears app cache via context.cacheDir.deleteRecursively()
        if (settings == null || settings.autoCleanCache) {
            val cacheDir = applicationContext.cacheDir
            val sizeBefore = getFolderSize(cacheDir)
            try {
                val subFiles = cacheDir.listFiles()
                subFiles?.forEach { subFile ->
                    subFile.deleteRecursively()
                }
                val sizeAfter = getFolderSize(cacheDir)
                val cacheFreed = (sizeBefore - sizeAfter).coerceAtLeast(0L)
                totalBytesFreed += cacheFreed
                cleanupLogRepository.logCleanup("Cache", cacheFreed)
                Log.d("AutoCleanWorker", "Cache cleared. Bytes reclaimed: $cacheFreed")
            } catch (e: Exception) {
                Log.e("AutoCleanWorker", "Failed to clear cache", e)
            }
        }

        // 2. Deletes APK files from Downloads older than 30 days
        if (settings == null || settings.autoCleanApks) {
            try {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (downloadsDir.exists() && downloadsDir.isDirectory) {
                    val thirtyDaysAgo = System.currentTimeMillis() - 30L * 24L * 60L * 60L * 1000L
                    val oldApks = downloadsDir.listFiles()?.filter { file ->
                        file.isFile && file.name.endsWith(".apk", ignoreCase = true) && file.lastModified() < thirtyDaysAgo
                    }
                    var apkFreed = 0L
                    oldApks?.forEach { file ->
                        val length = file.length()
                        if (file.delete()) {
                            apkFreed += length
                        }
                    }
                    totalBytesFreed += apkFreed
                    cleanupLogRepository.logCleanup("APKs", apkFreed)
                    Log.d("AutoCleanWorker", "Stale APKs deleted. Bytes reclaimed: $apkFreed")
                }
            } catch (e: Exception) {
                Log.e("AutoCleanWorker", "Failed to clean up stale APKs in downloads", e)
            }
        }

        // Check critical storage for alerts
        try {
            NotificationHelper.checkAndNotifyCriticalStorage(applicationContext)
        } catch (e: Exception) {
            Log.e("AutoCleanWorker", "Failed to run storage notification check", e)
        }

        return Result.success()
    }

    private fun getFolderSize(file: File): Long {
        if (!file.exists()) return 0L
        if (file.isDirectory) {
            var size = 0L
            file.listFiles()?.forEach { child ->
                size += getFolderSize(child)
            }
            return size
        }
        return file.length()
    }

    companion object {
        const val WORK_NAME = "com.example.work.AutoCleanWorker"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.UNMETERED)
                .setRequiresBatteryNotLow(true)
                .build()

            // Daily periodic work req (24hr interval, minimum 1hr flex = 1hr)
            val workRequest = PeriodicWorkRequestBuilder<AutoCleanWorker>(
                24, TimeUnit.HOURS,
                1, TimeUnit.HOURS
            )
            .setConstraints(constraints)
            .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
            Log.d("AutoCleanWorker", "Scheduled unique periodic work successfully")
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
            Log.d("AutoCleanWorker", "Cancelled periodic work successfully")
        }
    }
}
