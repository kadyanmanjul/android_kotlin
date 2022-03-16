package com.joshtalks.badebhaiya.repository.model

import com.google.gson.annotations.SerializedName
import com.joshtalks.badebhaiya.core.EMPTY
import com.joshtalks.badebhaiya.core.PrefManager
import com.joshtalks.badebhaiya.repository.service.RetrofitInstance

const val USER_PERSISTENT_KEY = "USER_PERSISTENT_KEY"
data class User(
    @SerializedName("first_name") var firstName: String = EMPTY,
    @SerializedName("last_name") var lastName: String = EMPTY,
    @SerializedName("user_id") var userId: String = EMPTY,
    @SerializedName("token") var token: String = EMPTY,
    @SerializedName("profile_url") var profilePicUrl: String = EMPTY,
    @SerializedName("mobile") var mobile: String = EMPTY
) {
    companion object {
        @JvmStatic
        private var instance: User? = null

        @JvmStatic
        fun getInstance(): User {
            return try {
                RetrofitInstance.gsonMapper.fromJson(PrefManager.getStringValue(USER_PERSISTENT_KEY), User::class.java)
            } catch (ex: Exception) {
                User()
            }
        }
    }

    fun update() {
        val string = toString()
        PrefManager.put(USER_PERSISTENT_KEY, string)
    }

    override fun toString(): String {
        return RetrofitInstance.gsonMapper.toJson(this)
    }

    fun updateFromResponse(body: User) {

    }
}
