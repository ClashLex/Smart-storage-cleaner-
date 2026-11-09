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
    val device: DeviceInfo
)

@Keep
data class SubscriptionState(
    val activeProduct: String?,
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

@Keep
data class JunkItem(
    val id: String,
    val name: String,
    val size: Long,
    val detail: String,
    val dateString: String,
    val checked: Boolean = true
)
