package com.joshtalks.joshskills.repository.server

import com.google.gson.annotations.SerializedName
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.FirebaseRemoteConfigKey
import com.sinch.gson.reflect.TypeToken
import java.lang.reflect.Type

data class LanguageItem(
    @SerializedName("code")
    val code: String,
    @SerializedName("name")
    val name: String
) {

    companion object {

        @JvmStatic
        fun getLanguageList(): List<LanguageItem> {
            return try {
                val listType: Type = object : TypeToken<List<LanguageItem>>() {}.type
                return AppObjectController.gsonMapper.fromJson(
                    AppObjectController.getFirebaseRemoteConfig().getString(
                        FirebaseRemoteConfigKey.LANGUAGES_SUPPORTED
                    ), listType
                )
            } catch (ex: Exception) {
                ex.printStackTrace()
                emptyList()
            }
        }
    }
}
