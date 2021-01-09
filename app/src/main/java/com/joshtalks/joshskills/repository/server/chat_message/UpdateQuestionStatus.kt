package com.joshtalks.joshskills.repository.server.chat_message

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class UpdateQuestionStatus(
    @SerializedName("status")
    @Expose
    private var status: String,
    @SerializedName("lesson_id")
    @Expose
    private var lessonId: Int,
    @SerializedName("mentor_id")
    @Expose
    private var mentorId: String,
    @SerializedName("question_id")
    @Expose
    private var questionId: Int,
    @SerializedName("course_id")
    @Expose
    private var courseId: Int,
    @SerializedName("video")
    @Expose
    private var video: Boolean=false,
    @SerializedName("show_leaderboard_animation")
    @Expose
    private var show_leaderboard_animation: Boolean=false,
    @SerializedName("correct_questions")
    @Expose
    private var correctQuestions: ArrayList<Int> = ArrayList()
)
