package com.joshtalks.joshskills.voip.communication.model

import com.google.gson.annotations.SerializedName

class Timeout(@field:SerializedName("type") private val type: Int) : OutgoingData {

    override fun getType(): Int {
        return type
    }
}