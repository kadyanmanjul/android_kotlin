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

    @SerializedName("answer_order")
    val correctAnswerOrder: Int,

    @SerializedName("card_side")
    val column: ChoiceColumn,

    @SerializedName("selected_order")
    val userSelectedOrder: Int = -1,

    @SerializedName("is_selected_by_user")
    val isSelectedByUser: Boolean = false

) : Parcelable

enum class ChoiceColumn(val type: String) {

    @SerializedName("LEFT")
    LEFT("LEFT"),

    @SerializedName("RIGHT")
    RIGHT("RIGHT")

}
