package com.contractproof.core.analytics

import android.content.Context
import android.util.Log
import com.posthog.android.PostHogAndroid
import com.posthog.android.PostHogAndroidConfig

object PostHogInitializer {
    private const val TAG = "PostHog"

    fun configure(applicationContext: Context): Boolean {
        val apiKey = PostHogLocalConfig.apiKey.trim()
        if (apiKey.isEmpty()) {
            Log.i(TAG, "API key missing; PostHog disabled")
            return false
        }
        return try {
            val config = PostHogAndroidConfig(
                apiKey = apiKey,
                host = PostHogLocalConfig.host,
            )
            PostHogAndroid.setup(applicationContext, config)
            Log.i(TAG, "PostHog configured")
            true
        } catch (error: Throwable) {
            Log.e(TAG, "PostHog configure failed", error)
            false
        }
    }
}
