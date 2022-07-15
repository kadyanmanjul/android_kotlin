package com.joshtalks.badebhaiya.showCallRequests.model

import android.os.Parcelable
import com.joshtalks.badebhaiya.utils.Utils
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
        get() = Utils.getMessageTimeInHours(Date(created))

}