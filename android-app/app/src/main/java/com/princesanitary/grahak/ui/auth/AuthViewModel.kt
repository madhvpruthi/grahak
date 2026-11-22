package com.princesanitary.grahak.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Source
import com.princesanitary.grahak.ui.UserRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Date

data class AuthUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    /** Set once sign-in/sign-up fully succeeded, including the profile write. */
    val signedInAs: UserRole? = null,
    val requiresOwnerVerification: Boolean = false,
    val pendingOwnerEmail: String? = null
)

class AuthViewModel(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val userRepository: com.princesanitary.grahak.data.repository.UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    // ---------------------------------------------------------------- sign up

    fun signUpWithEmail(
        name: String,
        email: String,
        phone: String,
        password: String,
        role: UserRole
    ) {
        val trimmedName = name.trim()
        val trimmedEmail = email.trim()
        val digits = phone.filter(Char::isDigit)

        when {
            trimmedName.isEmpty() -> return fail("Please enter your name")
            trimmedEmail.isEmpty() -> return fail("Please enter your email")
            !trimmedEmail.contains("@") || !trimmedEmail.contains(".") ->
                return fail("Please enter a valid email address")
            // Required: a plumber who can't phone the customer can't find the site.
            digits.length != 10 -> return fail("Enter a 10-digit mobile number")
            password.length < 6 -> return fail("Password must be at least 6 characters")
        }

        viewModelScope.launch {
            _uiState.value = AuthUiState(isLoading = true)
            try {
                val result = auth.createUserWithEmailAndPassword(trimmedEmail, password).await()
                val uid = result.user?.uid ?: error("Sign up did not return a user")

                writeProfile(
                    uid = uid,
                    name = trimmedName,
                    email = trimmedEmail,
                    role = role,
                    phone = digits
                )
                try {
                    result.user?.sendEmailVerification()
                } catch (_: Exception) {}
                try {
                    com.princesanitary.grahak.data.service.NotificationHelper.showNotification(
                        context = com.princesanitary.grahak.di.ServiceLocator.application,
                        title = "Welcome to Prince Sanitary! ✨",
                        body = "Welcome $trimmedName! Thank you for joining Prince Sanitary Store. We're delighted to serve your home."
                    )
                } catch (_: Exception) {}
                userRepository.syncDeviceToken()
                com.princesanitary.grahak.GrahakApplication.cacheUserRole(
                    com.princesanitary.grahak.di.ServiceLocator.application,
                    uid,
                    role.name
                )
                _uiState.value = AuthUiState(signedInAs = role)
            } catch (collision: FirebaseAuthUserCollisionException) {
                // The Firebase account exists but may have no profile document -
                // that happens when account creation succeeded and the profile
                // write then failed (e.g. the Firestore rules rejected it).
                // Recover in place instead of dead-ending the user.
                recoverExistingAccount(trimmedName, trimmedEmail, password, role, collision)
            } catch (e: Exception) {
                _uiState.value = AuthUiState(error = signUpMessage(e))
            }
        }
    }

    private suspend fun recoverExistingAccount(
        name: String,
        email: String,
        password: String,
        role: UserRole,
        collision: Exception
    ) {
        try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val uid = result.user?.uid ?: error("Sign in did not return a user")

            val stored = readRole(uid)
            if (stored == null) {
                // Half-created account: finish the job.
                writeProfile(uid = uid, name = name, email = email, role = role)
                com.princesanitary.grahak.GrahakApplication.cacheUserRole(
                    com.princesanitary.grahak.di.ServiceLocator.application,
                    uid,
                    role.name
                )
                _uiState.value = AuthUiState(signedInAs = role)
            } else if (stored == role) {
                com.princesanitary.grahak.GrahakApplication.cacheUserRole(
                    com.princesanitary.grahak.di.ServiceLocator.application,
                    uid,
                    stored.name
                )
                _uiState.value = AuthUiState(signedInAs = stored)
            } else {
                auth.signOut()
                _uiState.value = AuthUiState(
                    error = "This account is registered as a ${stored.label.lowercase()}. " +
                        "Switch the tab above to continue."
                )
            }
        } catch (e: Exception) {
            // Wrong password, or the account uses Google sign-in only.
            _uiState.value = AuthUiState(error = signUpMessage(collision))
        }
    }

    // ---------------------------------------------------------------- sign in

    fun signInWithEmail(email: String, password: String, role: UserRole) {
        val trimmedEmail = email.trim()
        when {
            trimmedEmail.isEmpty() -> return fail("Please enter your email")
            password.isEmpty() -> return fail("Please enter your password")
        }

        viewModelScope.launch {
            _uiState.value = AuthUiState(isLoading = true)
            try {
                val result = auth.signInWithEmailAndPassword(trimmedEmail, password).await()
                val uid = result.user?.uid ?: error("Sign in did not return a user")

                // Check if this is a designated Store Owner account
                val isOwner = com.princesanitary.grahak.data.repository.admin.AdminRepository.isOwnerEmail(trimmedEmail)
                if (isOwner) {
                    // Owner email detected: require 2FA Master Verification Step before granting owner dashboard access!
                    _uiState.value = AuthUiState(
                        isLoading = false,
                        requiresOwnerVerification = true,
                        pendingOwnerEmail = trimmedEmail
                    )
                    return@launch
                }

                // Standard customer or plumber account
                val stored = readRole(uid)
                val resolved = stored ?: role.also {
                    writeProfile(uid, result.user?.displayName ?: "", trimmedEmail, it)
                }

                if (stored != null && stored != role && stored != UserRole.OWNER) {
                    _uiState.value = AuthUiState(
                        error = "This account is registered as a " +
                            "${stored.label.lowercase()}. Switch the tab above to continue."
                    )
                    auth.signOut()
                    return@launch
                }

                // Tokens rotate, and a reinstall issues a new one - refresh on
                // every sign-in so this device stays reachable.
                userRepository.syncDeviceToken()
                com.princesanitary.grahak.GrahakApplication.cacheUserRole(
                    com.princesanitary.grahak.di.ServiceLocator.application,
                    uid,
                    resolved.name
                )
                _uiState.value = AuthUiState(signedInAs = resolved)
            } catch (e: Exception) {
                _uiState.value = AuthUiState(error = signInMessage(e))
            }
        }
    }

    fun verifyOwnerVerificationCode(code: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val valid = com.princesanitary.grahak.di.ServiceLocator.adminRepository.verifyOwnerSecurityPin(code)
            if (valid) {
                val uid = auth.currentUser?.uid ?: ""
                writeProfile(uid, auth.currentUser?.displayName ?: "Store Owner", auth.currentUser?.email ?: "", UserRole.OWNER)
                userRepository.syncDeviceToken()
                com.princesanitary.grahak.GrahakApplication.cacheUserRole(
                    com.princesanitary.grahak.di.ServiceLocator.application,
                    uid,
                    UserRole.OWNER.name
                )
                _uiState.value = AuthUiState(signedInAs = UserRole.OWNER)
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Incorrect Owner Verification Code. Access denied."
                )
            }
        }
    }

    fun cancelOwnerVerification() {
        auth.signOut()
        _uiState.value = AuthUiState()
    }

    // ----------------------------------------------------------------- google

    /**
     * Completes Google sign-in.
     *
     * The previous implementation caught [ApiException] and did nothing, so a
     * missing SHA-1 fingerprint looked like a dead button. Every failure now
     * produces a message the user can act on.
     */
    fun signInWithGoogle(
        idToken: String?,
        role: UserRole,
        displayName: String?,
        email: String?,
        phone: String = ""
    ) {
        if (idToken.isNullOrBlank()) {
            return fail("Google sign-in did not return a token. Check that this app's SHA-1 fingerprint is registered in Firebase.")
        }

        viewModelScope.launch {
            _uiState.value = AuthUiState(isLoading = true)
            try {
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                val result = auth.signInWithCredential(credential).await()
                val user = result.user ?: error("Google sign-in did not return a user")

                val stored = readRole(user.uid)
                if (stored != null && stored != role) {
                    auth.signOut()
                    _uiState.value = AuthUiState(
                        error = "This Google account is registered as a " +
                            "${stored.label.lowercase()}. Switch the tab above to continue."
                    )
                    return@launch
                }

                if (stored == null) {
                    // Google never returns a phone number, so we keep whatever the
                    // user typed on the form alongside the Google button.
                    writeProfile(
                        uid = user.uid,
                        name = displayName ?: user.displayName ?: "",
                        email = email ?: user.email ?: "",
                        role = role,
                        phone = phone.filter(Char::isDigit)
                    )
                }

                userRepository.syncDeviceToken()
                val targetRole = stored ?: role
                com.princesanitary.grahak.GrahakApplication.cacheUserRole(
                    com.princesanitary.grahak.di.ServiceLocator.application,
                    user.uid,
                    targetRole.name
                )
                _uiState.value = AuthUiState(signedInAs = targetRole)
            } catch (e: Exception) {
                _uiState.value = AuthUiState(error = googleMessage(e))
            }
        }
    }

    /** Called when the Google sheet itself fails, before we get a token. */
    fun onGoogleSignInFailed(e: Exception) {
        _uiState.value = AuthUiState(error = googleMessage(e))
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    // ---------------------------------------------------------------- helpers

    private fun fail(message: String) {
        _uiState.value = AuthUiState(error = message)
    }

    private suspend fun readRole(uid: String): UserRole? {
        val doc = firestore.collection("users").document(uid)
        
        // Try cache first for instant login
        try {
            val cached = doc.get(Source.CACHE).await()
            if (cached.exists()) {
                UserRole.fromStored(cached.getString("userRole"))?.let { return it }
            }
        } catch (_: Exception) {
            // No cache - fall through to server
        }
        
        // Fallback to server
        val snapshot = doc.get(Source.SERVER).await()
        if (!snapshot.exists()) return null
        return UserRole.fromStored(snapshot.getString("userRole"))
    }

    private suspend fun writeProfile(
        uid: String,
        name: String,
        email: String,
        role: UserRole,
        phone: String = ""
    ) {
        val profile = buildMap<String, Any> {
            put("name", name)
            put("email", email)
            put("userRole", role.name)
            put("updatedAt", Date())
            // Never blank out an existing number on re-login.
            if (phone.isNotBlank()) put("phone", phone)
        }
        // merge so we never clobber addresses / reward points on re-login
        firestore.collection("users").document(uid)
            .set(profile + mapOf("createdAt" to Date()), SetOptions.merge())
            .await()
    }

    private fun signUpMessage(e: Exception): String = when (e) {
        is FirebaseAuthUserCollisionException ->
            "An account already exists with this email. Try logging in instead."
        is FirebaseAuthWeakPasswordException ->
            "That password is too weak. Use at least 6 characters."
        is FirebaseAuthInvalidCredentialsException ->
            "That email address doesn't look valid."
        else -> e.message ?: "Could not create your account. Please try again."
    }

    private fun signInMessage(e: Exception): String = when (e) {
        is FirebaseAuthInvalidUserException ->
            "No account found for this email. Create one first."
        is FirebaseAuthInvalidCredentialsException ->
            "Incorrect email or password."
        else -> e.message ?: "Could not sign you in. Please try again."
    }

    private fun googleMessage(e: Exception): String = when {
        e is ApiException && e.statusCode == CommonStatusCodes.DEVELOPER_ERROR ->
            "Google sign-in isn't configured for this build. Add this machine's debug " +
                "SHA-1 fingerprint to the Firebase project, then re-download google-services.json."
        e is ApiException && e.statusCode == CommonStatusCodes.NETWORK_ERROR ->
            "No internet connection. Check your network and try again."
        e is ApiException && e.statusCode == CommonStatusCodes.CANCELED ->
            "Google sign-in was cancelled."
        e is ApiException -> "Google sign-in failed (code ${e.statusCode})."
        else -> e.message ?: "Google sign-in failed. Please try again."
    }
}
