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
const val SHOW_RECORDING_PERMISSION_DIALOG = 132
const val SHOW_RECORDING_REJECTED_DIALOG = 133
const val HIDE_RECORDING_PERMISSION_DIALOG = 134

const val CALL_CONNECT_REQUEST = 115
const val IPC_CONNECTION_ESTABLISHED = 117

const val IDLE = 118 // // Doing Nothing - Can make Call
const val JOINING = 119 // Join Channel Called and Success Returned but haven't joined the channel
const val JOINED = 120 // Local User Joined the Channel
const val CONNECTED = 121 // Remote User Joined the Channel and can Talk
const val LEAVING = 122 // LeaveChannel Called but haven't left the channel
const val LEAVING_AND_JOINING = 126 // LeaveChannel Previous Channel and Joining New Channel
const val GET_FRAGMENT_BITMAP = 127 // LeaveChannel Previous Channel and Joining New Channel

// Content Provider Voip State
const val CONTENT_VOIP_STATE_AUTHORITY = "content://com.joshtalks.joshskills.voipstate"
const val VOIP_STATE_PATH = "/current_voip_state"
const val PSTN_STATE_PATH = "/current_pstn_state"


//    Content values for Voip State
const val CURRENT_VOIP_STATE = "josh_current_voip_state"
const val CURRENT_PSTN_STATE = "josh_current_pstn_state"


//PSTN states
const val PSTN_STATE_IDLE = "pstn_state_Idle"
const val PSTN_STATE_ONCALL = "pstn_state_oncall"
const val PREF_KEY_PSTN_STATE = "pstn_state_pstn_state"



enum class Event {
    ERROR, //onVoipCallStateChange
    CALL_INITIATED_EVENT, //onVoipCallStateChange
    CALL_CONNECTED_EVENT, //onVoipCallStateChange
    CALL_DISCONNECTED, //onVoipCallStateChange
    RECONNECTING_FAILED, //onVoipCallStateChange
    MUTE, //onRemoteMicStateChange
    UNMUTE, //onRemoteMicStateChange
    HOLD, //onRemotePhoneStateChange
    UNHOLD, //onRemotePhoneStateChange
    RECONNECTING, // onVoipCallStateChange
    RECONNECTED, // onVoipCallStateChange
    INCOMING_CALL,
    RECEIVED_CHANNEL_DATA, // onVoipCallStateChange
    UI_STATE_UPDATED, // Will be handled by base class
    CLOSE_CALL_SCREEN,
    REMOTE_USER_DISCONNECTED_AGORA, // onCallDisconnectRequest
    REMOTE_USER_DISCONNECTED_USER_LEFT, // onCallDisconnectRequest
    REMOTE_USER_DISCONNECTED_MESSAGE, // onCallDisconnectRequest
    SYNC_UI_STATE, // Will be handled by base class
    SPEAKER_ON_REQUEST, // onUserAction
    SPEAKER_OFF_REQUEST, // onUserAction
    HOLD_REQUEST, //onUserAction
    UNHOLD_REQUEST, //onUserAction
    MUTE_REQUEST, //onUserAction
    UNMUTE_REQUEST, //onUserAction
    TOPIC_IMAGE_CHANGE_REQUEST, // onUserAction
    TOPIC_IMAGE_RECEIVED, // onUserAction
    //-------------------------------------------
    START_RECORDING,
    STOP_RECORDING,
    CALL_RECORDING_ACCEPT,
    CALL_RECORDING_REJECT,
    CANCEL_RECORDING_REQUEST,
    AGORA_CALL_RECORDED,
    AGORA_CALL_SPEAKER_VOLUME
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
