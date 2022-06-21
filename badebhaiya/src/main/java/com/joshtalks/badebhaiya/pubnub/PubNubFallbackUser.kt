package com.joshtalks.badebhaiya.pubnub

import java.io.Serializable

data class PubNubFallbackUser(
    val user_list: List<User>? = null
): Serializable