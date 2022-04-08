package com.joshtalks.badebhaiya.repository.model

import com.google.gson.annotations.SerializedName
import com.joshtalks.badebhaiya.core.AppObjectController
import com.joshtalks.badebhaiya.core.EMPTY
import com.joshtalks.badebhaiya.core.PrefManager

const val USER_PERSISTENT_KEY = "USER_PERSISTENT_KEY"

data class User(
    @SerializedName("first_name") var firstName: String? = null,
    @SerializedName("last_name") var lastName: String? = null,
    @SerializedName("user_id") var userId: String = EMPTY,
    @SerializedName("token") var token: String = EMPTY,
    @SerializedName("photo_url") var profilePicUrl: String? = null,
    @SerializedName("mobile") var mobile: String = EMPTY,
    @SerializedName("is_speaker") var isSpeaker: Boolean = false,
) {
    companion object {
        @JvmStatic
        private var instance: User? = null

        @JvmStatic
        fun getInstance(): User {
            return try {
                instance = AppObjectController.gsonMapper.fromJson(
                    PrefManager.getStringValue(USER_PERSISTENT_KEY),
                    User::class.java
                )
                instance!!
            } catch (ex: Exception) {
                User()
            }
        }
    }

    fun update() {
        PrefManager.put(USER_PERSISTENT_KEY, this.toString())
    }

    override fun toString(): String {
        return AppObjectController.gsonMapper.toJson(this)
    }

    fun updateFromResponse(user: User) {
        this.firstName = user.firstName
        this.lastName = user.lastName
        this.mobile = user.mobile
        this.isSpeaker = user.isSpeaker
        if (this.profilePicUrl.isNullOrEmpty()) this.profilePicUrl = user.profilePicUrl
        if (this.userId.isEmpty()) this.userId = user.userId
        if (this.token.isEmpty()) this.token = user.token
        update()
    }
}
