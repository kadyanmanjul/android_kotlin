package com.joshtalks.joshskills.repository.server.assessment


import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import com.joshtalks.joshskills.core.EMPTY
import kotlinx.android.parcel.Parcelize

@Parcelize
data class OnlineTestRequest(

    @SerializedName("question_id")
    val questionId: Int,

    @SerializedName("status")
    val status: QuestionStatus,
    @SerializedName("answer")
    val answer: String = EMPTY


) : Parcelable {

    constructor(
        questionId: Int,
        status: QuestionStatus
    ) : this(
        questionId = questionId,
        status = status,
        answer = EMPTY
    )
}
