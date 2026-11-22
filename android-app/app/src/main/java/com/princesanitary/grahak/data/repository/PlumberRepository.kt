package com.princesanitary.grahak.data.repository

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.princesanitary.grahak.data.model.KYCInfo
import com.princesanitary.grahak.data.model.OperationalStatus
import com.princesanitary.grahak.data.model.Plumber
import com.princesanitary.grahak.data.model.PlumberRates
import com.princesanitary.grahak.data.model.PlumberStats
import com.princesanitary.grahak.data.model.PlumberWallet
import com.princesanitary.grahak.data.model.ServiceSpeciality
import com.princesanitary.grahak.data.model.VerificationStatus
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlumberRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage
) {
    
    private val plumbersCollection = firestore.collection("plumbers")
    
    /**
     * Create the plumber profile at the end of onboarding.
     *
     * Media uploads are best-effort on purpose: Firebase Storage is not enabled
     * on every environment, and losing a profile photo must never cost the
     * plumber their whole application. A failed upload leaves the URL empty and
     * the UI falls back to an initials avatar.
     */
    suspend fun registerPlumber(
        name: String,
        phone: String,
        photoUri: Uri?,
        aadhaarNo: String,
        kycDocuments: List<Uri>,
        visitCharge: Double,
        customRates: Map<String, Double>,
        specialities: List<String>,
        experienceYears: Int,
        about: String,
        servicePincode: String,
        baseAddress: String
    ): Result<String> {
        return try {
            val userId = auth.currentUser?.uid ?: throw Exception("You are not signed in")

            val photoUrl = photoUri?.let { runCatching { uploadProfilePhoto(userId, it) }.getOrNull() } ?: ""
            val docUrls = kycDocuments.mapNotNull { uri ->
                runCatching { uploadKYCDocument(userId, uri) }.getOrNull()
            }

            val plumber = Plumber(
                id = userId,
                name = name,
                phone = phone,
                photoUrl = photoUrl,
                specialities = specialities,
                experienceYears = experienceYears,
                about = about,
                servicePincode = servicePincode,
                baseAddress = baseAddress,
                kyc = KYCInfo(
                    aadhaarNo = aadhaarNo,
                    docUrls = docUrls,
                    // The store reviews every new plumber before they can work.
                    verified = false,
                    reviewStatus = VerificationStatus.PENDING.name
                ),
                rates = PlumberRates(
                    visitCharge = visitCharge,
                    customRates = customRates
                ),
                operationalStatus = OperationalStatus.OFFLINE,
                stats = PlumberStats(),
                wallet = PlumberWallet()
            )

            plumbersCollection.document(userId).set(plumber).await()
            Result.success(userId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** True when media uploads are possible; used to soften the onboarding copy. */
    suspend fun isStorageAvailable(): Boolean {
        if (com.princesanitary.grahak.data.remote.CloudinaryConfig.CLOUD_NAME != "YOUR_CLOUD_NAME" &&
            com.princesanitary.grahak.data.remote.CloudinaryConfig.UPLOAD_PRESET != "YOUR_UPLOAD_PRESET") {
            return true
        }
        return runCatching {
            storage.reference.child("healthcheck").metadata.await()
            true
        }.getOrDefault(false)
    }
    
    /**
     * Get current plumber profile
     */
    fun getCurrentPlumberProfile(): Flow<Result<Plumber?>> = callbackFlow {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            trySend(Result.success(null))
            close()
            return@callbackFlow
        }
        
        val listener = plumbersCollection.document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }
                
                val plumber = try {
                    snapshot?.toObject(Plumber::class.java)?.copy(id = snapshot.id)
                } catch (e: Exception) {
                    null
                }
                if (plumber != null) {
                    trySend(Result.success(plumber))
                } else {
                    // If the snapshot is from cache and does not exist yet, don't emit null immediately
                    // because the server fetch is still in flight and will arrive shortly.
                    if (snapshot?.metadata?.isFromCache == true && snapshot?.exists() == false) {
                        return@addSnapshotListener
                    }
                    
                    // Fallback: check if existing plumber profile matches userId or phone
                    plumbersCollection.whereEqualTo("userId", userId).limit(1).get()
                        .addOnSuccessListener { querySnap ->
                            val found = querySnap.documents.firstOrNull()?.let { doc ->
                                try {
                                    doc.toObject(Plumber::class.java)?.copy(id = doc.id)
                                } catch (e: Exception) {
                                    null
                                }
                            }
                            if (found != null) {
                                trySend(Result.success(found))
                            } else {
                                val phone = auth.currentUser?.phoneNumber.orEmpty()
                                val digits = phone.filter(Char::isDigit).takeLast(10)
                                if (digits.length == 10) {
                                    plumbersCollection.whereEqualTo("phone", digits).limit(1).get()
                                        .addOnSuccessListener { pSnap ->
                                            val pFound = pSnap.documents.firstOrNull()?.let { doc ->
                                                try {
                                                    doc.toObject(Plumber::class.java)?.copy(id = doc.id)
                                                } catch (e: Exception) {
                                                    null
                                                }
                                            }
                                            trySend(Result.success(pFound))
                                        }
                                        .addOnFailureListener {
                                            trySend(Result.success(null))
                                        }
                                } else {
                                    trySend(Result.success(null))
                                }
                            }
                        }
                        .addOnFailureListener {
                            trySend(Result.success(null))
                        }
                }
            }
        
        awaitClose { listener.remove() }
    }
    
    /**
     * Update plumber profile
     */
    suspend fun updatePlumberProfile(updates: Map<String, Any>): Result<Unit> {
        return try {
            val userId = auth.currentUser?.uid ?: throw Exception("User not authenticated")
            plumbersCollection.document(userId).update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Update operational status
     */
    suspend fun updateOperationalStatus(status: OperationalStatus): Result<Unit> {
        return try {
            val userId = auth.currentUser?.uid ?: throw Exception("User not authenticated")
            plumbersCollection.document(userId)
                .update("operationalStatus", status.name)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Update rates
     */
    suspend fun updateRates(rates: PlumberRates): Result<Unit> {
        return try {
            val userId = auth.currentUser?.uid ?: throw Exception("User not authenticated")
            plumbersCollection.document(userId)
                .update("rates", rates)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Live roster of approved plumbers for the customer's booking screen.
     *
     * Deliberately NOT filtered by [OperationalStatus]: the customer should see
     * the whole approved roster with an online/offline badge, so they can still
     * request someone who is currently offline. Filtering to ONLINE only - which
     * the old `getVerifiedPlumbers()` did - meant the list was almost always
     * empty and the feature looked broken.
     *
     * Speciality filtering happens in memory; the roster is small and this
     * avoids needing an array-contains composite index.
     */
    fun observeApprovedPlumbers(): Flow<Result<List<Plumber>>> = callbackFlow {
        val listener = plumbersCollection
            .whereEqualTo("kyc.verified", true)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }
                val plumbers = snapshot?.documents?.mapNotNull {
                    val p = it.toObject(Plumber::class.java)
                    if (p?.hiddenFromOrders == true) null else p
                } ?: emptyList()

                // Online first, then better rated, then more experienced.
                trySend(
                    Result.success(
                        plumbers.sortedWith(
                            compareByDescending<Plumber> {
                                it.operationalStatus == OperationalStatus.ONLINE
                            }
                                .thenByDescending { it.stats.rating }
                                .thenByDescending { it.stats.jobsCompleted }
                        )
                    )
                )
            }

        awaitClose { listener.remove() }
    }

    /**
     * Get all verified plumbers (for customer view)
     */
    suspend fun getVerifiedPlumbers(): Result<List<Plumber>> {
        return try {
            val snapshot = plumbersCollection
                .whereEqualTo("kyc.verified", true)
                .whereEqualTo("operationalStatus", OperationalStatus.ONLINE.name)
                .get()
                .await()
            
            val plumbers = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Plumber::class.java)
            }
            
            Result.success(plumbers)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * One plumber by id, for the customer's tracking screen.
     *
     * Any signed-in user may read plumber profiles, which is what lets a customer
     * see who accepted their job and call them.
     */
    suspend fun getPlumberById(plumberId: String): Result<Plumber?> {
        return try {
            Result.success(
                plumbersCollection.document(plumberId).get().await()
                    .toObject(Plumber::class.java)
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Check if current user is registered as plumber
     */
    suspend fun isPlumberRegistered(): Result<Boolean> {
        return try {
            val userId = auth.currentUser?.uid ?: return Result.success(false)
            val snapshot = plumbersCollection.document(userId).get().await()
            Result.success(snapshot.exists())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private suspend fun uploadProfilePhoto(userId: String, uri: Uri): String {
        val app = com.princesanitary.grahak.di.ServiceLocator.application
        val cloudRes = com.princesanitary.grahak.data.remote.CloudinaryUploader.uploadUri(
            context = app,
            uri = uri,
            folder = "plumbers/profiles"
        )
        if (cloudRes.isSuccess) {
            return cloudRes.getOrThrow()
        }
        val ref = storage.reference.child("plumbers/$userId/profile/photo.jpg")
        ref.putFile(uri).await()
        return ref.downloadUrl.await().toString()
    }
    
    private suspend fun uploadKYCDocument(userId: String, uri: Uri): String {
        val app = com.princesanitary.grahak.di.ServiceLocator.application
        val cloudRes = com.princesanitary.grahak.data.remote.CloudinaryUploader.uploadUri(
            context = app,
            uri = uri,
            folder = "plumbers/kyc"
        )
        if (cloudRes.isSuccess) {
            return cloudRes.getOrThrow()
        }
        val timestamp = System.currentTimeMillis()
        val ref = storage.reference.child("plumbers/$userId/kyc/doc_$timestamp.jpg")
        ref.putFile(uri).await()
        return ref.downloadUrl.await().toString()
    }
}
