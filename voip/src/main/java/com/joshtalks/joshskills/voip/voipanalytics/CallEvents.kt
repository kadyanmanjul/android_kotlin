package com.joshtalks.joshskills.voip.voipanalytics

data class CallEvents(
    val event : EventName,
    val timestamp :String,
    val agoraCallId : String? = null,
    val agoraMentorId : String? = null
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
    PUBNUB_LISTENER_RESTART("PUBNUB_LISTENER_RESTART")
}
//CallAnalytics.addAnalytics(
//event = EventName.CALL_RECONNECTING,
//agoraCallId = CallDetails.callId.toString(),
//agoraMentorId = CallDetails.localUserAgoraId.toString()
//)
//
//CallAnalytics.addAnalytics(
//event = EventName.DISCONNECTED_BY_RED_BUTTON,
//agoraMentorId =  VoipPref.getCurrentUserAgoraId().toString(),
//agoraCallId = VoipPref.getCurrentCallId().toString()
//)