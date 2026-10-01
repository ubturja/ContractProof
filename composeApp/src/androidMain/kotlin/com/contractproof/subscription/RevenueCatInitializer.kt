package com.contractproof.subscription

import android.content.Context
import android.util.Log
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesConfiguration

object RevenueCatInitializer {
    private const val TAG = "RevenueCat"

    fun configure(applicationContext: Context): Boolean {
        val apiKey = RevenueCatLocalConfig.apiKey.trim()
        if (apiKey.isEmpty()) {
            Log.i(TAG, "API key missing; RevenueCat disabled")
            return false
        }
        return try {
            Purchases.configure(
                PurchasesConfiguration.Builder(applicationContext, apiKey).build(),
            )
            Log.i(TAG, "RevenueCat configured")
            true
        } catch (error: Throwable) {
            Log.e(TAG, "RevenueCat configure failed", error)
            false
        }
    }
}
