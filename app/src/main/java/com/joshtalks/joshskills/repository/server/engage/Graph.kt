package com.joshtalks.joshskills.repository.server.engage


import com.google.gson.annotations.SerializedName
import java.io.Serializable

class Graph(@SerializedName("start") var startTime: Long) :Serializable {
    @SerializedName("end")
    var endTime: Long = 0
}