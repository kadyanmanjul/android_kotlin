package com.joshtalks.joshskills.base.constants

const val DIR = "api/skill/v1"

const val KEY_AUTHORIZATION = "Authorization"
const val KEY_APP_VERSION_CODE = "app-version-code"
const val KEY_APP_VERSION_NAME = "app-version-name"
const val KEY_APP_USER_AGENT = "HTTP_USER_AGENT"
const val KEY_APP_ACCEPT_LANGUAGE = "Accept-Language"

// Intent Data Constant
const val INTENT_DATA_API_HEADER = "josh_intent_data_api_header"
const val INTENT_DATA_MENTOR_ID = "josh_intent_data_mentor_id"
const val INTENT_DATA_TOPIC_ID = "josh_intent_data_topic_id"
const val INTENT_DATA_COURSE_ID = "josh_intent_data_course_id"
const val INTENT_DATA_CONNECT_CALL = "josh_intent_data_connect_call"
const val INTENT_DATA_INCOMING_CALL_ID = "josh_intent_data_incoming_call_id"

// Service Action
const val SERVICE_ACTION_STOP_SERVICE = "josh_service_action_stop_service"
const val SERVICE_ACTION_MAIN_PROCESS_IN_BACKGROUND = "josh_service_action_main_process_in_background"
const val SERVICE_ACTION_DISCONNECT_CALL = "josh_service_action_disconnect_call"
const val SERVICE_ACTION_INCOMING_CALL_DECLINE = "josh_service_action_incoming_call_decline"

// Content Provider UI
const val CONTENT_URI = "content://com.joshtalks.joshskills.contentprovider"
const val START_CALL_TIME_URI = "/start_call_time"
const val VOIP_USER_DATA_URI = "/voip_user_data"
const val INCOMING_CALL_URI = "/incoming_call"
const val VOIP_STATE_URI = "/voip_state"
const val VOIP_STATE_LEAVING_URI = "/voip_state_leaving"
const val CALL_DISCONNECTED_URI = "/call_disconnect"
const val CURRENT_MUTE_STATE_URI = "/current_mute_state"
const val CURRENT_SPEAKER_STATE_URI = "/current_speaker_state"
const val CURRENT_HOLD_STATE_URI = "/current_hold_state"
const val CURRENT_REMOTE_MUTE_STATE_URI = "/current_remote_mute_state"
const val RESET_CURRENT_CALL_STATE_URI = "/reset_current_call_state"
const val CURRENT_STATE_URI = "/current_call_state"
const val API_HEADER = "/api_header"
const val MENTOR_ID = "/mentor_id"

// Content Values
const val VOIP_STATE = "josh_voip_state"
const val CALL_START_TIME = "josh_call_start_time"
const val CALL_DURATION = "josh_call_duration"
const val REMOTE_USER_NAME = "josh_remote_user_name"
const val REMOTE_USER_IMAGE = "josh_remote_user_image"
const val REMOTE_USER_AGORA_ID = "josh_remote_user_agora_id"
const val CALL_ID = "josh_call_id"
const val CALL_TYPE = "josh_call_type"
const val CURRENT_USER_AGORA_ID = "josh_current_user_agora_id"
const val TOPIC_NAME = "josh_topic_name"
const val CHANNEL_NAME = "josh_channel_name"
const val IS_MUTE = "josh_is_mute"
const val IS_REMOTE_USER_MUTE = "josh_is_remote_user_mute"
const val IS_SPEAKER_ON = "josh_is_speaker_on"
const val IS_ON_HOLD = "josh_is_on_hold"
const val API_HEADER_DATA = "josh_api_header_data"
const val AUTHORIZATION = "josh_authorization"
const val APP_VERSION_CODE = "josh_app_version_code"
const val APP_VERSION_NAME = "josh_app_version_name"
const val APP_USER_AGENT = "josh_user_agent"
const val APP_ACCEPT_LANGUAGE = "josh_accept_language"

// Content Provider Voip State
const val CONTENT_VOIP_STATE_AUTHORITY = "content://com.joshtalks.joshskills.voipstate"
const val VOIP_STATE_PATH = "/current_voip_state"
const val VOIP_STATE_STACK_PATH = "/current_voip_state_stack"

