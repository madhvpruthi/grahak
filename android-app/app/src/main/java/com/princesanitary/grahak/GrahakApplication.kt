package com.princesanitary.grahak

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.princesanitary.grahak.di.ServiceLocator

class GrahakApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Initialize ServiceLocator with Application context
        ServiceLocator.init(this)

        // Initialize ThemeManager
        com.princesanitary.grahak.ui.theme.ThemeManager.init(this)
        
        // Enable offline persistence explicitly
        val firestore = FirebaseFirestore.getInstance()
        val settings = FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true)
            .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
            .build()
        firestore.firestoreSettings = settings
    }
    
    companion object {
        private const val PREFS_NAME = "grahak_prefs"
        private const val KEY_USER_ROLE = "cached_user_role"
        
        fun cacheUserRole(context: Context, uid: String, role: String) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString("${KEY_USER_ROLE}_$uid", role)
                .apply()
        }
        
        fun getCachedUserRole(context: Context, uid: String): String? {
            return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString("${KEY_USER_ROLE}_$uid", null)
        }
        
        fun clearCachedUserRole(context: Context, uid: String) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .remove("${KEY_USER_ROLE}_$uid")
                .apply()
        }
    }
}

