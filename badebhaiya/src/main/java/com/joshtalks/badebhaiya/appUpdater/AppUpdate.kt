package com.joshtalks.badebhaiya.appUpdater

import com.joshtalks.badebhaiya.BuildConfig

data class AppUpdate(
    val expired_version: Long,
    val is_enabled: Boolean,
    val update_type: String
) {
    fun isStrictUpdate(): Boolean {
        return is_enabled && update_type == UpdateType.STRICT && expired_version >= BuildConfig.VERSION_CODE
    }
}

object UpdateType {
    const val STRICT = "strict"
    const val NORMAL = "normal"
}