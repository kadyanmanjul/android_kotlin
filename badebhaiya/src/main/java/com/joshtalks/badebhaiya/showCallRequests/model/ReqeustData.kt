package com.joshtalks.badebhaiya.showCallRequests.model

import com.joshtalks.badebhaiya.utils.Utils
import com.joshtalks.badebhaiya.utils.datetimeutils.DateTimeStyle
import java.util.*

data class ReqeustData(
    val created: Long,
    val request_submitted: String,
) {
//    val submitTime: String
//    get() = Utils.getMessageTimeInHours(Date(created ?: 0))

    val submitDate: String
        get() = Utils.getMessageTime((created), false, DateTimeStyle.SMALL)

    val submitTime: String
        get() = Utils.getMessageTimeInHours(Date(created))
}