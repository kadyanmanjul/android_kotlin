package com.joshtalks.joshskills.ui.voip.analytics.data.network

import com.google.gson.annotations.SerializedName
import java.util.*

data class PurchasePopUp(
    @SerializedName("title")
    val popUpTitle: String?,
    @SerializedName("body")
    val popUpBody: String?,
    @SerializedName("price")
    val popUpPrice: String?,
    @SerializedName("expire_time")
    val expireTime: Date?
)