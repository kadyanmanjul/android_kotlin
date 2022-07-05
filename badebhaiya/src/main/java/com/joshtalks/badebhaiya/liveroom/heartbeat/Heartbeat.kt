package com.joshtalks.badebhaiya.liveroom.heartbeat

import com.google.gson.annotations.SerializedName

data class Heartbeat(

    @SerializedName("room_id")
    val roomId: Int,
//    val agoraUid: Int
)