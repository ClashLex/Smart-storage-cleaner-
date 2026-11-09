package com.example.data

import android.content.Context
import android.os.Build
import android.util.Log
import com.example.domain.DeviceInfo
import com.example.domain.AuthSyncRequest
import com.example.domain.User
import com.example.domain.SubscriptionState
import com.example.network.AuthApi
import com.example.network.PurchaseVerificationRequest
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

class AuthRepository(
    private val context: Context,
    private val authApi: AuthApi,
    private val userPreferencesRepository: UserPreferencesRepository
) {
    private val tag = "AuthRepository"
    private val firebaseAuth: FirebaseAuth? by lazy {
        runCatching { FirebaseAuth.getInstance() }
            .onFailure { Log.e(tag, "Firebase Auth is unavailable.", it) }
            .getOrNull()
    }

    val userSession: Flow<UserPreferencesRepository.UserSession> = userPreferencesRepository.userSession

    fun getCurrentFirebaseUser(): FirebaseUser? = firebaseAuth?.currentUser

    suspend fun syncWithBackend(idToken: String): Result<User> {
        if (idToken.isBlank()) return Result.failure(IllegalArgumentException("Firebase ID token is missing."))
        val deviceInfo = DeviceInfo(
            deviceId = Build.FINGERPRINT ?: "unknown_android_device",
            model = "${Build.MANUFACTURER} ${Build.MODEL}",
            osVersion = "Android ${Build.VERSION.RELEASE}"
        )

        return try {
            val response = authApi.syncUser(AuthSyncRequest(deviceInfo), "Bearer $idToken")
            if (!response.isSuccessful) {
                return Result.failure(Exception("Backend authentication failed (HTTP ${response.code()})."))
            }
            val syncResponse = response.body()
                ?: return Result.failure(Exception("Backend returned an empty authentication response."))
            val user = syncResponse.user.toDomain()
            userPreferencesRepository.saveUserSession(
                userId = user.uid,
                name = user.name,
                email = user.email,
                token = syncResponse.token,
                isPremium = user.isPremium,
                expiryTime = user.subscriptionExpiry
            )
            Result.success(user)
        } catch (error: Exception) {
            Log.e(tag, "Backend authentication synchronization failed.", error)
            Result.failure(error)
        }
    }

    suspend fun verifyPurchaseOnBackend(productId: String, purchaseToken: String): Result<SubscriptionState> {
        if (productId.isBlank() || purchaseToken.isBlank()) {
            return Result.failure(IllegalArgumentException("Purchase details are incomplete."))
        }
        return try {
            val response = authApi.verifyPurchase(PurchaseVerificationRequest(productId, purchaseToken))
            if (response.isSuccessful) {
                val state = response.body()?.toDomain()
                    ?: return Result.failure(Exception("Backend returned an empty purchase verification response."))
                userPreferencesRepository.updatePremiumStatus(state.isPremium, state.expiryTime)
                Result.success(state)
            } else {
                Result.failure(Exception("Purchase verification failed (HTTP ${response.code()})."))
            }
        } catch (error: Exception) {
            Log.e(tag, "Purchase verification failed.", error)
            Result.failure(error)
        }
    }

    suspend fun getEntitlementState(): Result<SubscriptionState> {
        return try {
            val response = authApi.getEntitlement()
            if (!response.isSuccessful) {
                return Result.failure(Exception("Entitlement request failed (HTTP ${response.code()})."))
            }
            val state = response.body()?.toDomain()
                ?: return Result.failure(Exception("Backend returned an empty entitlement response."))
            userPreferencesRepository.updatePremiumStatus(state.isPremium, state.expiryTime)
            Result.success(state)
        } catch (error: Exception) {
            Log.e(tag, "Unable to refresh entitlement from backend.", error)
            Result.failure(error)
        }
    }

    suspend fun logout() {
        runCatching { firebaseAuth?.signOut() }
            .onFailure { Log.e(tag, "Firebase sign-out failed.", it) }
        userPreferencesRepository.clearUserSession()
    }

    suspend fun deleteAccount(): Result<Boolean> {
        return try {
            val response = authApi.deleteAccount()
            if (!response.isSuccessful) {
                return Result.failure(Exception("Account deletion failed (HTTP ${response.code()}). Retry when the server is available."))
            }
            runCatching { firebaseAuth?.currentUser?.delete() }
                .onFailure { Log.w(tag, "Backend account was deleted, but Firebase account deletion needs attention.", it) }
            logout()
            Result.success(true)
        } catch (error: Exception) {
            Log.e(tag, "Account deletion could not be confirmed by the backend.", error)
            Result.failure(error)
        }
    }
}
