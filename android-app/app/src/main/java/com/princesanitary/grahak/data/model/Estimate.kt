package com.princesanitary.grahak.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName
import java.util.Date

data class Estimate(
    @DocumentId
    val id: String = "",
    
    @PropertyName("jobId")
    val jobId: String = "",
    
    @PropertyName("plumberId")
    val plumberId: String = "",
    
    @PropertyName("customerId")
    val customerId: String = "",
    
    @PropertyName("items")
    val items: List<EstimateItem> = emptyList(),
    
    @PropertyName("subtotal")
    val subtotal: Double = 0.0,
    
    @PropertyName("taxTotal")
    val taxTotal: Double = 0.0,
    
    @PropertyName("grandTotal")
    val grandTotal: Double = 0.0,
    
    @PropertyName("labourCharge")
    val labourCharge: Double = 0.0,
    
    @PropertyName("status")
    val status: EstimateStatus = EstimateStatus.DRAFT,
    
    @PropertyName("notes")
    val notes: String = "",
    
    @PropertyName("createdAt")
    val createdAt: Date = Date(),
    
    @PropertyName("updatedAt")
    val updatedAt: Date = Date(),
    
    @PropertyName("approvedAt")
    val approvedAt: Date? = null
)

data class EstimateItem(
    @PropertyName("productId")
    val productId: String = "",
    
    @PropertyName("productName")
    val productName: String = "",
    
    @PropertyName("quantity")
    val quantity: Int = 0,
    
    @PropertyName("unitPrice")
    val unitPrice: Double = 0.0,
    
    @PropertyName("total")
    val total: Double = 0.0,
    
    @PropertyName("gstRate")
    val gstRate: Double = 0.0
)

enum class EstimateStatus {
    @PropertyName("DRAFT")
    DRAFT,
    
    @PropertyName("SUBMITTED")
    SUBMITTED,
    
    @PropertyName("APPROVED")
    APPROVED,
    
    @PropertyName("REJECTED")
    REJECTED,
    
    @PropertyName("CONVERTED_TO_ORDER")
    CONVERTED_TO_ORDER
}
