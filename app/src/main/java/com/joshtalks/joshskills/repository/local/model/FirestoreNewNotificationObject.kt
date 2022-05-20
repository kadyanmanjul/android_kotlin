package com.joshtalks.joshskills.repository.local.model

import com.google.firebase.Timestamp
import com.google.gson.annotations.SerializedName

data class FirestoreNewNotificationObject(

    var id: Int? = null,

    @SerializedName("name")
    var name: String? = null,

    @SerializedName("mentorId")
    var mentorId: String? = null,

    @SerializedName("type")
    var type: String? = null,

    @SerializedName("title")
    var title: String? = "",

    @SerializedName("body")
    var body: String? = "",

    @SerializedName("isDelivered")
    var isDelivered: Boolean = false,

    @SerializedName("isClicked")
    var isClicked: Boolean = false,

    @SerializedName("action")
    var action: String? = null,

    @SerializedName("action_data")
    var action_data: String? = null,

    @SerializedName("largeIcon")
    var largeIcon: String? = null,

    @SerializedName("id")
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
    var modified: Timestamp? = Timestamp.now(),
) {
    fun toNotificationObject(id: String?) = NotificationObject().also {
        it.id = id.toString()
        it.name = name
        it.mentorId = mentorId
        it.type = type
        it.contentTitle = title
        it.contentText = body
        it.isDelivered = isDelivered
        it.isClicked = isClicked
        it.actionData = action_data
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
            NotificationAction.INCOMING_CALL_NOTIFICATION.type -> NotificationAction.INCOMING_CALL_NOTIFICATION
            NotificationAction.CALL_DISCONNECT_NOTIFICATION.type -> NotificationAction.CALL_DISCONNECT_NOTIFICATION
            NotificationAction.CALL_FORCE_CONNECT_NOTIFICATION.type -> NotificationAction.CALL_FORCE_CONNECT_NOTIFICATION
            NotificationAction.CALL_FORCE_DISCONNECT_NOTIFICATION.type -> NotificationAction.CALL_FORCE_DISCONNECT_NOTIFICATION
            NotificationAction.CALL_DECLINE_NOTIFICATION.type -> NotificationAction.CALL_DECLINE_NOTIFICATION
            NotificationAction.CALL_NO_USER_FOUND_NOTIFICATION.type -> NotificationAction.CALL_NO_USER_FOUND_NOTIFICATION
            NotificationAction.CALL_ON_HOLD_NOTIFICATION.type -> NotificationAction.CALL_ON_HOLD_NOTIFICATION
            NotificationAction.CALL_RESUME_NOTIFICATION.type -> NotificationAction.CALL_RESUME_NOTIFICATION
            NotificationAction.CALL_CONNECTED_NOTIFICATION.type -> NotificationAction.CALL_CONNECTED_NOTIFICATION
            NotificationAction.JOIN_CONVERSATION_ROOM.type -> NotificationAction.JOIN_CONVERSATION_ROOM
            NotificationAction.ACTION_OPEN_FREE_TRIAL_SCREEN.type -> NotificationAction.ACTION_OPEN_FREE_TRIAL_SCREEN
            NotificationAction.ACTION_COMPLETE_ONBOARDING.type -> NotificationAction.ACTION_COMPLETE_ONBOARDING
            NotificationAction.GROUP_CHAT_PIN_MESSAGE.type -> NotificationAction.GROUP_CHAT_PIN_MESSAGE
            NotificationAction.GROUP_CHAT_VOICE_NOTE_HEARD.type -> NotificationAction.GROUP_CHAT_VOICE_NOTE_HEARD
            NotificationAction.GROUP_CHAT_REPLY.type -> NotificationAction.GROUP_CHAT_REPLY
            NotificationAction.GROUP_CHAT_MESSAGE_NOTIFICATION.type -> NotificationAction.GROUP_CHAT_MESSAGE_NOTIFICATION
            NotificationAction.AWARD_DECLARE.type -> NotificationAction.AWARD_DECLARE
            NotificationAction.AUDIO_FEEDBACK_REPORT.type -> NotificationAction.AUDIO_FEEDBACK_REPORT
            NotificationAction.ACTION_OPEN_REMINDER.type -> NotificationAction.ACTION_OPEN_REMINDER
            NotificationAction.ACTION_LOGOUT_USER.type -> NotificationAction.ACTION_LOGOUT_USER
            NotificationAction.ACTION_DELETE_USER_AND_DATA.type -> NotificationAction.ACTION_DELETE_USER_AND_DATA
            NotificationAction.ACTION_DELETE_USER.type -> NotificationAction.ACTION_DELETE_USER
            NotificationAction.ACTION_DELETE_CONVERSATION_DATA.type -> NotificationAction.ACTION_DELETE_CONVERSATION_DATA
            NotificationAction.ACTION_DELETE_DATA.type -> NotificationAction.ACTION_DELETE_DATA
            NotificationAction.ACTION_OPEN_QUESTION.type -> NotificationAction.ACTION_OPEN_QUESTION
            NotificationAction.ACTION_OPEN_COURSE_REPORT.type -> NotificationAction.ACTION_OPEN_COURSE_REPORT
            NotificationAction.ACTION_OPEN_REFERRAL.type -> NotificationAction.ACTION_OPEN_REFERRAL
            NotificationAction.ACTION_UP_SELLING_POPUP.type -> NotificationAction.ACTION_UP_SELLING_POPUP
            NotificationAction.ACTION_OPEN_CONVERSATION_LIST.type -> NotificationAction.ACTION_OPEN_CONVERSATION_LIST
            NotificationAction.ACTION_OPEN_URL.type -> NotificationAction.ACTION_OPEN_URL
            NotificationAction.ACTION_OPEN_COURSE_EXPLORER.type -> NotificationAction.ACTION_OPEN_COURSE_EXPLORER
            NotificationAction.ACTION_OPEN_PAYMENT_PAGE.type -> NotificationAction.ACTION_OPEN_PAYMENT_PAGE
            NotificationAction.ACTION_OPEN_SPEAKING_SECTION.type -> NotificationAction.ACTION_OPEN_SPEAKING_SECTION
            NotificationAction.ACTION_OPEN_LESSON.type -> NotificationAction.ACTION_OPEN_LESSON
            NotificationAction.ACTION_OPEN_CONVERSATION.type -> NotificationAction.ACTION_OPEN_CONVERSATION
            NotificationAction.ACTION_OPEN_TEST.type -> NotificationAction.ACTION_OPEN_TEST
            else -> null
        }
    }
}