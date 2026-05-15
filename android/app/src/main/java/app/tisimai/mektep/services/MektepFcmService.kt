package app.tisimai.mektep.services

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * Firebase Cloud Messaging service.
 * Handles push notifications from the parent device:
 * - BONUS_TIME: parent granted extra screen time
 * - CONFIG_CHANGED: parent updated settings
 */
class MektepFcmService : FirebaseMessagingService() {

    override fun onMessageReceived(message: RemoteMessage) {
        val data = message.data
        val type = data["type"] ?: return

        Log.d("MektepFCM", "Received message type: $type")

        when (type) {
            "BONUS_TIME" -> {
                val seconds = data["amount_seconds"]?.toIntOrNull() ?: return
                // TODO: update screen time balance in Room
                Log.d("MektepFCM", "Bonus time received: $seconds seconds")
            }
            "CONFIG_CHANGED" -> {
                // Config sync happens via Firebase RTDB listener, but this
                // notification wakes up the app if it's in background
                Log.d("MektepFCM", "Config change notification received")
            }
        }
    }

    override fun onNewToken(token: String) {
        Log.d("MektepFCM", "New FCM token: $token")
        // TODO: update token in Firebase RTDB /families/{familyId}/members/{userId}/fcmToken
    }
}
