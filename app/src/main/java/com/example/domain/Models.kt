package com.example.domain

import androidx.annotation.Keep

@Keep
data class User(
    val uid: String,
    val name: String,
    val email: String,
    val isPremium: Boolean = false,
    val subscriptionExpiry: Long = 0L,
    val createdAt: Long = System.currentTimeMillis()
)

@Keep
data class DeviceInfo(
    val deviceId: String,
    val model: String,
    val osVersion: String
)

@Keep
data class AuthSyncRequest(
    val idToken: String,
    val device: DeviceInfo
)

@Keep
data class AuthSyncResponse(
    val user: User,
    val token: String
)

@Keep
data class SubscriptionState(
    val activeProduct: String?, // "cleaner_pro_monthly", "cleaner_pro_annual", "cleaner_lifetime"
    val isPremium: Boolean,
    val expiryTime: Long
)

@Keep
data class CleanupLogRequest(
    val category: String,
    val bytesFreed: Long
)

@Keep
data class ScanSession(
    val id: String,
    val totalPhotosScanned: Int,
    val duplicatesFound: Int,
    val blurryFound: Int,
    val totalSizeMB: Long,
    val timestamp: Long = System.currentTimeMillis()
)
