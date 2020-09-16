package com.joshtalks.joshskills.repository.local.model

import com.google.gson.annotations.SerializedName
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.core.PrefManager

const val USER_PERSISTANT_KEY = "user"

open class User {
    @SerializedName("mobile")
    var phoneNumber: String = EMPTY

    @SerializedName("token")
    var token: String = EMPTY

    @SerializedName("username")
    var username: String = EMPTY

    @SerializedName("email")
    var email: String = EMPTY

    @SerializedName("gender")
    var gender: String = EMPTY

    @SerializedName("date_of_birth")
    var dateOfBirth: String? = EMPTY

    @SerializedName("first_name")
    var firstName: String = EMPTY

    @SerializedName("photo_url")
    var photo: String? = null

    @SerializedName("social_id")
    var socialId: String = EMPTY

    @SerializedName("id")
    var userId: String = EMPTY

    @SerializedName("user_type")
    var userType: String = EMPTY

    @SerializedName("source")
    var source: String = EMPTY

    @SerializedName("is_verified")
    var isVerified: Boolean = false

    fun update() {
        PrefManager.put(USER_PERSISTANT_KEY, toString())
    }

    override fun toString(): String {
        return AppObjectController.gsonMapper.toJson(this)
    }


    fun updateFromResponse(user: User) {
        user.token = getInstance().token
        user.phoneNumber = user.phoneNumber
        user.dateOfBirth = user.dateOfBirth
        user.gender = user.gender
        user.firstName = user.firstName
        update(user.toString())
    }

    companion object {

        @JvmStatic
        fun getInstance(): User {
            return try {
                AppObjectController.gsonMapper.fromJson(
                    PrefManager.getStringValue(USER_PERSISTANT_KEY), User::class.java
                )
            } catch (ex: Exception) {
                return User()
            }
        }

        fun update(value: String) {
            PrefManager.put(USER_PERSISTANT_KEY, value)
        }
    }


}