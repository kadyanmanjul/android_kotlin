package com.joshtalks.joshskills.repository.local.model

import com.google.gson.annotations.SerializedName


class NotificationObject {

    @SerializedName("id")
    var id: String? = null

    @SerializedName("name")
    var name: String? = null

    @SerializedName("mentor_id")
    var mentorId: String? = null

    @SerializedName("type")
    var type: String? = null

    @SerializedName("content_title")
    var contentTitle: String? = ""

    @SerializedName("content_text")
    var contentText: String? = ""

    @SerializedName("is_delivered")
    var isDelivered: Boolean = false

    @SerializedName("is_clicked")
    var isClicked: Boolean = false

    @SerializedName("action")
    var action: String? = null

    @SerializedName("action_data")
    var actionData: String? = null

    @SerializedName("large_icon")
    var largeIcon: String? = null

    @SerializedName("notification_id")
    var notificationId = -1

    @SerializedName("is_strict")
    val isStrict: Boolean = false

    @SerializedName("is_back_home")
    val isBackHome: Boolean = false

    @SerializedName("isOngoing")
    var isOngoing = false

    @SerializedName("total")
    var total: Long = 0

    @SerializedName("progress")
    var progress: Long = 0

    @SerializedName("ticker")
    var ticker: String? = null

    @SerializedName("image_url")
    var bigPicture: String? = null

    @SerializedName("deep_link_url")
    var deeplink: String? = null

}

const val ACTION_OPEN_TEST = "open_test"
const val ACTION_OPEN_CONVERSATION = "open_conversation"
const val ACTION_OPEN_COURSE_EXPLORER = "course_explorer"
const val ACTION_OPEN_URL = "open_url"
const val ACTION_OPEN_CONVERSATION_LIST = "open_conversation_list"
const val ACTION_OPEN_DIALOG = "show_dialog"
const val ACTION_OPEN_POPUP = "popup"
const val ACTION_UPSELLING_POPUP = "upselling_popup"






