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
    val title: String,

    @SerializedName("image_url")
    val imageUrl: String,

    @SerializedName("description")
    val description: String,

    @SerializedName("type")
    val type: AssessmentType,

    @SerializedName("progress_status")
    val status: AssessmentStatus,

    @SerializedName("questions")
    val questions: List<AssessmentQuestionResponse>,

    @SerializedName("intro")
    val intro: List<AssessmentIntroResponse>

) : Parcelable

enum class AssessmentType(val type: String) {

    @SerializedName("QUIZ")
    QUIZ("QUIZ"),

    @SerializedName("TEST")
    TEST("TEST")
}

enum class AssessmentMediaType(val mediaType: String) {

    @SerializedName("IMAGE")
    IMAGE("IMAGE"),

    @SerializedName("AUDIO")
    AUDIO("AUDIO"),

    @SerializedName("VIDEO")
    VIDEO("VIDEO"),

    @SerializedName("NONE")
    NONE("NONE"),

}

enum class AssessmentStatus(val status: String) {

    @SerializedName("NOT_STARTED")
    NOT_STARTED("NOT_STARTED"),

    @SerializedName("STARTED")
    STARTED("STARTED"),

    @SerializedName("COMPLETED")
    COMPLETED("COMPLETED")
}
