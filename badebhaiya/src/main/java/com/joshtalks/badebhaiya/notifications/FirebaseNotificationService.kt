package com.joshtalks.badebhaiya.notifications

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.joshtalks.badebhaiya.core.PrefManager
import com.joshtalks.badebhaiya.repository.CommonRepository
import com.joshtalks.badebhaiya.repository.model.FCMData
import com.joshtalks.badebhaiya.repository.model.User
import com.joshtalks.badebhaiya.utils.ApiRespStatus
import com.joshtalks.badebhaiya.utils.Utils
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber

const val FCM_TOKEN = "fcmToken"

class FirebaseNotificationService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Timber.tag(FirebaseNotificationService::class.java.name).e(token)
        try {
            if (PrefManager.hasKey(FCM_TOKEN)) {
                val fcmResponse = FCMData.getInstance()
                fcmResponse?.apiStatus = ApiRespStatus.POST
                fcmResponse?.update()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        PrefManager.put(FCM_TOKEN, token)
        postFCMToken(token)
    }

    private fun postFCMToken(token: String) {
        CoroutineScope(SupervisorJob() + Dispatchers.IO +
                CoroutineExceptionHandler { _, _ -> }).launch {
            val userId = User.getInstance().userId
            if (userId.isNotBlank()) {
                try {
                    if (PrefManager.hasKey(FCM_TOKEN)) {
                        val data = mutableMapOf(
                            "registration_id" to token
                        )
                        val resp = CommonRepository().patchFCMToken(userId,data)
                        if (resp.isSuccessful) {
                            resp.body()?.update()
                            Timber.tag(FCMData::class.java.name).e("patch data : ${resp.body()}")
                        }
                    } else {
                        val data = mutableMapOf(
                            "name" to Utils.getDeviceName(),
                            "registration_id" to token,
                            "device_id" to Utils.getDeviceId(),
                            "active" to "true",
                            "user_id" to userId,
                            "type" to "android"
                        )
                        val resp = CommonRepository().postFCMToken(data)
                        if (resp.isSuccessful) {
                            resp.body()?.update()
                            Timber.tag(FCMData::class.java.name).e("post data : ${resp.body()}")
                        }
                    }
                } catch (ex: Exception) {
                    ex.printStackTrace()
                }
            }
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Timber.tag(FirebaseNotificationService::class.java.name).e("fcm")
    }
}
