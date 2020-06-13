package com.joshtalks.joshskills.repository.server

import com.google.gson.annotations.SerializedName

class CreateOrderResponse(
    @SerializedName("test_id")
    val testId: String,
    @SerializedName("instance_id")
    val instanceId: String,
    @SerializedName("mobile")
    val mobile: String,
    @SerializedName("encrypted_text")
    val encryptedText: String,
    @SerializedName("mentor_id")
    var mentorId: String?
)