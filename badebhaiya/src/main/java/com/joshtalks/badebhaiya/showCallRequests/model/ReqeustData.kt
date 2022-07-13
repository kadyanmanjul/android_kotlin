package com.joshtalks.badebhaiya.showCallRequests.model

import com.joshtalks.badebhaiya.utils.Utils
import java.util.*

data class ReqeustData(
    val created: Long,
    val request_submitted: String,
) {
    val submitTime: String
    get() = Utils.getMessageTimeInHours(Date(created ?: 0))

}