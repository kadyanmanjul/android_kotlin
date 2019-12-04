package com.joshtalks.joshskills.repository.local.model

import com.google.gson.annotations.SerializedName
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.PrefManager
import java.util.HashMap

const val INSTALL_REFERRER_OBJECT = "install_referrer_object"

open class InstallReferrerModel {
    @SerializedName("mentor_id")
    var mentorId: String? = null

    @SerializedName("referrer")
    var referrer: HashMap<String, String>? = null


    override fun toString(): String {
        return AppObjectController.gsonMapper.toJson(this)
    }


    companion object {
        @JvmStatic
        fun getPrefObject(): InstallReferrerModel {

            return AppObjectController.gsonMapper.fromJson(
                PrefManager.getStringValue(INSTALL_REFERRER_OBJECT), InstallReferrerModel::class.java
            )
        }

        fun update(value: String) {
            PrefManager.put(INSTALL_REFERRER_OBJECT, value)
        }
    }


}