package com.princesanitary.grahak.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName
import java.util.Date

data class Transaction(
    @DocumentId
    val id: String = "",
    
    @PropertyName("plumberId")
    val plumberId: String = "",
    
    @PropertyName("type")
    val type: TransactionType = TransactionType.COMMISSION_EARNED,
    
    @PropertyName("amount")
    val amount: Double = 0.0,
    
    @PropertyName("orderId")
    val orderId: String? = null,
    
    @PropertyName("jobId")
    val jobId: String? = null,
    
    @PropertyName("withdrawalRequestId")
    val withdrawalRequestId: String? = null,
    
    @PropertyName("status")
    val status: TransactionStatus = TransactionStatus.PENDING,
    
    @PropertyName("description")
    val description: String = "",
    
    @PropertyName("createdAt")
    val createdAt: Date = Date(),
    
    @PropertyName("completedAt")
    val completedAt: Date? = null
)

enum class TransactionType {
    @PropertyName("COMMISSION_EARNED")
    COMMISSION_EARNED,
    
    @PropertyName("COMMISSION_RELEASED")
    COMMISSION_RELEASED,

    @PropertyName("COMMISSION_RELEASE")
    COMMISSION_RELEASE,
    
    @PropertyName("WITHDRAWAL_REQUESTED")
    WITHDRAWAL_REQUESTED,
    
    @PropertyName("WITHDRAWAL_COMPLETED")
    WITHDRAWAL_COMPLETED,
    
    @PropertyName("WITHDRAWAL_REJECTED")
    WITHDRAWAL_REJECTED
}

enum class TransactionStatus {
    @PropertyName("PENDING")
    PENDING,
    
    @PropertyName("COMPLETED")
    COMPLETED,
    
    @PropertyName("FAILED")
    FAILED,
    
    @PropertyName("CANCELLED")
    CANCELLED
}

data class WithdrawalRequest(
    @DocumentId
    val id: String = "",
    
    @PropertyName("plumberId")
    val plumberId: String = "",
    
    @PropertyName("amount")
    val amount: Double = 0.0,
    
    @PropertyName("upiId")
    val upiId: String = "",
    
    @PropertyName("status")
    val status: WithdrawalStatus = WithdrawalStatus.PENDING,
    
    @PropertyName("requestedAt")
    val requestedAt: Date = Date(),
    
    @PropertyName("processedAt")
    val processedAt: Date? = null,
    
    @PropertyName("transactionId")
    val transactionId: String? = null,
    
    @PropertyName("notes")
    val notes: String = "",
    
    @PropertyName("rejectionReason")
    val rejectionReason: String? = null
)

enum class WithdrawalStatus {
    @PropertyName("PENDING")
    PENDING,
    
    @PropertyName("APPROVED")
    APPROVED,
    
    @PropertyName("COMPLETED")
    COMPLETED,
    
    @PropertyName("REJECTED")
    REJECTED
}
