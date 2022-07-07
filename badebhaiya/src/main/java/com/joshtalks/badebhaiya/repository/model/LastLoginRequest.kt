package com.joshtalks.badebhaiya.repository.model

data class LastLoginRequest(
    val user: String,
    val device_id: String
)