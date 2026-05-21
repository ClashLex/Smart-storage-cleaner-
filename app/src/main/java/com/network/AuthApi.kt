package com.example.network

import com.example.domain.*
import retrofit2.Response
import retrofit2.http.*

interface AuthApi {
    @POST("api/auth/sync")
    suspend fun syncUser(
        @Body request: AuthSyncRequest
    ): Response<AuthSyncResponse>

    @GET("api/auth/me")
    suspend fun getProfile(): Response<User>

    @DELETE("api/auth/me")
    suspend fun deleteAccount(): Response<Map<String, Boolean>>

    @POST("api/scan")
    suspend fun submitScan(
        @Body session: ScanSession
    ): Response<Map<String, String>> // return AI recommendations

    @POST("api/cleanup/log")
    suspend fun logCleanup(
        @Body log: CleanupLogRequest
    ): Response<Map<String, Any>>

    @POST("api/billing/verify")
    suspend fun verifyPurchase(
        @Body request: PurchaseVerificationRequest
    ): Response<SubscriptionState>

    @GET("api/billing/entitlement")
    suspend fun getEntitlement(): Response<SubscriptionState>
}

data class PurchaseVerificationRequest(
    val productId: String,
    val purchaseToken: String
)
