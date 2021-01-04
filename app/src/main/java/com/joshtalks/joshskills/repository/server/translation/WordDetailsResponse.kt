package com.joshtalks.joshskills.repository.server.translation


import com.google.gson.annotations.SerializedName

data class WordDetailsResponse(
    @SerializedName("message")
    val message: String,
    @SerializedName("response_data")
    val translationData: List<TranslationData>,
    @SerializedName("Success")
    val success: Boolean
)

data class TranslationData(
    @SerializedName("eng_meaning")
    val engMeaning: List<EngMeaning>,
    @SerializedName("fast_pronunciation")
    val fastPronunciation: String,
    @SerializedName("hin_meaning")
    val hinMeaning: String,
    @SerializedName("hin_transilteration")
    val hinTransilteration: String,
    @SerializedName("id")
    val id: Int,
    @SerializedName("slow_pronunciation")
    val slowPronunciation: String,
    @SerializedName("word")
    val word: String
)


data class EngMeaning(
    @SerializedName("Adjective")
    val adjective: String? = null,
    @SerializedName("Noun")
    val noun: String? = null,
    @SerializedName("Verb")
    val verb: String? = null
)