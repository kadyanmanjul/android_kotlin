package com.joshtalks.joshskills.repository.server.conversation_practice

import com.google.gson.annotations.SerializedName

data class SubmitConversationPractiseRequest(
    @SerializedName("conversation_practice_id") val conversationPracticeId: String,
    @SerializedName("answer_audio_url") val answerAudioUrl: String,
    @SerializedName("duration") val audioDuration: Int,
    @SerializedName("title") val title: String,
    @SerializedName("text") val text: String,
    @SerializedName("quiz") val quiz: List<Quiz>?

)

data class Quiz(
    @SerializedName("id") val id: Int,
    @SerializedName("is_attempted") val isAttempted: Boolean,
    @SerializedName("answers") val answers: List<Answer>

)

data class Answer(
    @SerializedName("id") val id: Int // 2
)
/*

fun QuizModel.toQuiz() = Quiz(
    id = id,
    isAttempted = isAttempted,
    answers = answersModel.filter { it.isSelectedByUser }.map { Answer(it.id) }
)*/
