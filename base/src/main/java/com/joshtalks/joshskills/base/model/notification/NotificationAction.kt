package com.joshtalks.joshskills.base.model.notification

import com.google.gson.annotations.SerializedName

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

    @SerializedName("open_groups")
    ACTION_OPEN_GROUPS("open_groups"),

    @SerializedName("open_group_chat_client")
    ACTION_OPEN_GROUP_CHAT_CLIENT("open_group_chat_client"),

    @SerializedName("open_fpp_request_list")
    ACTION_OPEN_FPP_REQUESTS("open_fpp_request_list"),

    @SerializedName("open_fpp_list")
    ACTION_OPEN_FPP_LIST("open_fpp_list"),

    @SerializedName("emergency_notification")
    EMERGENCY_NOTIFICATION("emergency_notification"),

    @SerializedName("open_p2p_call")
    ACTION_P2P_INCOMING_CALL("open_p2p_call"),

    @SerializedName("open_fpp_call")
    ACTION_FPP_INCOMING_CALL("open_fpp_call"),

    @SerializedName("open_group_call")
    ACTION_GROUP_INCOMING_CALL("open_group_call"),

    @SerializedName("call_recording_notification")
    CALL_RECORDING_NOTIFICATION("call_recording_notification"),
}