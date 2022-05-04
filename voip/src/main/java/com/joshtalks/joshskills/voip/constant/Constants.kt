package com.joshtalks.joshskills.voip.constant

// Error Code
const val ERROR_IN_CONNECTION_API = -103
const val ERROR_IN_JOINING_CHANNEL = -102
const val ERROR = -101

const val CALL_INITIATED_EVENT = 101
const val CALL_CONNECTED_EVENT = 102
const val CALL_DISCONNECTING_EVENT = 103
const val CALL_DISCONNECTED = 104
const val RECONNECTING_FAILED = 128
const val MUTE = 105
const val UNMUTE = 106
const val HOLD = 107
const val UNHOLD = 108
const val SWITCHED_TO_SPEAKER = 109
const val SWITCHED_TO_WIRED = 110
const val SWITCHED_TO_BLUETOOTH = 111
const val SWITCHED_TO_HANDSET = 112
const val RECONNECTING = 113
const val RECONNECTED = 114
const val INCOMING_CALL = 125
const val RECEIVED_CHANNEL_DATA = 127
const val UI_STATE_UPDATED = 129
const val CLOSE_CALL_SCREEN = 130
const val REMOTE_USER_DISCONNECTED_AGORA = 131
const val REMOTE_USER_DISCONNECTED_USER_DROP = 132
const val REMOTE_USER_DISCONNECTED_MESSAGE = 139
const val SYNC_UI_STATE = 138

const val CALL_CONNECT_REQUEST = 115
const val CALL_DISCONNECT_REQUEST = 116
const val IPC_CONNECTION_ESTABLISHED = 117
const val SPEAKER_ON_REQUEST = 123
const val SPEAKER_OFF_REQUEST = 124
const val HOLD_REQUEST = 134
const val UNHOLD_REQUEST = 135
const val MUTE_REQUEST = 136
const val UNMUTE_REQUEST = 137

const val IDLE = 118 // // Doing Nothing - Can make Call
const val JOINING = 119 // Join Channel Called and Success Returned but haven't joined the channel
const val JOINED = 120 // Local User Joined the Channel
const val CONNECTED = 121 // Remote User Joined the Channel and can Talk
const val LEAVING = 122 // LeaveChannel Called but haven't left the channel
const val LEAVING_AND_JOINING = 126 // LeaveChannel Previous Channel and Joining New Channel

// Content Provider Voip State
const val CONTENT_VOIP_STATE_AUTHORITY = "content://com.joshtalks.joshskills.voipstate"
const val VOIP_STATE_PATH = "/current_voip_state"

//    Content values for Voip State
const val CURRENT_VOIP_STATE = "josh_current_voip_state"

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
    CALL_CONNECT_REQUEST,
    SPEAKER_ON_REQUEST,
    SPEAKER_OFF_REQUEST,
    HOLD_REQUEST,
    UNHOLD_REQUEST,
    MUTE_REQUEST,
    UNMUTE_REQUEST,
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





