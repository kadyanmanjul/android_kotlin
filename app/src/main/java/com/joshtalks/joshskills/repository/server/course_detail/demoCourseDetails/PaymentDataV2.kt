package com.joshtalks.joshskills.repository.server.course_detail.demoCourseDetails


import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

@Parcelize
data class PaymentDataV2(

    @SerializedName("actual_amount")
    val actualAmount: String,

    @SerializedName("discounted_amount")
    val discountedAmount: String,

    @SerializedName("discount_text")
    val discountText: String?,

    @SerializedName("test_id")
    val testId: Int,

    @SerializedName("whatsapp_url")
    val whatsappUrl: String?

) : Parcelable
