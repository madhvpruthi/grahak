package com.princesanitary.grahak.di

import android.app.Application
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.princesanitary.grahak.data.repository.CartRepository
import com.princesanitary.grahak.data.repository.EstimateRepository
import com.princesanitary.grahak.data.repository.OrderRepository
import com.princesanitary.grahak.data.repository.PlumberRepository
import com.princesanitary.grahak.data.repository.ProductRepository
import com.princesanitary.grahak.data.repository.ReviewRepository
import com.princesanitary.grahak.data.repository.ServiceJobRepository
import com.princesanitary.grahak.data.repository.UserRepository
import com.princesanitary.grahak.data.repository.WalletRepository

/**
 * Single source of application-wide singletons.
 *
 * This replaces Hilt. The app has one module and a fixed, non-generic object
 * graph, so a hand-written locator gives the same lifetime guarantees as
 * `@Singleton` without pulling an annotation processor into every build.
 * Removing kapt/KSP is the single largest build-time and disk win available
 * here.
 *
 * Everything is `by lazy`, so nothing touches Firebase until it is first used
 * (FirebaseApp is initialised by its own ContentProvider before any of this
 * runs).
 */
object ServiceLocator {

    lateinit var application: Application
        private set
    
    fun init(app: Application) {
        application = app
    }

    val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    val storage: FirebaseStorage by lazy { FirebaseStorage.getInstance() }

    val productRepository: ProductRepository by lazy { ProductRepository(firestore) }
    val cartRepository: CartRepository by lazy { CartRepository(auth, firestore) }
    val orderRepository: OrderRepository by lazy { OrderRepository(auth, firestore) }
    val estimateRepository: EstimateRepository by lazy { EstimateRepository(auth, firestore) }
    val plumberRepository: PlumberRepository by lazy { PlumberRepository(auth, firestore, storage) }
    val serviceJobRepository: ServiceJobRepository by lazy {
        ServiceJobRepository(auth, firestore, userRepository)
    }
    val walletRepository: WalletRepository by lazy { WalletRepository(auth, firestore) }
    val userRepository: UserRepository by lazy { UserRepository(auth, firestore) }
    val reviewRepository: ReviewRepository by lazy { ReviewRepository(auth, firestore) }
    val adminRepository: com.princesanitary.grahak.data.repository.admin.AdminRepository by lazy {
        com.princesanitary.grahak.data.repository.admin.AdminRepository(auth, firestore, storage)
    }
}
