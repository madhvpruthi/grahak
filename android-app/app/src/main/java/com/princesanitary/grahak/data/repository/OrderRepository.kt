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
class OrderRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {
    
    private val ordersCollection = firestore.collection("orders")
    private val plumbersCollection = firestore.collection("plumbers")
    
    /**
     * Check if currently signed-in user is a verified plumber
     */
    suspend fun isCurrentUserPlumber(): Boolean {
        val uid = auth.currentUser?.uid ?: return false
        return try {
            plumbersCollection.document(uid).get().await().exists()
        } catch (_: Exception) {
            false
        }
    }

    private fun parseStoreSettingsSnapshot(snap: com.google.firebase.firestore.DocumentSnapshot): com.princesanitary.grahak.data.model.StoreSettings {
        val remote = try {
            snap.toObject(com.princesanitary.grahak.data.model.StoreSettings::class.java)
        } catch (_: Exception) {
            null
        }

        val phone = snap.getString("ownerPhone") ?: remote?.ownerPhone.orEmpty()
        val bizName = snap.getString("ownerBusinessName") ?: remote?.ownerBusinessName.orEmpty()
        val upiId = snap.getString("ownerUpiId") ?: remote?.ownerUpiId.orEmpty()

        val freeThreshold = snap.getDouble("freeDeliveryThreshold")
            ?: (snap.getLong("freeDeliveryThreshold")?.toDouble())
            ?: remote?.freeDeliveryThreshold
            ?: 0.0

        val fee = snap.getDouble("deliveryFee")
            ?: (snap.getLong("deliveryFee")?.toDouble())
            ?: remote?.deliveryFee
            ?: 0.0

        val plumberComm = snap.getDouble("plumberCommissionRate")
            ?: (snap.getLong("plumberCommissionRate")?.toDouble())
            ?: remote?.plumberCommissionRate
            ?: 0.0

        val rawOpen = snap.getBoolean("isStoreOpen")
            ?: snap.getBoolean("storeOpen")
            ?: remote?.isStoreOpen
            ?: true

        val festHeadline = snap.getString("festivalHeadline") ?: remote?.festivalHeadline.orEmpty()
        val festTag = snap.getString("festivalTag") ?: remote?.festivalTag.orEmpty()
        val rawFestive = snap.getBoolean("isFestivalBannerActive")
            ?: snap.getBoolean("festivalBannerActive")
            ?: remote?.isFestivalBannerActive
            ?: false

        val nextDispatch = snap.getString("nextDispatchSchedule") ?: remote?.nextDispatchSchedule.orEmpty()
        val closedNotice = snap.getString("storeClosedNotice") ?: remote?.storeClosedNotice.orEmpty()

        return com.princesanitary.grahak.data.model.StoreSettings(
            id = "config",
            ownerUpiId = upiId,
            ownerBusinessName = bizName,
            ownerPhone = phone,
            freeDeliveryThreshold = freeThreshold,
            deliveryFee = fee,
            plumberCommissionRate = plumberComm,
            isStoreOpen = rawOpen,
            nextDispatchSchedule = nextDispatch,
            storeClosedNotice = closedNotice,
            isFestivalBannerActive = rawFestive,
            festivalTag = festTag,
            festivalHeadline = festHeadline,
            updatedAt = snap.getDate("updatedAt") ?: Date()
        )
    }

    suspend fun getStoreSettings(): com.princesanitary.grahak.data.model.StoreSettings {
        return try {
            val snap = firestore.collection("store_settings").document("config").get().await()
            if (snap != null && snap.exists()) {
                val resolved = parseStoreSettingsSnapshot(snap)
                com.princesanitary.grahak.data.local.SettingsStorage.cacheStoreSettings(resolved)
                resolved
            } else {
                com.princesanitary.grahak.data.local.SettingsStorage.getCachedStoreSettings()
            }
        } catch (_: Exception) {
            com.princesanitary.grahak.data.local.SettingsStorage.getCachedStoreSettings()
        }
    }

