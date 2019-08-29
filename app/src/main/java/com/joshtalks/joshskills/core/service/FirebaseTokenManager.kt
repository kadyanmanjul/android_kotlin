package com.joshtalks.joshskills.core.service

import com.joshtalks.joshskills.core.PrefManager



object FCMTokenManager {

    fun pushToken() {

        val token = PrefManager.getStringValue(FCM_TOKEN)

        if (token.isEmpty())
            return

        if (PrefManager.hasKey("fcmId")) {
            patch(token)
        } else {
            post(token)
        }

    }

    private fun patch(token: String) {

       /* val patch = Patch(
            BaseApplication.getContext(),
            Endpoint.patchFcmToken(PrefManager.getLong("fcmId"))
        )
            .addParameter("registration_id", token)
            .addParameter("active", "true")

        patch.setCallback(object : ApiCallback() {
            fun onSuccess(response: String, isFromCache: Boolean) {
                super.onSuccess(response, isFromCache)
            }
        })

        patch.hit()*/

    }

    private fun post(token: String) {

       /* val fcmApi = Post(BaseApplication.getContext(), Endpoint.postFCMToken())
            .addParameter("registration_id", token)
            .addParameter("name", Utils.getDeviceName())
            .addParameter("device_id", Utils.getDeviceId())
            .addParameter("active", "true")
            .addParameter("type", "android")


        val deviceApi = Post(BaseApplication.getContext(), Endpoint.postFCMToken())
            .addParameter("name", Utils.getDeviceName())
            .addParameter("device_id", Utils.getDeviceId())
            .addParameter("type", "android")


        if (Mentor.getInstance().hasId()) {
            fcmApi.addParameter("user_id", Mentor.getInstance().getId())
            deviceApi.addParameter("user_id", Mentor.getInstance().getId())
        }

        deviceApi.hit()

        fcmApi.setCallback(object : ApiCallback() {
            fun onSuccess(response: String, isFromCache: Boolean) {
                super.onSuccess(response, isFromCache)

                try {
                    val jsonObject = JSONObject(response)
                    val id = jsonObject.getInt("id")
                    PrefManager.put("fcmId", id)
                } catch (e: JSONException) {
                    JoshLog.e("Exception", e.toString())
                    Crashlytics.logException(e)
                }

            }
        })

        fcmApi.hit()*/
    }

}
