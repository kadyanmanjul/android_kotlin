package com.joshtalks.joshskills.repository.server

import com.google.gson.annotations.SerializedName
import com.joshtalks.joshskills.core.AppObjectController


data class CoreMeta(@SerializedName("triggers") val triggers: List<String>, @SerializedName("preload_img") val preloadImage: List<String>) {


    override fun toString(): String {
        return AppObjectController.gsonMapper.toJson(this)
    }
}


