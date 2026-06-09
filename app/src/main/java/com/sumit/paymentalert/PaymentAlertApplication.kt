package com.sumit.paymentalert

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

class PaymentAlertApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            // Programmatic Firebase setup using the user's Google Services credentials.
            // Safe from any mismatching package configurations or keystore requirements.
            val options = FirebaseOptions.Builder()
                .setApplicationId("1:469595226485:android:e0d713bbf16df11caa533c")
                .setProjectId("payment-86d79")
                .setDatabaseUrl("https://payment-86d79-default-rtdb.firebaseio.com")
                .setApiKey("AIzaSyAipa_pOTnAfF3QJp2eO4P1xVu14YwELc0")
                .build()
            FirebaseApp.initializeApp(this, options)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
