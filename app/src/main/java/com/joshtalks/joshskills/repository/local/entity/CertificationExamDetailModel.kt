package com.joshtalks.joshskills.repository.local.entity


import com.google.gson.annotations.SerializedName

data class CertificationExamDetailModel(
    @SerializedName("attempt_left")
    val attemptLeft: Int,
    @SerializedName("attempt_on")
    val attemptOn: String,
    @SerializedName("attempted")
    val attempted: Int,
    @SerializedName("batch_icon")
    val batchIcon: String,
    @SerializedName("code_no")
    val code: String,
    @SerializedName("eligibility_date")
    val eligibilityDate: String,
    @SerializedName("marks")
    val marks: Double,
    @SerializedName("passed_on")
    val passedOn: String,
    @SerializedName("status")
    val examStatus: CExamStatus,
    @SerializedName("text")
    val text: String
) : java.io.Serializable


enum class CExamStatus {
    @SerializedName("fresh")
    FRESH,

    @SerializedName("attempted")
    ATTEMPTED,

    @SerializedName("passed")
    PASSED,

    @SerializedName("reattempted")
    REATTEMPTED,

    @SerializedName("check_result")
    CHECK_RESULT,

    @SerializedName("nil")
    NIL,

}
