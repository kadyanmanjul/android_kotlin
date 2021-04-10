package com.joshtalks.joshskills.repository.server.points


import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.FirebaseRemoteConfigKey
import java.lang.reflect.Type

class PointsHistoryTitles {

    companion object {

        @JvmStatic
        private var instance: List<PointsHistoryTitleModel>? = null

        @JvmStatic
        fun getInstance(): List<PointsHistoryTitleModel> {
            return try {
                val listType: Type = object : TypeToken<List<PointsHistoryTitleModel>>() {}.type
                instance = AppObjectController.gsonMapper.fromJson(
                    AppObjectController.getFirebaseRemoteConfig().getString(
                        FirebaseRemoteConfigKey.POINTS_HISTORY_TITLES
                    ), listType
                )
                instance!!
            } catch (ex: Exception) {
                ex.printStackTrace()
                emptyList()
            }
        }

        fun getTitleForIndex(titleIndex: Int): String {
            if (instance==null){
                getInstance()
            }
            return instance?.filter { it.index == titleIndex }?.getOrNull(0)?.label ?: "Manjul"
        }
    }
}

data class PointsHistoryTitleModel(
    @SerializedName("label") val label: String,
    @SerializedName("index") val index: Int = 0
)
