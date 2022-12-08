package com.joshtalks.joshskills.voip.communication.model

import com.google.gson.annotations.SerializedName
import com.joshtalks.joshskills.voip.Utils

class Timeout(@field:SerializedName("type") private val type: Int) : OutgoingData {

    override fun getAddress(): String? {
        return Utils.uuid
    }

    override fun getType(): Int {
        return type
    }
}