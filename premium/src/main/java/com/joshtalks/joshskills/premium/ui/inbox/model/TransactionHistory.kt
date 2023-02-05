package com.joshtalks.joshskills.premium.ui.inbox.model

import com.google.gson.annotations.SerializedName
import java.text.SimpleDateFormat
import java.util.*

data class TransactionHistory(
    @SerializedName("id")
    val orderId: Long,
    @SerializedName("amount")
    val amount: Float,
    @SerializedName("str_datetime")
    val time: String,
    @SerializedName("course_icon")
    val courseIcon: String?,
    @SerializedName("course_name")
    val courseName: String,
    @SerializedName("transaction_id")
    val paymentId: String?,
    @SerializedName("transaction_status")
    val status: Boolean?
) {
    fun getTransactionTime(): Date {
        val sourceFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        return sourceFormat.parse(time.substringBefore("+"))
    }
}