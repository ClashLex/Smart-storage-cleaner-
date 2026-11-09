package com.example.network

import com.example.domain.*
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import retrofit2.Response
import retrofit2.http.*

interface AuthApi {
    @POST("api/auth/sync")
    suspend fun syncUser(
        @Body request: AuthSyncRequest,
        @Header("Authorization") authorization: String
    ): Response<BackendAuthSyncResponse>

    @GET("api/auth/me")
    suspend fun getProfile(): Response<BackendUserDto>

    @DELETE("api/auth/me")
    suspend fun deleteAccount(): Response<Map<String, Boolean>>

    @POST("api/scan")
    suspend fun submitScan(
        @Body session: ScanSession
    ): Response<Map<String, String>>

    @POST("api/cleanup/log")
    suspend fun logCleanup(
        @Body log: CleanupLogRequest
    ): Response<Map<String, Any>>

    @POST("api/billing/verify")
    suspend fun verifyPurchase(
        @Body request: PurchaseVerificationRequest
    ): Response<BackendPurchaseVerificationResponse>

    @GET("api/billing/entitlement")
    suspend fun getEntitlement(): Response<BackendEntitlementResponse>
}

data class PurchaseVerificationRequest(
    val productId: String,
    val purchaseToken: String
)

data class BackendUserDto(
    val id: String,
    val firebaseUid: String,
    val email: String?,
    val displayName: String?,
    val premiumEntitled: Boolean,
    val role: String? = "user"
) {
    fun toDomain() = User(
        uid = firebaseUid,
        name = displayName ?: email?.substringBefore('@') ?: "User",
        email = email.orEmpty(),
        isPremium = premiumEntitled
    )
}

data class BackendAuthSyncResponse(
    val token: String,
    val user: BackendUserDto
)

data class BackendEntitlementResponse(
    val premiumEntitled: Boolean,
    val subscription: BackendSubscriptionDto?
) {
    fun toDomain() = SubscriptionState(
        activeProduct = subscription?.productId,
        isPremium = premiumEntitled,
        expiryTime = subscription?.expiryTime?.let {
            parseBackendDate(it)
        } ?: 0L
    )
}

data class BackendPurchaseVerificationResponse(
    val success: Boolean,
    val subscription: BackendSubscriptionDto?,
    val entitlement: BackendEntitlementDto?
) {
    fun toDomain() = SubscriptionState(
        activeProduct = subscription?.productId,
        isPremium = entitlement?.premiumEntitled ?: false,
        expiryTime = subscription?.expiryTime?.let {
            runCatching { java.time.Instant.parse(it).toEpochMilli() }.getOrDefault(0L)
        } ?: 0L
    )
}

data class BackendSubscriptionDto(
    val productId: String?,
    val orderId: String?,
    val expiryTime: String?
)

data class BackendEntitlementDto(val premiumEntitled: Boolean)

private fun parseBackendDate(value: String): Long {
    val formats = listOf(
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        "yyyy-MM-dd'T'HH:mm:ss'Z'"
    )
    return formats.firstNotNullOfOrNull { pattern ->
        runCatching {
            SimpleDateFormat(pattern, Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
                isLenient = false
            }.parse(value)?.time
        }.getOrNull()
    } ?: 0L
}
