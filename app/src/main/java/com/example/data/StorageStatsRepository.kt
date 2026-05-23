package com.example.data

import android.os.Environment
import android.os.StatFs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class StorageStats(
    val totalBytes: Long,
    val usedBytes: Long,
    val availableBytes: Long,
    val usedPercentage: Float
)

class StorageStatsRepository {
    suspend fun getStorageStats(): StorageStats = withContext(Dispatchers.IO) {
        val path = Environment.getDataDirectory().absolutePath
        val stat = StatFs(path)
        val totalBytes = stat.blockCountLong * stat.blockSizeLong
        val availableBytes = stat.availableBlocksLong * stat.blockSizeLong
        val usedBytes = totalBytes - availableBytes
        val usedPercentage = if (totalBytes > 0L) {
            ((usedBytes.toDouble() / totalBytes.toDouble()) * 100.0).toFloat()
        } else {
            0f
        }
        StorageStats(
            totalBytes = totalBytes,
            usedBytes = usedBytes,
            availableBytes = availableBytes,
            usedPercentage = usedPercentage
        )
    }
}
