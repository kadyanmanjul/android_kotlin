package com.joshtalks.badebhaiya.showCallRequests.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class User(
    val full_name: String,
    val photo_url: String,
    val short_name: String,
    val user_id: String
): Parcelable