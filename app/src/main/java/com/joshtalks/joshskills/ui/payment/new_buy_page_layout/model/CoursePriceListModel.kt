package com.joshtalks.joshskills.ui.payment.new_buy_page_layout.model

import com.google.gson.annotations.SerializedName

data class CoursePriceListModel(
    @SerializedName("course_details")
    val courseDetails: List<CourseDetailsList>? = null
)

data class CourseDetailsList(
    @SerializedName("actual_amount")
    val actualAmount: Int? = null,
    @SerializedName("discount")
    val discountedPrice: Int? = null,
    @SerializedName("button_text")
    val courseName: String? = null,
    @SerializedName("encrypted_text")
    val encryptedText: String? = null,
    @SerializedName("is_recommended")
    val isRecommended: Boolean? = false,
    @SerializedName("per_day")
    var perDayPrice: String? = null,
    @SerializedName("sub_text")
    var subText: List<String>? = null,
    @SerializedName("course_text")
    var courseText: List<String>? = null,
    @SerializedName("test_id")
    var testId:String? = null,
    @SerializedName("title")
    var courseTitle:String? = null
)

data class PriceParameterModel(
    @SerializedName("code")
    val code: String? = null,
)
