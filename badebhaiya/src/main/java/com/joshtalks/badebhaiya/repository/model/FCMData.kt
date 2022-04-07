package com.joshtalks.badebhaiya.repository.model


import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import com.joshtalks.badebhaiya.core.AppObjectController
import com.joshtalks.badebhaiya.core.PrefManager
import com.joshtalks.badebhaiya.utils.ApiRespStatus

const val FCM_PERSISTENT_KEY = "fcm_response"

class FCMData(
    @SerializedName("id")
    val id: Int,

    @SerializedName("user_id")
    var userId: String?,

    @SerializedName("date_created")
    val dateCreated: String?,

    @Expose
    var apiStatus: ApiRespStatus = ApiRespStatus.EMPTY

) {
    companion object {

        @JvmStatic
        fun getInstance(): FCMData? {
            return try {
                AppObjectController.gsonMapper.fromJson(
                    PrefManager.getStringValue(FCM_PERSISTENT_KEY), FCMData::class.java
                )
            } catch (ex: Exception) {
                null
            }
        }

        @JvmStatic
        fun removeOldFCM() {
            PrefManager.removeKey(FCM_PERSISTENT_KEY)
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