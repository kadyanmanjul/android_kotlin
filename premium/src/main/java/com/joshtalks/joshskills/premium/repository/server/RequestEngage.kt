package com.joshtalks.joshskills.premium.repository.server


import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import com.joshtalks.joshskills.premium.repository.local.model.Mentor

class RequestEngage {
    @SerializedName("answer_url")
    var answerUrl: String? = null

    @SerializedName("feedback_require")
    var feedbackRequire: String? = null

    @SerializedName("mentor")
    var mentor: String = Mentor.getInstance().getId()

    @SerializedName("question")
    var questionId: String = ""

    @SerializedName("text")
    var text: String? = null

    @SerializedName("duration")
    var duration: Int? = null

    @Expose
    var localPath: String? = null
}