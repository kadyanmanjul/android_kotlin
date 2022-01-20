package com.joshtalks.joshskills.conversationRoom.model

import com.google.gson.annotations.SerializedName

data class ConversationRoomDetailsResponse(
    @SerializedName("id") val questionId: String? = null,
    @SerializedName("duration") val duration: Int? = null,
    @SerializedName("already_conversed") val alreadyConversed: Int? = null,
    @SerializedName("is_favourite_practice_partner_available") val is_favourite_practice_partner_available: Boolean
)

