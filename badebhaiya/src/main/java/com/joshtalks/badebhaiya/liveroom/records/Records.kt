package com.joshtalks.badebhaiya.liveroom.records

import com.joshtalks.badebhaiya.BuildConfig
import com.joshtalks.badebhaiya.repository.model.User

data class Records(
//    val expired_version: Long,
//    val is_enabled: Boolean,
    val id: String
) {
    fun isRecordingAllowed(): Boolean {
        return User.getInstance().userId==id
//        return is_enabled && update_type == UpdateType.STRICT && expired_version >= BuildConfig.VERSION_CODE
    }
}

object UpdateType {
    const val STRICT = "strict"
    const val NORMAL = "normal"
}