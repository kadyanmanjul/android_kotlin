package com.joshtalks.joshskills.repository.local.model

import com.google.gson.annotations.SerializedName
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.EMPTY

open class User {
    @SerializedName("mobile")
    var phoneNumber: String? = EMPTY

    @SerializedName("token")
    var token: String = EMPTY

    @SerializedName("username")
    var username: String = EMPTY

    @SerializedName("email")
    var email: String? = EMPTY

    @SerializedName("gender")
    var gender: String? = EMPTY

    @SerializedName("date_of_birth")
    var dateOfBirth: String? = EMPTY

    @SerializedName("first_name")
    var firstName: String? = EMPTY
        set(value) {
            if (value == null) field = EMPTY
        }
        get() {
            return field ?: EMPTY
        }

    @SerializedName("photo_url")
    var photo: String? = null

    @SerializedName("social_id")
    var socialId: String = EMPTY

    @SerializedName("id")
    var userId: String = EMPTY

    @SerializedName("user_type")
    var userType: String = EMPTY

    @SerializedName("created_source")
    var source: String = EMPTY

    @SerializedName("is_verified")
    var isVerified: Boolean = false

    override fun toString(): String {
        return AppObjectController.gsonMapper.toJson(this)
    }


    fun updateFromResponse(user: User) {
        user.token = getInstance().token
        update(user)
    }

    companion object {

        @JvmStatic
        fun getInstance(): User {
            return try {
                Mentor.getInstance().getUser() ?: User()
            } catch (ex: Exception) {
                return User()
            }
        }

        fun update(user: User) {
            Mentor.getInstance().updateUser(user).update()
        }
    }


}