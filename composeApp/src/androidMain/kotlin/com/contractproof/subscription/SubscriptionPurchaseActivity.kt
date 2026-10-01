package com.contractproof.subscription

import android.app.Activity
import java.lang.ref.WeakReference

object SubscriptionPurchaseActivity {
    private var activityRef: WeakReference<Activity>? = null

    fun bind(activity: Activity) {
        activityRef = WeakReference(activity)
    }

    fun current(): Activity? = activityRef?.get()
}
