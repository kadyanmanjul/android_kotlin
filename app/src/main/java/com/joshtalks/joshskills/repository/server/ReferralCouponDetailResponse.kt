package com.joshtalks.joshskills.repository.server

import com.google.gson.annotations.SerializedName

data class ReferralCouponDetailResponse(
    @SerializedName("referral_status")
    val referralStatus: Boolean,
    @SerializedName("referrer_name")
    val referrerName: String,
    @SerializedName("offer_text")
    val offerText: String
)