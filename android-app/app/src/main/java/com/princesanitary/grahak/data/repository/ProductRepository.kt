package com.princesanitary.grahak.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.princesanitary.grahak.data.model.Product
import com.princesanitary.grahak.data.model.ProductCategory
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProductRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    
    private val catalogCollection = firestore.collection("catalog")
    
    /**
     * Get all published products as a Flow
     */
    fun getPublishedProducts(): Flow<Result<List<Product>>> = callbackFlow {
        val listener = catalogCollection
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }
                
                val products = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        doc.toObject(Product::class.java)?.copy(id = doc.id)
                    } catch (e: Exception) {
                        null
                    }
                }?.filter { it.isPublished }
                 ?.sortedBy { it.name.lowercase() } ?: emptyList()
                
                trySend(Result.success(products))
            }
        
        awaitClose { listener.remove() }
    }
    
    /**
     * Get products by category
     */
    fun getProductsByCategory(categoryId: String): Flow<Result<List<Product>>> = callbackFlow {
        val listener = catalogCollection
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }
                
                val products = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        doc.toObject(Product::class.java)?.copy(id = doc.id)
                    } catch (e: Exception) {
                        null
                    }
                }?.filter { it.isPublished && (categoryId.isBlank() || it.categoryId.equals(categoryId, ignoreCase = true)) }
                 ?.sortedBy { it.name.lowercase() } ?: emptyList()
                
                trySend(Result.success(products))
            }
        
        awaitClose { listener.remove() }
    }
    
    /**
     * Get single product by ID
     */
    suspend fun getProductById(productId: String): Result<Product> {
        return try {
            val snapshot = catalogCollection.document(productId).get().await()
            val product = snapshot.toObject(Product::class.java)
            
            if (product != null && product.isPublished) {
                Result.success(product)
            } else {
                Result.failure(Exception("Product not found or not published"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Search products by name
     */
    suspend fun searchProducts(query: String): Result<List<Product>> {
        return try {
            val snapshot = catalogCollection
                .whereEqualTo("isPublished", true)
                .orderBy("name")
                .startAt(query)
                .endAt(query + "\uf8ff")
                .get()
                .await()
            
            val products = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Product::class.java)
            }
            
            Result.success(products)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get featured/top products
     */
    suspend fun getFeaturedProducts(limit: Int = 10): Result<List<Product>> {
        return try {
            val snapshot = catalogCollection
                .whereEqualTo("isPublished", true)
                .limit(limit.toLong())
                .get()
                .await()
            
            val products = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Product::class.java)
            }
            
            Result.success(products)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
