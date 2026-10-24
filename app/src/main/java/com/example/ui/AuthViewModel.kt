package com.example.ui

import android.app.Application
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.GetCredentialProviderConfigurationException
import androidx.credentials.exceptions.GetCredentialUnsupportedException
import androidx.credentials.exceptions.NoCredentialException
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class AuthUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSuccess: Boolean = false,
    val userDisplayName: String? = null,
    val userEmail: String? = null
)

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val _authState = MutableStateFlow(AuthUiState())
    val authState: StateFlow<AuthUiState> = _authState.asStateFlow()

    /**
     * Safely retrieves the FirebaseAuth instance without throwing IllegalStateException
     * if FirebaseApp has not been initialized.
     */
    fun getSafeFirebaseAuth(): FirebaseAuth? {
        val context = getApplication<Application>()
        return try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context)
            }
            if (FirebaseApp.getApps(context).isNotEmpty()) {
                Firebase.auth
            } else {
                null
            }
        } catch (e: Throwable) {
            Log.w("LECTRA_AUTH", "Firebase is not initialized or configured: ${e.message}")
            null
        }
    }

    /**
     * Safely retrieves the current authenticated Firebase user without throwing exceptions.
     */
    fun getSafeCurrentUser(): FirebaseUser? {
        return try {
            getSafeFirebaseAuth()?.currentUser
        } catch (e: Throwable) {
            Log.w("LECTRA_AUTH", "Failed to retrieve currentUser: ${e.message}")
            null
        }
    }

    /**
     * Resolves the OAuth web client ID dynamically from generated resources.
     */
    fun getResolvedClientId(): String? {
        val context = getApplication<Application>()
        val resId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
        if (resId == 0) return null
        return try {
            val id = context.getString(resId)
            if (id.isNotBlank() && id != "MY_WEB_CLIENT_ID") id else null
        } catch (e: Throwable) {
            null
        }
    }

    fun clearError() {
        _authState.value = _authState.value.copy(errorMessage = null)
    }

    /**
     * Executes Google Sign-In using Credential Manager with comprehensive exception handling.
     * All exceptions and cancellations are caught and reported via StateFlow without terminating the app.
     */
    fun signInWithGoogle(
        activity: ComponentActivity?,
        onSuccess: (displayName: String) -> Unit
    ) {
        if (activity == null || activity.isFinishing || activity.isDestroyed) {
            Log.w("LECTRA_AUTH", "Cannot initiate sign-in: Activity is null, finishing, or destroyed.")
            _authState.value = _authState.value.copy(
                isLoading = false,
                errorMessage = "Activity window unavailable for authentication dialog."
            )
            return
        }

        val clientId = getResolvedClientId()
        if (clientId == null) {
            Log.w("LECTRA_AUTH", "default_web_client_id is missing from resources.")
            _authState.value = _authState.value.copy(
                isLoading = false,
                errorMessage = "Google Sign-In is unavailable: 'default_web_client_id' was not found in google-services configuration. You can tap 'Skip Sign-In (Local Mode)' below to continue."
            )
            return
        }

        _authState.value = _authState.value.copy(isLoading = true, errorMessage = null)

        viewModelScope.launch {
            try {
                val credentialManager = try {
                    CredentialManager.create(activity)
                } catch (e: Throwable) {
                    Log.e("LECTRA_AUTH", "CredentialManager creation failed: ${e.javaClass.simpleName}: ${e.message}")
                    _authState.value = _authState.value.copy(
                        isLoading = false,
                        errorMessage = "Credential Manager could not be initialized on this device: ${e.localizedMessage ?: "Unknown error"}"
                    )
                    return@launch
                }

                val signInOption = try {
                    GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
                } catch (e: Throwable) {
                    Log.e("LECTRA_AUTH", "Failed to construct GetSignInWithGoogleOption: ${e.javaClass.simpleName}: ${e.message}")
                    _authState.value = _authState.value.copy(
                        isLoading = false,
                        errorMessage = "Google Sign-In configuration error: Invalid server client ID format."
                    )
                    return@launch
                }

                val request = try {
                    GetCredentialRequest.Builder()
                        .addCredentialOption(signInOption)
                        .build()
                } catch (e: Throwable) {
                    Log.e("LECTRA_AUTH", "Failed to build GetCredentialRequest: ${e.javaClass.simpleName}: ${e.message}")
                    _authState.value = _authState.value.copy(
                        isLoading = false,
                        errorMessage = "Failed to construct credential request: ${e.localizedMessage}"
                    )
                    return@launch
                }

                val result = try {
                    credentialManager.getCredential(activity, request)
                } catch (e: GetCredentialCancellationException) {
                    Log.i("LECTRA_AUTH", "Google Sign-In cancelled or dismissed by user.")
                    _authState.value = _authState.value.copy(isLoading = false)
                    return@launch
                } catch (e: NoCredentialException) {
                    Log.w("LECTRA_AUTH", "No credentials available on device: ${e.message}")
                    _authState.value = _authState.value.copy(
                        isLoading = false,
                        errorMessage = "No Google account found on this device. Please add a Google account in device Settings and try again."
                    )
                    return@launch
                } catch (e: GetCredentialProviderConfigurationException) {
                    Log.e("LECTRA_AUTH", "Credential provider configuration error: ${e.message}")
                    _authState.value = _authState.value.copy(
                        isLoading = false,
                        errorMessage = "Google Sign-In configuration error: Provider configuration failed. Please verify your SHA-1 and OAuth client setup in Firebase Console."
                    )
                    return@launch
                } catch (e: GetCredentialUnsupportedException) {
                    Log.e("LECTRA_AUTH", "Credential Manager unsupported on this device: ${e.message}")
                    _authState.value = _authState.value.copy(
                        isLoading = false,
                        errorMessage = "Google Sign-In is not supported on this device, or Google Play Services is missing/disabled."
                    )
                    return@launch
                } catch (e: GetCredentialException) {
                    Log.e("LECTRA_AUTH", "GetCredentialException: ${e.javaClass.simpleName}: ${e.message}")
                    _authState.value = _authState.value.copy(
                        isLoading = false,
                        errorMessage = "Sign-in error: ${e.localizedMessage ?: e.message ?: "Authentication service error"}"
                    )
                    return@launch
                } catch (e: Throwable) {
                    Log.e("LECTRA_AUTH", "Unexpected error during getCredential: ${e.javaClass.simpleName}: ${e.message}")
                    _authState.value = _authState.value.copy(
                        isLoading = false,
                        errorMessage = "Unexpected sign-in error: ${e.localizedMessage ?: "Please try again"}"
                    )
                    return@launch
                }

                // Process credential data safely
                val credential = result.credential
                if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdToken = try {
                        GoogleIdTokenCredential.createFrom(credential.data).idToken
                    } catch (e: Throwable) {
                        Log.e("LECTRA_AUTH", "Failed to extract Google ID token: ${e.javaClass.simpleName}: ${e.message}")
                        _authState.value = _authState.value.copy(
                            isLoading = false,
                            errorMessage = "Failed to parse Google credentials. Please try again."
                        )
                        return@launch
                    }

                    if (googleIdToken.isBlank()) {
                        Log.e("LECTRA_AUTH", "Received blank Google ID token")
                        _authState.value = _authState.value.copy(
                            isLoading = false,
                            errorMessage = "Google authentication returned an empty identity token."
                        )
                        return@launch
                    }

                    val auth = getSafeFirebaseAuth()
                    if (auth == null) {
                        Log.e("LECTRA_AUTH", "Firebase Authentication instance is null or uninitialized.")
                        _authState.value = _authState.value.copy(
                            isLoading = false,
                            errorMessage = "Firebase is not initialized. Please ensure google-services.json is properly configured."
                        )
                        return@launch
                    }

                    try {
                        val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                        val authResult = auth.signInWithCredential(authCredential).await()
                        val firebaseUser = authResult.user ?: auth.currentUser
                        val userDisplayName = firebaseUser?.displayName.orEmpty()
                        val userEmail = firebaseUser?.email.orEmpty()

                        Log.i("LECTRA_AUTH", "Firebase Google Sign-In succeeded for uid=${firebaseUser?.uid}")

                        _authState.value = _authState.value.copy(
                            isLoading = false,
                            isSuccess = true,
                            userDisplayName = userDisplayName,
                            userEmail = userEmail
                        )
                        onSuccess(userDisplayName)
                    } catch (e: FirebaseAuthInvalidUserException) {
                        Log.e("LECTRA_AUTH", "FirebaseAuthInvalidUserException: ${e.errorCode}")
                        _authState.value = _authState.value.copy(
                            isLoading = false,
                            errorMessage = "The user account has been disabled or not found."
                        )
                    } catch (e: FirebaseAuthInvalidCredentialsException) {
                        Log.e("LECTRA_AUTH", "FirebaseAuthInvalidCredentialsException: ${e.errorCode}")
                        _authState.value = _authState.value.copy(
                            isLoading = false,
                            errorMessage = "Sign-in failed: Invalid credentials or SHA-1 fingerprint mismatch in Firebase Console."
                        )
                    } catch (e: FirebaseAuthException) {
                        Log.e("LECTRA_AUTH", "FirebaseAuthException: ${e.errorCode}: ${e.message}")
                        _authState.value = _authState.value.copy(
                            isLoading = false,
                            errorMessage = "Firebase Authentication error: ${e.localizedMessage ?: e.errorCode}"
                        )
                    } catch (e: Throwable) {
                        Log.e("LECTRA_AUTH", "Firebase signInWithCredential failed: ${e.javaClass.simpleName}: ${e.message}")
                        _authState.value = _authState.value.copy(
                            isLoading = false,
                            errorMessage = "Failed to complete sign-in: ${e.localizedMessage ?: "Network or service error"}"
                        )
                    }
                } else {
                    Log.w("LECTRA_AUTH", "Unexpected credential type: ${credential.type}")
                    _authState.value = _authState.value.copy(
                        isLoading = false,
                        errorMessage = "Received unexpected credential type from provider."
                    )
                }
            } catch (e: Throwable) {
                // Outermost safety net to prevent any unexpected throwable from crashing the app
                Log.e("LECTRA_AUTH", "Unhandled fatal exception prevented in Google Sign-In flow", e)
                _authState.value = _authState.value.copy(
                    isLoading = false,
                    errorMessage = "An unexpected error occurred during sign-in: ${e.localizedMessage ?: "Unknown error"}"
                )
            }
        }
    }
}
