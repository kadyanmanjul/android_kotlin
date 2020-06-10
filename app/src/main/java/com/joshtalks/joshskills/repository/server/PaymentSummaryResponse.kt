package com.joshtalks.joshskills.repository.server


import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

@Parcelize
data class PaymentSummaryResponse(
    @SerializedName("name")
    val courseName: String,
    @SerializedName("total_enrolled")
    val totalEnrolled: Int,
    @SerializedName("rating")
    val rating: Double,
    @SerializedName("coupon_details")
    val couponDetails: CouponDetails,
    @SerializedName("teacher_name")
    val teacherName: String,
    @SerializedName("features")
    val features: String?,
    @SerializedName("image")
    val imageUrl: String,
    @SerializedName("amount")
    val amount: Double,
    @SerializedName("encrypted_text")
    val encryptedText: String,
    @SerializedName("currency")
    val currency: String,
    @SerializedName("discounted_amount")
    val discountedAmount: Double
) : Parcelable

@Parcelize
data class CouponDetails(
    @SerializedName("id")
    val id: Int,
    @SerializedName("name")
    val name: String,
    @SerializedName("title")
    val title: String,
    @SerializedName("header")
    val header: String,
    @SerializedName("validity")
    val validity: String,
    @SerializedName("discount")
    val discount: Int,
    @SerializedName("discount_type")
    val discountType: DISCOUNT_TYPE
) : Parcelable

enum class DISCOUNT_TYPE(val state: String) {
    PERCENTAGE("PERCENTAGE"), OTHER("OTHER")
}
