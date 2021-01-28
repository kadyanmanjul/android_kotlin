package com.joshtalks.joshskills.repository.local.model


import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import com.joshtalks.joshskills.core.ApiRespStatus
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.PrefManager

const val FCM_PERSISTENT_KEY = "fcm_response"

class FCMResponse(
    @SerializedName("id")
    val id: Int,

    @SerializedName("user_id")
    var userId: String,

    @Expose
    var apiStatus: ApiRespStatus = ApiRespStatus.EMPTY

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