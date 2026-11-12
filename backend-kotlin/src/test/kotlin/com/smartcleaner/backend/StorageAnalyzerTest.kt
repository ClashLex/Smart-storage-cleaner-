package com.smartcleaner.backend

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class StorageAnalyzerTest {
    @Test
    fun formatsBytesUsingBinaryUnits() {
        assertEquals("1.00 KB", StorageAnalyzer.formatBytes(1024))
        assertEquals("1.00 MB", StorageAnalyzer.formatBytes(1024L * 1024))
        assertEquals("0 Bytes", StorageAnalyzer.formatBytes(0))
    }

    @Test
    fun calculatesTotalsAndRecommendations() {
        val result = StorageAnalyzer.analyze(listOf(
            StorageCategory("Cache", 50_000_000),
            StorageCategory("Duplicates", 250_000_000),
            StorageCategory("APKs", 120_000_000),
        ))
        assertEquals(420_000_000L, result.totalSizeBytes)
        assertEquals("400.54 MB", result.totalSizeFormatted)
        assertEquals(4, result.recommendations.size)
    }

    @Test
    fun rejectsNegativeSizes() {
        assertFailsWith<IllegalArgumentException> {
            StorageAnalyzer.analyze(listOf(StorageCategory("Cache", -1)))
        }
    }

    @Test
    fun rejectsTotalOverflow() {
        assertFailsWith<ArithmeticException> {
            StorageAnalyzer.analyze(listOf(StorageCategory("A", Long.MAX_VALUE), StorageCategory("B", 1)))
        }
    }
}
