package com.joshtalks.joshskills.ui.userprofile.models

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

data class AwardHeader(
    @SerializedName("award_category_list")
    val awardCategoryList: List<AwardCategory>
)
@Parcelize
data class AwardCategory(
    @SerializedName("id")
    val id: Int?,
    @SerializedName("label")
    val label: String?,
    @SerializedName("sort_order")
    val sortOrder: Int?,
    @SerializedName("awards")
    var awards: List<Award>?
) : Parcelable

@Parcelize
data class Award(
    @SerializedName("id")
    val id: Int?,
    @SerializedName("award_text")
    val awardText: String?,
    @SerializedName("sort_order")
    val sortOrder: Int?,
    @SerializedName("date_text")
    var dateText:String?,
    @SerializedName("image_url")
    val imageUrl: String?,
    @SerializedName("award_description")
    val awardDescription: String?,
    @SerializedName("is_achieved")
    val is_achieved: Boolean = false,
    @SerializedName("is_seen")
    val isSeen: Boolean?,
    @SerializedName("count")
    val count: Int = 0,
    @SerializedName("date_list")
    val dateList: List<String?>?,

    var recentDate:String?


) : Parcelable