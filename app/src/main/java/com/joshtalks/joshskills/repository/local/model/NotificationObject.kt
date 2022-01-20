package com.joshtalks.joshskills.repository.local.model

import com.google.gson.annotations.SerializedName
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

    @SerializedName("additional_data")
    var extraData: String? = null
}

enum class NotificationAction(val type: String) {
    @SerializedName("open_test")
    ACTION_OPEN_TEST("open_test"),

    @SerializedName("open_conversation")
    ACTION_OPEN_CONVERSATION("open_conversation"),

    @SerializedName("OPEN_LESSON")
    ACTION_OPEN_LESSON("OPEN_LESSON"),

    @SerializedName("OPEN_SPEAKING_SECTION")
    ACTION_OPEN_SPEAKING_SECTION("OPEN_SPEAKING_SECTION"),

    @SerializedName("OPEN_PAYMENT_PAGE")
    ACTION_OPEN_PAYMENT_PAGE("OPEN_PAYMENT_PAGE"),

    @SerializedName("course_explorer")
    ACTION_OPEN_COURSE_EXPLORER("course_explorer"),

    @SerializedName("open_url")
    ACTION_OPEN_URL("open_url"),

    @SerializedName("open_conversation_list")
    ACTION_OPEN_CONVERSATION_LIST("open_conversation_list"),

    @SerializedName("upselling_popup")
    ACTION_UP_SELLING_POPUP("up_selling_popup"),

    @SerializedName("open_referral")
    ACTION_OPEN_REFERRAL("open_referral"),

    @SerializedName("course_report")
    ACTION_OPEN_COURSE_REPORT("course_report"),

    @SerializedName("open_question")
    ACTION_OPEN_QUESTION("open_question"),

    @SerializedName("delete_data")
    ACTION_DELETE_DATA("delete_data"),

    @SerializedName("delete_conversation_data")
    ACTION_DELETE_CONVERSATION_DATA("delete_conversation_data"),

    @SerializedName("delete_user")
    ACTION_DELETE_USER("delete_user"),

    @SerializedName("delete_user_and_data")
    ACTION_DELETE_USER_AND_DATA("delete_user_and_data"),

    @SerializedName("logout_user")
    ACTION_LOGOUT_USER("logout_user"),

    @SerializedName("open_reminder")
    ACTION_OPEN_REMINDER("open_reminder"),

    @SerializedName("audio_feedback_report")
    AUDIO_FEEDBACK_REPORT("audio_feedback_report"),

    @SerializedName("award_declare_notification")
    AWARD_DECLARE("award_declare_notification"),

    @SerializedName("call_receive_notification")
    INCOMING_CALL_NOTIFICATION("call_receive_notification"),

    @SerializedName("call_disconnect_notification")
    CALL_DISCONNECT_NOTIFICATION("call_disconnect_notification"),

    @SerializedName("call_force_receive_notification")
    CALL_FORCE_CONNECT_NOTIFICATION("call_force_receive_notification"),

    @SerializedName("call_force_disconnect_notification")
    CALL_FORCE_DISCONNECT_NOTIFICATION("call_force_disconnect_notification"),

    @SerializedName("no_user_found_notification")
    CALL_NO_USER_FOUND_NOTIFICATION("no_user_found_notification"),

    @SerializedName("call_onhold_notification")
    CALL_ON_HOLD_NOTIFICATION("call_onhold_notification"),

    @SerializedName("call_resume_notification")
    CALL_RESUME_NOTIFICATION("call_resume_notification"),

    @SerializedName("call_connected_notification")
    CALL_CONNECTED_NOTIFICATION("call_connected_notification"),

    @SerializedName("GROUP_CHAT_MESSAGE_NOTIFICATION")
    GROUP_CHAT_MESSAGE_NOTIFICATION("GROUP_CHAT_MESSAGE_NOTIFICATION"),

    @SerializedName("GROUP_CHAT_REPLY")
    GROUP_CHAT_REPLY("GROUP_CHAT_REPLY"),

    @SerializedName("GROUP_CHAT_VOICE_NOTE_HEARD")
    GROUP_CHAT_VOICE_NOTE_HEARD("GROUP_CHAT_VOICE_NOTE_HEARD"),

    @SerializedName("GROUP_CHAT_PIN_MESSAGE")
    GROUP_CHAT_PIN_MESSAGE("GROUP_CHAT_PIN_MESSAGE"),

    @SerializedName("ACTION_COMPLETE_ONBOARDING")
    ACTION_COMPLETE_ONBOARDING("ACTION_COMPLETE_ONBOARDING"),

    @SerializedName("OPEN_FREE_TRIAL_SCREEN")
    ACTION_OPEN_FREE_TRIAL_SCREEN("OPEN_FREE_TRIAL_SCREEN"),

    @SerializedName("JOIN_CONVERSATION_ROOM")
    JOIN_CONVERSATION_ROOM("JOIN_CONVERSATION_ROOM")

}

enum class NotificationChannelNames(val type: String) {
    CLASSES("Classes"),
    P2P("Voice Calling"),
    UPDATES("Updates"),
    GROUP_CHATS("Group Chats"),
    OTHERS("Others"),
    DEFAULT("JoshTalksDefault"),
}
