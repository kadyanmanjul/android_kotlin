package com.joshtalks.joshskills.repository.server.assessment


import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Choice(

    @SerializedName("id")
    val id: Int,

    @SerializedName("text")
    val text: String?,

    @SerializedName("image_url")
    val imageUrl: String,

    @SerializedName("is_correct")
    val isCorrect: Boolean,

    @SerializedName("sort_order")
    val sortOrder: Int,

    @SerializedName("matching_left_text")
    val matchingLeftText: String?,

    @SerializedName("matching_right_text")
    val matchingRightText: String?,

    @SerializedName("correct_right_text")
    val correctRightText: String?,

    @SerializedName("answer_order")
    val answerOrder: Int

) : Parcelable
