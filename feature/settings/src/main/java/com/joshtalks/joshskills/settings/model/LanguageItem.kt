package com.joshtalks.joshskills.settings.model

import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import com.joshtalks.joshskills.common.core.AppObjectController
import com.joshtalks.joshskills.common.core.FirebaseRemoteConfigKey
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
                        FirebaseRemoteConfigKey.NEW_LANGUAGES_SUPPORTED
                    ), listType
                )
            } catch (ex: Exception) {
                ex.printStackTrace()
                emptyList()
            }
        }
    }
}
