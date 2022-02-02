package com.joshtalks.joshskills.repository.server

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

@Parcelize
data class LinkAttribution(

    @SerializedName("mentor_id")
    val mentorId : String,

    @SerializedName("content_id")
    val contentId : String,

    @SerializedName("shared_item")
    val sharedItem : String,

    @SerializedName("shared_item_type")
    val sharedItemType : String,

    @SerializedName("deep_link")
    val deepLink : String,

    @SerializedName("course_id")
    val courseId: String

) : Parcelable
