package com.joshtalks.joshskills.expertcall.model

data class TransactionResponse(
    val transactions: List<Transaction>
)

data class Transaction(
    val amount: Int,
    val created: Long,
    val event_log: String?,
    val is_failed: Boolean
)