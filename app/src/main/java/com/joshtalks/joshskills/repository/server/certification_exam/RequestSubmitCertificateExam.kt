package com.joshtalks.joshskills.repository.server.certification_exam


import com.google.gson.annotations.SerializedName

data class RequestSubmitCertificateExam(
    @SerializedName("certificateexam_id")
    var certificateExamId: Int = -1,
    @SerializedName("attempt_no")
    var attemptNo: Int = 0,
    @SerializedName("answers")
    var answers: List<RequestSubmitAnswer> = emptyList(),
)

data class RequestSubmitAnswer(
    @SerializedName("answer")
    val answerId: Int?,
    @SerializedName("is_answer_correct")
    val isAnswerCorrect: Boolean,
    @SerializedName("question")
    val question: Int
)