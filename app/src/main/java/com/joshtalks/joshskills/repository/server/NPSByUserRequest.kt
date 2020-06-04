package com.joshtalks.joshskills.repository.server


import com.google.gson.annotations.SerializedName
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.PrefManager

const val NPS_REQUEST_WITHOUT_LOGIN = "nps_request_without_login"

data class NPSByUserRequest(
    @SerializedName("mentor")
    val mentor: String,
    @SerializedName("event_name")
    val eventName: String?,
    @SerializedName("rating")
    val rating: Int,
    @SerializedName("text")
    val text: String?
) {
    companion object {

        @JvmStatic
        fun getInstance(): NPSByUserRequest? {
            return try {
                AppObjectController.gsonMapper.fromJson(
                    PrefManager.getStringValue(NPS_REQUEST_WITHOUT_LOGIN),
                    NPSByUserRequest::class.java
                )
            } catch (ex: Exception) {
                return null
            }
        }

        fun update(value: String) {
            PrefManager.put(NPS_REQUEST_WITHOUT_LOGIN, value)
        }
    }

}