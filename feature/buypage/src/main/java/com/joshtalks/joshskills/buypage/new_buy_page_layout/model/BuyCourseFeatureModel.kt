package com.joshtalks.joshskills.buypage.new_buy_page_layout.model

import com.google.gson.annotations.SerializedName
import java.util.*

data class BuyCourseFeatureModel(
    @SerializedName("features") var features: List<String>? = null,
    @SerializedName("information") var information: List<String>? = null,
    @SerializedName("rating") var rating: Float? = null,
    @SerializedName("ratings_count") var ratingCount: Int? = null,
    @SerializedName("image") var teacherImage: String? = null,
    @SerializedName("video") var video: String? = null,
    @SerializedName("teacher_name") var teacherName: String? = null,
    @SerializedName("youtube_channel") var youtubeChannel: String? = null,
    @SerializedName("youtube_link") var youtubeLink: String? = null,
    @SerializedName("description") var teacherDesc: String? = null,
    @SerializedName("expire_time") var expiryTime: Date? = null,
    @SerializedName("suman_profile") var sumanProfile: String? = null,
    @SerializedName("call_us_text") var callUsText: String? = null,
    @SerializedName("course_name") var courseName: String? = null,
    @SerializedName("image_name") var otherCourseImage: String? = null,
    @SerializedName("certificate_text") var certificateText: String? = null,
    @SerializedName("certificate_url") var certificateUrl: String? = null,
    @SerializedName("know_more") var knowMore: String? = null,
    @SerializedName("bp_text") val priceEnglishText: String? = null,
    @SerializedName("banner_text") val timerBannerText: String? = null,
    @SerializedName("ab_test_video_url") val abTestVideoUrl: String? = null,
    @SerializedName("is_video") val isVideo: Boolean? = null,
    @SerializedName("is_call_us_active") var isCallUsActive: Int? = null,
    @SerializedName("payment_button_text") var paymentButtonText: Int = 0
)