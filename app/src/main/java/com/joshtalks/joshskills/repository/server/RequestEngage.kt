package com.joshtalks.joshskills.repository.server


import android.os.Parcelable
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import com.joshtalks.joshskills.repository.local.model.Mentor
import kotlinx.android.parcel.Parcelize

@Parcelize
class RequestEngage : Parcelable {
    @SerializedName("answer_url")
    var answerUrl: String? = null

    @SerializedName("feedback_require")
    var feedbackRequire: String? = null

    @SerializedName("mentor")
    var mentor: String = Mentor.getInstance().getId()

    @SerializedName("question")
    var question: String = ""

    @SerializedName("text")
    var text: String? = null

    @SerializedName("duration")
    var duration: Int? = null

    @Expose
    var localPath: String? = null
}