package com.joshtalks.joshskills.explore.course_details.models

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import com.joshtalks.joshskills.common.repository.local.entity.BASE_MESSAGE_TYPE
import com.joshtalks.joshskills.common.repository.local.model.explore.VideoModel
import kotlinx.parcelize.Parcelize

@Parcelize
data class CourseDetailsResponse(
    @SerializedName("card_state")
    val cardState: CARD_STATE,
    @SerializedName("course")
    val course: Course,
    @SerializedName("description")
    val description: String?,
    @SerializedName("id")
    val id: Int,
    @SerializedName("sequence_no")
    val sequenceNo: Int,
    @SerializedName("test")
    val test: Test,
    @SerializedName("thumbnail")
    val thumbnail: String,
    @SerializedName("title")
    val title: String?,
    @SerializedName("type")
    val type: BASE_MESSAGE_TYPE,
    @SerializedName("url")
    val url: String,
    @SerializedName("video")
    var videoObj: VideoModel? = null

) : Parcelable


@Parcelize
data class Course(
    @SerializedName("description")
    val description: String,
    @SerializedName("duration")
    val duration: Int,
    @SerializedName("name")
    val name: String,
    @SerializedName("rating")
    val rating: Double,
    @SerializedName("total_enrolled")
    val totalEnrolled: Int
) : Parcelable


@Parcelize
data class Test(
    @SerializedName("amount")
    val amount: Double,
    @SerializedName("show_amount")
    val showAmount: Double,
    @SerializedName("thumbnail")
    val thumbnail: String,
    @SerializedName("video_link")
    val videoLink: String

) : Parcelable


enum class CARD_STATE(val state: String) {
    NM("NM"), SM("SM"), CO("CO"), UCO("UCO"), ML("ML")
//Co - collaspse , // sm- show more // uco- uncollplse // nm -normal //Multiline
}