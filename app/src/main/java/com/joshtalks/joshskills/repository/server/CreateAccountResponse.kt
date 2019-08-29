package com.joshtalks.joshskills.repository.server

import com.google.gson.annotations.SerializedName


data class CreateAccountResponse(

    @SerializedName("mentor_id") val mentor_id: String,
    @SerializedName("token") val token: ProfileToken,
    @SerializedName("user_id") val user_id: String
)
