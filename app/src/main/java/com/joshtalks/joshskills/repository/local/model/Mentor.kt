package com.joshtalks.joshskills.repository.local.model

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.core.PrefManager
import com.joshtalks.joshskills.repository.local.model.googlelocation.Locality

const val MENTOR_PERSISTANT_KEY = "mentor"

class Mentor {

    @SerializedName("id")
    private var id: String? = null


    @SerializedName("user")
    private var user: User? = null

    @SerializedName("locality")
    private var locality: Locality? = null


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
    }


    fun getLocality(): Locality? {
        return locality
    }

    fun setLocality(locality: Locality?): Mentor {
        this.locality = locality
        return this
    }

    fun logout() {
        Mentor().update()
        instance = null
        User().update()
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
        User().update()
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
        return id.toString()
    }


    fun setId(id: String): Mentor {
        this.id = id
        return this
    }

    fun setReferralCode(code: String): Mentor {
        this.referralCode = code
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