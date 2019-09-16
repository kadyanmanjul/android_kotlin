package com.joshtalks.joshskills.core.service

import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.PrefManager
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.joshskills.repository.local.model.Mentor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


object FCMTokenManager {

    fun pushToken() {
        val token = PrefManager.getStringValue(FCM_TOKEN)
        if (token.isEmpty())
            return

        if (PrefManager.hasKey(FCM_ID)) {
            patchToken(token)
        } else {
            postToken(token)
        }

    }

    private fun patchToken(token: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val data = mapOf("registration_id" to token, "active" to "true")
                var token=PrefManager.getLongValue(FCM_ID).toString()
                val response: Any = AppObjectController.signUpNetworkService.updateFCMToken(token,data).await()

            }catch (ex:Exception){
                ex.printStackTrace()
            }
        }
    }

    private fun postToken(token: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {

                val data = mutableMapOf<String, String>(
                    "registration_id" to token,
                    "name" to Utils.getDeviceName(),
                    "device_id" to Utils.getDeviceId(),
                    "active" to "true",
                    "type" to "android"
                )
                if (Mentor.getInstance().hasId()) {
                    data["user_id"]=Mentor.getInstance().getId()

                }
                val response: Any =
                    AppObjectController.signUpNetworkService.uploadFCMToken(data).await()

            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }

    }

}
