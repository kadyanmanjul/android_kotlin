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
    var userSelectedOrder: Int = 100

) : Parcelable {
    constructor(choice: Choice) : this(
        id = choice.remoteId,
        userSelectedOrder = choice.userSelectedOrder
    )
}
