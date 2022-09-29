package com.joshtalks.joshskills.ui.payment.new_buy_page_layout.model

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
    @SerializedName("youtube_link") var youtubeLink:String?=null,
    @SerializedName("description") var teacherDesc: String? = null,
    @SerializedName("expire_time") var expiryTime: Date? = null,
    @SerializedName("suman_profile") var sumanProfile:String?=null,
    @SerializedName("call_us_text") var callUsText: String? = null,
    @SerializedName("course_name") var courseName:String?= null,
    @SerializedName("image_name") var otherCourseImage :String?=null
)

data class CouponListModel(
    @SerializedName("first_impression") var firstImpression: String,
    @SerializedName("coupons") var listOfCoupon: MutableList<Coupon>? = null
)

data class Coupon(
    @SerializedName("coupon_code") val couponCode: String,
    @SerializedName("amount_percent") val amountPercent: Int,
    @SerializedName("expire_at") val validDuration: Date,
    @SerializedName("max_discount_amount") val maxDiscountAmount: Int,
    var isCouponSelected: Int = 0
)