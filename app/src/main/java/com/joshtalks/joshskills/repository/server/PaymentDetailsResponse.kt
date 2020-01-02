package com.joshtalks.joshskills.repository.server


import com.google.gson.annotations.SerializedName

data class PaymentDetailsResponse(
    @SerializedName("amount")
    val amount: Int,
    @SerializedName("course_name")
    val courseName: String,
    @SerializedName("currency")
    val currency: String,
    @SerializedName("joshtalks_order_id")
    val joshtalksOrderId: Int,
    @SerializedName("razorpay_key_id")
    val razorpayKeyId: String,
    @SerializedName("razorpay_order_id")
    val razorpayOrderId: String
)