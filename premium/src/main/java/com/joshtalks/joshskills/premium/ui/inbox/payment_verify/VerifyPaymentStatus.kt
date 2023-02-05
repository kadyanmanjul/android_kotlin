package com.joshtalks.joshskills.premium.ui.inbox.payment_verify

import com.google.gson.annotations.SerializedName

data class VerifyPaymentStatus(
    @SerializedName("payment")
    val payment: PaymentStatus?
)

enum class PaymentStatus {
    @SerializedName("capture")
    SUCCESS,
    @SerializedName("authorized")
    PROCESSING,
    @SerializedName("failed")
    FAILED,
    @SerializedName("created")
    CREATED
}