package com.joshtalks.joshskills.repository.server.assessment


import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import com.joshtalks.joshskills.repository.local.model.assessment.Choice
import kotlinx.android.parcel.Parcelize

@Parcelize
data class ChoiceRequest(

    @SerializedName("id")
    val id: Int,

    @SerializedName("selected_order")
    var userSelectedOrder: Int? = 100,

    @SerializedName("right_matching")
    var rightMatchingId: Int? = 100,

    @SerializedName("is_correct")
    val isCorrect: Boolean = false

) : Parcelable {
    constructor(choice: Choice) : this(
        id = choice.remoteId,
        userSelectedOrder = choice.userSelectedOrder,
        rightMatchingId = null,
        isCorrect = choice.isCorrect
    )

    constructor(choice: Choice, choiceList: List<Choice>) : this(
        id = choice.remoteId,
        rightMatchingId = if (choice.isSelectedByUser) {
            when (choice.column) {
                ChoiceColumn.RIGHT -> {
                    choiceList.filter { it.column == ChoiceColumn.RIGHT }.sortedBy { it.sortOrder }
                        .get(choice.userSelectedOrder).remoteId
                }
                ChoiceColumn.LEFT -> {
                    choiceList.filter { it.column == ChoiceColumn.LEFT }.sortedBy { it.sortOrder }
                        .get(choice.userSelectedOrder).remoteId
                }
            }

        } else {
            100
        },
        userSelectedOrder = choice.userSelectedOrder.plus(1),
        isCorrect = choice.isCorrect
    )
}
