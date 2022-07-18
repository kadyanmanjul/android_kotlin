package com.joshtalks.joshskills.repository.server


import com.google.gson.annotations.SerializedName

data class FreemiumPaymentResponse(
    @SerializedName("test_id")
    val testId: String?,

    @SerializedName("actual_amount")
    val actualAmount: String?,

    @SerializedName("discounted_amount")
    val discountedAmount: String?,

    @SerializedName("heading")
    val heading: String?,

    @SerializedName("button_text")
    val buttonText: String?,

    @SerializedName("encrypted_text")
    val encryptedText: String?,

    @SerializedName("sub_heading")
    val featureList: List<FreemiumPaymentFeature>,

    @SerializedName("coupon_details")
    val couponDetails: CouponDetails?,

    @SerializedName("course_name")
    val courseName: String?,

    @SerializedName("teacher_name")
    val teacherName: String?,

    @SerializedName("image_url")
    val imageUrl: String?,
)

data class FreemiumPaymentFeature(
    @SerializedName("feature")
    val feature: String?,

    @SerializedName("feature_desc")
    var featureDesc: String?,

    @SerializedName("freemium")
    val freemium: String,

    @SerializedName("premium")
    val premium: String
) {

    fun showFreemiumIcon(): Boolean? = freemium.toBooleanStrictOrNull()
    fun showPremiumIcon(): Boolean? = premium.toBooleanStrictOrNull()

}