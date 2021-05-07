package com.joshtalks.joshskills.repository.server.assessment


import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import com.joshtalks.joshskills.core.EMPTY
import kotlinx.android.parcel.Parcelize

@Parcelize
data class OnlineTestResponse(

    @SerializedName("completed")
    val completed: Boolean,

    @SerializedName("message")
    val message: String = EMPTY,

    @SerializedName("question")
    val question: AssessmentQuestionResponse? = null

) : Parcelable
