package com.joshtalks.joshskills.repository.server.course_detail


import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

@Parcelize
data class PaymentData(

    @SerializedName("actual_amount")
    val actualAmount: Double,

    @SerializedName("discounted_amount")
    val discountedAmount: Double,

    @SerializedName("discount_text")
    val discountText: String

) : Parcelable
