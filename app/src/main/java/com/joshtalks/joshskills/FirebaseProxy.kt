package com.joshtalks.joshskills

import android.content.Intent
import com.google.firebase.messaging.RemoteMessage

interface FirebaseProxy {
    fun onNewToken(token: String)
    fun onMessageReceived(remoteMessage: RemoteMessage)
    fun handleIntent(intent: Intent)
}