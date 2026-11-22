package com.princesanitary.grahak.ui

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import com.princesanitary.grahak.GrahakApplication
import com.princesanitary.grahak.ui.UserRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Owns the app-wide session.
 *
 * Replaces `GrahakAppViewModel`, which inferred the role from *collection
 * membership* (`plumbers/{uid}` exists? else `users/{uid}` exists?). Any plumber
 * who had a `users` document - which every sign-up creates - was classified as a
 * consumer. The role now comes from the `userRole` field written at sign-up.
 */
class RootViewModel(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val application: Application
) : ViewModel() {

    private val _session = MutableStateFlow<SessionState>(SessionState.Loading)
    val session: StateFlow<SessionState> = _session.asStateFlow()

    private val authListener = FirebaseAuth.AuthStateListener { resolve() }

    init {
        auth.addAuthStateListener(authListener)
    }

    /** Re-read the session, e.g. right after a successful sign-in. */
    fun refresh() = resolve()

    private fun resolve() {
        val user = auth.currentUser
        if (user == null) {
            _session.value = SessionState.SignedOut
            return
        }

        viewModelScope.launch {
            val role = readRole(user.uid)

            _session.value = if (role != null) {
                SessionState.SignedIn(user.uid, role)
            } else {
                SessionState.NeedsRole(user.uid)
            }
        }
    }

    /**
     * Resolves the stored role with triple-layer fallback:
     * 1. SharedPreferences (survives Firestore cache clears)
     * 2. Firestore cache (fast, offline-capable)
     * 3. Firestore server (slowest, requires network)
     */
    private suspend fun readRole(uid: String): UserRole? {
        // Layer 0: Whitelisted owner emails automatically get OWNER role
        val currentUser = auth.currentUser
        val email = currentUser?.email?.lowercase() ?: ""
        if (com.princesanitary.grahak.data.repository.admin.AdminRepository.isOwnerEmail(email)) {
            return UserRole.OWNER
        }

        // Layer 1: SharedPreferences (always survives, instant 0ms local lookup)
        GrahakApplication.getCachedUserRole(application, uid)?.let { roleString ->
            UserRole.fromStored(roleString)?.let { return it }
        }

        // Layer 2: Firestore cache (fast, offline-capable)
        try {
            val adminDoc = firestore.collection("admins").document(uid).get(Source.CACHE).await()
            if (adminDoc.exists()) {
                GrahakApplication.cacheUserRole(application, uid, UserRole.OWNER.name)
                return UserRole.OWNER
            }
        } catch (_: Exception) {}

        val userDoc = firestore.collection("users").document(uid)
        try {
            val cached = userDoc.get(Source.CACHE).await()
            if (cached.exists()) {
                val roleString = cached.getString("userRole")
                UserRole.fromStored(roleString)?.let { role ->
                    roleString?.let { GrahakApplication.cacheUserRole(application, uid, it) }
                    return role
                }
            }
        } catch (_: Exception) {}

        try {
            val plumberDoc = firestore.collection("plumbers").document(uid).get(Source.CACHE).await()
            if (plumberDoc.exists()) {
                GrahakApplication.cacheUserRole(application, uid, UserRole.PLUMBER.name)
                return UserRole.PLUMBER
            }
        } catch (_: Exception) {}

        // Layer 3: Server (last resort)
        try {
            val snapshot = userDoc.get(Source.SERVER).await()
            val roleString = snapshot.getString("userRole")
            UserRole.fromStored(roleString)?.let { role ->
                roleString?.let { GrahakApplication.cacheUserRole(application, uid, it) }
                return role
            }
        } catch (_: Exception) {}

        try {
            val plumberDoc = firestore.collection("plumbers").document(uid).get(Source.SERVER).await()
            if (plumberDoc.exists()) {
                GrahakApplication.cacheUserRole(application, uid, UserRole.PLUMBER.name)
                return UserRole.PLUMBER
            }
        } catch (_: Exception) {}

        try {
            val adminDoc = firestore.collection("admins").document(uid).get(Source.SERVER).await()
            if (adminDoc.exists()) {
                GrahakApplication.cacheUserRole(application, uid, UserRole.OWNER.name)
                return UserRole.OWNER
            }
        } catch (_: Exception) {}

        return null
    }

    fun signOut() {
        auth.currentUser?.uid?.let { uid ->
            GrahakApplication.clearCachedUserRole(application, uid)
        }
        com.princesanitary.grahak.data.local.SettingsStorage.setOwnerPinSessionUnlocked(false, application)
        auth.signOut()
        _session.value = SessionState.SignedOut
    }

    override fun onCleared() {
        auth.removeAuthStateListener(authListener)
        super.onCleared()
    }
}
