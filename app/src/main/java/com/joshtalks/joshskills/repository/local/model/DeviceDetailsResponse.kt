package com.joshtalks.joshskills.repository.local.model


import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import com.joshtalks.joshskills.core.ApiRespStatus
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.core.PrefManager

const val DEVICE_DETAILS_KEY = "device_details"

data class DeviceDetailsResponse(
    @SerializedName("app_version_code")
    val appVersionCode: Int,
    @SerializedName("app_version_name")
    val appVersionName: String,
    @SerializedName("brand")
    val brand: String,
    @SerializedName("device")
    val device: String,
    @SerializedName("device_id")
    val deviceId: String,
    @SerializedName("gaid_id")
    val gaidId: String,
    @SerializedName("id")
    val id: Int,
    @SerializedName("manufacture")
    val manufacture: String,
    @SerializedName("mentor_id")
    val mentorId: String = EMPTY,
    @SerializedName("model")
    val model: String,
    @SerializedName("os_version_code")
    val osVersionCode: Int,
    @SerializedName("os_version_name")
    val osVersionName: String,
    @Expose
    var apiStatus: ApiRespStatus = ApiRespStatus.EMPTY

) {
    companion object {

        @JvmStatic
        fun getInstance(): DeviceDetailsResponse? {
            return try {
                AppObjectController.gsonMapper.fromJson(
                    PrefManager.getStringValue(DEVICE_DETAILS_KEY),
                    DeviceDetailsResponse::class.java
                )
            } catch (ex: Exception) {
                null
            }
        }

        @JvmStatic
        fun removeOldDevice() {
            PrefManager.removeKey(DEVICE_DETAILS_KEY)
        }
    }

    fun update() {
        val string: String = toString()
        PrefManager.put(DEVICE_DETAILS_KEY, string)
    }



    override fun toString(): String {
        return AppObjectController.gsonMapper.toJson(this)
    }
}