package com.joshtalks.joshskills.repository.server.course_detail


import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import com.joshtalks.joshskills.repository.local.entity.BASE_MESSAGE_TYPE
import com.joshtalks.joshskills.repository.local.entity.DataBaseClass
import kotlinx.android.parcel.Parcelize

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


@Parcelize
data class VideoModel(
    @SerializedName("video_url") var video_url: String? = "",

    @SerializedName("id") var id: String = "",

    @SerializedName("thumbnail_url") var video_image_url: String = "",

    @SerializedName("duration") var duration: Int = 0,

    @ColumnInfo
    @SerializedName("video_height") var video_height: Int = 0,

    @ColumnInfo
    @SerializedName("video_width") var video_width: Int = 0,

    @ColumnInfo
    @SerializedName("thumbnail_height") var thumbnail_height: Int = 0,

    @ColumnInfo
    @SerializedName("thumbnail_width") var thumbnail_width: Int = 0,

    @ColumnInfo
    @SerializedName("bit_rate") var bit_rate: Int = 0,

    @ColumnInfo
    @SerializedName("is_deleted") var is_deleted: Boolean = false

) : Parcelable


enum class CARD_STATE(val state: String) {
    NM("NM"), SM("SM"), CO("CO"), UCO("UCO"), ML("ML")
//Co - collaspse , // sm- show more // uco- uncollplse // nm -normal //Multiline
}