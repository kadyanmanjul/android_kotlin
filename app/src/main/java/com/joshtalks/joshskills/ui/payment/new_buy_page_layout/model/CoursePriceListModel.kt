package com.joshtalks.joshskills.ui.payment.new_buy_page_layout.model

import com.google.gson.annotations.SerializedName

data class CoursePriceListModel(
    @SerializedName("course_details")
    val courseDetails: List<CourseDetailsList>? = null
)

data class CourseDetailsList(
    @SerializedName("actual_amount")
    val actualAmount: String? = null,
    @SerializedName("discount")
    val discountedPrice: String? = null,
    @SerializedName("savings")
    val savingInCourse: String? = null,
    @SerializedName("course_type")
    val courseType: String? = null,
    @SerializedName("button_text")
    val courseName: String? = null,
    @SerializedName("encrypted_text")
    val encryptedText: String? = null,
    @SerializedName("test_id")
    val testId: String? = null,
    @SerializedName("coupon_text")
    val couponText: String? = null,
    @SerializedName("is_recommended")
    val isRecommended: Boolean? = false,
    @SerializedName("image_url")
    val imageUrl: String? = null,
    @SerializedName("teacher_name")
    val teacherName: String? = null,
    @SerializedName("per_day")
    var perDayPrice: String? = null
)

data class PriceParameterModel(
    @SerializedName("gaid")
    val gaid: String,
    @SerializedName("test_id")
    val testId: Int,
    @SerializedName("code")
    val code: String? = null,
    @SerializedName("is_mentor_coupon")
    val isSpecificMentorCoupon: Boolean?= null
)
