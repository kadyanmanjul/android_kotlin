package com.joshtalks.joshskills.repository.server


import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class PaymentDetailsResponse(
    @SerializedName("amount")
    val amount: Double,
    @SerializedName("course_name")
    val courseName: String,
    @SerializedName("currency")
    val currency: String,
    @SerializedName("joshtalks_order_id")
    val joshtalksOrderId: Int,
    @SerializedName("razorpay_key_id")
    val razorpayKeyId: String,
    @SerializedName("razorpay_order_id")
    val razorpayOrderId: String,
    @SerializedName("discount_amount")
    val discountAmount: Double,
    @SerializedName("original_amount")
    val originalAmount: Double


) : Serializable