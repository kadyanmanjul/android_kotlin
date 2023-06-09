package com.joshtalks.joshskills.voip.constant

// Error Code
const val ERROR = -101

const val CALL_INITIATED_EVENT = 101
const val CALL_CONNECTED_EVENT = 102
const val RECONNECTING_FAILED = 128
const val MUTE = 105
const val UNMUTE = 106
const val HOLD = 107
const val UNHOLD = 108
const val RECONNECTING = 113
const val RECONNECTED = 114
const val CLOSE_CALL_SCREEN = 130
const val CANCEL_INCOMING_TIMER = 131


const val CALL_CONNECT_REQUEST = 115
const val IPC_CONNECTION_ESTABLISHED = 117

const val IDLE = 118 // // Doing Nothing - Can make Call
const val JOINING = 119 // Join Channel Called and Success Returned but haven't joined the channel
const val JOINED = 120 // Local User Joined the Channel
const val CONNECTED = 121 // Remote User Joined the Channel and can Talk
const val LEAVING = 122 // LeaveChannel Called but haven't left the channel
const val LEAVING_AND_JOINING = 126 // LeaveChannel Previous Channel and Joining New Channel

// Content Provider Voip State
const val CONTENT_VOIP_STATE_AUTHORITY = "content://com.joshtalks.joshskills.voipstate"
const val PSTN_STATE_PATH = "/current_pstn_state"


//    Content values for Voip State
const val CURRENT_VOIP_STATE = "josh_current_voip_state"
const val CURRENT_PSTN_STATE = "josh_current_pstn_state"



//PSTN states
const val PSTN_STATE_IDLE = "pstn_state_Idle"
const val PSTN_STATE_ONCALL = "pstn_state_oncall"
const val PREF_KEY_PSTN_STATE = "pstn_state_pstn_state"

// Incoming Notification Constants
const val INCOMING_CALL_ID = "call_id"
const val INCOMING_CALL_CATEGORY = "call_category"
const val REMOTE_USER_NAME = "remote_user_name"
const val INCOMING_GROUP_NAME = "group_name"
const val INCOMING_GROUP_IMAGE = "group_icon"

//API response Constants
const val TOAST_MESSAGE = "msg"



enum class Event {
    ERROR,
    CALL_INITIATED_EVENT,
    CALL_CONNECTED_EVENT,
    CALL_DISCONNECTED,
    RECONNECTING_FAILED,
    MUTE,
    UNMUTE,
    HOLD,
    UNHOLD,
    RECONNECTING,
    RECONNECTED,
    INCOMING_CALL,
    RECEIVED_CHANNEL_DATA,
    UI_STATE_UPDATED,
    CLOSE_CALL_SCREEN,
    REMOTE_USER_DISCONNECTED_AGORA,
    REMOTE_USER_DISCONNECTED_USER_LEFT,
    REMOTE_USER_DISCONNECTED_MESSAGE,
    SYNC_UI_STATE,
    SPEAKER_ON_REQUEST,
    SPEAKER_OFF_REQUEST,
    HOLD_REQUEST,
    UNHOLD_REQUEST,
    MUTE_REQUEST,
    UNMUTE_REQUEST,
    TOPIC_IMAGE_CHANGE_REQUEST,
    TOPIC_IMAGE_RECEIVED,
    GROUP_INCOMING_CALL,
    FPP_INCOMING_CALL,
}

enum class State {
    IDLE,
    SEARCHING,
    JOINING,
    JOINED,
    CONNECTED,
    RECONNECTING,
    LEAVING,
}

enum class Category(val category : String) {
    PEER_TO_PEER("PEER_TO_PEER"),
    FPP("FPP"),
    GROUP("GROUP"),
    EXPERT("EXPERT")
}
