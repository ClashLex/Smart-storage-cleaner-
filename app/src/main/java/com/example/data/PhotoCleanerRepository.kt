package com.example.data

import android.content.Context
import android.util.Log
import com.example.data.database.PhotoEmbedding
import com.example.data.database.PhotoEmbeddingDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlin.math.sqrt

class PhotoCleanerRepository(
    private val context: Context,
    private val photoEmbeddingDao: PhotoEmbeddingDao
) {
    private val tag = "PhotoCleanerRepository"

    val allEmbeddings: Flow<List<PhotoEmbedding>> = photoEmbeddingDao.getAllEmbeddings()

    suspend fun clearAll() {
        photoEmbeddingDao.clearAll()
    }

    suspend fun deletePhotos(uris: List<String>) {
        withContext(Dispatchers.IO) {
            photoEmbeddingDao.deleteByUris(uris)
        }
    }

    /**
     * Simulates scanning internal storage directories (Camera, WhatsApp, Screenshots),
     * generating robust high-fidelity mock image metadata with dHash attributes, blur scores,
     * and 1024-float embeddings (represented as text) for classification.
     */
    suspend fun performSmartScan(onProgress: (Int) -> Unit) {
        withContext(Dispatchers.IO) {
            // First clear old database scanning data to simulate a clean updated scan
            photoEmbeddingDao.clearAll()

            val totalSteps = 100
            val mockPhotos = getRealisticMockPhotos()

            // Simulate progress ticks for realistic UI experience
            for (progress in 1..100) {
                delay(15) // Simulate heavy computations (e.g., running TFLite embeddings in NPU)
                onProgress(progress)
            }

            photoEmbeddingDao.insertAll(mockPhotos)
            Log.d(tag, "Successfully processed and indexed ${mockPhotos.size} photos into database")
        }
    }

    /**
     * Group items from scanned results by duplicates (using TFLite embedding cosine similarity or matching dhash)
     */
    fun groupDuplicates(photos: List<PhotoEmbedding>): List<DuplicateGroup> {
        val visited = mutableSetOf<String>()
        val groups = mutableListOf<DuplicateGroup>()

        for (i in photos.indices) {
            val photoA = photos[i]
            if (photoA.uri in visited) continue

            val currentGroup = mutableListOf<PhotoEmbedding>()
            currentGroup.add(photoA)

            val vecA = parseEmbedding(photoA.embedding)

            for (j in (i + 1) until photos.size) {
                val photoB = photos[j]
                if (photoB.uri in visited) continue

                // Check strict matching dHash or cosine similarity from model embeddings
                val vecB = parseEmbedding(photoB.embedding)
                val similarity = calculateCosineSimilarity(vecA, vecB)

                if (photoA.dhash == photoB.dhash || similarity > 0.92f) {
                    currentGroup.add(photoB)
                }
            }

            if (currentGroup.size > 1) {
                // Determine recommendation keeper tag: keeper is the oldest, largest, or clearest image
                val sortedGroup = currentGroup.sortedWith(compareByDescending<PhotoEmbedding> { it.blurScore }
                    .thenBy { it.lastModified })

                val keeper = sortedGroup.first()
                val duplicates = sortedGroup.drop(1)

                visited.addAll(currentGroup.map { it.uri })
                groups.add(DuplicateGroup(
                    groupId = photoA.uri.hashCode().toString(),
                    keeper = keeper,
                    duplicates = duplicates
                ))
            }
        }

        return groups
    }

    private fun parseEmbedding(embeddingStr: String): FloatArray {
        if (embeddingStr.isEmpty()) return FloatArray(1024)
        return try {
            embeddingStr.split(",").map { it.toFloat() }.toFloatArray()
        } catch (e: Exception) {
            FloatArray(1024)
        }
    }

    private fun calculateCosineSimilarity(vecA: FloatArray, vecB: FloatArray): Float {
        if (vecA.size != vecB.size || vecA.isEmpty()) return 0f
        var dotProduct = 0.0f
        var normA = 0.0f
        var normB = 0.0f
        for (i in vecA.indices) {
            dotProduct += vecA[i] * vecB[i]
            normA += vecA[i] * vecA[i]
            normB += vecB[i] * vecB[i]
        }
        val denom = sqrt(normA) * sqrt(normB)
        return if (denom == 0.0f) 0f else (dotProduct / denom)
    }

    private fun getRealisticMockPhotos(): List<PhotoEmbedding> {
        val now = System.currentTimeMillis()
        val list = mutableListOf<PhotoEmbedding>()

        // 1. Group 1: Identical WhatsApp copies -> strict hash duplicate
        val dhash1 = 89452378901L
        val emb1 = generateMockEmbedding(0.95f)
        list.add(PhotoEmbedding(
            uri = "content://media/external/images/media/101",
            fileName = "IMG_20260520_142211.jpg",
            fileSize = 4851200L, // 4.62 MB
            lastModified = now - 86400000,
            width = 4000,
            height = 3000,
            dhash = dhash1,
            blurScore = 32.5, // Crisp
            embedding = emb1
        ))
        list.add(PhotoEmbedding(
            uri = "content://media/external/images/media/102",
            fileName = "IMG_20260520_142211_WA.jpg",
            fileSize = 1048576L, // 1 MB (compressed)
            lastModified = now - 43200000,
            width = 1600,
            height = 1200,
            dhash = dhash1,
            blurScore = 28.1, // slightly compressed
            embedding = emb1
        ))

        // 2. Group 2: High similarity AI clustering (TFLite continuous capture burst shots)
        val dhash2 = 123498761234L
        val dhash2B = 123498761235L // slight dhash drift due to movement
        val baseEmb2 = generateMockEmbedding(0.48f)
        val similarEmb2 = mutateEmbedding(baseEmb2, 0.96f)
        list.add(PhotoEmbedding(
            uri = "content://media/external/images/media/201",
            fileName = "DSC_0981_BURST_01.jpg",
            fileSize = 8388608L, // 8.00 MB
            lastModified = now - 172800000,
            width = 4032,
            height = 3024,
            dhash = dhash2,
            blurScore = 44.2, // Perfect focus
            embedding = baseEmb2
        ))
        list.add(PhotoEmbedding(
            uri = "content://media/external/images/media/202",
            fileName = "DSC_0981_BURST_02.jpg",
            fileSize = 8295500L, // 7.91 MB
            lastModified = now - 172795000,
            width = 4032,
            height = 3024,
            dhash = dhash2B,
            blurScore = 41.9, // Excellent focus
            embedding = similarEmb2
        ))

        // 3. Blurry Space Wasters (Singletons with poor Laplacian score)
        list.add(PhotoEmbedding(
            uri = "content://media/external/images/media/301",
            fileName = "IMG_OUT_OF_FOCUS_NIGHT.jpg",
            fileSize = 12582912L, // Massive 12MB blurry photo
            lastModified = now - 259200000,
            width = 4160,
            height = 3120,
            dhash = 665544332211L,
            blurScore = 4.8, // Severe blur (< 15 means waste candidate!)
            embedding = generateMockEmbedding(0.12f)
        ))
        list.add(PhotoEmbedding(
            uri = "content://media/external/images/media/302",
            fileName = "IMG_BLURRY_MOTION.jpg",
            fileSize = 5120000L, // 4.88 MB
            lastModified = now - 345600000,
            width = 3000,
            height = 4000,
            dhash = 771122334411L,
            blurScore = 6.2, // Blur candidate
            embedding = generateMockEmbedding(0.22f)
        ))

        // 4. Large single unique files (to test space wasters listings)
        list.add(PhotoEmbedding(
            uri = "content://media/external/images/media/401",
            fileName = "HD_SYSTEM_RENDER_BACKGROUND.png",
            fileSize = 25165824L, // 24 MB huge rendering
            lastModified = now - 432000000,
            width = 8000,
            height = 6000,
            dhash = 998877665544L,
            blurScore = 85.0, // Extremely sharp
            embedding = generateMockEmbedding(0.78f)
        ))

        // 5. Normal sharp photos (no duplicate, no blur - should never be touched)
        list.add(PhotoEmbedding(
            uri = "content://media/external/images/media/501",
            fileName = "DSC_FAMILY_PORTRAIT.jpg",
            fileSize = 3456000L,
            lastModified = now - 500000000,
            width = 4000,
            height = 3000,
            dhash = 111222333444L,
            blurScore = 39.1,
            embedding = generateMockEmbedding(0.55f)
        ))

        return list
    }

    private fun generateMockEmbedding(seed: Float): String {
        val array = FloatArray(1024) { i ->
            (seed + i * 0.0001f) % 1.0f
        }
        return array.joinToString(",")
    }

    private fun mutateEmbedding(baseStr: String, similarity: Float): String {
        val baseVec = parseEmbedding(baseStr)
        val array = FloatArray(1024) { i ->
            baseVec[i] + ((i % 10) * 0.001f * (1.0f - similarity))
        }
        return array.joinToString(",")
    }
}

data class DuplicateGroup(
    val groupId: String,
    val keeper: PhotoEmbedding,
    val duplicates: List<PhotoEmbedding>
) {
    // Total size of items that can be freed safely
    val potentialSavings: Long get() = duplicates.sumOf { it.fileSize }
}
