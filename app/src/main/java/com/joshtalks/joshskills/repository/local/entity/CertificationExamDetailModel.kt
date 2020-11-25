package com.joshtalks.joshskills.repository.local.entity


import com.google.gson.annotations.SerializedName
import com.joshtalks.joshskills.core.EMPTY

data class CertificationExamDetailModel(
    @SerializedName("attempt_left")
    val attemptLeft: Int = 0,
    @SerializedName("attempt_on")
    val attemptOn: String = EMPTY,
    @SerializedName("attempted")
    val attempted: Int = 0,
    @SerializedName("batch_icon")
    val batchIcon: String = EMPTY,
    @SerializedName("code_no")
    val code: String = EMPTY,
    @SerializedName("eligibility_date")
    val eligibilityDate: String = EMPTY,
    @SerializedName("marks")
    val marks: Double = 0.0,
    @SerializedName("passed_on")
    val passedOn: String = EMPTY,
    @SerializedName("status")
    val examStatus: CExamStatus = CExamStatus.NIL,
    @SerializedName("text")
    val text: String = EMPTY
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
