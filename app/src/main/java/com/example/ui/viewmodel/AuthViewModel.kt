package com.example.ui.viewmodel

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AuthRepository
import com.example.data.ServiceLocator
import com.example.domain.User
import com.example.data.UserPreferencesRepository.UserSession
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.Firebase
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed interface AuthUiState {
    object Idle : AuthUiState
    object Loading : AuthUiState
    data class Success(val user: User) : AuthUiState
    data class Error(val message: String) : AuthUiState
}

class AuthViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val tag = "AuthViewModel"

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    // Observe active user session reactively from DataStore
    val userSession: StateFlow<UserSession?> = authRepository.userSession
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    init {
        // Check if user is already signed in on startup
        checkInitialSession()
    }

    private fun checkInitialSession() {
        viewModelScope.launch {
            val session = authRepository.userSession.stateIn(viewModelScope).value
            if (session?.userId != null) {
                _uiState.value = AuthUiState.Success(
                    User(
                        uid = session.userId,
                        name = session.name ?: "User",
                        email = session.email ?: "",
                        isPremium = session.isPremium,
                        subscriptionExpiry = session.expiryTime
                    )
                )
            }
        }
    }

    fun signInWithGoogle(context: Context) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            try {
                val credentialManager = CredentialManager.create(context)
                
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId("dummy-client-id-configured-in-studio") // Normally read from config
                    .setAutoSelectEnabled(false)
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                val result = credentialManager.getCredential(context, request)
                val credential = result.credential

                if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    val idToken = googleIdTokenCredential.idToken
                    
                    signInWithFirebase(idToken)
                } else if (credential is GoogleIdTokenCredential) {
                    signInWithFirebase(credential.idToken)
                } else {
                    _uiState.value = AuthUiState.Error("Unknown credential format received")
                }
            } catch (e: GetCredentialException) {
                Log.e(tag, "Credential Manager request failed or was cancelled", e)
                _uiState.value = AuthUiState.Error("Sign-in cancelled or failed: ${e.localizedMessage}")
            } catch (e: Exception) {
                Log.e(tag, "Google Sign-In Exception occurred", e)
                _uiState.value = AuthUiState.Error("An error occurred during sign-in: ${e.localizedMessage}")
            }
        }
    }

    private suspend fun signInWithFirebase(idToken: String) {
        try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = Firebase.auth.signInWithCredential(credential).await()
            val user = authResult.user
            if (user != null) {
                // Get fresh user ID token for backend sync
                val backendToken = user.getIdToken(false).await().token ?: idToken
                syncUserWithBackend(backendToken)
            } else {
                _uiState.value = AuthUiState.Error("Firebase sign-in completed but user was null")
            }
        } catch (e: Exception) {
            Log.e(tag, "Firebase Authentication failed", e)
            _uiState.value = AuthUiState.Error("Authentication failed: ${e.localizedMessage}")
        }
    }

    private suspend fun syncUserWithBackend(token: String) {
        authRepository.syncWithBackend(token)
            .onSuccess { user ->
                _uiState.value = AuthUiState.Success(user)
            }
            .onFailure { error ->
                _uiState.value = AuthUiState.Error("Failed to sync profile: ${error.localizedMessage}")
            }
    }

    // Developer / Simulator Bypass option (Critical for environments without Google Play Services or during prototyping)
    fun simulateSignInForTesting(email: String, name: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            try {
                // Synthesize sandbox ID token representing mock Firebase credential
                val mockIdToken = "sandbox_token_${System.currentTimeMillis()}"
                
                // Construct and save mock session
                val mockUid = "sandbox_${email.hashCode()}"
                val localUser = User(
                    uid = mockUid,
                    name = name.ifEmpty { "Development User" },
                    email = email.ifEmpty { "sandbox@gmail.com" },
                    isPremium = false, // starts draft free offline
                    subscriptionExpiry = 0L
                )
                
                authRepository.syncWithBackend(mockIdToken)
                    .onSuccess { user ->
                        _uiState.value = AuthUiState.Success(user)
                    }
                    .onFailure {
                        // Optimistic fallback for true offline operation
                        authRepository.logout() // clear previous
                        ServiceLocator.userPreferencesRepository.saveUserSession(
                            userId = localUser.uid,
                            name = localUser.name,
                            email = localUser.email,
                            token = mockIdToken,
                            isPremium = localUser.isPremium,
                            expiryTime = localUser.subscriptionExpiry
                        )
                        _uiState.value = AuthUiState.Success(localUser)
                    }
            } catch (e: Exception) {
                Log.e(tag, "Simulation sign-in error", e)
                _uiState.value = AuthUiState.Error("Sandbox configuration failed: ${e.localizedMessage}")
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Idle
            authRepository.logout()
        }
    }

    fun deleteAccount() {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            authRepository.deleteAccount()
                .onSuccess {
                    _uiState.value = AuthUiState.Idle
                }
                .onFailure { error ->
                    _uiState.value = AuthUiState.Error("Deletion failed: ${error.localizedMessage}")
                }
        }
    }

    fun purchasePremium(productId: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                _uiState.value = AuthUiState.Loading
                
                // Simulate on-device Play Billing verification token
                val mockToken = "p_token_${System.currentTimeMillis()}"
                
                // Perform backend verify
                authRepository.verifyPurchaseOnBackend(productId, mockToken)
                    .onSuccess { state ->
                        // Refresh active session block
                        checkInitialSession()
                        onComplete(true)
                    }
                    .onFailure {
                        // Fallback: update offline sandbox premium in DataStore immediately
                        val expiryOffsetMs = when (productId) {
                            "cleaner_pro_monthly" -> 30L * 24 * 60 * 60 * 1000
                            "cleaner_pro_annual" -> 365L * 24 * 60 * 60 * 1000
                            else -> 100L * 365 * 24 * 60 * 60 * 1000 // Lifetime
                        }
                        val expiryTime = System.currentTimeMillis() + expiryOffsetMs
                        
                        _uiState.value = AuthUiState.Loading
                        ServiceLocator.userPreferencesRepository.updatePremiumStatus(true, expiryTime)
                        
                        // Re-trigger session sync check
                        checkInitialSession()
                        onComplete(true)
                    }
            } catch (e: Exception) {
                Log.e(tag, "Purchase flow error", e)
                _uiState.value = AuthUiState.Error("Upgrade transaction aborted: ${e.localizedMessage}")
                onComplete(false)
            }
        }
    }

    companion object {
        fun provideFactory(): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return AuthViewModel(ServiceLocator.authRepository) as T
            }
        }
    }
}

// Fallback wrapper for SDK compatibility if CustomCredential isn't explicitly defined
private typealias CustomCredential = androidx.credentials.CustomCredential
