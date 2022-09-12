package com.joshtalks.joshskills.repository.server


import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import com.joshtalks.joshskills.repository.local.entity.LESSON_STATUS
import com.joshtalks.joshskills.ui.userprofile.models.Award
import kotlinx.android.parcel.Parcelize
import java.util.*

data class UpdateLessonResponse(
    @SerializedName("award_mentor_list")
    val awardMentorList: List<Award>?,
    @SerializedName("message")
    val message: String?,
    @SerializedName("response_data")
    val responseData: LESSON_STATUS = LESSON_STATUS.NO,
    @SerializedName("Success")
    val success: Boolean?,
    @SerializedName("outranked")
    val outranked: Boolean?,
    @SerializedName("rank_data")
    val outrankedData: OutrankedDataResponse?,
    @SerializedName("points_list")
    val pointsList: List<String>?,
    @SerializedName("pop_up")
    val popUpText: PurchaseDataResponse?
)

@Parcelize
data class OutrankedDataResponse(
    @SerializedName("new")
    val new: RankData?,
    @SerializedName("old")
    val old: RankData?
) : Parcelable

@Parcelize
data class PurchaseDataResponse(
    @SerializedName("name")
    var name: PurchasePopupType?,
    @SerializedName("title")
    val popUpTitle: String?,
    @SerializedName("body")
    val popUpBody: String?,
    @SerializedName("price")
    val popUpPrice: String?,
    @SerializedName("expire_time")
    val expireTime: Date?,
    @SerializedName("is_coupon_popup")
    val isCouponPopup: Boolean = false,
    @SerializedName("coupon_code")
    val couponCode: String?,
    @SerializedName("coupon_expiry_text")
    val couponExpiry: String?,
) : Parcelable

enum class PurchasePopupType {
    CALL_COMPLETED,
    LESSON_LOCKED,
    GRAMMAR_COMPLETED,
    SPEAKING_COMPLETED,
    READING_COMPLETED,
    VOCAB_COMPLETED,
    LESSON_COMPLETED,
}

@Parcelize
data class RankData(
    @SerializedName("points")
    val points: Int?,
    @SerializedName("rank")
    val rank: Int?
) : Parcelable