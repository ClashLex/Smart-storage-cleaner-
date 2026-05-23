package com.example.data

import android.content.Context
import com.example.data.database.AppDatabase
import com.example.network.NetworkClient

object ServiceLocator {
    private var applicationContext: Context? = null

    val appDatabase: AppDatabase by lazy {
        AppDatabase.getDatabase(requireContext())
    }

    val userPreferencesRepository: UserPreferencesRepository by lazy {
        UserPreferencesRepository(requireContext())
    }

    val networkClient: NetworkClient by lazy {
        NetworkClient(userPreferencesRepository)
    }

    val authRepository: AuthRepository by lazy {
        AuthRepository(
            context = requireContext(),
            authApi = networkClient.authApi,
            userPreferencesRepository = userPreferencesRepository
        )
    }

    val photoCleanerRepository: PhotoCleanerRepository by lazy {
        PhotoCleanerRepository(
            context = requireContext(),
            photoEmbeddingDao = appDatabase.photoEmbeddingDao()
        )
    }

    val storageStatsRepository: StorageStatsRepository by lazy {
        StorageStatsRepository()
    }

    val cleanupLogRepository: CleanupLogRepository by lazy {
        CleanupLogRepository(networkClient.authApi)
    }

    fun initialize(context: Context) {
        if (applicationContext == null) {
            applicationContext = context.applicationContext
        }
    }

    private fun requireContext(): Context {
        return applicationContext ?: throw IllegalStateException("ServiceLocator has not been initialized. Call initialize(context) first in Application class.")
    }
}
