package com.joshtalks.badebhaiya.feed.model

data class Room(
    val channel_name: String,
    val ended: String,
    val start_time: String,
    val started_by: Int,
    val topic: String
)