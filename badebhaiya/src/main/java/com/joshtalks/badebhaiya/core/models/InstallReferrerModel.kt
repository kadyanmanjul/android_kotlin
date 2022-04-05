package com.joshtalks.badebhaiya.core.models

import com.google.gson.annotations.SerializedName
import com.joshtalks.badebhaiya.core.AppObjectController
import com.joshtalks.badebhaiya.core.PrefManager
import java.util.Date

const val INSTALL_REFERRER_OBJECT = "install_referrer_object"

open class InstallReferrerModel {
    @SerializedName("id")
    var id: String? = null

    @SerializedName("install_on")
    var installOn: Long = (Date().time / 1000)

    @SerializedName("user")
    var user: String? = null

    @SerializedName("other_info")
    var otherInfo: HashMap<String, String>? = null

    @SerializedName("utm_medium")
    var utmMedium: String? = null

    @SerializedName("utm_source")
    var utmSource: String? = null

    @SerializedName("utm_term")
    var utmTerm: String? = null

    override fun toString(): String {
        return AppObjectController.gsonMapper.toJson(this)
    }


    companion object {
        @JvmStatic
        fun getPrefObject(): InstallReferrerModel? {
            return try {
                AppObjectController.gsonMapper.fromJson(
                    PrefManager.getStringValue(INSTALL_REFERRER_OBJECT),
                    InstallReferrerModel::class.java
                )
            } catch (ex: Exception) {
                ex.printStackTrace()
                null
            }
        }

        fun update(value: InstallReferrerModel) {
            PrefManager.put(INSTALL_REFERRER_OBJECT, AppObjectController.gsonMapper.toJson(value))
        }
    }


}