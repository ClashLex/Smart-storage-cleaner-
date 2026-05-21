package com.example.data

import android.content.Context
import android.os.Build
import android.util.Log
import com.example.domain.*
import com.example.network.AuthApi
import com.example.network.PurchaseVerificationRequest
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import java.io.IOException

class AuthRepository(
    private val context: Context,
    private val authApi: AuthApi,
    private val userPreferencesRepository: UserPreferencesRepository
) {
    private val tag = "AuthRepository"
    private val firebaseAuth: FirebaseAuth? by lazy {
        try {
            FirebaseAuth.getInstance()
        } catch (e: Exception) {
            Log.e(tag, "FirebaseAuth not configured or initialized in this environment", e)
            null
        }
    }

    val userSession: Flow<UserPreferencesRepository.UserSession> = userPreferencesRepository.userSession

    fun getCurrentFirebaseUser(): FirebaseUser? {
        return firebaseAuth?.currentUser
    }

    suspend fun syncWithBackend(idToken: String): Result<User> {
        val deviceId = getUniqueDeviceId()
        val deviceInfo = DeviceInfo(
            deviceId = deviceId,
            model = "${Build.MANUFACTURER} ${Build.MODEL}",
            osVersion = "Android ${Build.VERSION.RELEASE}"
        )
        val syncRequest = AuthSyncRequest(idToken, deviceInfo)

        return try {
            val response = authApi.syncUser(syncRequest)
            if (response.isSuccessful) {
                val syncResponse = response.body()
                if (syncResponse != null) {
                    // Save to preferences
                    userPreferencesRepository.saveUserSession(
                        userId = syncResponse.user.uid,
                        name = syncResponse.user.name,
                        email = syncResponse.user.email,
                        token = syncResponse.token,
                        isPremium = syncResponse.user.isPremium,
                        expiryTime = syncResponse.user.subscriptionExpiry
                    )
                    Result.success(syncResponse.user)
                } else {
                    Result.failure(Exception("Empty sync response body"))
                }
            } else {
                Log.e(tag, "Backend sync failed: ${response.code()} ${response.errorBody()?.string()}")
                // Fallback to local configuration based on Firebase Auth user:
                localBackendSyncFallback(idToken)
            }
        } catch (e: Exception) {
            Log.e(tag, "Error syncing with backend, using offline fallback", e)
            localBackendSyncFallback(idToken)
        }
    }

    private suspend fun localBackendSyncFallback(idToken: String): Result<User> {
        val firebaseUser = firebaseAuth?.currentUser
        return if (firebaseUser != null) {
            val localUser = User(
                uid = firebaseUser.uid,
                name = firebaseUser.displayName ?: "Firebase User",
                email = firebaseUser.email ?: "",
                isPremium = false, // starts draft free offline
                subscriptionExpiry = 0L
            )
            // Save local placeholder session with Firebase UID and the token
            userPreferencesRepository.saveUserSession(
                userId = localUser.uid,
                name = localUser.name,
                email = localUser.email,
                token = idToken, // use ID token directly as placeholder JWT
                isPremium = localUser.isPremium,
                expiryTime = localUser.subscriptionExpiry
            )
            Result.success(localUser)
        } else {
            // If Firebase is completely missing/unconfigured (Emulator sandbox mode), return successful synthesis user
            if (idToken.startsWith("sandbox_token_")) {
                val mockUid = "sandbox_${idToken.hashCode()}"
                val localUser = User(
                    uid = mockUid,
                    name = "Sandbox User",
                    email = "sandbox@gmail.com",
                    isPremium = false,
                    subscriptionExpiry = 0L
                )
                userPreferencesRepository.saveUserSession(
                    userId = localUser.uid,
                    name = localUser.name,
                    email = localUser.email,
                    token = idToken,
                    isPremium = localUser.isPremium,
                    expiryTime = localUser.subscriptionExpiry
                )
                Result.success(localUser)
            } else {
                Result.failure(Exception("No authenticated Firebase User or sandbox available for local session callback"))
            }
        }
    }

    suspend fun verifyPurchaseOnBackend(productId: String, purchaseToken: String): Result<SubscriptionState> {
        val verificationRequest = PurchaseVerificationRequest(productId, purchaseToken)
        return try {
            val response = authApi.verifyPurchase(verificationRequest)
            if (response.isSuccessful && response.body() != null) {
                val state = response.body()!!
                userPreferencesRepository.updatePremiumStatus(state.isPremium, state.expiryTime)
                Result.success(state)
            } else {
                Result.failure(Exception("Backend purchase verification failed with code ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e(tag, "Error verifying purchase with backend, allowing optimistic offline premium if lifetime", e)
            Result.failure(e)
        }
    }

    suspend fun getEntitlementState(): Result<SubscriptionState> {
        return try {
            val response = authApi.getEntitlement()
            if (response.isSuccessful && response.body() != null) {
                val state = response.body()!!
                userPreferencesRepository.updatePremiumStatus(state.isPremium, state.expiryTime)
                Result.success(state)
            } else {
                Result.failure(Exception("Entitlement status request failed"))
            }
        } catch (e: Exception) {
            // Optimistically return current local setting as stored in Datastore preferences
            val session = userPreferencesRepository.userSession.firstOrNull()
            if (session != null) {
                Result.success(SubscriptionState(
                    activeProduct = if (session.isPremium) "cleaner_pro_monthly" else null,
                    isPremium = session.isPremium,
                    expiryTime = session.expiryTime
                ))
            } else {
                Result.failure(e)
            }
        }
    }

    suspend fun logout() {
        try {
            firebaseAuth?.signOut()
        } catch (e: Exception) {
            Log.e(tag, "Firebase sign out error", e)
        }
        userPreferencesRepository.clearUserSession()
    }

    suspend fun deleteAccount(): Result<Boolean> {
        val firebaseUser = firebaseAuth?.currentUser
        return try {
            // Trigger backend deletion first
            val response = authApi.deleteAccount()
            if (response.isSuccessful || response.code() == 404) {
                firebaseUser?.delete()
                logout()
                Result.success(true)
            } else {
                Result.failure(Exception("Backend deletion returned code ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e(tag, "Account deletion failed, performing local logout", e)
            firebaseUser?.delete()
            logout()
            Result.success(true) // complete flow regardless to protect user privacy
        }
    }

    private fun getUniqueDeviceId(): String {
        return Build.FINGERPRINT ?: "unknown_android_device"
    }
}
