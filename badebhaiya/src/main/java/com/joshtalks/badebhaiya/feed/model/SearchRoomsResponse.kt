package com.joshtalks.badebhaiya.feed.model

import com.google.gson.annotations.SerializedName

data class SearchRoomsResponse(
    @SerializedName("rooms")
    val rooms:Room,
    @SerializedName("users")
    val users:Users
)

data class SearchRoomsResponseList(
    @SerializedName("rooms")
    val rooms:List<Room>,
    @SerializedName("users")
    val users:List<Users>
)