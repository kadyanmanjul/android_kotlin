package com.joshtalks.joshskills.ui.lesson.conversationRoom.model

import com.google.gson.annotations.SerializedName

data class CreateConversionRoomRequest(
    @SerializedName("mentor_id") val mentorId: String,
    @SerializedName("topic") val topic: String,
    @SerializedName("is_favourite_practice_partner") val isFavouritePracticePartner: Boolean?,
    @SerializedName("conversation_question_id") val conversationQuestionId: Int?
)
