package com.joshtalks.joshskills.common.repository.server

import com.google.gson.annotations.SerializedName

class CreateOrderResponse(
    @SerializedName("test_id")
    val testId: String,
    @SerializedName("gaid")
    val gaid: String,
    @SerializedName("mobile")
    val mobile: String,
    @SerializedName("encrypted_text")
    val encryptedText: String,
    @SerializedName("mentor_id")
    var mentorId: String?
)