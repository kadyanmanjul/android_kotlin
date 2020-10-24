package com.joshtalks.joshskills.repository.local.model

import com.google.gson.annotations.SerializedName
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.PrefManager

const val PLIVO_USER_PERSISTANT_KEY = "plivo_user"

data class UserPlivoDetailsModel(
    @SerializedName("username")
    val username: String,
    @SerializedName("password")
    val password: String,
    @SerializedName("endpoint_id")
    val endpointId: String,
    @SerializedName("mentor_id")
    val mentorId: String,
) {

    fun savePlivoUser() {
        val string: String = toString()
        PrefManager.put(PLIVO_USER_PERSISTANT_KEY, string)
    }

    override fun toString(): String {
        return AppObjectController.gsonMapper.toJson(this)
    }

    companion object {

        @JvmStatic
        fun getPlivoUser(): UserPlivoDetailsModel? {
            return try {
                AppObjectController.gsonMapper.fromJson(
                    PrefManager.getStringValue(PLIVO_USER_PERSISTANT_KEY),
                    UserPlivoDetailsModel::class.java
                )
            } catch (ex: Exception) {
                null
            }
        }
    }
}