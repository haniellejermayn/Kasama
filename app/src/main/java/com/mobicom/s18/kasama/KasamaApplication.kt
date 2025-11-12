package com.mobicom.s18.kasama

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.mobicom.s18.kasama.data.local.KasamaDatabase
import com.mobicom.s18.kasama.data.repository.*
import com.mobicom.s18.kasama.notifications.NotificationHelper
import com.mobicom.s18.kasama.notifications.NotificationScheduler

class KasamaApplication : Application() {

    val database: KasamaDatabase by lazy {
        KasamaDatabase.getDatabase(this)
    }

    val firebaseAuth: FirebaseAuth by lazy {
        FirebaseAuth.getInstance()
    }

    val firestore: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance()
    }

    val authRepository: AuthRepository by lazy {
        AuthRepository(firebaseAuth, firestore, database)
    }

    val userRepository: UserRepository by lazy {
        UserRepository(firestore, database)
    }

    val householdRepository: HouseholdRepository by lazy {
        HouseholdRepository(firestore, database)
    }

    val choreRepository: ChoreRepository by lazy {
        ChoreRepository(firestore, database)
    }

    val noteRepository: NoteRepository by lazy {
        NoteRepository(firestore, database)
    }

    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)

        // Create notification channels - Only for Android 9.0+
        // NOTE: This does not work on Nougat
        NotificationHelper.createNotificationChannels(this)

        // Get FCM token and save it
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                android.util.Log.d("FCM_TOKEN", "Token: $token")
                saveFcmToken(token)
            }
        }

        // Schedule chore reminders if user is logged in
        if (firebaseAuth.currentUser != null) {
            NotificationScheduler.scheduleChoreReminders(this)
        }
    }

    private fun saveFcmToken(token: String) {
        val currentUser = firebaseAuth.currentUser
        if (currentUser != null) {
            firestore.collection("users")
                .document(currentUser.uid)
                .update("fcmToken", token)
                .addOnSuccessListener {
                    android.util.Log.d("FCM_TOKEN", "Token saved to Firestore")
                }
                .addOnFailureListener { e ->
                    android.util.Log.e("FCM_TOKEN", "Failed to save token", e)
                }
        }
    }
}