package com.joshtalks.joshskills.explore.course_details.models

import com.google.gson.annotations.SerializedName

data class DemoCourseDetailsResponse(

    @SerializedName("cards")
    val cards: List<Card>,

    @SerializedName("version")
    val version: String?,

    @SerializedName("payment_data")
    val paymentData: PaymentDataV2
)
