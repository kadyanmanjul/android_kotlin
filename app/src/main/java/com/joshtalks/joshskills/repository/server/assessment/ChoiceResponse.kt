package com.joshtalks.joshskills.repository.server.assessment


import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

@Parcelize
data class ChoiceResponse(

    @SerializedName("id")
    val id: Int,

    @SerializedName("text")
    val text: String?,

    @SerializedName("image_url")
    val imageUrl: String?,

    @SerializedName("is_correct")
    val isCorrect: Boolean,

    @SerializedName("sort_order")
    val sortOrder: Int,

    @SerializedName("answer_order")
    val correctAnswerOrder: Int,

    @SerializedName("card_side")
    val column: ChoiceColumn,

    @SerializedName("selected_order")
    var userSelectedOrder: Int = 100,

    @SerializedName("is_selected_by_user")
    var isSelectedByUser: Boolean = false

) : Parcelable

enum class ChoiceColumn(val type: String) {

    @SerializedName("LEFT")
    LEFT("LEFT"),

    @SerializedName("RIGHT")
    RIGHT("RIGHT")

}
