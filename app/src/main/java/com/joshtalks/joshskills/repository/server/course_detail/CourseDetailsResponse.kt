package com.joshtalks.joshskills.repository.server.course_detail


import com.google.gson.annotations.SerializedName
import com.joshtalks.joshskills.repository.local.entity.BASE_MESSAGE_TYPE
import com.joshtalks.joshskills.repository.local.entity.VideoType
import java.io.Serializable

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
    var videoObj: VideoType? = null

) : Serializable


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
)


data class Test(
    @SerializedName("amount")
    val amount: Double,
    @SerializedName("show_amount")
    val showAmount: Double,
    @SerializedName("thumbnail")
    val thumbnail: String,
    @SerializedName("video_link")
    val videoLink: String

)


enum class CARD_STATE(val state: String) {
    NM("NM"), SM("SM"), CO("CO"), UCO("UCO"), ML("ML")
//Co - collaspse , // sm- show more // uco- uncollplse // nm -normal //Multiline
}