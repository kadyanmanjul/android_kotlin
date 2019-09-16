package com.joshtalks.joshskills.repository.local.model

import com.google.gson.annotations.SerializedName
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.PrefManager
import com.joshtalks.joshskills.repository.server.ProfileToken
import java.lang.Exception


const val USER_PERSISTANT_KEY = "user"

open class User {
    @SerializedName("phoneNumber")
    var phoneNumber: String = ""

    @SerializedName("token")
    var token: ProfileToken? = null

    @SerializedName("username")
    var username: String = ""

    @SerializedName("email")
    var email: String = ""

    @SerializedName("gender")
    var gender: String = ""

    @SerializedName("date_of_birth")
    var dateOfBirth: String? = ""

    @SerializedName("first_name")
    var firstName: String = ""
       // get() = field.substring(0, 1).toUpperCase().plus(field.substring(1))


    @SerializedName("photo_url")
    var photo: String = ""

    @SerializedName("social_id")
    var socialId: String = ""

    @SerializedName("user_id")
    var id: String = ""

    @SerializedName("user_type")
    var userType: String = ""

    @SerializedName("source")
    var source: String = ""

    @SerializedName("isLoggedIn")
    var isLoggedIn = false


    fun update() {
        PrefManager.put(USER_PERSISTANT_KEY, toString())
    }

    override fun toString(): String {
        return AppObjectController.gsonMapper.toJson(this)
    }

    fun updateFromResponse(user: User) {
        this.photo = user.photo
        this.phoneNumber = user.phoneNumber
        this.gender = user.gender
        this.dateOfBirth = user.dateOfBirth
        this.firstName = user.firstName
        this.socialId = user.socialId
        this.userType = user.userType
        update()
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