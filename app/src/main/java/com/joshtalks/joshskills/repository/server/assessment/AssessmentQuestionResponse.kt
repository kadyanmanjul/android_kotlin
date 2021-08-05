package com.joshtalks.joshskills.repository.server.assessment


import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import com.joshtalks.joshskills.core.EMPTY
import kotlinx.android.parcel.Parcelize

@Parcelize
data class AssessmentQuestionResponse(

    @SerializedName("id")
    val id: Int,

    @SerializedName("text")
    val text: String,

    @SerializedName("sub_text")
    val subText: String = EMPTY,

    @SerializedName("sort_order")
    val sortOrder: Int,

    @SerializedName("media_url")
    val mediaUrl: String = EMPTY,

    @SerializedName("media_type")
    val mediaType: AssessmentMediaType = AssessmentMediaType.NONE,

    @SerializedName("media_url_2")
    val mediaUrl2: String = EMPTY,

    @SerializedName("media_type_2")
    val mediaType2: AssessmentMediaType = AssessmentMediaType.NONE,

    @SerializedName("video_thumbnail_url")
    val videoThumbnailUrl: String?,

    @SerializedName("choice_type")
    val choiceType: ChoiceType,

    @SerializedName("choices")
    val choices: List<ChoiceResponse>,

    @SerializedName("revise_concept")
    val reviseConcept: ReviseConceptResponse?,

    @SerializedName("feedback")
    val feedback: AssessmentQuestionFeedbackResponse? = null,

    @SerializedName("is_attempted")
    val isAttempted: Boolean = false,

    @SerializedName("is_new_header")
    val isNewHeader: Boolean = false,

    @SerializedName("status")
    val status: QuestionStatus = QuestionStatus.NONE,

    @SerializedName("list_of_answers")
    val listOfAnswers: List<String>? = arrayListOf()

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

    @SerializedName("ARRANGE_THE_SENTENCE")
    ARRANGE_THE_SENTENCE("ARRANGE_THE_SENTENCE"),

    @SerializedName("ARRANGE_THE_SENTENCE_NEW")
    ARRANGE_THE_SENTENCE_NEW("ARRANGE_THE_SENTENCE_NEW"),

    @SerializedName("INPUT_TEXT")
    INPUT_TEXT("INPUT_TEXT"),

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
