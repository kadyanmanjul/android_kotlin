package com.joshtalks.joshskills.ui.payment.new_buy_page_layout.model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize
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
    @SerializedName("youtube_link") var youtubeLink:String?=null,
    @SerializedName("description") var teacherDesc: String? = null,
    @SerializedName("expire_time") var expiryTime: Date? = null,
    @SerializedName("suman_profile") var sumanProfile:String?=null,
    @SerializedName("call_us_text") var callUsText: String? = null,
    @SerializedName("course_name") var courseName:String?= null,
    @SerializedName("image_name") var otherCourseImage :String?=null,
    @SerializedName("certificate_text") var certificateText :String?=null,
    @SerializedName("certificate_url") var certificateUrl: String? = null,
    @SerializedName("know_more") var knowMore: String? = null,
    @SerializedName("bp_text") val priceEnglishText: String? = null,
    @SerializedName("banner_text") val timerBannerText: String? = null,
    @SerializedName("ab_test_video_url") val abTestVideoUrl: String? = null,
    @SerializedName("is_video") val isVideo: Boolean? = null,
    @SerializedName("is_call_us_active") var isCallUsActive:Int?= null
)

data class CouponListModel(
    @SerializedName("coupons") var listOfCoupon: MutableList<Coupon>? = null
)

@Parcelize
data class Coupon(
    @SerializedName("coupon_code") val couponCode: String,
    @SerializedName("title") val title: String,
    @SerializedName("expire_at") val validDuration: Date ?= null,
    @SerializedName("max_discount_amount") val maxDiscountAmount: Int,
    @SerializedName("is_mentor_coupon") val isMentorSpecificCoupon: Boolean? = null,
    @SerializedName("description") val couponDesc: String? = null,
    @SerializedName("is_auto_apply") val isAutoApply: Boolean ?=null,
    @SerializedName("type") val couponType: String,
    @SerializedName("is_enabled") val isEnable:Boolean?=null,
    var isCouponSelected: Int = 0
) : Parcelable