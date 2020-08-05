package com.joshtalks.joshskills.repository.server.conversation_practice


import com.google.gson.annotations.SerializedName
import java.util.*

class SubmittedConversationPractiseModel(
    @SerializedName("answer_audio_url") val answerAudioUrl: String,
    @SerializedName("created") val created: Date,
    @SerializedName("text") val text: String,
    @SerializedName("title") val title: String,
    @SerializedName("duration") val duration: Int,
    var isPlaying: Boolean = false

)