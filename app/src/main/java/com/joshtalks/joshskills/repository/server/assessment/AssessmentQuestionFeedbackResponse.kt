package com.joshtalks.joshskills.repository.server.assessment

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import com.joshtalks.joshskills.core.EMPTY
import kotlinx.android.parcel.Parcelize

@Parcelize
data class AssessmentQuestionFeedbackResponse(

    @SerializedName("id")
    val id: Int,

    @SerializedName("correct_answer_heading")
    val correctAnswerHeading: String = EMPTY,

    @SerializedName("correct_answer_text")
    val correctAnswerText: String = EMPTY,

    @SerializedName("wrong_answer_heading")
    val wrongAnswerHeading: String = EMPTY,

    @SerializedName("wrong_answer_text")
    val wrongAnswerText: String = EMPTY,

    @SerializedName("wrong_answer_heading2")
    val wrongAnswerHeading2: String = EMPTY,

    @SerializedName("wrong_answer_text2")
    val wrongAnswerText2: String = EMPTY,

    ) : Parcelable
