package com.joshtalks.joshskills.repository.server.feedback


import com.google.gson.annotations.SerializedName

data class FeedbackStatusResponse(
    @SerializedName("feedback_require")
    val feedbackRequire: Boolean,
    @SerializedName("submited_data")
    val submittedData: SubmittedData
)