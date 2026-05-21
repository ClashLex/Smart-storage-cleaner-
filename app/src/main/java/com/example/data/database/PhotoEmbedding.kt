package com.example.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "photo_embeddings")
data class PhotoEmbedding(
    @PrimaryKey val uri: String,
    val fileName: String,
    val fileSize: Long,
    val lastModified: Long,
    val width: Int,
    val height: Int,
    val dhash: Long, // decimal representation of dHash
    val blurScore: Double, // Laplacian variance
    val embedding: String, // String representation of 1024 float array (comma separated)
    val isScanned: Boolean = true,
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface PhotoEmbeddingDao {
    @Query("SELECT * FROM photo_embeddings")
    fun getAllEmbeddings(): Flow<List<PhotoEmbedding>>

    @Query("SELECT * FROM photo_embeddings WHERE uri = :uri LIMIT 1")
    suspend fun getEmbeddingByUri(uri: String): PhotoEmbedding?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEmbedding(embedding: PhotoEmbedding)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(embeddings: List<PhotoEmbedding>)

    @Query("DELETE FROM photo_embeddings WHERE uri = :uri")
    suspend fun deleteByUri(uri: String)

    @Query("DELETE FROM photo_embeddings WHERE uri IN (:uris)")
    suspend fun deleteByUris(uris: List<String>)

    @Query("DELETE FROM photo_embeddings")
    suspend fun clearAll()
}
