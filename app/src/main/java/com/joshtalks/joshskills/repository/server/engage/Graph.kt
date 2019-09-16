package com.joshtalks.joshskills.repository.server.engage


import com.google.gson.annotations.SerializedName

data class Graph(
    @SerializedName("end")
    val end: Int,
    @SerializedName("start")
    val start: Int
)