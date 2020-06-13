package com.joshtalks.joshskills.repository.server

import com.google.gson.annotations.SerializedName

data class OrderDetailResponse(
    @SerializedName("amount")
    val amount: Double,
    @SerializedName("currency")
    val currency: String,
    @SerializedName("joshtalks_order_id")
    val joshtalksOrderId: Int,
    @SerializedName("razorpay_key_id")
    val razorpayKeyId: String,
    @SerializedName("razorpay_order_id")
    val razorpayOrderId: String,
    @SerializedName("email")
    val email: String
)