package com.joshtalks.joshskills.repository.local.model

import com.google.gson.annotations.SerializedName
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.core.PrefManager

const val GAID_MENTOR_MAP_OBJECT = "gaid_mentor_map_object"

class GaIDMentorModel {

    @SerializedName("id")
    var gaID: String = EMPTY

    @SerializedName("mentors")
    var mapMentorList: List<String>? = emptyList()

    override fun toString(): String {
        return AppObjectController.gsonMapper.toJson(this)
    }


    companion object {
        @JvmStatic
        fun getMapObject(): GaIDMentorModel? {
            return try {
                AppObjectController.gsonMapper.fromJson(PrefManager.getStringValue(GAID_MENTOR_MAP_OBJECT),
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