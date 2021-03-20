package com.joshtalks.joshskills.repository.server.course_detail.demoCourseDetails

import com.google.gson.annotations.SerializedName
import com.joshtalks.joshskills.repository.server.course_detail.Card

data class DemoCourseDetailsResponse(

    @SerializedName("cards")
    val cards: List<Card>,

    @SerializedName("version")
    val version: String?,

    @SerializedName("payment_data")
    val paymentData: PaymentDataV2
)
