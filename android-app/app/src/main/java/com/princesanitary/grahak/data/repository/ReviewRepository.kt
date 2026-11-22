package com.princesanitary.grahak.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.princesanitary.grahak.data.model.Review
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReviewRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {

    private val reviews = firestore.collection("reviews")
    private val plumbers = firestore.collection("plumbers")

    /**
     * Submits a rating and folds it into the plumber's running average.
     *
     * The review document and the plumber's aggregate are updated in one
     * transaction, so a rating can never be recorded without moving the average
     * (or vice versa). The document id is the job id, which is what enforces
     * one review per job.
     *
     * The average is maintained incrementally rather than recomputed:
     *   newAvg = (oldAvg * oldCount + rating) / (oldCount + 1)
     *
     * NOTE: this is a client-side aggregate, so it trusts the customer's own
     * arithmetic. It is the right trade-off while there is no backend, but it
     * belongs in a Cloud Function before ratings influence anything that matters.
     */
    suspend fun submitReview(
        jobId: String,
        plumberId: String,
        rating: Int,
        comment: String,
        customerName: String,
        serviceCategory: String
    ): Result<Unit> {
        return try {
            val uid = auth.currentUser?.uid ?: throw Exception("You are not signed in")
            require(rating in 1..5) { "Rating must be between 1 and 5" }

            val reviewRef = reviews.document(jobId)
            val plumberRef = plumbers.document(plumberId)

            firestore.runTransaction { txn ->
                if (txn.get(reviewRef).exists()) {
                    throw Exception("You've already rated this job")
                }

                val plumberSnap = txn.get(plumberRef)
                val currentAvg = plumberSnap.getDouble("stats.rating") ?: 0.0
                val currentCount = (plumberSnap.getLong("stats.ratingCount") ?: 0L).toInt()

                val newCount = currentCount + 1
                val newAvg = ((currentAvg * currentCount) + rating) / newCount

                txn.set(
                    reviewRef,
                    Review(
                        jobId = jobId,
                        plumberId = plumberId,
                        customerId = uid,
                        customerName = customerName,
                        rating = rating,
                        comment = comment.trim(),
                        serviceCategory = serviceCategory,
                        createdAt = Date()
                    )
                )

                txn.update(
                    plumberRef,
                    mapOf(
                        // Rounded so the stored value stays readable.
                        "stats.rating" to (Math.round(newAvg * 10.0) / 10.0),
                        "stats.ratingCount" to newCount
                    )
                )
            }.await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Has this job already been rated? Drives whether the prompt is shown. */
    suspend fun hasReview(jobId: String): Boolean = runCatching {
        reviews.document(jobId).get().await().exists()
    }.getOrDefault(false)

    /** Recent reviews for a plumber, newest first. */
    fun observeReviewsFor(plumberId: String): Flow<Result<List<Review>>> = callbackFlow {
        val listener = reviews
            .whereEqualTo("plumberId", plumberId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(25)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }
                trySend(
                    Result.success(
                        snapshot?.documents?.mapNotNull { it.toObject(Review::class.java) }
                            ?: emptyList()
                    )
                )
            }

        awaitClose { listener.remove() }
    }
}