//    Content values for Voip State
const val CURRENT_VOIP_STATE = "josh_current_voip_state"
const val CURRENT_VOIP_STATE_STACKS = "josh_current_voip_state_stack"

//Voip Current States
const val CONNECTED_STATE = "connected_state"
const val JOINED_STATE = "joined_state"
const val IDLE_STATE = "idle_state"

const val PREF_KEY_CURRENT_VOIP_STATE = "josh_current_voip_state"
const val PREF_KEY_CURRENT_VOIP_STATE_STACK = "josh_current_voip_stack"


// Pref Keys
const val PREF_KEY_WEBRTC_CURRENT_STATE = "josh_webrtc_current_state"
const val PREF_KEY_MAIN_PROCESS_PID = "josh_main_process_pid"

// Current Call Details
const val PREF_KEY_CURRENT_CALL_START_TIME = "josh_current_call_start_time"
const val PREF_KEY_CURRENT_REMOTE_USER_NAME = "josh_current_remote_user_name"
const val PREF_KEY_CURRENT_REMOTE_USER_IMAGE = "josh_current_remote_user_image"
const val PREF_KEY_CURRENT_CALL_ID = "josh_current_call_id"
const val PREF_KEY_CURRENT_CALL_TYPE = "josh_current_call_type"
const val PREF_KEY_CURRENT_REMOTE_USER_AGORA_ID = "josh_current_remote_user_agora_id"
const val PREF_KEY_CURRENT_CHANNEL_NAME = "josh_current_channel_name"
const val PREF_KEY_CURRENT_USER_AGORA_ID= "josh_current_user_id"
const val PREF_KEY_CURRENT_TOPIC_NAME= "josh_current_topic_name"
const val PREF_KEY_CURRENT_USER_ON_MUTE= "josh_current_user_on_mute"
const val PREF_KEY_CURRENT_REMOTE_USER_ON_MUTE= "josh_current_remote_user_on_mute"
const val PREF_KEY_CURRENT_USER_SPEAKER_ON= "josh_current_user_speaker_on"
const val PREF_KEY_CURRENT_USER_ON_HOLD= "josh_current_user_on_hold"

// Last Call Details
const val PREF_KEY_LAST_CALL_START_TIME = "josh_last_call_start_time"
const val PREF_KEY_LAST_REMOTE_USER_NAME = "josh_last_remote_user_name"
const val PREF_KEY_LAST_REMOTE_USER_IMAGE = "josh_last_remote_user_image"
const val PREF_KEY_LAST_CALL_ID = "josh_last_call_id"
const val PREF_KEY_LAST_CALL_TYPE = "josh_last_call_type"
const val PREF_KEY_LAST_REMOTE_USER_AGORA_ID = "josh_last_remote_user_agora_id"
const val PREF_KEY_LAST_CHANNEL_NAME = "josh_last_channel_name"
const val PREF_KEY_LAST_TOPIC_NAME= "josh_last_topic_name"
const val PREF_KEY_LAST_CALL_DURATION= "josh_last_call_duration"

// Recent Incoming Call Details
const val PREF_KEY_INCOMING_CALL_ID = "josh_recent_incoming_call_id"
const val PREF_KEY_INCOMING_CALL_TYPE = "josh_recent_incoming_call_type"

// Call Type
const val PEER_TO_PEER = 1
const val FPP = 2
const val GROUP = 3

// Call Direction
const val INCOMING = 1
const val OUTGOING = 2

// Cursor Column Name
const val START_CALL_TIME_COLUMN = "START_CALL_TIME"
const val API_HEADER_COLUMN = "API_HEADER"
const val MENTOR_ID_COLUMN = "MENTOR_ID"

// Broadcast Receiver
const val SERVICE_BROADCAST_KEY = "service_broadcast_key"
const val START_SERVICE = true
const val STOP_SERVICE = false
const val CALLING_SERVICE_ACTION = "com.joshtalks.joshskills.CALLING_SERVICE"

// Pending Intent
const val FROM_INCOMING_CALL = "josh_from_incoming_call"
const val FROM_ACTIVITY = "josh_from_activity"
const val INCOMING_CALL_ID = "josh_incoming_call_id"
const val FROM_CALL_BAR = "josh_from_call_bar"
const val STARTING_POINT = "josh_starting_point"

