package com.joshtalks.joshskills.repository.server.course_detail

import com.google.gson.annotations.SerializedName
import java.util.Date

data class CourseDetailsResponseV2(

    @SerializedName("cards")
    val cards: List<Card>,

    @SerializedName("version")
    val version: String,

    @SerializedName("payment_data")
    val paymentData: PaymentData,

    @SerializedName("is_free_trial")
    val isFreeTrial: Boolean = false,

    @SerializedName("expiry_time")
    val expiredDate: Date
)
