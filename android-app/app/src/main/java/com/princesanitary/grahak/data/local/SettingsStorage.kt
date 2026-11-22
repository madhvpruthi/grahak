package com.princesanitary.grahak.data.local

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.princesanitary.grahak.data.model.Coupon
import com.princesanitary.grahak.data.model.DiscountType
import com.princesanitary.grahak.data.model.StoreSettings
import com.princesanitary.grahak.di.ServiceLocator
import org.json.JSONArray
import org.json.JSONObject
import java.util.Date
import java.util.UUID

object SettingsStorage {
    private const val TAG = "SettingsStorage"
    private const val PREFS_NAME = "grahak_store_settings"

    private const val KEY_UPI_ID = "owner_upi_id"
    private const val KEY_BUSINESS_NAME = "owner_business_name"
    private const val KEY_PHONE = "owner_phone"
    private const val KEY_FREE_THRESHOLD = "free_delivery_threshold"
    private const val KEY_DELIVERY_FEE = "delivery_fee"
    private const val KEY_PLUMBER_COMMISSION = "plumber_commission_rate"
    private const val KEY_FESTIVAL_HEADLINE = "festival_headline"
    private const val KEY_IS_FESTIVAL_ACTIVE = "is_festival_banner_active"
    private const val KEY_FESTIVAL_TAG = "festival_tag"
    private const val KEY_IS_STORE_OPEN = "is_store_open"
    private const val KEY_NEXT_DISPATCH = "next_dispatch_schedule"
    private const val KEY_CLOSED_NOTICE = "store_closed_notice"
    private const val KEY_HAS_CUSTOM = "has_custom_settings"
    private const val KEY_COUPONS_JSON = "cached_coupons_json"
    private const val KEY_OWNER_SECURITY_PIN = "owner_security_pin"
    private const val KEY_OWNER_PIN_UNLOCKED = "owner_pin_unlocked_session"

    private fun getPrefs(context: Context? = null): SharedPreferences {
        val ctx = context ?: ServiceLocator.application
        return ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    // ------------------------------------------------------------- STORE SETTINGS

    fun cacheStoreSettings(settings: StoreSettings, context: Context? = null) {
        try {
            getPrefs(context).edit()
                .putString(KEY_UPI_ID, settings.ownerUpiId)
                .putString(KEY_BUSINESS_NAME, settings.ownerBusinessName)
                .putString(KEY_PHONE, settings.ownerPhone)
                .putFloat(KEY_FREE_THRESHOLD, settings.freeDeliveryThreshold.toFloat())
                .putFloat(KEY_DELIVERY_FEE, settings.deliveryFee.toFloat())
                .putFloat(KEY_PLUMBER_COMMISSION, settings.plumberCommissionRate.toFloat())
                .putString(KEY_FESTIVAL_HEADLINE, settings.festivalHeadline)
                .putBoolean(KEY_IS_FESTIVAL_ACTIVE, settings.isFestivalBannerActive)
                .putString(KEY_FESTIVAL_TAG, settings.festivalTag)
                .putBoolean(KEY_IS_STORE_OPEN, settings.isStoreOpen)
                .putString(KEY_NEXT_DISPATCH, settings.nextDispatchSchedule)
                .putString(KEY_CLOSED_NOTICE, settings.storeClosedNotice)
                .putBoolean(KEY_HAS_CUSTOM, true)
                .apply()
            Log.d(TAG, "Successfully cached store settings locally: upi=${settings.ownerUpiId}, threshold=${settings.freeDeliveryThreshold}, fee=${settings.deliveryFee}, festiveActive=${settings.isFestivalBannerActive}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cache store settings: ${e.message}", e)
        }
    }

    fun hasCustomSettings(context: Context? = null): Boolean {
        return getPrefs(context).getBoolean(KEY_HAS_CUSTOM, false)
    }

    fun getCachedStoreSettings(context: Context? = null): StoreSettings {
        val prefs = getPrefs(context)
        return try {
            val upi = prefs.getString(KEY_UPI_ID, "") ?: ""
            val name = prefs.getString(KEY_BUSINESS_NAME, "") ?: ""
            val phone = prefs.getString(KEY_PHONE, "") ?: ""
            val threshold = prefs.getFloat(KEY_FREE_THRESHOLD, 0f).toDouble()
            val fee = prefs.getFloat(KEY_DELIVERY_FEE, 0f).toDouble()
            val comm = prefs.getFloat(KEY_PLUMBER_COMMISSION, 0f).toDouble()
            val festHeadline = prefs.getString(KEY_FESTIVAL_HEADLINE, "") ?: ""
            val festActive = prefs.getBoolean(KEY_IS_FESTIVAL_ACTIVE, false)
            val festTag = prefs.getString(KEY_FESTIVAL_TAG, "") ?: ""
            val storeOpen = prefs.getBoolean(KEY_IS_STORE_OPEN, true)
            val nextDispatch = prefs.getString(KEY_NEXT_DISPATCH, "") ?: ""
            val closedNotice = prefs.getString(KEY_CLOSED_NOTICE, "") ?: ""

            StoreSettings(
                id = "config",
                ownerUpiId = upi,
                ownerBusinessName = name,
                ownerPhone = phone,
                freeDeliveryThreshold = threshold,
                deliveryFee = fee,
                plumberCommissionRate = comm,
                festivalHeadline = festHeadline,
                isFestivalBannerActive = festActive,
                festivalTag = festTag,
                isStoreOpen = storeOpen,
                nextDispatchSchedule = nextDispatch,
                storeClosedNotice = closedNotice,
                updatedAt = Date()
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read cached store settings: ${e.message}", e)
            StoreSettings()
        }
    }

    // ------------------------------------------------------------- OWNER PIN AUTH

    fun getOwnerSecurityPin(context: Context? = null): String {
        return getPrefs(context).getString(KEY_OWNER_SECURITY_PIN, "246810") ?: "246810"
    }

    fun setOwnerSecurityPin(pin: String, context: Context? = null) {
        getPrefs(context).edit().putString(KEY_OWNER_SECURITY_PIN, pin.trim()).apply()
    }

    fun isOwnerPinSessionUnlocked(context: Context? = null): Boolean {
        return getPrefs(context).getBoolean(KEY_OWNER_PIN_UNLOCKED, false)
    }

    fun setOwnerPinSessionUnlocked(unlocked: Boolean, context: Context? = null) {
        getPrefs(context).edit().putBoolean(KEY_OWNER_PIN_UNLOCKED, unlocked).apply()
    }

    // ------------------------------------------------------------------- COUPONS

    private fun getStarterCoupons(): List<Coupon> {
        return emptyList()
    }

    @Synchronized
    fun getCachedCoupons(context: Context? = null): List<Coupon> {
        val prefs = getPrefs(context)
        val jsonStr = prefs.getString(KEY_COUPONS_JSON, null)
        if (jsonStr.isNullOrBlank()) {
            val defaults = getStarterCoupons()
            cacheCoupons(defaults, context)
            return defaults
        }

        return try {
            val array = JSONArray(jsonStr)
            val list = mutableListOf<Coupon>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    Coupon(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        code = obj.optString("code", "").uppercase(),
                        discountType = obj.optString("discountType", DiscountType.PERCENTAGE),
                        discountValue = obj.optDouble("discountValue", 10.0),
                        minOrderAmount = obj.optDouble("minOrderAmount", 0.0),
                        maxDiscountAmount = obj.optDouble("maxDiscountAmount", 0.0),
                        isActive = obj.optBoolean("isActive", true),
                        description = obj.optString("description", "")
                    )
                )
            }
            list
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse cached coupons: ${e.message}", e)
            getStarterCoupons()
        }
    }

