package com.joshtalks.joshskills.premium.ui.payment.new_buy_page_layout.model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize
import java.util.*

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