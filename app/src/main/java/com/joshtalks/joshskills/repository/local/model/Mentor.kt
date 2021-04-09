package com.joshtalks.joshskills.repository.local.model

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import com.joshtalks.joshskills.core.API_TOKEN
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.core.PrefManager
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.repository.local.model.googlelocation.Locality
import com.joshtalks.joshskills.repository.server.signup.LoginResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

const val MENTOR_PERSISTANT_KEY = "mentor"

class Mentor {

    @SerializedName("id")
    private var id: String? = null

    @SerializedName("user")
    private var user: User? = null

    @SerializedName("locality")
    private var locality: Locality? = null

    @SerializedName("user_id")
    private var userId: String? = null


    @Expose
    var referralCode: String = EMPTY

    companion object {
        @JvmStatic
        private var instance: Mentor? = null

        @JvmStatic
        fun getInstance(): Mentor {
            return try {
                instance = AppObjectController.gsonMapper.fromJson(
                    PrefManager.getStringValue(MENTOR_PERSISTANT_KEY), Mentor::class.java
                )
                instance!!
            } catch (ex: Exception) {
                Mentor()
            }
        }

        fun updateFromLoginResponse(loginResponse: LoginResponse) {
            CoroutineScope(Dispatchers.IO).launch {
                val user = User.getInstance()
                user.userId = loginResponse.userId
                user.isVerified = false
                user.token = loginResponse.token
                User.update(user)
                PrefManager.put(API_TOKEN, loginResponse.token)
                getInstance()
                    .setId(loginResponse.mentorId)
                    .setReferralCode(loginResponse.referralCode)
                    .setUserId(loginResponse.userId)
                    .update()
                AppAnalytics.updateUser()
            }
        }
    }

    fun getLocality(): Locality? {
        return locality
    }

    fun setLocality(locality: Locality?): Mentor {
        this.locality = locality
        return this
    }

    fun logout() {
        instance = null
        Mentor().update()
    }

    fun update() {
        val string: String = toString()
        PrefManager.put(MENTOR_PERSISTANT_KEY, string)
    }

    fun updateUser(user: User): Mentor {
        this.user = user
        return this
    }

    fun reset() {
        instance = null
    }

    fun updateFromResponse(mentor: Mentor) {
        setLocality(mentor.getLocality())
        mentor.user?.let { updateUser(it) }
        update()
    }


    fun isCurrentUser(): Boolean {
        return getId() == getInstance().getId()
    }

    fun getId(): String {
        return id ?: EMPTY
    }

    fun getUserId(): String {
        return userId ?: EMPTY
    }

    fun setId(id: String): Mentor {
        this.id = id
        return this
    }

    fun setReferralCode(code: String): Mentor {
        this.referralCode = code
        return this
    }

    fun setUserId(userId: String): Mentor {
        this.userId = userId
        return this
    }

    fun hasId(): Boolean {
        return id != null && id?.isNotEmpty()!!
    }

    fun getUser(): User? {
        return user
    }

    override fun toString(): String {
        return AppObjectController.gsonMapper.toJson(this)
    }


}



