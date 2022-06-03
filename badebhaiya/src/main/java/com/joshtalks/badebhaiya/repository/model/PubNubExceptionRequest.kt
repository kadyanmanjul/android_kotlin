package com.joshtalks.badebhaiya.repository.model

data class PubNubExceptionRequest(
    val android_version: Double,
    val app_version_code: Int,
    val device_id: String,
    val stack_tree: String
)