package com.joshtalks.joshskills.repository.server.certification_exam


import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

@Parcelize
data class CertificateExamReportModel(
    @SerializedName("answers")
    val answers: List<UserSelectedAnswer>,
    @SerializedName("heading")
    val heading: String,
    @SerializedName("is_exam_pass")
    val isExamPass: Boolean,
    @SerializedName("max_score")
    val maxScore: Int,
    @SerializedName("score")
    val score: Double,
    @SerializedName("text")
    val text: String
) : Parcelable

@Parcelize
data class UserSelectedAnswer(
    @SerializedName("answer")
    val answerId: Int?,
    @SerializedName("is_answer_correct")
    val isAnswerCorrect: Boolean,
    @SerializedName("question")
    val question: Int
) : Parcelable