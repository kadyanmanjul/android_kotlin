package com.joshtalks.joshskills.repository.server.conversation_practice


import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import com.joshtalks.joshskills.repository.local.entity.AudioType
import kotlinx.android.parcel.Parcelize

@Parcelize
data class ConversationPractiseModel(
    @SerializedName("id") val id: Int,
    @SerializedName("title") val title: String,
    @SerializedName("sub_title") val subTitle: String,
    @SerializedName("person_a") val characterNameA: String,
    @SerializedName("person_b") val characterNameB: String,
    @SerializedName("person_a_url") val characterUrlA: String,
    @SerializedName("person_b_url") val characterUrlB: String,
    @SerializedName("listen") val listen: List<ListenModel>,
    @SerializedName("quiz") val quizModel: List<QuizModel>

) : Parcelable

@Parcelize
data class ListenModel(
    @SerializedName("id") val id: Int,
    @SerializedName("text") val text: String,
    @SerializedName("sort_order") val sortOrder: Int,
    @SerializedName("name") val name: String,
    @SerializedName("duration") val duration: Long,
    @SerializedName("audio") val audio: AudioType,
    var disable: Boolean = false,
    var viewType: Int = 0
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
