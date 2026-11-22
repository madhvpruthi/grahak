package com.princesanitary.grahak.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.princesanitary.grahak.data.model.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ServiceJobRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val userRepository: UserRepository
) {
    
    private val serviceJobsCollection = firestore.collection("service_jobs")
    private val plumbersCollection = firestore.collection("plumbers")
    
    /**
     * Create a new service job request (Consumer)
     */
    suspend fun createServiceJob(
        serviceCategory: String,
        siteAddress: SavedAddress,
        description: String,
        requestedPlumberId: String? = null,
        agreedVisitRate: Double = 0.0
    ): Result<String> {
        return try {
            val userId = auth.currentUser?.uid ?: throw Exception("You are not signed in")

            // Copied onto the job because rules stop a plumber reading the
            // customer's profile - without this they have no way to call.
            val (customerName, customerPhone) = userRepository.getContactDetails()

            val job = ServiceJob(
                customerId = userId,
                customerName = customerName,
                customerPhone = customerPhone,
                serviceCategory = serviceCategory,
                // `description` used to be accepted here and thrown away, so the
                // plumber received a job with no description of the work.
                description = description,
                requestedPlumberId = requestedPlumberId,
                agreedVisitRate = agreedVisitRate,
                siteAddress = siteAddress,
                status = JobStatus.SEARCHING,
                createdAt = Date(),
                updatedAt = Date()
            )

            val docRef = serviceJobsCollection.add(job).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Plumber passes on a job.
     *
     * Accepting is always the plumber's choice, so declining has to be a real
     * action rather than just ignoring the row. Their id is appended to
     * `declinedBy`, which removes the job from their queue. A job that was
     * addressed to them specifically also loses its `requestedPlumberId`, so it
     * returns to the open pool instead of being stuck waiting on one person.
     */
    suspend fun declineJob(jobId: String): Result<Unit> {
        return try {
            val userId = auth.currentUser?.uid ?: throw Exception("You are not signed in")

            val snapshot = serviceJobsCollection.document(jobId).get().await()
            val job = snapshot.toObject(ServiceJob::class.java)
                ?: throw Exception("This job no longer exists")

            val updates = mutableMapOf<String, Any>(
                "declinedBy" to (job.declinedBy + userId).distinct(),
                "updatedAt" to Date()
            )
            if (job.requestedPlumberId == userId) {
                updates["requestedPlumberId"] = FieldValue.delete()
            }

            serviceJobsCollection.document(jobId).update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Open jobs this plumber should actually be offered.
     *
     * Filtering happens in memory because Firestore cannot express
     * "declinedBy does not contain me" in a query. The open queue is capped at
     * 20 documents, so the cost is trivial.
     */
    fun getJobsForPlumber(
        specialities: List<String>
    ): Flow<Result<List<ServiceJob>>> = callbackFlow {
        val me = auth.currentUser?.uid
        if (me == null) {
            trySend(Result.success(emptyList()))
            close()
            return@callbackFlow
        }

        val listener = serviceJobsCollection
            .whereEqualTo("status", JobStatus.SEARCHING.name)
            .limit(30)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }

                val jobs = snapshot?.documents
                    ?.mapNotNull { doc -> doc.toObject(ServiceJob::class.java)?.copy(id = doc.id) }
                    ?.filter { job ->
                        // Already said no.
                        if (me in job.declinedBy) return@filter false
                        // Addressed to someone else: not ours to take yet.
                        val requested = job.requestedPlumberId
                        if (requested != null && requested != me) return@filter false
                        true
                    }
                    ?.sortedWith(
                        compareByDescending<ServiceJob> { job ->
                            when {
                                job.requestedPlumberId == me -> 2
                                job.serviceCategory in specialities -> 1
                                else -> 0
                            }
                        }.thenByDescending { it.createdAt.time }
                    )
                    ?: emptyList()

                trySend(Result.success(jobs))
            }

        awaitClose { listener.remove() }
    }
    
    /**
     * Get available plumbers near location
     */
    suspend fun getAvailablePlumbers(limit: Int = 10): Result<List<Plumber>> {
        return try {
            val snapshot = plumbersCollection
                .whereEqualTo("kyc.verified", true)
                .whereEqualTo("operationalStatus", OperationalStatus.ONLINE.name)
                .limit(limit.toLong())
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
     * Consumer: Get their service jobs
     */
    fun getConsumerJobs(): Flow<Result<List<ServiceJob>>> = callbackFlow {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            trySend(Result.success(emptyList()))
            close()
            return@callbackFlow
        }
        
        val listener = serviceJobsCollection
            .whereEqualTo("customerId", userId)
            .limit(50)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }
                
                val jobs = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        doc.toObject(ServiceJob::class.java)?.copy(id = doc.id)
                    } catch (e: Exception) {
                        null
                    }
                }?.sortedByDescending { it.createdAt.time } ?: emptyList()
                
                trySend(Result.success(jobs))
            }
        
        awaitClose { listener.remove() }
    }
    
    /**
     * Plumber: Get available jobs (searching status)
     */
    fun getAvailableJobs(): Flow<Result<List<ServiceJob>>> = callbackFlow {
        val listener = serviceJobsCollection
            .whereEqualTo("status", JobStatus.SEARCHING.name)
            .limit(30)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }
                
                val jobs = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        doc.toObject(ServiceJob::class.java)?.copy(id = doc.id)
                    } catch (e: Exception) {
                        null
                    }
                }?.sortedByDescending { it.createdAt.time } ?: emptyList()
                
                trySend(Result.success(jobs))
            }
        
        awaitClose { listener.remove() }
    }
    
    /**
     * Every job this plumber has ever been assigned, newest activity first.
     *
     * [getPlumberJobs] deliberately filters to in-flight statuses for the "work
     * in progress" view, which meant a plumber had no way to see anything they
     * had finished. This is the history feed.
     */
    fun getPlumberJobHistory(): Flow<Result<List<ServiceJob>>> = callbackFlow {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            trySend(Result.success(emptyList()))
            close()
            return@callbackFlow
        }

        val listener = serviceJobsCollection
            .whereEqualTo("plumberId", userId)
            .limit(50)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }
                val jobs = snapshot?.documents
                    ?.mapNotNull { doc -> doc.toObject(ServiceJob::class.java)?.copy(id = doc.id) }
                    ?.sortedByDescending { it.updatedAt.time }
                    ?: emptyList()
                trySend(Result.success(jobs))
            }

        awaitClose { listener.remove() }
    }

    /**
     * Plumber: Get their accepted jobs
     */
    fun getPlumberJobs(): Flow<Result<List<ServiceJob>>> = callbackFlow {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            trySend(Result.success(emptyList()))
            close()
            return@callbackFlow
        }
        
        val listener = serviceJobsCollection
            .whereEqualTo("plumberId", userId)
            .limit(50)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }
                
                val inFlightStatuses = setOf(
                    JobStatus.ACCEPTED,
                    JobStatus.EN_ROUTE,
                    JobStatus.INSPECTING,
                    JobStatus.IN_PROGRESS
                )
                val jobs = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(ServiceJob::class.java)?.copy(id = doc.id)
                }?.filter { it.status in inFlightStatuses }
                 ?.sortedByDescending { it.updatedAt.time }
                 ?: emptyList()
                
                trySend(Result.success(jobs))
            }
        
        awaitClose { listener.remove() }
    }
    
    /**
     * Plumber: Accept a job
     */
    /**
     * Plumber claims a job.
     *
     * Runs in a transaction because the open queue is visible to every plumber
     * at once: without it, two plumbers tapping Accept within the same moment
     * would both "win" and the second would silently overwrite the first. The
     * transaction re-reads the job and refuses if it is no longer SEARCHING.
     *
     * Being ONLINE is no longer required - accepting a job *is* the act of going
     * to work, and rejecting the tap for a toggle the plumber didn't know about
     * was needless friction. Approval, however, is still mandatory.
     */
    suspend fun acceptJob(jobId: String): Result<Unit> {
        return try {
            val userId = auth.currentUser?.uid ?: throw Exception("You are not signed in")

            val plumber = plumbersCollection.document(userId).get().await()
                .toObject(Plumber::class.java)
                ?: throw Exception("Finish your plumber profile before accepting work")

            if (!plumber.kyc.verified) {
                throw Exception("Your profile is still being reviewed by the store")
            }

            val jobRef = serviceJobsCollection.document(jobId)

            firestore.runTransaction { txn ->
                val snapshot = txn.get(jobRef)
                val status = snapshot.getString("status")

                if (status != JobStatus.SEARCHING.name) {
                    throw Exception("Another plumber just took this job")
                }

                txn.update(
                    jobRef,
                    mapOf(
                        "plumberId" to userId,
                        "status" to JobStatus.ACCEPTED.name,
                        "updatedAt" to Date()
                    )
                )
            }.await()

            // Mark them busy so they drop out of the "available now" list.
            plumbersCollection.document(userId)
                .update("operationalStatus", OperationalStatus.BUSY.name)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Update job status
     */
    suspend fun updateJobStatus(jobId: String, status: JobStatus): Result<Unit> {
        return try {
            val updates = mutableMapOf<String, Any>(
                "status" to status.name,
                "updatedAt" to Date()
            )
            if (status == JobStatus.COMPLETED) {
                updates["completedAt"] = Date()
            }
            serviceJobsCollection.document(jobId).update(updates).await()
            
            // If job is completed or cancelled, set plumber back to ONLINE
            if (status == JobStatus.COMPLETED || status == JobStatus.CANCELLED) {
                val jobDoc = serviceJobsCollection.document(jobId).get().await()
                val job = try { jobDoc.toObject(ServiceJob::class.java) } catch (e: Exception) { null }
                
                job?.plumberId?.let { plumberId ->
                    val plumberUpdates = mutableMapOf<String, Any>(
                        "operationalStatus" to OperationalStatus.ONLINE.name
                    )
                    if (status == JobStatus.COMPLETED) {
                        plumberUpdates["stats.jobsCompleted"] = com.google.firebase.firestore.FieldValue.increment(1)
                    }
                    plumbersCollection.document(plumberId).update(plumberUpdates).await()
                }
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get single job details
     */
    fun getJobDetails(jobId: String): Flow<Result<ServiceJob?>> = callbackFlow {
        val listener = serviceJobsCollection.document(jobId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }
                
                val job = snapshot?.toObject(ServiceJob::class.java)
                trySend(Result.success(job))
            }
        
        awaitClose { listener.remove() }
    }
    
    /**
     * Cancel job (Consumer)
     */
    suspend fun cancelJob(jobId: String): Result<Unit> {
        return try {
            val userId = auth.currentUser?.uid ?: throw Exception("User not authenticated")
            
            val jobDoc = serviceJobsCollection.document(jobId).get().await()
            val job = jobDoc.toObject(ServiceJob::class.java)
            
            if (job?.customerId != userId) {
                throw Exception("Not authorized to cancel this job")
            }
            
            if (job.status != JobStatus.SEARCHING && job.status != JobStatus.ACCEPTED) {
                throw Exception("Cannot cancel job in current status")
            }
            
            updateJobStatus(jobId, JobStatus.CANCELLED)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
