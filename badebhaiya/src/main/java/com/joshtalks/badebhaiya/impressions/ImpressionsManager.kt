package com.joshtalks.badebhaiya.impressions

import com.google.gson.annotations.SerializedName
import javax.inject.Inject

/**
    This class is responsible to send events.
*/

data class Impression(
    @SerializedName("from_page")
    val from_page: String,
    @SerializedName("event_name")
    val event_name: String,
)