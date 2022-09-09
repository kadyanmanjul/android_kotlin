package com.joshtalks.joshskills.ui.callWithExpert.model

data class WalletLogs (
    val amount: Int,
    val created: Long,
    val event_log: String?,
    val is_failed: Boolean,
    val payment_id: String
)
