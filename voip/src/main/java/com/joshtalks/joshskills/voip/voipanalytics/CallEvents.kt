package com.joshtalks.joshskills.voip.voipanalytics

data class CallEvents(
    val event : EventName,
    val timestamp :String,
    val agoraCallId : String? = null,
    val agoraMentorId : String? = null,
    val extra : String = ""
)

enum class EventName(val eventName : String){
    CALL_INITIATED ("CALL_INITIATED"),
    CHANNEL_RECEIVED("CHANNEL_RECEIVED"),
    CHANNEL_JOINING("CHANNEL_JOINING"),
    CHANNEL_JOINED("CHANNEL_JOINED"),
    CHANNEL_LEAVING("CHANNEL_LEAVING"),
    CHANNEL_LEFT("CHANNEL_LEFT"),
    CALL_SCREEN_SHOWN("CALL_SCREEN_SHOWN"),
    MIC_ON("MIC_ON"),
    SPEAKER_ON("SPEAKER_ON"),
    PSTN_CALL_RECEIVED("PSTN_CALL_RECEIVED"),
    INCOMING_CALL_RECEIVED("INCOMING_CALL_RECEIVED"),
    INCOMING_CALL_SHOWN("INCOMING_CALL_SHOWN"),
    INCOMING_CALL_IGNORE("INCOMING_CALL_IGNORE"),
    INCOMING_CALL_ACCEPT("INCOMING_CALL_ACCEPT"),
    INCOMING_CALL_DECLINE("INCOMING_CALL_DECLINE"),
    CALL_RECONNECTING("CALL_RECONNECTING"),
    DISCONNECTED_BY_RED_BUTTON("DISCONNECTED_BY_RED_BUTTON"),
    DISCONNECTED_BY_REMOTE_USER("DISCONNECTED_BY_REMOTE_USER"),
    DISCONNECTED_BY_RECONNECTING("DISCONNECTED_BY_RECONNECTING"),
    DISCONNECTED_BY_BACKPRESS("DISCONNECTED_BY_BACKPRESS"),
    DISCONNECTED_BY_HANG_UP("DISCONNECTED_BY_HANG_UP"),
    DISCONNECTED_BY_AGORA_USER_OFFLINE("DISCONNECTED_BY_AGORA_USER_OFFLINE"),
    DISCONNECTED_BY_CONNECTING_TIMEOUT("DISCONNECTED_BY_CONNECTING_TIMEOUT"),
    DISCONNECTED_BY_INCOMING_TIMEOUT("DISCONNECTED_BY_INCOMING_TIMEOUT"),
    PUBNUB_LISTENER_RESTART("PUBNUB_LISTENER_RESTART"),
    MIC_OFF("MIC_OFF"),
    SPEAKER_OFF("SPEAKER_OFF"),
    SPEAKING("MIC_STARTED"),
    LISTENING("SPEAKER_STARTED"),
    ON_ERROR("ON_ERROR"),
    ILLEGAL_EVENT_RECEIVED("ILLEGAL_EVENT_RECEIVED"),
    BACK_PRESSED("BACK_PRESSED"),
    NEXT_TOPIC_BTN_PRESS("NEXT_TOPIC_BTN_PRESS"),
    NEXT_CHANNEL_REQUESTED("NEXT_CHANNEL_REQUESTED"),
    RECORDING_INITIATED("RECORDING_INITIATED"),
    RECORDING_ACCEPTED("RECORDING_ACCEPTED"),
    RECORDING_REJECTED("RECORDING_REJECTED"),
    RECORDING_STOPPED("RECORDING_STOPPED"),
    PLAY_GAME_CLICK("PLAY_BUTTON_CLICK"),
    NEW_WORD_CLICK("NEW_WORD_CLICK"),
    END_GAME_CLICK("END_GAME_CLICK"),
    CALL_RECORDING_NOTIFICATION_CLICKED("CALL_RECORDING_NOTIFICATION_CLICKED"),
    RECORDING_SHARE_BUTTON_CLICKED("RECORDING_SHARE_BUTTON_CLICKED")
}