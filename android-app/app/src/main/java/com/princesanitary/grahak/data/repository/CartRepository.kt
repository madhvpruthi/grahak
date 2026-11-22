package com.princesanitary.grahak.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.princesanitary.grahak.data.model.Cart
import com.princesanitary.grahak.data.model.CartItem
import com.princesanitary.grahak.data.model.CartSummary
import com.princesanitary.grahak.data.model.Product
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CartRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {
    private val cartsCollection = firestore.collection("carts")
    
    /**
     * Resolves the current user's ID.
     * If user is not yet signed in (e.g. on first launch), automatically signs in
     * anonymously so that cart items are NEVER lost and can be added immediately.
     */
    suspend fun ensureUserId(): String {
        val current = auth.currentUser
        if (current != null) return current.uid
        return try {
            auth.signInAnonymously().await().user?.uid
                ?: throw Exception("Could not initialize guest session")
        } catch (e: Exception) {
            auth.currentUser?.uid ?: throw e
        }
    }

    private fun getUserCartId(): String? {
        return auth.currentUser?.uid
    }
    
    /**
     * Add product to cart
     */
    suspend fun addToCart(product: Product, quantity: Int = 1): Result<Unit> {
        return try {
            val userId = ensureUserId()
            
            val cartRef = cartsCollection.document(userId)
            val cartDoc = cartRef.get().await()
            
            val currentCart = cartDoc.toObject(Cart::class.java) ?: Cart(
                id = userId,
                userId = userId,
                items = emptyList()
            )
            
            // Check if product already in cart
            val existingItemIndex = currentCart.items.indexOfFirst { it.productId == product.id }
            
            val updatedItems = if (existingItemIndex >= 0) {
                // Update quantity of existing item
                currentCart.items.toMutableList().apply {
                    this[existingItemIndex] = this[existingItemIndex].copy(
                        quantity = this[existingItemIndex].quantity + quantity
                    )
                }
            } else {
                // Add new item
                currentCart.items + CartItem(
                    productId = product.id,
                    productName = product.name,
                    itemNumber = product.displayItemCode,
                    size = product.size,
                    brand = product.brand,
                    quantity = quantity,
                    unitPrice = product.unitPrice,
                    gstRate = product.tax.gstRate,
                    imageUrl = product.mediaUrls.firstOrNull() ?: "",
                    addedAt = Date()
                )
            }
            
            cartRef.set(
                currentCart.copy(
                    items = updatedItems,
                    updatedAt = Date()
                )
            ).await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Update item quantity in cart
     */
    suspend fun updateQuantity(productId: String, newQuantity: Int): Result<Unit> {
        return try {
            val userId = ensureUserId()
            
            if (newQuantity <= 0) {
                return removeFromCart(productId)
            }
            
            val cartRef = cartsCollection.document(userId)
            val cartDoc = cartRef.get().await()
            
            val currentCart = cartDoc.toObject(Cart::class.java) 
                ?: throw Exception("Cart not found")
            
            val updatedItems = currentCart.items.map { item ->
                if (item.productId == productId) {
                    item.copy(quantity = newQuantity)
                } else {
                    item
                }
            }
            
            cartRef.set(
                currentCart.copy(
                    items = updatedItems,
                    updatedAt = Date()
                )
            ).await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Remove item from cart
     */
    suspend fun removeFromCart(productId: String): Result<Unit> {
        return try {
            val userId = ensureUserId()
            
            val cartRef = cartsCollection.document(userId)
            val cartDoc = cartRef.get().await()
            
            val currentCart = cartDoc.toObject(Cart::class.java) 
                ?: throw Exception("Cart not found")
            
            val updatedItems = currentCart.items.filter { it.productId != productId }
            
            cartRef.set(
                currentCart.copy(
                    items = updatedItems,
                    updatedAt = Date()
                )
            ).await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Clear entire cart
     */
    suspend fun clearCart(): Result<Unit> {
        return try {
            val userId = ensureUserId()
            
            cartsCollection.document(userId).set(
                Cart(
                    id = userId,
                    userId = userId,
                    items = emptyList(),
                    updatedAt = Date()
                )
            ).await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get cart with real-time updates.
     * Stays active and reactive across first-start, guest sessions, and user logins.
     */
    fun getCart(): Flow<Result<Cart>> = callbackFlow {
        var currentRegistration: ListenerRegistration? = null
        var activeUserId: String? = null

        fun attachListenerForUser(uid: String) {
            if (activeUserId == uid && currentRegistration != null) return
            activeUserId = uid
            currentRegistration?.remove()
            currentRegistration = cartsCollection.document(uid)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(Result.failure(error))
                        return@addSnapshotListener
                    }
                    
                    val cart = snapshot?.toObject(Cart::class.java) ?: Cart(
                        id = uid,
                        userId = uid
                    )
                    
                    trySend(Result.success(cart))
                }
        }

        val initialUser = auth.currentUser
        if (initialUser != null) {
            attachListenerForUser(initialUser.uid)
        } else {
            trySend(Result.success(Cart()))
            // Initialize anonymous session in background so listener attaches on first start
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val uid = ensureUserId()
                    attachListenerForUser(uid)
                } catch (_: Exception) {}
            }
        }

        val authListener = FirebaseAuth.AuthStateListener { fbAuth ->
            val user = fbAuth.currentUser
            if (user != null) {
                attachListenerForUser(user.uid)
            }
        }
        auth.addAuthStateListener(authListener)

        awaitClose {
            auth.removeAuthStateListener(authListener)
            currentRegistration?.remove()
        }
    }
    
    /**
     * Calculate cart summary.
     * All store product prices are already inclusive of all taxes (GST),
     * so no additional tax is tacked on.
     */
    fun calculateSummary(cart: Cart): CartSummary {
        val itemCount = cart.items.sumOf { it.quantity }
        val subtotal = cart.items.sumOf { it.total }
        val taxTotal = 0.0 // Prices are fully GST inclusive
        val grandTotal = subtotal
        
        return CartSummary(
            itemCount = itemCount,
            subtotal = subtotal,
            taxTotal = taxTotal,
            grandTotal = grandTotal
        )
    }
    
    /**
     * Check if product is in cart
     */
    suspend fun isInCart(productId: String): Boolean {
        return try {
            val userId = getUserCartId() ?: return false
            val cartDoc = cartsCollection.document(userId).get().await()
            val cart = cartDoc.toObject(Cart::class.java) ?: return false
            cart.items.any { it.productId == productId }
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Get item count in cart
     */
    suspend fun getCartItemCount(): Int {
        return try {
            val userId = getUserCartId() ?: return 0
            val cartDoc = cartsCollection.document(userId).get().await()
            val cart = cartDoc.toObject(Cart::class.java) ?: return 0
            cart.items.sumOf { it.quantity }
        } catch (e: Exception) {
            0
        }
    }
}
