package com.contractproof.app

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp

class ContractProofApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(this)
                Log.i("FCM", "Firebase initialized")
            }
        } catch (error: Throwable) {
            Log.i("FCM", "Firebase not configured: ${error.message}")
        }
    }
}
