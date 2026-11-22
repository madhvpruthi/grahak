package com.princesanitary.grahak.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName
import java.util.Date

data class Order(
    @DocumentId
    val id: String = "",

    @PropertyName("orderNumber")
    val orderNumber: String = "",
    
    @PropertyName("customerId")
    val customerId: String = "",

    @PropertyName("customerName")
    val customerName: String = "",

    @PropertyName("customerPhone")
    val customerPhone: String = "",
    
    @PropertyName("attributedPlumberId")
    val attributedPlumberId: String? = null,

    @PropertyName("attributedPlumberName")
    val attributedPlumberName: String = "",
    
    @PropertyName("linkedJobId")
    val linkedJobId: String? = null,
    
    @PropertyName("linkedEstimateId")
    val linkedEstimateId: String? = null,
    
    @PropertyName("source")
    val source: OrderSource = OrderSource.DIRECT_CHECKOUT,
    
    @PropertyName("status")
    val status: OrderStatus = OrderStatus.PLACED,
    
    @PropertyName("items")
    val items: List<OrderItem> = emptyList(),
    
    @PropertyName("billing")
    val billing: BillingInfo = BillingInfo(),
    
    @PropertyName("delivery")
    val delivery: DeliveryInfo = DeliveryInfo(),
    
    @PropertyName("plumberCommission")
    val plumberCommission: Double = 0.0,
    
    @PropertyName("commissionReleased")
    val commissionReleased: Boolean = false,
    
    @PropertyName("payment")
    val payment: PaymentInfo = PaymentInfo(),
    
    @PropertyName("notes")
    val notes: String = "",
    
    @PropertyName("createdAt")
    val createdAt: Date = Date(),
    
    @PropertyName("updatedAt")
    val updatedAt: Date = Date(),
    
    @PropertyName("completedAt")
    val completedAt: Date? = null
) {
    fun getDisplayOrderNumber(): String {
        if (orderNumber.isNotBlank()) return orderNumber
        val cleanHash = kotlin.math.abs(id.hashCode()) % 9000 + 1000
        return "PSS-$cleanHash"
    }
}

enum class OrderSource {
    @PropertyName("DIRECT_CHECKOUT")
    DIRECT_CHECKOUT,
    
    @PropertyName("PLUMBER_ESTIMATE")
    PLUMBER_ESTIMATE
}

enum class OrderStatus {
    @PropertyName("PLACED")
    PLACED,
    
    @PropertyName("CONFIRMED")
    CONFIRMED,
    
    @PropertyName("PACKED")
    PACKED,
    
    @PropertyName("SHIPPED")
    SHIPPED,
    
    @PropertyName("DELIVERED")
    DELIVERED,
    
    @PropertyName("CANCELLED")
    CANCELLED
}

data class OrderItem(
    @PropertyName("productId")
    val productId: String = "",
    
    @PropertyName("name")
    val name: String = "",

    @PropertyName("itemNumber")
    val itemNumber: String = "",

    @PropertyName("size")
    val size: String = "",

    @PropertyName("brand")
    val brand: String = "",
    
    @PropertyName("qty")
    val qty: Int = 0,
    
    @PropertyName("unitPrice")
    val unitPrice: Double = 0.0,
    
    @PropertyName("gstRate")
    val gstRate: Double = 0.0,
    
    @PropertyName("total")
    val total: Double = 0.0
) {
    val displayItemCode: String
        get() = if (itemNumber.isNotBlank()) itemNumber else "PS-${1000 + (kotlin.math.abs(productId.hashCode()) % 9000)}"
}

data class BillingInfo(
    @PropertyName("subtotal")
    val subtotal: Double = 0.0,
    
    @PropertyName("taxTotal")
    val taxTotal: Double = 0.0,

    @PropertyName("deliveryFee")
    val deliveryFee: Double = 0.0,

    @PropertyName("discountAmount")
    val discountAmount: Double = 0.0,

    @PropertyName("couponCode")
    val couponCode: String = "",
    
    @PropertyName("grandTotal")
    val grandTotal: Double = 0.0
)

data class DeliveryInfo(
    @PropertyName("type")
    val type: DeliveryType = DeliveryType.STORE_PICKUP,
    
    @PropertyName("address")
    val address: SavedAddress = SavedAddress(),
    
    @PropertyName("status")
    val status: DeliveryStatus = DeliveryStatus.PACKING
)

enum class DeliveryType {
    @PropertyName("STORE_PICKUP")
    STORE_PICKUP,
    
    @PropertyName("SITE_DROP")
    SITE_DROP
}

enum class DeliveryStatus {
    @PropertyName("PACKING")
    PACKING,
    
    @PropertyName("DISPATCHED")
    DISPATCHED,
    
    @PropertyName("DELIVERED")
    DELIVERED
}

data class PaymentInfo(
    @PropertyName("method")
    val method: PaymentMethod = PaymentMethod.UPI,
    
    @PropertyName("status")
    val status: PaymentStatus = PaymentStatus.PENDING,
    
    @PropertyName("transactionId")
    val transactionId: String = ""
)

enum class PaymentMethod {
    @PropertyName("UPI")
    UPI,
    
    @PropertyName("COD")
    COD
}

enum class PaymentStatus {
    @PropertyName("PENDING")
    PENDING,
    
    @PropertyName("COMPLETED")
    COMPLETED,
    
    @PropertyName("FAILED")
    FAILED
}
