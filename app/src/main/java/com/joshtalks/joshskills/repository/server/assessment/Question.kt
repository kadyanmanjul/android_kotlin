package com.joshtalks.joshskills.repository.server.assessment


import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Question(

    @SerializedName("id")
    val id: Int,

    @SerializedName("text")
    val text: String,

    @SerializedName("sort_order")
    val sortOrder: Int,

    @SerializedName("media_url")
    val mediaUrl: String,

    @SerializedName("media_type")
    val mediaType: AssessmentMediaType,

    @SerializedName("video_thumbnail_url")
    val videoThumbnailUrl: String?,

    @SerializedName("choice_type")
    val choiceType: ChoiceType,

    @SerializedName("choices")
    val choices: List<Choice>,

    @SerializedName("revise_concept")
    val reviseConcept: ReviseConcept?,

    @SerializedName("is_attempted")
    val isAttempted: Boolean = false,

    @SerializedName("status")
    val status: QuestionStatus = QuestionStatus.NONE

) : Parcelable

enum class ChoiceType(val type: String) {

    @SerializedName("SINGLE_SELECTION_TEXT")
    SINGLE_SELECTION_TEXT("SINGLE_SELECTION_TEXT"),

    @SerializedName("MULTI_SELECTION_TEXT")
    MULTI_SELECTION_TEXT("MULTI_SELECTION_TEXT"),

    @SerializedName("MATCH_TEXT")
    MATCH_TEXT("MATCH_TEXT"),

    @SerializedName("SINGLE_SELECTION_IMAGE")
    SINGLE_SELECTION_IMAGE("SINGLE_SELECTION_IMAGE"),

    @SerializedName("MULTI_SELECTION_IMAGE")
    MULTI_SELECTION_IMAGE("MULTI_SELECTION_IMAGE"),

    @SerializedName("FILL_IN_THE_BLANKS_TEXT")
    FILL_IN_THE_BLANKS_TEXT("FILL_IN_THE_BLANKS_TEXT"),

}

enum class QuestionStatus(val status: String) {

    @SerializedName("NONE")
    NONE("NONE"),

    @SerializedName("WRONG")
    WRONG("WRONG"),

    @SerializedName("CORRECT")
    CORRECT("CORRECT"),

    @SerializedName("SKIPPED")
    SKIPPED("SKIPPED")
}
