package com.joshtalks.joshskills.repository.server.chat_message

import com.google.gson.annotations.SerializedName

data class UpdateQuestionStatus(

    @SerializedName("question_id")
    private var questionId: String?,

    @SerializedName("status")
    private var status: String,

    @SerializedName("video")
    private var video: Boolean = false,

    @SerializedName("show_leaderboard_animation")
    private var show_leaderboard_animation: Boolean = false,

    @SerializedName("correct_questions")
    private var correctQuestions: ArrayList<Int> = ArrayList()

)
