package com.mobicom.s18.kasama.notifications

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.mobicom.s18.kasama.KasamaApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// TODO: Only notify when another person is assigned, don't notify self-assignment
class KasamaFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Refreshed token: $token")

        // Send token to your server or save to Firestore
        sendTokenToServer(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        Log.d(TAG, "Message received from: ${message.from}")

        // Check if message contains a data payload
        if (message.data.isNotEmpty()) {
            Log.d(TAG, "Message data payload: ${message.data}")
            handleDataMessage(message.data)
        }

        // Check if message contains a notification payload
        message.notification?.let {
            Log.d(TAG, "Message Notification Body: ${it.body}")
            showNotification(it.title ?: "Kasama", it.body ?: "")
        }
    }

    private fun handleDataMessage(data: Map<String, String>) {
        val type = data["type"]
        val title = data["title"] ?: "Kasama"
        val message = data["message"] ?: ""

        when (type) {
            "chore_assigned" -> {
                val choreId = data["chore_id"] ?: ""
                val choreTitle = data["chore_title"] ?: ""
                val dueDate = data["due_date"] ?: ""

                NotificationHelper.showChoreReminderNotification(
                    context = this,
                    choreId = choreId,
                    choreTitle = choreTitle,
                    dueDate = dueDate,
                    isOverdue = false
                )
            }
            "chore_completed" -> {
                NotificationHelper.showHouseholdNotification(
                    context = this,
                    title = title,
                    message = message
                )
            }
            "new_member" -> {
                NotificationHelper.showHouseholdNotification(
                    context = this,
                    title = title,
                    message = message
                )
            }
            "new_note" -> {
                NotificationHelper.showHouseholdNotification(
                    context = this,
                    title = title,
                    message = message
                )
            }
            else -> {
                NotificationHelper.showGeneralNotification(
                    context = this,
                    title = title,
                    message = message
                )
            }
        }
    }

    private fun showNotification(title: String, message: String) {
        NotificationHelper.showGeneralNotification(
            context = this,
            title = title,
            message = message
        )
    }

    private fun sendTokenToServer(token: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val app = application as KasamaApplication
                val currentUser = app.firebaseAuth.currentUser

                if (currentUser != null) {
                    // Save FCM token to Firestore user document
                    app.firestore.collection("users")
                        .document(currentUser.uid)
                        .update("fcmToken", token)
                        .addOnSuccessListener {
                            Log.d(TAG, "FCM token saved to Firestore")
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "Failed to save FCM token", e)
                        }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error sending token to server", e)
            }
        }
    }

    companion object {
        private const val TAG = "KasamaFCM"
    }
}