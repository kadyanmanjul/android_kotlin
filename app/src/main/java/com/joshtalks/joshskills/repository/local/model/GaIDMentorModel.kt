package com.joshtalks.joshskills.repository.local.model

import com.google.gson.annotations.SerializedName
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.core.PrefManager

const val GAID_MENTOR_MAP_OBJECT = "gaid_mentor_map_object"

class GaIDMentorModel {

    @SerializedName("id")
    var gaidServerDbId: Int = 0

    @SerializedName("gaid")
    var gaID: String = EMPTY

    @SerializedName("mentors")
    var mapMentorList: List<String>? = emptyList()

    @SerializedName("instance_id")
    var instanceId: String = EMPTY

    @SerializedName("explore_type")
    var exploreCardType: ExploreCardType? = ExploreCardType.NORMAL

    @SerializedName("test")
    var test: String = EMPTY

    override fun toString(): String {
        return AppObjectController.gsonMapper.toJson(this)
    }

    companion object {
        @JvmStatic
        fun getMapObject(): GaIDMentorModel? {
            return try {
                AppObjectController.gsonMapper.fromJson(
                    PrefManager.getStringValue(GAID_MENTOR_MAP_OBJECT),
                    GaIDMentorModel::class.java
                )
            } catch (ex: Exception) {
                ex.printStackTrace()
                null
            }
        }

        fun update(obj: GaIDMentorModel) {
            PrefManager.put(GAID_MENTOR_MAP_OBJECT, AppObjectController.gsonMapper.toJson(obj))
        }
    }

}