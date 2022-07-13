package com.joshtalks.badebhaiya.showCallRequests.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class RequestData(
    val request_submitted: String,
    val user: User,
    val didRead: Boolean = true
): Parcelable