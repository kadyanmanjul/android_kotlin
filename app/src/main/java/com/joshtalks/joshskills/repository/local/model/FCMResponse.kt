package com.joshtalks.joshskills.repository.local.model


import com.google.gson.annotations.SerializedName
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.PrefManager

const val FCM_PERSISTENT_KEY = "fcm_response"

class FCMResponse(
    @SerializedName("active")
    val active: Boolean,
    @SerializedName("date_created")
    val dateCreated: String,
    @SerializedName("device_id")
    val deviceId: String,
    @SerializedName("id")
    val id: Int,
    @SerializedName("name")
    val name: String,
    @SerializedName("registration_id")
    val registrationId: String,
    @SerializedName("type")
    val type: String,
    @SerializedName("user_id")
    var userId: String
) {
    companion object {

        @JvmStatic
        fun getInstance(): FCMResponse? {
            return try {
                AppObjectController.gsonMapper.fromJson(
                    PrefManager.getStringValue(FCM_PERSISTENT_KEY), FCMResponse::class.java
                )
            } catch (ex: Exception) {
                null
            }
        }
    }

    fun update() {
        val string: String = toString()
        PrefManager.put(FCM_PERSISTENT_KEY, string)
    }


    override fun toString(): String {
        return AppObjectController.gsonMapper.toJson(this)
    }
}