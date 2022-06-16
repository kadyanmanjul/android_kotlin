package com.joshtalks.badebhaiya.utils

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken

fun JsonObject?.toHashMap(): HashMap<Any, Any> {
//    return Gson().fromJson(this.toString(), HashMap::class.java)
    val type = object : TypeToken<HashMap<Any, Any>?>() {}.type
    return Gson().fromJson<HashMap<Any, Any>?>(this, type)
}