package com.joshtalks.badebhaiya.liveroom.model

import com.google.gson.annotations.SerializedName

data class JoinConversionRoomRequest(
    @SerializedName("mentor_id") val mentorId: String,
    @SerializedName("room_id") val roomId: Int,
    @SerializedName("conversation_question_id") val conversationQuestionId: Int?
)