    fun observeStoreSettings(): Flow<com.princesanitary.grahak.data.model.StoreSettings> = callbackFlow {
        trySend(com.princesanitary.grahak.data.local.SettingsStorage.getCachedStoreSettings())
        val listener = firestore.collection("store_settings").document("config")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(com.princesanitary.grahak.data.local.SettingsStorage.getCachedStoreSettings())
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    val resolved = parseStoreSettingsSnapshot(snapshot)
                    com.princesanitary.grahak.data.local.SettingsStorage.cacheStoreSettings(resolved)
                    trySend(resolved)
                    return@addSnapshotListener
                }
                trySend(com.princesanitary.grahak.data.local.SettingsStorage.getCachedStoreSettings())
            }
        awaitClose { listener.remove() }
    }

    suspend fun validateCoupon(code: String, subtotal: Double): Result<com.princesanitary.grahak.data.model.Coupon> {
        val cleanCode = code.trim().uppercase()
        // 1. Try remote Firestore first
        try {
            val snap = firestore.collection("coupons")
                .whereEqualTo("code", cleanCode)
                .whereEqualTo("isActive", true)
                .get().await()

            val remoteCoupon = snap.documents.firstOrNull()?.toObject(com.princesanitary.grahak.data.model.Coupon::class.java)
            if (remoteCoupon != null) {
                if (subtotal < remoteCoupon.minOrderAmount) {
                    return Result.failure(Exception("Minimum order of ₹${"%.0f".format(remoteCoupon.minOrderAmount)} required for this coupon"))
                }
                return Result.success(remoteCoupon)
            }
        } catch (e: Exception) {
            android.util.Log.w("OrderRepository", "Firestore coupon validation fallback to local storage: ${e.message}")
        }

        // 2. Fallback to local storage (SettingsStorage)
        val localCoupon = com.princesanitary.grahak.data.local.SettingsStorage.findCouponByCode(cleanCode)
        if (localCoupon != null) {
            if (subtotal < localCoupon.minOrderAmount) {
                return Result.failure(Exception("Minimum order of ₹${"%.0f".format(localCoupon.minOrderAmount)} required for this coupon"))
            }
            return Result.success(localCoupon)
        }

        return Result.failure(Exception("Coupon code '$code' is not valid or expired"))
    }

    /**
     * Retrieve all active store coupons available for customers from Firestore.
     */
    suspend fun getActiveCoupons(): List<com.princesanitary.grahak.data.model.Coupon> {
        return try {
            val snap = firestore.collection("coupons")
                .whereEqualTo("isActive", true)
                .get().await()
            val remoteList = snap.documents.mapNotNull { it.toObject(com.princesanitary.grahak.data.model.Coupon::class.java) }
            if (remoteList.isNotEmpty()) {
                com.princesanitary.grahak.data.local.SettingsStorage.cacheCoupons(remoteList)
            }
            remoteList
        } catch (_: Exception) {
            com.princesanitary.grahak.data.local.SettingsStorage.getCachedCoupons()
        }
    }

    /**
     * Create order from shopping cart
     */
    suspend fun createOrder(
        items: List<OrderItem>,
        deliveryAddress: SavedAddress,
        deliveryType: DeliveryType,
        paymentMethod: PaymentMethod,
        customerName: String = "",
        customerPhone: String = "",
        deliveryFee: Double = 0.0,
        discountAmount: Double = 0.0,
        couponCode: String = "",
        notes: String = ""
    ): Result<String> {
        return try {
            val userId = auth.currentUser?.uid ?: throw Exception("User not authenticated")
            val settings = getStoreSettings()
            
            // Calculate totals (Prices are fully GST inclusive - no extra tax added)
            val subtotal = items.sumOf { it.total }
            val taxTotal = 0.0
            val grandTotal = (subtotal + deliveryFee - discountAmount).coerceAtLeast(0.0)

            // Check if placing user is a registered plumber
            val plumberDoc = try {
                plumbersCollection.document(userId).get().await()
            } catch (_: Exception) {
                null
            }
            val isPlumber = plumberDoc != null && plumberDoc.exists()
            val plumberName = if (isPlumber) plumberDoc.getString("name").orEmpty() else ""
            val plumberPhone = if (isPlumber) plumberDoc.getString("phone").orEmpty() else ""

            val commissionRatePercent = (settings.plumberCommissionRate).coerceAtLeast(1.0)
            val plumberCommission = if (isPlumber) {
                Math.round(subtotal * (commissionRatePercent / 100.0) * 100.0) / 100.0
            } else {
                0.0
            }

            val finalCustomerName = customerName.ifBlank { plumberName.ifBlank { auth.currentUser?.displayName.orEmpty() } }
            val finalCustomerPhone = customerPhone.ifBlank { plumberPhone.ifBlank { auth.currentUser?.phoneNumber.orEmpty() } }

            val orderNotes = if (isPlumber && plumberCommission > 0) {
                if (notes.isNotBlank()) "$notes\n[${commissionRatePercent.toInt()}% Plumber Self-Order Incentive: ₹$plumberCommission]"
                else "[${commissionRatePercent.toInt()}% Plumber Self-Order Incentive: ₹$plumberCommission]"
            } else {
                notes
            }
            
            val serialNum = "PSS-${(System.currentTimeMillis() / 1000 % 90000 + 10000)}"

            val order = Order(
                orderNumber = serialNum,
                customerId = userId,
                customerName = finalCustomerName,
                customerPhone = finalCustomerPhone,
                attributedPlumberId = if (isPlumber) userId else null,
                attributedPlumberName = plumberName,
                source = OrderSource.DIRECT_CHECKOUT,
                status = OrderStatus.PLACED,
                items = items,
                billing = BillingInfo(
                    subtotal = subtotal,
                    taxTotal = taxTotal,
                    deliveryFee = deliveryFee,
                    discountAmount = discountAmount,
                    couponCode = couponCode,
                    grandTotal = grandTotal
                ),
                delivery = DeliveryInfo(
                    type = deliveryType,
                    address = deliveryAddress,
                    status = DeliveryStatus.PACKING
                ),
                payment = PaymentInfo(
                    method = paymentMethod,
                    status = if (paymentMethod == PaymentMethod.COD) 
                        PaymentStatus.PENDING 
                    else 
                        PaymentStatus.PENDING
                ),
                plumberCommission = plumberCommission,
                notes = orderNotes,
                createdAt = Date(),
                updatedAt = Date()
            )
            
            val docRef = ordersCollection.add(order).await()

            // Credit the plumber incentive directly to the plumber's wallet
            if (isPlumber && plumberCommission > 0) {
                releaseCommissionToPlumber(userId, plumberCommission, docRef.id)
            }

            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Create order from approved estimate
     */
    suspend fun createOrderFromEstimate(
        estimate: Estimate,
        jobId: String,
        deliveryAddress: SavedAddress
    ): Result<String> {
        return try {
            val userId = auth.currentUser?.uid ?: throw Exception("User not authenticated")
            
            // Convert estimate items to order items
            val orderItems = estimate.items.map { item ->
                OrderItem(
                    productId = item.productId,
                    name = item.productName,
                    itemNumber = "PS-${1000 + (kotlin.math.abs(item.productId.hashCode()) % 9000)}",
                    qty = item.quantity,
                    unitPrice = item.unitPrice,
                    gstRate = item.gstRate,
                    total = item.total
                )
            }
            
            // Calculate plumber commission (3-5% of materials + 100% labour)
            val materialCommission = estimate.subtotal * 0.04 // 4% average
            val labourCommission = estimate.labourCharge // 100%
            val totalCommission = materialCommission + labourCommission
            
            val order = Order(
                orderNumber = "PSS-${(System.currentTimeMillis() / 1000 % 90000 + 10000)}",
                customerId = estimate.customerId,
                attributedPlumberId = estimate.plumberId,
                linkedJobId = jobId,
                linkedEstimateId = estimate.id,
                source = OrderSource.PLUMBER_ESTIMATE,
                status = OrderStatus.PLACED,
                items = orderItems,
                billing = BillingInfo(
                    subtotal = estimate.subtotal,
                    taxTotal = estimate.taxTotal,
                    grandTotal = estimate.grandTotal
                ),
                delivery = DeliveryInfo(
                    type = DeliveryType.SITE_DROP, // Estimate orders are always site drops
                    address = deliveryAddress,
                    status = DeliveryStatus.PACKING
                ),
                payment = PaymentInfo(
                    method = PaymentMethod.UPI,
                    status = PaymentStatus.COMPLETED // Assumed paid during estimate approval
                ),
                plumberCommission = totalCommission,
                notes = estimate.notes,
                createdAt = Date(),
                updatedAt = Date()
            )
            
            val docRef = ordersCollection.add(order).await()
            
            // Update estimate status
            firestore.collection("estimates")
                .document(estimate.id)
                .update(
                    mapOf(
                        "status" to EstimateStatus.CONVERTED_TO_ORDER.name,
                        "updatedAt" to Date()
                    )
                ).await()
            
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Update order status (Admin function - but available for testing)
     */
    suspend fun updateOrderStatus(orderId: String, newStatus: OrderStatus): Result<Unit> {
        return try {
            val updates = mutableMapOf<String, Any>(
                "status" to newStatus.name,
                "updatedAt" to Date()
            )
            
            if (newStatus == OrderStatus.DELIVERED) {
                updates["completedAt"] = Date()
            }
            
            ordersCollection.document(orderId).update(updates).await()
            
            // If order is delivered and has plumber commission, release to plumber wallet
            if (newStatus == OrderStatus.DELIVERED) {
                val order = getOrderById(orderId)
                if (order != null && !order.attributedPlumberId.isNullOrBlank() && order.plumberCommission > 0 && !order.commissionReleased) {
                    releaseCommissionToPlumber(order.attributedPlumberId!!, order.plumberCommission, orderId)
                }
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Update payment status
     */
    suspend fun updatePaymentStatus(
        orderId: String,
        status: PaymentStatus,
        transactionId: String = ""
    ): Result<Unit> {
        return try {
            ordersCollection.document(orderId).update(
                mapOf(
                    "payment.status" to status.name,
                    "payment.transactionId" to transactionId,
                    "updatedAt" to Date()
                )
            ).await()
            
            // Auto-confirm order when payment completes
            if (status == PaymentStatus.COMPLETED) {
                updateOrderStatus(orderId, OrderStatus.CONFIRMED)
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get customer's orders with live updates that survive auth lifecycle changes.
     */
    fun getCustomerOrders(): Flow<Result<List<Order>>> = callbackFlow {
        var listenerRegistration: com.google.firebase.firestore.ListenerRegistration? = null

        fun attachListener(uid: String) {
            listenerRegistration?.remove()
            listenerRegistration = ordersCollection
                .whereEqualTo("customerId", uid)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(Result.failure(error))
                        return@addSnapshotListener
                    }
                    val orders = snapshot?.documents?.mapNotNull { doc ->
                        try {
                            doc.toObject(Order::class.java)?.copy(id = doc.id)
                        } catch (e: Exception) {
                            null
                        }
                    }?.sortedByDescending { it.createdAt.time } ?: emptyList()
                    trySend(Result.success(orders))
                }
        }

        val initialUid = auth.currentUser?.uid
        if (!initialUid.isNullOrBlank()) {
            attachListener(initialUid)
        } else {
            trySend(Result.success(emptyList()))
        }

        val authStateListener = com.google.firebase.auth.FirebaseAuth.AuthStateListener { firebaseAuth ->
            val uid = firebaseAuth.currentUser?.uid
            if (!uid.isNullOrBlank()) {
                attachListener(uid)
            }
        }
        auth.addAuthStateListener(authStateListener)

        awaitClose {
            auth.removeAuthStateListener(authStateListener)
            listenerRegistration?.remove()
        }
    }

    /**
     * Get orders attributed to the current plumber (materials / Digital Parchi orders)
     */
    fun getPlumberOrders(): Flow<Result<List<Order>>> = callbackFlow {
        var listenerRegistration: com.google.firebase.firestore.ListenerRegistration? = null

        fun attachListener(uid: String) {
            listenerRegistration?.remove()
            listenerRegistration = ordersCollection
                .whereEqualTo("attributedPlumberId", uid)
                .limit(50)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(Result.failure(error))
                        return@addSnapshotListener
                    }
                    val orders = snapshot?.documents?.mapNotNull { doc ->
                        try {
                            doc.toObject(Order::class.java)?.copy(id = doc.id)
                        } catch (e: Exception) {
                            null
                        }
                    }?.sortedByDescending { it.createdAt.time } ?: emptyList()
                    trySend(Result.success(orders))
                }
        }

        val initialUid = auth.currentUser?.uid
        if (!initialUid.isNullOrBlank()) {
            attachListener(initialUid)
        } else {
            trySend(Result.success(emptyList()))
        }

        val authStateListener = com.google.firebase.auth.FirebaseAuth.AuthStateListener { firebaseAuth ->
            val uid = firebaseAuth.currentUser?.uid
            if (!uid.isNullOrBlank()) {
                attachListener(uid)
            }
        }
        auth.addAuthStateListener(authStateListener)

        awaitClose {
            auth.removeAuthStateListener(authStateListener)
            listenerRegistration?.remove()
        }
    }

    /**
     * Get orders linked to a specific service job (e.g. material parchi orders)
     */
    fun getOrdersForJob(jobId: String): Flow<Result<List<Order>>> = callbackFlow {
        val listener = ordersCollection
            .whereEqualTo("linkedJobId", jobId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }
                val orders = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        doc.toObject(Order::class.java)?.copy(id = doc.id)
                    } catch (e: Exception) {
                        null
                    }
                }?.sortedByDescending { it.createdAt.time } ?: emptyList()
                trySend(Result.success(orders))
            }
        awaitClose { listener.remove() }
    }
    
    /**
     * Get single order with real-time updates
     */
    fun getOrder(orderId: String): Flow<Result<Order?>> = callbackFlow {
        val listener = ordersCollection.document(orderId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }
                
                val order = snapshot?.toObject(Order::class.java)
                trySend(Result.success(order))
            }
        
        awaitClose { listener.remove() }
    }
    
    /**
     * Get order by ID (one-time fetch)
     */
    private suspend fun getOrderById(orderId: String): Order? {
        return try {
            ordersCollection.document(orderId).get().await()
                .toObject(Order::class.java)
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Cancel order (only if not yet shipped)
     */
    suspend fun cancelOrder(orderId: String, reason: String): Result<Unit> {
        return try {
            val order = getOrderById(orderId)
            
            if (order == null) {
                throw Exception("Order not found")
            }
            
            // Can only cancel before shipping
            if (order.status == OrderStatus.SHIPPED || order.status == OrderStatus.DELIVERED) {
                throw Exception("Cannot cancel order that has been shipped")
            }
            
            ordersCollection.document(orderId).update(
                mapOf(
                    "status" to OrderStatus.CANCELLED.name,
                    "notes" to "${order.notes}\nCancellation reason: $reason",
                    "updatedAt" to Date()
                )
            ).await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Release commission to plumber wallet
     */
    private suspend fun releaseCommissionToPlumber(
        plumberId: String,
        amount: Double,
        orderId: String
    ) {
        try {
            // Mark commission released on the order first
            ordersCollection.document(orderId).update(
                mapOf(
                    "commissionReleased" to true,
                    "updatedAt" to Date()
                )
            ).await()

            // Update withdrawable balance & total earnings atomically
            plumbersCollection.document(plumberId).update(
                mapOf(
                    "wallet.withdrawableBalance" to com.google.firebase.firestore.FieldValue.increment(amount),
                    "wallet.totalEarnings" to com.google.firebase.firestore.FieldValue.increment(amount),
                    "wallet.updatedAt" to Date()
                )
            ).await()
            
            // Create transaction record
            firestore.collection("transactions").add(
                mapOf(
                    "plumberId" to plumberId,
                    "type" to com.princesanitary.grahak.data.model.TransactionType.COMMISSION_RELEASED.name,
                    "amount" to amount,
                    "orderId" to orderId,
                    "status" to "COMPLETED",
                    "description" to "Commission for Order #${orderId.takeLast(4).uppercase()}",
                    "createdAt" to Date()
                )
            ).await()
        } catch (e: Exception) {
            // Log error but don't fail the order update
            e.printStackTrace()
        }
    }
}
