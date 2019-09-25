package com.joshtalks.joshskills.repository.local.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable


class ListenGraph : Serializable {

    @SerializedName("start_time")
    var startTime: Long = 0
    @SerializedName("end_time")
    var endTime: Long = 0

    constructor(startTime: Long) {
        this.startTime = startTime
    }

    constructor(startTime: Long, endTime: Long) {
        this.startTime = startTime
        this.endTime = endTime
    }
}
