package com.joshtalks.joshskills.ui.payment.model

data class VerifyPayment(
    val message: String
)

enum class PaymentStatus(val type: String) {
    SUCCESS("SUCCESS"),
    PENDING("PENDING"),
    FAILED("FAILED")
}
