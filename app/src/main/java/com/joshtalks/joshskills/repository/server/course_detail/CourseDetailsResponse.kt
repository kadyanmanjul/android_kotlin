package com.joshtalks.joshskills.repository.server.course_detail


import com.google.gson.annotations.SerializedName

data class CourseDetailsResponse(
    @SerializedName("course_id")
    var courseId: Int,
    @SerializedName("course_description")
    val courseDescription: String,
    @SerializedName("bg_image")
    val bgImage: String,
    @SerializedName("course_name")
    val courseName: String,
    @SerializedName("course_discount_price")
    val courseDiscountPrice: Double,
    @SerializedName("course_duration")
    val courseDuration: Int,
    @SerializedName("course_information")
    val courseInformation: List<CourseInformation> = emptyList(),
    @SerializedName("course_mentor")
    val courseMentor: List<CourseMentor> = emptyList(),
    @SerializedName("course_price")
    val coursePrice: Double,
    @SerializedName("course_rating")
    val courseRating: Double,
    @SerializedName("course_title")
    val courseTitle: String,
    @SerializedName("feedback_image_url")
    val feedbackImageUrl: String,
    @SerializedName("course_enrolled_user")
    val courseEnrolledUser: Long,
    @SerializedName("video_thumbnail")
    val videoThumbnail: String,
    @SerializedName("video_url")
    val videoUrl: String,
    @SerializedName("course_mentor_image_url")
    val courseMentorImageUrl: String,
    @SerializedName("course_structure")
    val courseStructure: List<CourseStructure> = emptyList()
)