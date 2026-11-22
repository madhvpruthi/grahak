package com.princesanitary.grahak.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName
import java.util.Date

/**
 * A customer's rating of a plumber after a completed job.
 *
 * Until this existed, `PlumberStats.rating` was never written by anything, so
 * the star ratings on the "choose your plumber" list were decorative. Reviews
 * are what make that list trustworthy.
 *
 * The document id is the job id, which makes "one review per job" a property of
 * the data rather than something the client has to police.
 */
data class Review(
    @DocumentId
    val id: String = "",

    @PropertyName("jobId")
    val jobId: String = "",

    @PropertyName("plumberId")
    val plumberId: String = "",

    @PropertyName("customerId")
    val customerId: String = "",

    /** Display name shown next to the review; blank falls back to "Customer". */
    @PropertyName("customerName")
    val customerName: String = "",

    /** 1..5 */
    @PropertyName("rating")
    val rating: Int = 0,

    @PropertyName("comment")
    val comment: String = "",

    /** Speciality id the job was booked under, for per-service breakdowns later. */
    @PropertyName("serviceCategory")
    val serviceCategory: String = "",

    @PropertyName("createdAt")
    val createdAt: Date = Date()
)
