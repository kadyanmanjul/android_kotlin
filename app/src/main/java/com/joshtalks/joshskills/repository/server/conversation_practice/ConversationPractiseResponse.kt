package com.joshtalks.joshskills.repository.server.conversation_practice


import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

@Parcelize
data class ConversationPractiseModel(
    @SerializedName("character_name_a") val characterNameA: String,
    @SerializedName("character_name_b") val characterNameB: String,
    @SerializedName("character_url_a") val characterUrlA: String,
    @SerializedName("character_url_b") val characterUrlB: String,
    @SerializedName("Id") val id: Int,
    @SerializedName("sub_title") val subTitle: String,
    @SerializedName("title") val title: String,
    @SerializedName("listen") val listen: List<ListenModel>,
    @SerializedName("quiz") val quizModel: List<QuizModel>

) : Parcelable

@Parcelize
data class ListenModel(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("audio_url") val audioUrl: String,
    @SerializedName("text") val text: String,
    @SerializedName("sort_order") val sortOrder: Int
) : Parcelable

@Parcelize
data class QuizModel(
    @SerializedName("id") val id: Int,
    @SerializedName("text") val text: String,
    @SerializedName("sort_order") val sortOrder: Int,
    @SerializedName("media_url") val mediaUrl: String,
    @SerializedName("answers") val answersModel: List<AnswersModel>

) : Parcelable

@Parcelize
data class AnswersModel(
    @SerializedName("id") val id: Int,
    @SerializedName("text") val text: String,
    @SerializedName("sort_order") val sortOrder: Int,
    @SerializedName("is_correct") val isCorrect: Boolean
) : Parcelable
