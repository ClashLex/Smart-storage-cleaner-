package com.example.ui.viewmodel

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.AuthRepository
import com.example.data.ServiceLocator
import com.example.domain.User
import com.example.data.UserPreferencesRepository.UserSession
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import com.google.android.gms.tasks.Task
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

sealed interface AuthUiState {
    data object Idle : AuthUiState
    data object Loading : AuthUiState
    data class Success(val user: User) : AuthUiState
    data class Error(val message: String) : AuthUiState
}

class AuthViewModel(private val authRepository: AuthRepository) : ViewModel() {
    private val tag = "AuthViewModel"
    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    val userSession: StateFlow<UserSession?> = authRepository.userSession.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    init { checkInitialSession() }

    private fun checkInitialSession() {
        viewModelScope.launch {
            val session = authRepository.userSession.firstOrNull()
            if (!session?.userId.isNullOrBlank()) {
                _uiState.value = AuthUiState.Success(
                    User(
                        uid = session!!.userId!!,
                        name = session.name ?: "User",
                        email = session.email.orEmpty(),
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
                val clientId = BuildConfig.GOOGLE_WEB_CLIENT_ID
                if (clientId.isBlank()) {
                    throw IllegalStateException("Configure GOOGLE_WEB_CLIENT_ID in Gradle properties before Google sign-in.")
                }
                val credentialManager = CredentialManager.create(context)
                val option = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(clientId)
                    .setAutoSelectEnabled(false)
                    .build()
                val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
                val credential = credentialManager.getCredential(context, request).credential
                val idToken = when {
                    credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL ->
                        GoogleIdTokenCredential.createFrom(credential.data).idToken
                    credential is GoogleIdTokenCredential -> credential.idToken
                    else -> throw IllegalStateException("Google sign-in returned an unsupported credential.")
                }
                val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
                val user = FirebaseAuth.getInstance().signInWithCredential(firebaseCredential).awaitResult().user
                    ?: throw IllegalStateException("Firebase sign-in did not return a user.")
                val freshToken = user.getIdToken(true).await().token
                    ?: throw IllegalStateException("Unable to obtain a Firebase ID token.")
                authRepository.syncWithBackend(freshToken)
                    .onSuccess { _uiState.value = AuthUiState.Success(it) }
                    .onFailure { _uiState.value = AuthUiState.Error("Backend sign-in failed: ${it.localizedMessage}") }
            } catch (error: GetCredentialException) {
                _uiState.value = AuthUiState.Error("Google sign-in was cancelled or failed: ${error.localizedMessage}")
            } catch (error: Exception) {
                Log.e(tag, "Google sign-in failed.", error)
                _uiState.value = AuthUiState.Error(error.localizedMessage ?: "Google sign-in failed.")
            }
        }
    }

    fun simulateSignInForTesting(email: String, name: String) {
        _uiState.value = AuthUiState.Error("Sandbox authentication is disabled. Configure Firebase Authentication to sign in.")
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            _uiState.value = AuthUiState.Idle
        }
    }

    fun deleteAccount() {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            authRepository.deleteAccount()
                .onSuccess { _uiState.value = AuthUiState.Idle }
                .onFailure { _uiState.value = AuthUiState.Error("Deletion could not be confirmed: ${it.localizedMessage}") }
        }
    }

    fun purchasePremium(productId: String, onComplete: (Boolean) -> Unit) {
        _uiState.value = AuthUiState.Error("Google Play Billing checkout is not connected yet. No charge was made.")
        onComplete(false)
    }

    fun purchasePremium(productId: String, purchaseToken: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            authRepository.verifyPurchaseOnBackend(productId, purchaseToken)
                .onSuccess {
                    checkInitialSession()
                    onComplete(it.isPremium)
                }
                .onFailure { error ->
                    _uiState.value = AuthUiState.Error("Purchase verification failed: ${error.localizedMessage}")
                    onComplete(false)
                }
        }
    }

    companion object {
        fun provideFactory(): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                AuthViewModel(ServiceLocator.authRepository) as T
        }
    }
}


private suspend fun <T> Task<T>.awaitResult(): T = suspendCancellableCoroutine { continuation ->
    addOnCompleteListener { task ->
        if (task.isSuccessful) {
            @Suppress("UNCHECKED_CAST")
            continuation.resume(task.result as T)
        } else {
            continuation.resumeWithException(task.exception ?: IllegalStateException("Google task failed."))
        }
    }
}
