package com.joshtalks.badebhaiya.utils

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.joshtalks.badebhaiya.pubnub.PubNubManager

fun JsonObject?.toHashMap(): HashMap<*, *>{
    return Gson().fromJson(this.toString(), HashMap::class.java)
}