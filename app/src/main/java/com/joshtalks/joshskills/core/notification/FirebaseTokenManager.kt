package com.joshtalks.joshskills.core.notification

import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.PrefManager
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.joshskills.repository.local.model.Mentor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


object FCMTokenManager {

    fun pushToken() {
        try {
            val token = PrefManager.getStringValue(FCM_TOKEN)

            if (token.isEmpty())
                return

            if (PrefManager.hasKey(FCM_ID)) {
                patchToken(
                    token
                )
            } else {
                postToken(
                    token
                )
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }

    }

    private fun patchToken(token: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val data = mapOf("registration_id" to token, "active" to "true")
                val fcmId = PrefManager.getLongValue(FCM_ID).toString()
                AppObjectController.signUpNetworkService.updateFCMToken(fcmId, data).await()

            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    private fun postToken(token: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {

                val data = mutableMapOf(
                    "registration_id" to token,
                    "name" to Utils.getDeviceName(),
                    "device_id" to Utils.getDeviceId(),
                    "active" to "true",
                    "type" to "android"
                )
                if (Mentor.getInstance().hasId()) {
                    data["user_id"] = Mentor.getInstance().getId()

                }
                AppObjectController.signUpNetworkService.uploadFCMToken(data).await()
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }

    }

}
