package com.joshtalks.joshskills.ui.callWithExpert.model

data class Transaction(
    val amount: Int,
    val created: Long,
    val event_log: String,
    val is_failed: Boolean
)