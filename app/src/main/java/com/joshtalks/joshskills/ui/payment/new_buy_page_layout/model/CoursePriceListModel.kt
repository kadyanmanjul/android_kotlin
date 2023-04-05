package com.joshtalks.joshskills.ui.payment.new_buy_page_layout.model

import com.google.gson.annotations.SerializedName

data class CoursePriceListModel(
    @SerializedName("course_details")
    val courseDetails: List<CourseDetailsList>
)

data class CourseDetailsList(
    @SerializedName("actual_amount")
    val actualAmount: Int,
    @SerializedName("discount")
    val discountedPrice: Int,
    @SerializedName("button_text")
    val courseName: String,
    @SerializedName("encrypted_text")
    val encryptedText: String,
    @SerializedName("is_recommended")
    val isRecommended: Boolean,
    @SerializedName("per_day")
    var perDayPrice: String,
    @SerializedName("sub_text")
    var subText: List<String>,
    @SerializedName("course_text")
    var courseText: List<String>,
    @SerializedName("test_id")
    var testId:String,
    @SerializedName("title")
    var courseTitle:String
)
