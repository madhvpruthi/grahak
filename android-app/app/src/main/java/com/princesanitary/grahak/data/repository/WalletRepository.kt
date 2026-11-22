package com.princesanitary.grahak.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.princesanitary.grahak.data.model.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WalletRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {
    
    private val plumbersCollection = firestore.collection("plumbers")
    private val transactionsCollection = firestore.collection("transactions")
    private val withdrawalRequestsCollection = firestore.collection("withdrawal_requests")
    private val ordersCollection = firestore.collection("orders")

    /**
     * Reconcile any delivered orders whose commission has not yet been released.
     */
    suspend fun reconcileDeliveredCommissions(plumberId: String) {
        try {
            val deliveredOrders = ordersCollection
                .whereEqualTo("attributedPlumberId", plumberId)
                .whereEqualTo("status", com.princesanitary.grahak.data.model.OrderStatus.DELIVERED.name)
                .get()
                .await()

            for (doc in deliveredOrders.documents) {
                val commissionReleased = doc.getBoolean("commissionReleased") ?: false
                val commission = doc.getDouble("plumberCommission") ?: 0.0
                if (!commissionReleased && commission > 0) {
                    doc.reference.update(
                        mapOf(
                            "commissionReleased" to true,
                            "updatedAt" to Date()
                        )
                    ).await()

                    plumbersCollection.document(plumberId).update(
                        mapOf(
                            "wallet.withdrawableBalance" to com.google.firebase.firestore.FieldValue.increment(commission),
                            "wallet.totalEarnings" to com.google.firebase.firestore.FieldValue.increment(commission),
                            "wallet.updatedAt" to Date()
                        )
                    ).await()

                    val orderNum = doc.getString("orderNumber") ?: doc.id.takeLast(4).uppercase()
                    transactionsCollection.add(
                        mapOf(
                            "plumberId" to plumberId,
                            "type" to TransactionType.COMMISSION_RELEASED.name,
                            "amount" to commission,
                            "orderId" to doc.id,
                            "status" to "COMPLETED",
                            "description" to "Commission for Order #$orderNum",
                            "createdAt" to Date()
                        )
                    ).await()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * Get plumber's wallet with real-time updates
     */
    fun getWallet(): Flow<Result<PlumberWallet>> = callbackFlow {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            trySend(Result.success(PlumberWallet()))
            close()
            return@callbackFlow
        }
        
        // Reconcile delivered commissions on background thread
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            reconcileDeliveredCommissions(userId)
        }

        val listener = plumbersCollection.document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }
                
                val plumber = runCatching { snapshot?.toObject(Plumber::class.java) }.getOrNull()
                val wallet = plumber?.wallet ?: PlumberWallet()
                
                trySend(Result.success(wallet))
            }
        
        awaitClose { listener.remove() }
    }
    
    /**
     * Get transaction history
     */
    fun getTransactions(limit: Int = 50): Flow<Result<List<Transaction>>> = callbackFlow {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            trySend(Result.success(emptyList()))
            close()
            return@callbackFlow
        }
        
        val listener = transactionsCollection
            .whereEqualTo("plumberId", userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }
                
                val transactions = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        doc.toObject(Transaction::class.java)
                    } catch (e: Exception) {
                        null
                    }
                } ?: emptyList()
                
                trySend(Result.success(transactions))
            }
        
        awaitClose { listener.remove() }
    }
    
    /**
     * Get withdrawal requests
     */
    fun getWithdrawalRequests(): Flow<Result<List<WithdrawalRequest>>> = callbackFlow {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            trySend(Result.success(emptyList()))
            close()
            return@callbackFlow
        }
        
        val listener = withdrawalRequestsCollection
            .whereEqualTo("plumberId", userId)
            .orderBy("requestedAt", Query.Direction.DESCENDING)
            .limit(20)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }
                
                val requests = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        doc.toObject(WithdrawalRequest::class.java)
                    } catch (e: Exception) {
                        null
                    }
                } ?: emptyList()
                
                trySend(Result.success(requests))
            }
        
        awaitClose { listener.remove() }
    }
    
    /**
     * Request withdrawal
     */
    suspend fun requestWithdrawal(amount: Double, upiId: String, notes: String = ""): Result<String> {
        return try {
            val userId = auth.currentUser?.uid ?: throw Exception("User not authenticated")
            
            // Get current wallet balance
            val plumberDoc = plumbersCollection.document(userId).get().await()
            val plumber = plumberDoc.toObject(Plumber::class.java)
            
            if (plumber == null) {
                throw Exception("Plumber profile not found")
            }
            
            // Validate withdrawal amount
            if (amount <= 0) {
                throw Exception("Invalid withdrawal amount")
            }
            
            if (amount > plumber.wallet.withdrawableBalance) {
                throw Exception("Insufficient balance. Available: ₹${plumber.wallet.withdrawableBalance}")
            }
            
            if (upiId.isEmpty()) {
                throw Exception("UPI ID is required")
            }
            
            // Create withdrawal request
            val withdrawalRequest = WithdrawalRequest(
                plumberId = userId,
                amount = amount,
                upiId = upiId,
                status = WithdrawalStatus.PENDING,
                requestedAt = Date(),
                notes = notes
            )
            
            val docRef = withdrawalRequestsCollection.add(withdrawalRequest).await()
            
            // Create transaction record
            transactionsCollection.add(
                Transaction(
                    plumberId = userId,
                    type = TransactionType.WITHDRAWAL_REQUESTED,
                    amount = amount,
                    withdrawalRequestId = docRef.id,
                    status = TransactionStatus.PENDING,
                    description = "Withdrawal request to $upiId",
                    createdAt = Date()
                )
            ).await()
            
            // Deduct from withdrawable balance and add to escrow temporarily
            plumbersCollection.document(userId).update(
                mapOf(
                    "wallet.withdrawableBalance" to plumber.wallet.withdrawableBalance - amount,
                    "wallet.escrowBalance" to plumber.wallet.escrowBalance + amount,
                    "wallet.updatedAt" to Date()
                )
            ).await()
            
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Update UPI ID
     */
    suspend fun updateUpiId(upiId: String): Result<Unit> {
        return try {
            val userId = auth.currentUser?.uid ?: throw Exception("User not authenticated")
            
            if (upiId.isEmpty()) {
                throw Exception("UPI ID cannot be empty")
            }
            
            // Basic UPI ID validation (format: username@bank)
            if (!upiId.matches(Regex("^[a-zA-Z0-9.\\-_]{2,}@[a-zA-Z]{2,}$"))) {
                throw Exception("Invalid UPI ID format")
            }
            
            plumbersCollection.document(userId).update(
                mapOf(
                    "wallet.upiId" to upiId,
                    "wallet.updatedAt" to Date()
                )
            ).await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get wallet statistics
     */
    suspend fun getWalletStats(): Result<WalletStats> {
        return try {
            val userId = auth.currentUser?.uid ?: throw Exception("User not authenticated")
            reconcileDeliveredCommissions(userId)
            
            val plumberDoc = plumbersCollection.document(userId).get().await()
            val plumber = plumberDoc.toObject(Plumber::class.java)
            
            if (plumber == null) {
                throw Exception("Plumber profile not found")
            }
            
            // Get pending withdrawals count
            val pendingWithdrawalsSnapshot = withdrawalRequestsCollection
                .whereEqualTo("plumberId", userId)
                .whereEqualTo("status", WithdrawalStatus.PENDING.name)
                .get()
                .await()
            
            val stats = WalletStats(
                withdrawableBalance = plumber.wallet.withdrawableBalance,
                escrowBalance = plumber.wallet.escrowBalance,
                totalEarnings = plumber.wallet.totalEarnings,
                pendingWithdrawals = pendingWithdrawalsSnapshot.documents.size
            )
            
            Result.success(stats)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

data class WalletStats(
    val withdrawableBalance: Double = 0.0,
    val escrowBalance: Double = 0.0,
    val totalEarnings: Double = 0.0,
    val pendingWithdrawals: Int = 0
)
