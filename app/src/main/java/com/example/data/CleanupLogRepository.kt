package com.example.data

import android.util.Log
import com.example.domain.CleanupLogRequest
import com.example.network.AuthApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CleanupLogRepository(private val authApi: AuthApi) {
    private val tag = "CleanupLogRepository"

    suspend fun logCleanup(category: String, bytesFreed: Long): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = authApi.logCleanup(CleanupLogRequest(category, bytesFreed))
            if (response.isSuccessful) {
                Log.d(tag, "Successfully logged cleanup: $category, bytes: $bytesFreed")
                Result.success(Unit)
            } else {
                Log.e(tag, "Failed to log cleanup: ${response.code()} ${response.errorBody()?.string()}")
                Result.failure(Exception("Backend logging returned code ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e(tag, "Error logging cleanup with server: ${e.message}", e)
            Result.failure(e)
        }
    }
}
