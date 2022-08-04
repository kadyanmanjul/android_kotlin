package com.joshtalks.badebhaiya.showCallRequests.model

import android.os.Parcelable
import com.joshtalks.badebhaiya.utils.Utils
import com.joshtalks.badebhaiya.utils.datetimeutils.DateTimeStyle
import com.joshtalks.badebhaiya.utils.datetimeutils.DateTimeUtils
import kotlinx.android.parcel.Parcelize
import java.util.*

@Parcelize
data class RequestData(
    val request_submitted: String,
    val user: User,
    val is_read: Boolean,
    val created: Long
): Parcelable {

    val submitTime: String
        get() = if (isSubmittedToday()) Utils.getMessageTimeInHours(Date(created)) else Utils.getMessageTime(created, style = DateTimeStyle.SEMI_MEDIUM).removeRange(0..3)

    private fun isSubmittedToday(): Boolean{
        return DateTimeUtils.isToday(Date(created))
    }
}