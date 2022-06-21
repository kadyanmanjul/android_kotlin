package com.joshtalks.badebhaiya.pubnub

data class Custom(
    var full_name: String? = null,
    var id: Int = 0,
    var is_hand_raised: Boolean? = null,
    var is_mic_on: Boolean? = null,
    var is_moderator: Boolean? = null,
    var is_speaker: Boolean? = null,
    var is_speaking: Boolean? = null,
    var short_name: String? = null,
    var sort_order: Int = 0,
    var user_id: String? = null,
    var photo_url: String? = null
)