package com.joshtalks.joshskills.repository.server.assessment


import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

@Parcelize
data class AssessmentResponse(

    @SerializedName("id")
    val id: Int,

    @SerializedName("heading")
    val heading: String,

    @SerializedName("title")
    val title: String?,

    @SerializedName("icon_url")
    val iconUrl: String?,

    @SerializedName("text1")
    val text1: String?,

    @SerializedName("text2")
    val text2: String?,

    @SerializedName("score_text")
    val scoreText: String?,

    @SerializedName("image_url")
    val imageUrl: String?,

    @SerializedName("description")
    val description: String?,

    @SerializedName("type")
    val type: AssessmentType,

    @SerializedName("progress_status")
    val status: AssessmentStatus,

    @SerializedName("questions")
    val questions: List<AssessmentQuestionResponse>,

    @SerializedName("intro")
    val intro: List<AssessmentIntroResponse>?

) : Parcelable

enum class AssessmentType(val type: String) {

    @SerializedName("QUIZ")
    QUIZ("QUIZ"),

    @SerializedName("TEST")
    TEST("TEST")
}

enum class AssessmentMediaType(val mediaType: String, val intValue: Int) {

    @SerializedName("IMAGE")
    IMAGE("IMAGE", 0),

    @SerializedName("AUDIO")
    AUDIO("AUDIO", 1),

    @SerializedName("VIDEO")
    VIDEO("VIDEO", 2),

    @SerializedName("NONE")
    NONE("NONE", -1),

}

enum class AssessmentStatus(val status: String) {

    @SerializedName("NOT_STARTED")
    NOT_STARTED("NOT_STARTED"),

    @SerializedName("STARTED")
    STARTED("STARTED"),

    @SerializedName("COMPLETED")
    COMPLETED("COMPLETED")
}
