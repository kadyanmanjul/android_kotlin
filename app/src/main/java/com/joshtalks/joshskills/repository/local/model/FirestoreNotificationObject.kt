package com.joshtalks.joshskills.repository.local.model

import com.google.firebase.Timestamp
import com.google.gson.annotations.SerializedName

data class FirestoreNotificationObject(

    @SerializedName("id")
    var id: String? = null,

    @SerializedName("name")
    var name: String? = null,

    @SerializedName("mentorId")
    var mentorId: String? = null,

    @SerializedName("type")
    var type: String? = null,

    @SerializedName("contentTitle")
    var contentTitle: String? = "",

    @SerializedName("contentText")
    var contentText: String? = "",

    @SerializedName("isDelivered")
    var isDelivered: Boolean = false,

    @SerializedName("isClicked")
    var isClicked: Boolean = false,

    @SerializedName("action")
    var action: FirestoreNotificationAction? = null,

    @SerializedName("actionData")
    var actionData: String? = null,

    @SerializedName("largeIcon")
    var largeIcon: String? = null,

    @SerializedName("notificationId")
    var notificationId: Int = -1,

    @SerializedName("isStrict")
    val isStrict: Boolean = false,

    @SerializedName("isBackHome")
    val isBackHome: Boolean = false,

    @SerializedName("isOngoing")
    var isOngoing: Boolean = false,

    @SerializedName("total")
    var total: Long = 0,

    @SerializedName("progress")
    var progress: Long = 0,

    @SerializedName("ticker")
    var ticker: String? = null,

    @SerializedName("imageUrl")
    var bigPicture: String? = null,

    @SerializedName("deepLinkUrl")
    var deeplink: String? = null,

    @SerializedName("additionalData")
    var extraData: String? = null,

    @SerializedName("callTopic")
    var topicName: String? = null,

    @SerializedName("topicId")
    var topicId: Int = -1,

    @SerializedName("profilePic")
    var profilePicUrl: String? = null,

    @SerializedName("userName")
    var userName: String? = null,

    @SerializedName("created")
    var created: Timestamp? = null,

    @SerializedName("modified")
    var modified: Timestamp? = null,
) {
    fun toNotificationObject(id: String?) = NotificationObject().also {
        it.id = id
        it.name = name
        it.mentorId = mentorId
        it.type = type
        it.contentTitle = contentTitle
        it.contentText = contentText
        it.isDelivered = isDelivered
        it.isClicked = isClicked
        it.actionData = actionData
        it.largeIcon = largeIcon
        it.notificationId = notificationId
        it.isOngoing = isOngoing
        it.total = total
        it.progress = progress
        it.ticker = ticker
        it.bigPicture = bigPicture
        it.deeplink = deeplink
        it.extraData = extraData
        it.action = when (action) {
            FirestoreNotificationAction.CALL_RECEIVE_NOTIFICATION -> NotificationAction.INCOMING_CALL_NOTIFICATION
            FirestoreNotificationAction.CALL_DISCONNECT_NOTIFICATION -> NotificationAction.CALL_DISCONNECT_NOTIFICATION
            FirestoreNotificationAction.CALL_FORCE_RECEIVE_NOTIFICATION -> NotificationAction.CALL_FORCE_CONNECT_NOTIFICATION
            FirestoreNotificationAction.CALL_FORCE_DISCONNECT_NOTIFICATION -> NotificationAction.CALL_FORCE_DISCONNECT_NOTIFICATION
            FirestoreNotificationAction.CALL_DECLINE_NOTIFICATION -> NotificationAction.CALL_DECLINE_NOTIFICATION
            FirestoreNotificationAction.NO_USER_FOUND_NOTIFICATION -> NotificationAction.CALL_NO_USER_FOUND_NOTIFICATION
            FirestoreNotificationAction.CALL_ONHOLD_NOTIFICATION -> NotificationAction.CALL_ON_HOLD_NOTIFICATION
            FirestoreNotificationAction.CALL_RESUME_NOTIFICATION -> NotificationAction.CALL_RESUME_NOTIFICATION
            FirestoreNotificationAction.CALL_CONNECTED_NOTIFICATION -> NotificationAction.CALL_CONNECTED_NOTIFICATION
            FirestoreNotificationAction.JOIN_CONVERSATION_ROOM -> NotificationAction.JOIN_CONVERSATION_ROOM
            else -> null
        }
    }
}

enum class FirestoreNotificationAction(val value: String) {
    @SerializedName("CALL_RECEIVE_NOTIFICATION")
    CALL_RECEIVE_NOTIFICATION("CALL_RECEIVE_NOTIFICATION"),

    @SerializedName("CALL_DISCONNECT_NOTIFICATION")
    CALL_DISCONNECT_NOTIFICATION("CALL_DISCONNECT_NOTIFICATION"),

    @SerializedName("CALL_FORCE_RECEIVE_NOTIFICATION")
    CALL_FORCE_RECEIVE_NOTIFICATION("CALL_FORCE_RECEIVE_NOTIFICATION"),

    @SerializedName("CALL_FORCE_DISCONNECT_NOTIFICATION")
    CALL_FORCE_DISCONNECT_NOTIFICATION("CALL_FORCE_DISCONNECT_NOTIFICATION"),

    @SerializedName("NO_USER_FOUND_NOTIFICATION")
    NO_USER_FOUND_NOTIFICATION("NO_USER_FOUND_NOTIFICATION"),

    @SerializedName("CALL_ONHOLD_NOTIFICATION")
    CALL_ONHOLD_NOTIFICATION("CALL_ONHOLD_NOTIFICATION"),

    @SerializedName("CALL_RESUME_NOTIFICATION")
    CALL_RESUME_NOTIFICATION("CALL_RESUME_NOTIFICATION"),

    @SerializedName("CALL_CONNECTED_NOTIFICATION")
    CALL_CONNECTED_NOTIFICATION("CALL_CONNECTED_NOTIFICATION"),

    @SerializedName("JOIN_CONVERSATION_ROOM")
    JOIN_CONVERSATION_ROOM("JOIN_CONVERSATION_ROOM"),

    @SerializedName("CALL_DECLINE_NOTIFICATION")
    CALL_DECLINE_NOTIFICATION("CALL_DECLINE_NOTIFICATION")

}
