package com.smartcleaner.backend

import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.pow

data class StorageCategory(val category: String, val sizeBytes: Long)
data class ScanAnalysis(val totalSizeBytes: Long, val totalSizeFormatted: String, val recommendations: List<String>)

object StorageAnalyzer {
    private val units = listOf("Bytes", "KB", "MB", "GB", "TB")

    fun formatBytes(bytes: Long): String {
        require(bytes >= 0) { "Byte count cannot be negative." }
        if (bytes < 1024) return "$bytes Bytes"
        val unitIndex = floor(ln(bytes.toDouble()) / ln(1024.0)).toInt().coerceIn(0, units.lastIndex)
        val value = bytes / 1024.0.pow(unitIndex)
        return "%.2f %s".format(java.util.Locale.US, value, units[unitIndex])
    }

    fun analyze(categories: List<StorageCategory>): ScanAnalysis {
        require(categories.all { it.category.isNotBlank() && it.sizeBytes >= 0 }) {
            "Each category needs a name and a non-negative size."
        }
        val total = categories.fold(0L) { sum, item -> Math.addExact(sum, item.sizeBytes) }
        val cacheBytes = categories.filter { it.category.equals("Cache", true) }.sumOf { it.sizeBytes }
        val duplicateBytes = categories.filter { it.category.equals("Duplicates", true) }.sumOf { it.sizeBytes }
        val advice = listOf(
            "Clear temporary app caches first (estimated ${formatBytes(cacheBytes)}).",
            "Review duplicate files before deleting them (estimated ${formatBytes(duplicateBytes)}).",
            "Review downloaded APKs and large media manually before removal.",
            "Keep personal photos and documents protected unless you explicitly select them.",
        )
        return ScanAnalysis(total, formatBytes(total), advice)
    }
}
