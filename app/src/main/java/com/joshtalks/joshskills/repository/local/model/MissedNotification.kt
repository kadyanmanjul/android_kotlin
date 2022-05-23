package com.joshtalks.joshskills.repository.local.model

import com.google.gson.annotations.SerializedName

data class MissedNotification(

    @SerializedName("id")
    var id: Int,

    @SerializedName("action")
    var action: NotificationAction? = null,

    @SerializedName("action_data")
    var actionData: String? = null,

    @SerializedName("title")
    var title: String,

    @SerializedName("body")
    var body: String
)
