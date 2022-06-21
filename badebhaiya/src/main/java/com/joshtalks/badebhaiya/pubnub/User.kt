package com.joshtalks.badebhaiya.pubnub

import com.joshtalks.badebhaiya.feed.model.LiveRoomUser

data class User(
    val custom: Custom? = null,
    val eTag: String? = null,
    val updated: String? = null,
    val uuid: Uuid? = null
)