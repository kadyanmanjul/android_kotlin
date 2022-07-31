package com.joshtalks.joshskills.repository.local.model

import com.google.gson.annotations.SerializedName
import com.joshtalks.joshskills.base.core.AppObjectController
import com.joshtalks.joshskills.base.model.notification.NotificationAction
import com.joshtalks.joshskills.core.EMPTY

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
        get() {
            return field?.replace("_username_", User.getInstance().firstName ?: EMPTY)
        }

    @SerializedName("content_text")
    var contentText: String? = ""
        get() {
            return field?.replace("_username_", User.getInstance().firstName ?: EMPTY)
        }

    @SerializedName("is_delivered")
    var isDelivered: Boolean = false

    @SerializedName("is_clicked")
    var isClicked: Boolean = false

    @SerializedName("action")
    var action: NotificationAction? = null

    @SerializedName("action_data")
    var actionData: String? = null

    @SerializedName("large_icon")
    var largeIcon: String? = null

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

    @SerializedName("additional_data")
    var extraData: String? = null

    @SerializedName("channelId")
    var channelId: String? = null

    @SerializedName("priority")
    var priority: Int? = null

    override fun toString(): String {
        return AppObjectController.gsonMapper.toJson(this)
    }
}