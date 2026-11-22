package com.princesanitary.grahak.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName
import java.util.Date

data class StoreSettings(
    @DocumentId
    val id: String = "config",

    @get:PropertyName("ownerUpiId") @set:PropertyName("ownerUpiId")
    var ownerUpiId: String = "",

    @get:PropertyName("ownerBusinessName") @set:PropertyName("ownerBusinessName")
    var ownerBusinessName: String = "",

    @get:PropertyName("ownerPhone") @set:PropertyName("ownerPhone")
    var ownerPhone: String = "",

    @get:PropertyName("freeDeliveryThreshold") @set:PropertyName("freeDeliveryThreshold")
    var freeDeliveryThreshold: Double = 0.0,

    @get:PropertyName("deliveryFee") @set:PropertyName("deliveryFee")
    var deliveryFee: Double = 0.0,

    @get:PropertyName("plumberCommissionRate") @set:PropertyName("plumberCommissionRate")
    var plumberCommissionRate: Double = 0.0,

    @get:PropertyName("festivalHeadline") @set:PropertyName("festivalHeadline")
    var festivalHeadline: String = "",

    @get:PropertyName("isFestivalBannerActive") @set:PropertyName("isFestivalBannerActive")
    var isFestivalBannerActive: Boolean = false,

    @get:PropertyName("festivalTag") @set:PropertyName("festivalTag")
    var festivalTag: String = "",

    @get:PropertyName("isStoreOpen") @set:PropertyName("isStoreOpen")
    var isStoreOpen: Boolean = true,

    @get:PropertyName("nextDispatchSchedule") @set:PropertyName("nextDispatchSchedule")
    var nextDispatchSchedule: String = "",

    @get:PropertyName("storeClosedNotice") @set:PropertyName("storeClosedNotice")
    var storeClosedNotice: String = "",

    @get:PropertyName("updatedAt") @set:PropertyName("updatedAt")
    var updatedAt: Date = Date()
)

object DiscountType {
    const val PERCENTAGE = "PERCENTAGE"
    const val FLAT = "FLAT"
}

data class Coupon(
    @DocumentId
    val id: String = "",

    @PropertyName("code")
    val code: String = "", // e.g. "WELCOME10", "PRINCE50"

    @PropertyName("discountType")
    val discountType: String = "PERCENTAGE", // "PERCENTAGE" or "FLAT"

    @PropertyName("discountValue")
    val discountValue: Double = 10.0, // 10% or ₹50

    @PropertyName("minOrderAmount")
    val minOrderAmount: Double = 500.0,

    @PropertyName("maxDiscountAmount")
    val maxDiscountAmount: Double = 500.0,

    @PropertyName("isActive")
    val isActive: Boolean = true,

    @PropertyName("description")
    val description: String = ""
)
