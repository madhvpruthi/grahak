package com.princesanitary.grahak.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName
import java.util.Date

data class Cart(
    @DocumentId
    val id: String = "",
    
    @PropertyName("userId")
    val userId: String = "",
    
    @PropertyName("items")
    val items: List<CartItem> = emptyList(),
    
    @PropertyName("updatedAt")
    val updatedAt: Date = Date()
)

data class CartItem(
    @PropertyName("productId")
    val productId: String = "",
    
    @PropertyName("productName")
    val productName: String = "",

    @PropertyName("itemNumber")
    val itemNumber: String = "",

    @PropertyName("size")
    val size: String = "",

    @PropertyName("brand")
    val brand: String = "",
    
    @PropertyName("quantity")
    val quantity: Int = 1,
    
    @PropertyName("unitPrice")
    val unitPrice: Double = 0.0,
    
    @PropertyName("gstRate")
    val gstRate: Double = 0.0,
    
    @PropertyName("imageUrl")
    val imageUrl: String = "",
    
    @PropertyName("addedAt")
    val addedAt: Date = Date()
) {
    val total: Double
        get() = unitPrice * quantity
    
    val taxAmount: Double
        get() = total * gstRate / 100
    
    val totalWithTax: Double
        get() = total + taxAmount
}

data class CartSummary(
    val itemCount: Int = 0,
    val subtotal: Double = 0.0,
    val taxTotal: Double = 0.0,
    val grandTotal: Double = 0.0
)
