package com.example.data

import android.content.Context
import android.os.Environment
import com.example.domain.JunkItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class JunkScanResult(
    val whatsappItems: List<JunkItem>,
    val apkItems: List<JunkItem>,
    val cacheItems: List<JunkItem>
)

class JunkScanRepository {
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    fun scanWhatsAppFiles(context: Context): List<JunkItem> {
        val mediaDirs = listOf(
            File(Environment.getExternalStorageDirectory(), "WhatsApp/Media/"),
            File(Environment.getExternalStorageDirectory(), "Android/media/com.whatsapp/WhatsApp/Media/")
        )

        val mediaDir = mediaDirs.firstOrNull { it.exists() && it.isDirectory && it.canRead() }
            ?: return emptyList()

        return mediaDir.walkTopDown()
            .filter { it.isFile }
            .map { file ->
                JunkItem(
                    id = file.absolutePath,
                    name = file.name,
                    size = file.length(),
                    detail = file.parentFile?.name.orEmpty(),
                    dateString = formatDate(file.lastModified())
                )
            }
            .toList()
    }

    fun scanApkFiles(context: Context): List<JunkItem> {
        val downloadDirs = listOf(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            File(Environment.getExternalStorageDirectory(), "Download")
        )

        return downloadDirs
            .filter { it.exists() && it.isDirectory && it.canRead() }
            .flatMap { dir ->
                dir.walkTopDown()
                    .filter { it.isFile && it.extension.equals("apk", ignoreCase = true) }
                    .map { file ->
                        JunkItem(
                            id = file.absolutePath,
                            name = file.name,
                            size = file.length(),
                            detail = "APK Installer / ${file.parent}",
                            dateString = formatDate(file.lastModified())
                        )
                    }
                    .toList()
            }
    }

    fun scanCacheFiles(context: Context): List<JunkItem> {
        val items = mutableListOf<JunkItem>()
        val internalCacheSize = calculateDirectorySize(context.cacheDir)
        items.add(
            JunkItem(
                id = "internal_cache",
                name = "${context.packageName} Cache",
                size = internalCacheSize,
                detail = "Internal App Cache",
                dateString = "Just now"
            )
        )

        context.externalCacheDirs.filterNotNull().forEachIndexed { index, dir ->
            items.add(
                JunkItem(
                    id = "external_cache_$index",
                    name = "${context.packageName} External Cache",
                    size = calculateDirectorySize(dir),
                    detail = "External App Cache",
                    dateString = "Just now"
                )
            )
        }

        return items
    }

    suspend fun performFullScan(context: Context): JunkScanResult = withContext(Dispatchers.IO) {
        JunkScanResult(
            whatsappItems = scanWhatsAppFiles(context),
            apkItems = scanApkFiles(context),
            cacheItems = scanCacheFiles(context)
        )
    }

    private fun calculateDirectorySize(dir: File): Long {
        if (!dir.exists() || !dir.isDirectory || !dir.canRead()) {
            return 0L
        }
        return dir.walkTopDown().sumOf { it.length() }
    }

    private fun formatDate(timestamp: Long): String {
        return dateFormat.format(Date(timestamp))
    }
}
