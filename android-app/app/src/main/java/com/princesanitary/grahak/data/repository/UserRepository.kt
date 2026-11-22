package com.princesanitary.grahak.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Source
import com.google.firebase.messaging.FirebaseMessaging
import com.princesanitary.grahak.data.model.User
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reads and writes the signed-in user's own profile document.
 *
 * There was no repository for `users` at all - the auth view model wrote the
 * document inline - so there was nowhere to put profile concerns like the phone
 * number or the push token.
 */
@Singleton
class UserRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {

    private val users = firestore.collection("users")

    /** Cache-first: this is called on startup paths where latency is visible. */
    suspend fun getProfile(): Result<User?> {
        return try {
            val uid = auth.currentUser?.uid ?: return Result.success(null)
            val ref = users.document(uid)

            val snapshot = runCatching { ref.get(Source.CACHE).await() }
                .getOrNull()
                ?.takeIf { it.exists() }
                ?: ref.get(Source.SERVER).await()

            Result.success(snapshot.toObject(User::class.java))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Contact details a plumber needs, copied onto each job they accept. */
    suspend fun getContactDetails(): Pair<String, String> {
        val profile = getProfile().getOrNull()
        val name = profile?.name?.takeIf { it.isNotBlank() }
            ?: auth.currentUser?.displayName.orEmpty()
        return name to profile?.phone.orEmpty()
    }

    suspend fun updatePhone(phone: String): Result<Unit> = merge("phone" to phone)

    suspend fun updateName(name: String): Result<Unit> = merge("name" to name)

    /**
     * Stores the FCM registration token so this device can be pushed to.
     *
     * Kept per-user rather than in a separate collection: one device per account
     * is the realistic case here, and it keeps the security rules trivial (a user
     * may only write their own document).
     */
    suspend fun saveDeviceToken(token: String): Result<Unit> = merge(
        "fcmToken" to token,
        "fcmTokenUpdatedAt" to Date()
    )

    /** Fetches the current token and stores it. Safe to call on every launch. */
    suspend fun syncDeviceToken(): Result<Unit> {
        return try {
            if (auth.currentUser == null) return Result.success(Unit)
            val token = FirebaseMessaging.getInstance().token.await()
            saveDeviceToken(token)
        } catch (e: Exception) {
            // Push is a nice-to-have; never let it break a sign-in.
            Result.failure(e)
        }
    }

    private suspend fun merge(vararg pairs: Pair<String, Any>): Result<Unit> {
        return try {
            val uid = auth.currentUser?.uid ?: throw Exception("You are not signed in")
            users.document(uid).set(pairs.toMap(), SetOptions.merge()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
