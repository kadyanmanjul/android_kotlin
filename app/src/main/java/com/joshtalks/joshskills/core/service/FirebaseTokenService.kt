package com.joshtalks.joshskills.core.service

import com.google.firebase.messaging.FirebaseMessagingService
import com.joshtalks.joshskills.core.PrefManager

const val FCM_TOKEN = "fcmToken"
const val FCM_ID = "fcmId"

class FirebaseTokenService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        PrefManager.put(FCM_TOKEN, token)
        FCMTokenManager.pushToken()
    }
}
