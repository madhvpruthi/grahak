package com.princesanitary.grahak.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.princesanitary.grahak.data.model.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EstimateRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {
    
    private val estimatesCollection = firestore.collection("estimates")
    private val serviceJobsCollection = firestore.collection("service_jobs")
    private val ordersCollection = firestore.collection("orders")
    private val plumbersCollection = firestore.collection("plumbers")
    
    /**
     * Create a new estimate (Plumber)
     */
    suspend fun createEstimate(
        jobId: String,
        customerId: String,
        items: List<EstimateItem>,
        labourCharge: Double,
        notes: String
    ): Result<String> {
        return try {
            val userId = auth.currentUser?.uid ?: throw Exception("User not authenticated")
            
            // Calculate totals
            val subtotal = items.sumOf { it.total }
            val taxTotal = items.sumOf { it.total * it.gstRate / 100 }
            val grandTotal = subtotal + taxTotal + labourCharge
            
            val estimate = Estimate(
                jobId = jobId,
                plumberId = userId,
                customerId = customerId,
                items = items,
                subtotal = subtotal,
                taxTotal = taxTotal,
                grandTotal = grandTotal,
                labourCharge = labourCharge,
                status = EstimateStatus.DRAFT,
                notes = notes,
                createdAt = Date(),
                updatedAt = Date()
            )
            
            val docRef = estimatesCollection.add(estimate).await()
            
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Submit estimate to customer and sync digital parchi to Store Orders
     */
    suspend fun submitEstimate(estimateId: String): Result<Unit> {
        return try {
            estimatesCollection.document(estimateId).update(
                mapOf(
                    "status" to EstimateStatus.SUBMITTED.name,
                    "updatedAt" to Date()
                )
            ).await()

            // Fetch estimate details
            val estDoc = estimatesCollection.document(estimateId).get().await()
            val estimate = estDoc.toObject(Estimate::class.java)

            if (estimate != null) {
                // Fetch customer & job details
                val jobDoc = if (estimate.jobId.isNotBlank()) {
                    runCatching { serviceJobsCollection.document(estimate.jobId).get().await() }.getOrNull()
                } else null
                val job = jobDoc?.toObject(ServiceJob::class.java)

                // Fetch plumber details
                val plumberDoc = if (estimate.plumberId.isNotBlank()) {
                    runCatching { plumbersCollection.document(estimate.plumberId).get().await() }.getOrNull()
                } else null
                val plumber = plumberDoc?.toObject(Plumber::class.java)

                val commissionRate = 0.05
                val commission = estimate.subtotal * commissionRate

                val orderItems = estimate.items.map { it ->
                    OrderItem(
                        productId = it.productId,
                        name = it.productName,
                        qty = it.quantity,
                        unitPrice = it.unitPrice,
                        total = it.total,
                        gstRate = it.gstRate
                    )
                }

                val order = Order(
                    orderNumber = "PSS-${kotlin.math.abs(estimateId.hashCode() % 9000 + 1000)}",
                    customerId = estimate.customerId,
                    customerName = job?.customerName.orEmpty().ifBlank { "Customer" },
                    customerPhone = job?.customerPhone.orEmpty(),
                    attributedPlumberId = estimate.plumberId,
                    attributedPlumberName = plumber?.name.orEmpty().ifBlank { "Partner Plumber" },
                    linkedJobId = estimate.jobId,
                    linkedEstimateId = estimateId,
                    source = OrderSource.PLUMBER_ESTIMATE,
                    status = OrderStatus.PLACED,
                    items = orderItems,
                    billing = BillingInfo(
                        subtotal = estimate.subtotal,
                        taxTotal = estimate.taxTotal,
                        deliveryFee = 0.0,
                        grandTotal = estimate.grandTotal
                    ),
                    delivery = DeliveryInfo(
                        address = SavedAddress(
                            tag = "Job Site",
                            fullAddress = job?.siteAddress?.fullAddress.orEmpty().ifBlank { "Customer Job Site" }
                        )
                    ),
                    plumberCommission = commission,
                    notes = "Digital Parchi created by plumber ${plumber?.name ?: ""} at customer site",
                    createdAt = Date(),
                    updatedAt = Date()
                )

                ordersCollection.add(order).await()
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Customer: Approve estimate
     */
    suspend fun approveEstimate(estimateId: String): Result<Unit> {
        return try {
            val userId = auth.currentUser?.uid ?: throw Exception("User not authenticated")
            
            val estimateDoc = estimatesCollection.document(estimateId).get().await()
            val estimate = estimateDoc.toObject(Estimate::class.java)
            
            if (estimate?.customerId != userId) {
                throw Exception("Not authorized to approve this estimate")
            }
            
            estimatesCollection.document(estimateId).update(
                mapOf(
                    "status" to EstimateStatus.APPROVED.name,
                    "approvedAt" to Date(),
                    "updatedAt" to Date()
                )
            ).await()
            
            // Update job status to IN_PROGRESS
            serviceJobsCollection.document(estimate.jobId).update(
                mapOf(
                    "status" to JobStatus.IN_PROGRESS.name,
                    "linkedMaterialOrderId" to estimateId,
                    "updatedAt" to Date()
                )
            ).await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Customer: Reject estimate
     */
    suspend fun rejectEstimate(estimateId: String, reason: String): Result<Unit> {
        return try {
            val userId = auth.currentUser?.uid ?: throw Exception("User not authenticated")
            
            val estimateDoc = estimatesCollection.document(estimateId).get().await()
            val estimate = estimateDoc.toObject(Estimate::class.java)
            
            if (estimate?.customerId != userId) {
                throw Exception("Not authorized to reject this estimate")
            }
            
            estimatesCollection.document(estimateId).update(
                mapOf(
                    "status" to EstimateStatus.REJECTED.name,
                    "notes" to "${estimate.notes}\n\nRejection reason: $reason",
                    "updatedAt" to Date()
                )
            ).await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get estimates for a job
     */
    fun getJobEstimates(jobId: String): Flow<Result<List<Estimate>>> = callbackFlow {
        val listener = estimatesCollection
            .whereEqualTo("jobId", jobId)
            .limit(50)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }
                
                val estimates = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        doc.toObject(Estimate::class.java)?.copy(id = doc.id)
                    } catch (e: Exception) {
                        null
                    }
                }?.sortedByDescending { it.createdAt.time } ?: emptyList()
                
                trySend(Result.success(estimates))
            }
        
        awaitClose { listener.remove() }
    }
    
    /**
     * Get single estimate
     */
    fun getEstimate(estimateId: String): Flow<Result<Estimate?>> = callbackFlow {
        val listener = estimatesCollection.document(estimateId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }
                
                val estimate = snapshot?.toObject(Estimate::class.java)?.copy(id = snapshot.id)
                trySend(Result.success(estimate))
            }
        
        awaitClose { listener.remove() }
    }
    
    /**
     * Get plumber's estimates
     */
    fun getPlumberEstimates(): Flow<Result<List<Estimate>>> = callbackFlow {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            trySend(Result.success(emptyList()))
            close()
            return@callbackFlow
        }
        
        val listener = estimatesCollection
            .whereEqualTo("plumberId", userId)
            .limit(50)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }
                
                val estimates = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        doc.toObject(Estimate::class.java)?.copy(id = doc.id)
                    } catch (e: Exception) {
                        null
                    }
                }?.sortedByDescending { it.createdAt.time } ?: emptyList()
                
                trySend(Result.success(estimates))
            }
        
        awaitClose { listener.remove() }
    }
    
    /**
     * Update estimate items
     */
    suspend fun updateEstimate(
        estimateId: String,
        items: List<EstimateItem>,
        labourCharge: Double,
        notes: String
    ): Result<Unit> {
        return try {
            val subtotal = items.sumOf { it.total }
            val taxTotal = items.sumOf { it.total * it.gstRate / 100 }
            val grandTotal = subtotal + taxTotal + labourCharge
            
            estimatesCollection.document(estimateId).update(
                mapOf(
                    "items" to items,
                    "subtotal" to subtotal,
                    "taxTotal" to taxTotal,
                    "grandTotal" to grandTotal,
                    "labourCharge" to labourCharge,
                    "notes" to notes,
                    "updatedAt" to Date()
                )
            ).await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
