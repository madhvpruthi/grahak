package com.princesanitary.grahak.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName
import java.util.Date

data class ServiceJob(
    @DocumentId
    val id: String = "",
    
    @PropertyName("customerId")
    val customerId: String = "",

    /*
     * Customer contact is COPIED onto the job on purpose.
     *
     * The security rules only let a user read their own `users/{uid}` document,
     * so an assigned plumber cannot look the customer up - and a plumber who
     * can't phone the customer can't find the gate. Denormalising name + phone
     * onto the job is what makes the contact reachable, and it also freezes the
     * number as it was when the job was booked.
     */
    @PropertyName("customerName")
    val customerName: String = "",

    @PropertyName("customerPhone")
    val customerPhone: String = "",
    
    @PropertyName("plumberId")
    val plumberId: String? = null, // nullable during broadcast
    
    @PropertyName("serviceCategory")
    val serviceCategory: String = "",

    /**
     * What the customer typed. This was previously accepted by
     * `createServiceJob(...)` and then silently dropped, so plumbers saw a job
     * with no idea what the work actually was.
     */
    @PropertyName("description")
    val description: String = "",

    /**
     * Set when the customer picked one specific plumber rather than broadcasting.
     * That plumber gets an exclusive window to accept; if they decline, the job
     * falls back to the open queue.
     */
    @PropertyName("requestedPlumberId")
    val requestedPlumberId: String? = null,

    /** Plumbers who passed on this job - it stops appearing in their queue. */
    @PropertyName("declinedBy")
    val declinedBy: List<String> = emptyList(),

    /** Rate the customer agreed to when choosing the plumber, in rupees. */
    @PropertyName("agreedVisitRate")
    val agreedVisitRate: Double = 0.0,
    
    @PropertyName("siteAddress")
    val siteAddress: SavedAddress = SavedAddress(),
    
    @PropertyName("status")
    val status: JobStatus = JobStatus.SEARCHING,
    
    @PropertyName("inspectionNotes")
    val inspectionNotes: InspectionNotes = InspectionNotes(),
    
    @PropertyName("linkedMaterialOrderId")
    val linkedMaterialOrderId: String? = null,
    
    @PropertyName("createdAt")
    val createdAt: Date = Date(),
    
    @PropertyName("updatedAt")
    val updatedAt: Date = Date()
)

enum class JobStatus {
    @PropertyName("SEARCHING")
    SEARCHING,
    
    @PropertyName("ACCEPTED")
    ACCEPTED,
    
    @PropertyName("EN_ROUTE")
    EN_ROUTE,
    
    @PropertyName("INSPECTING")
    INSPECTING,
    
    @PropertyName("IN_PROGRESS")
    IN_PROGRESS,
    
    @PropertyName("COMPLETED")
    COMPLETED,
    
    @PropertyName("CANCELLED")
    CANCELLED
}

data class InspectionNotes(
    @PropertyName("beforeMedia")
    val beforeMedia: List<String> = emptyList(),
    
    @PropertyName("afterMedia")
    val afterMedia: List<String> = emptyList(),
    
    @PropertyName("notes")
    val notes: String = ""
)
