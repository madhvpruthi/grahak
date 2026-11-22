package com.princesanitary.grahak.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.PropertyName

data class Plumber(
    @DocumentId
    val id: String = "",
    
    @PropertyName("name")
    val name: String = "",
    
    @PropertyName("phone")
    val phone: String = "",
    
    @PropertyName("photoUrl")
    val photoUrl: String = "",

    /** Speciality ids from [ServiceSpeciality]. Drives which jobs they match. */
    @PropertyName("specialities")
    val specialities: List<String> = emptyList(),

    @PropertyName("experienceYears")
    val experienceYears: Int = 0,

    /** Short self-introduction shown to customers. */
    @PropertyName("about")
    val about: String = "",

    /** Area the plumber covers, e.g. "247001". */
    @PropertyName("servicePincode")
    val servicePincode: String = "",

    @PropertyName("baseAddress")
    val baseAddress: String = "",
    
    @PropertyName("kyc")
    val kyc: KYCInfo = KYCInfo(),
    
    @PropertyName("rates")
    val rates: PlumberRates = PlumberRates(),
    
    @PropertyName("liveLocation")
    val liveLocation: GeoPoint? = null,
    
    @PropertyName("operationalStatus")
    val operationalStatus: OperationalStatus = OperationalStatus.OFFLINE,
    
    @PropertyName("hiddenFromOrders")
    val hiddenFromOrders: Boolean = false,
    
    @PropertyName("stats")
    val stats: PlumberStats = PlumberStats(),
    
    @PropertyName("wallet")
    val wallet: PlumberWallet = PlumberWallet()
)

data class KYCInfo(
    @PropertyName("aadhaarNo")
    val aadhaarNo: String = "",
    
    @PropertyName("docUrls")
    val docUrls: List<String> = emptyList(),
    
    /**
     * Kept as the authoritative gate for accepting work. [reviewStatus] carries
     * the richer state (so a rejection can be told apart from "not looked at
     * yet"), but anything that decides whether a plumber may take a job reads
     * this flag.
     */
    /*
     * Named `verified`, NOT `isVerified`, and that matters.
     *
     * Kotlin compiles `val isVerified: Boolean` to a getter `isVerified()`, and
     * the Firestore mapper follows the JavaBeans convention of stripping the
     * `is` prefix - so it writes the field as `verified`. A field-level
     * @PropertyName does not change that, because the annotation lands on the
     * backing field while the mapper reads the getter (it would need
     * `@get:PropertyName`).
     *
     * The result was that the app stored `kyc.verified` while every query asked
     * for `kyc.verified`, so approved plumbers were invisible to customers and
     * stayed stuck on "Under review". Using a name with no `is` prefix removes
     * the ambiguity entirely and matches the documents already written.
     */
    @get:PropertyName("verified")
    val verified: Boolean = false,

    @PropertyName("reviewStatus")
    val reviewStatus: String = "PENDING",

    @PropertyName("rejectionReason")
    val rejectionReason: String = ""
)

data class PlumberRates(
    @PropertyName("visitCharge")
    val visitCharge: Double = 0.0,
    
    @PropertyName("customRates")
    val customRates: Map<String, Double> = emptyMap() // service type -> rate
)

enum class OperationalStatus {
    @PropertyName("OFFLINE")
    OFFLINE,
    
    @PropertyName("ONLINE")
    ONLINE,
    
    @PropertyName("BUSY")
    BUSY
}

data class PlumberStats(
    @PropertyName("rating")
    val rating: Double = 0.0,
    
    @PropertyName("jobsCompleted")
    val jobsCompleted: Int = 0,

    @PropertyName("ratingCount")
    val ratingCount: Int = 0
)

data class PlumberWallet(
    @PropertyName("withdrawableBalance")
    val withdrawableBalance: Double = 0.0,
    
    @PropertyName("escrowBalance")
    val escrowBalance: Double = 0.0,
    
    @PropertyName("totalEarnings")
    val totalEarnings: Double = 0.0,
    
    @PropertyName("upiId")
    val upiId: String = "",
    
    @PropertyName("updatedAt")
    val updatedAt: java.util.Date = java.util.Date()
)
