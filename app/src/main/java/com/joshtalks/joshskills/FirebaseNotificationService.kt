package com.joshtalks.joshskills

import android.content.Intent
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class FirebaseNotificationService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        try {
            getFirebaseProxy().onNewToken(token)
        } catch (e: ClassNotFoundException) {
            e.printStackTrace()
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        try {
            getFirebaseProxy().onMessageReceived(message)
        } catch (e: ClassNotFoundException) {
            e.printStackTrace()
        }
    }

    override fun handleIntent(intent: Intent) {
        super.handleIntent(intent)

        try {
            getFirebaseProxy().handleIntent(intent)
        } catch (e: ClassNotFoundException) {
            e.printStackTrace()
        }
        // TODO: If visual then super.handleIntent(intent)
    }

    private fun getFirebaseProxy() : FirebaseProxy {
        return Class.forName("com.joshtalks.joshskills.premium.FirebaseProxyImpl").newInstance() as FirebaseProxy
    }
}