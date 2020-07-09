package com.joshtalks.joshskills.repository.server.assessment


import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Assessment(

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

    @SerializedName("questions")
    val questions: List<Question>

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
    VIDEO("VIDEO")
}