    @Synchronized
    fun cacheCoupons(coupons: List<Coupon>, context: Context? = null) {
        try {
            val array = JSONArray()
            for (c in coupons) {
                val obj = JSONObject().apply {
                    put("id", c.id)
                    put("code", c.code.uppercase().trim())
                    put("discountType", c.discountType)
                    put("discountValue", c.discountValue)
                    put("minOrderAmount", c.minOrderAmount)
                    put("maxDiscountAmount", c.maxDiscountAmount)
                    put("isActive", c.isActive)
                    put("description", c.description)
                }
                array.put(obj)
            }
            getPrefs(context).edit().putString(KEY_COUPONS_JSON, array.toString()).apply()
            Log.d(TAG, "Cached ${coupons.size} coupons locally")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to serialize coupons: ${e.message}", e)
        }
    }

    @Synchronized
    fun saveCoupon(coupon: Coupon, context: Context? = null): Coupon {
        val list = getCachedCoupons(context).toMutableList()
        val finalCoupon = if (coupon.id.isBlank()) {
            coupon.copy(id = "coupon_${System.currentTimeMillis()}")
        } else {
            coupon
        }
        val idx = list.indexOfFirst { it.id == finalCoupon.id || it.code.equals(finalCoupon.code, ignoreCase = true) }
        if (idx >= 0) {
            list[idx] = finalCoupon
        } else {
            list.add(finalCoupon)
        }
        cacheCoupons(list, context)
        return finalCoupon
    }

    @Synchronized
    fun deleteCoupon(couponId: String, context: Context? = null) {
        val list = getCachedCoupons(context).filterNot { it.id == couponId }
        cacheCoupons(list, context)
    }

    fun findCouponByCode(code: String, context: Context? = null): Coupon? {
        val clean = code.trim().uppercase()
        return getCachedCoupons(context).firstOrNull { it.code.equals(clean, ignoreCase = true) && it.isActive }
    }
}
