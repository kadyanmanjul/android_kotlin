package com.joshtalks.joshskills.repository.server

import com.google.gson.annotations.SerializedName

data class GetGuestMentorIdResponse (
    @SerializedName("guest_mentor_id")
    val guestMentorId : String
)