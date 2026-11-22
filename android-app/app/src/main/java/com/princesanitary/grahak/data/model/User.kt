package com.princesanitary.grahak.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.PropertyName

data class User(
    @DocumentId
    val id: String = "",
    
    @PropertyName("name")
    val name: String = "",
    
    @PropertyName("phone")
    val phone: String = "",

    @PropertyName("email")
    val email: String = "",

    /** FCM registration token for this account's device. */
    @PropertyName("fcmToken")
    val fcmToken: String = "",

    /**
     * "CONSUMER" or "PLUMBER". This is the single source of truth for routing
     * after sign-in - previously the app guessed the role by checking which
     * collection a document happened to live in, which sent plumbers to the
     * consumer UI.
     */
    @PropertyName("userRole")
    val userRole: String = "",

    @PropertyName("savedAddresses")
    val savedAddresses: List<SavedAddress> = emptyList(),
    
    @PropertyName("rewardPoints")
    val rewardPoints: Int = 0
)

data class SavedAddress(
    @PropertyName("id")
    val id: String = "",
    
    @PropertyName("tag")
    val tag: String = "", // Home, Work, Site, etc.
    
    @PropertyName("fullAddress")
    val fullAddress: String = "",
    
    @PropertyName("coordinates")
    val coordinates: GeoPoint = GeoPoint(0.0, 0.0)
) {
    constructor(
        tag: String,
        fullAddress: String,
        lat: Double,
        lng: Double
    ) : this(
        tag = tag,
        fullAddress = fullAddress,
        coordinates = GeoPoint(lat, lng)
    )
}
