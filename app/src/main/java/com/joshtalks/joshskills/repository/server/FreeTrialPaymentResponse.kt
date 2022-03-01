package com.joshtalks.joshskills.repository.server


import com.google.gson.annotations.SerializedName
import java.util.Date

data class FreeTrialPaymentResponse(
    @SerializedName("course_data")
    val courseData: List<CourseData>?,
    @SerializedName("expire_time")
    val expireTime: Date?,
    @SerializedName("start_time")
    val startTime: Double?,
    @SerializedName("sub_headings")
    val subHeadings: List<List<String>>?,
    @SerializedName("coupon_details")
    val couponDetails: CouponDetails
)


data class CourseData(
    @SerializedName("actual_amount")
    val actualAmount: String?,
    @SerializedName("button_text")
    val buttonText: String?,
    @SerializedName("course_heading")
    val courseHeading: String?,
    @SerializedName("course_name")
    val courseName: String?,
    @SerializedName("course_type")
    val courseType: String?,
    @SerializedName("discount")
    val discount: String?,
    @SerializedName("encrypted_text")
    val encryptedText: String?,
    @SerializedName("image_url")
    val imageUrl: String?,
    @SerializedName("rating")
    val rating: Double?,
    @SerializedName("ratings_count")
    val ratingsCount: Int?,
    @SerializedName("savings")
    val savings: String?,
    @SerializedName("teacher_name")
    val teacherName: String?,
    @SerializedName("heading")
    val heading: String,
    @SerializedName("per_course_price")
    val perCoursePrice: String?,
    @SerializedName("test_id")
    val testId: String
)