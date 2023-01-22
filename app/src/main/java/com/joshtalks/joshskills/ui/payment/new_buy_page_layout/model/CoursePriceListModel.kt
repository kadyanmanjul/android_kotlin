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
    val savingInCourse: String? = null, //Not needed will calculate on frontend
    @SerializedName("course_type")
    val courseType: String? = null, //Not needed
    @SerializedName("button_text")
    val courseName: String? = null,
    @SerializedName("encrypted_text")
    val encryptedText: String? = null,//Not needed
    @SerializedName("test_id")
    val testId: String? = null, //Not needed
    @SerializedName("coupon_text")
    val couponText: String? = null,//Not needed
    @SerializedName("is_recommended")
    val isRecommended: Boolean? = false,
    @SerializedName("image_url")
    val imageUrl: String? = null,//Not needed get from config
    @SerializedName("teacher_name")
    val teacherName: String? = null, //Not needed get from config
    @SerializedName("per_day")
    var perDayPrice: String? = null, //Not needed will calculate on frontend
    @SerializedName("sub_text")
    var subText: List<String>? = null
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
