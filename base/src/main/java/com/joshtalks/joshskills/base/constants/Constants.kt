package com.joshtalks.joshskills.base.constants

const val DIR = "api/skill/v1"
const val DIR_FPP_GROUP = "api/p2p"

const val KEY_AUTHORIZATION = "Authorization"
const val KEY_APP_VERSION_CODE = "app-version-code"
const val KEY_APP_VERSION_NAME = "app-version-name"
const val KEY_APP_USER_AGENT = "HTTP_USER_AGENT"
const val KEY_APP_ACCEPT_LANGUAGE = "Accept-Language"

// Intent Data Constant
const val INTENT_DATA_API_HEADER = "josh_intent_data_api_header"
const val INTENT_DATA_MENTOR_ID = "josh_intent_data_mentor_id"
const val INTENT_DATA_TOPIC_ID = "josh_intent_data_topic_id"
const val INTENT_DATA_CALL_CATEGORY = "josh_intent_data_call_category"
const val INTENT_DATA_COURSE_ID = "josh_intent_data_course_id"
const val INTENT_DATA_PREVIOUS_CALL_ID = "josh_intent_data_previous_call_id"
const val INTENT_DATA_CONNECT_CALL = "josh_intent_data_connect_call"
const val INTENT_DATA_INCOMING_CALL_ID = "josh_intent_data_incoming_call_id"
const val INTENT_DATA_FPP_MENTOR_ID = "josh_intent_fpp_mentor_id"
const val INTENT_DATA_FPP_NAME = "josh_intent_fpp_name"
const val INTENT_DATA_FPP_IMAGE = "josh_intent_fpp_image"
const val INTENT_DATA_GROUP_ID = "josh_intent_group_id"
const val INTENT_DATA_GROUP_NAME =  "josh_intent_group_name"
const val INTENT_DATA_TOTAL_AMOUNT =  "josh_intent_total_amount"
const val INTENT_DATA_EXPERT_PRICE_PER_MIN =  "josh_intent_name_expert_price_per_min"
const val INTENT_DATA_EXPERT_PREMIUM =  "josh_intent_is_expert_premium_call"


// Service Action
const val SERVICE_ACTION_STOP_SERVICE = "josh_service_action_stop_service"
const val SERVICE_ACTION_INCOMING_CALL = "josh_service_action_incoming_call"
const val SERVICE_ACTION_DISCONNECT_CALL = "josh_service_action_disconnect_call"
const val SERVICE_ACTION_INCOMING_CALL_DECLINE = "josh_service_action_incoming_call_decline"
const val SERVICE_ACTION_INCOMING_CALL_HIDE = "josh_service_action_incoming_call_hide"
const val ANALYTICS_EVENT = "analytics_event"
const val START_EXPERT_CALL_TIMER = "start_expert_call"


// Content Provider UI
const val CONTENT_URI = "content://com.joshtalks.joshskills.premium.contentprovider"
const val START_CALL_TIME_URI = "/start_call_time"
const val CALL_DISCONNECTED_URI = "/call_disconnect"
const val API_HEADER = "/api_header"
const val MENTOR_ID = "/mentor_id"
const val COURSE_ID = "/course_id"
const val CURRENT_ACTIVITY = "/current_activity"
const val IS_COURSE_BOUGHT_OR_FREE_TRIAL = "/is_course_bought_or_free_trial"
const val NOTIFICATION_DATA = "/notification_data"
const val MENTOR_NAME = "/mentor_name"
const val MENTOR_PROFILE = "/mentor_profile"
const val DEVICE_ID = "/device_id"
const val IS_FT_ENDED_OR_BLOCKED = "/is_ft_ended_or_blocked"


// Content Values
const val CALL_START_TIME = "josh_call_start_time"
const val CALL_DURATION = "josh_call_duration"
const val REMOTE_USER_NAME = "josh_remote_user_name"
const val REMOTE_USER_IMAGE = "josh_remote_user_image"
const val REMOTE_USER_AGORA_ID = "josh_remote_user_agora_id"
const val CALL_ID = "josh_call_id"
const val CALL_TYPE = "josh_call_type"
const val CURRENT_USER_AGORA_ID = "josh_current_user_agora_id"
const val FPP_SHOW_FLAG = "josh_fpp_flag_id"
const val TOPIC_NAME = "josh_topic_name"
const val CHANNEL_NAME = "josh_channel_name"
const val AUTHORIZATION = "josh_authorization"
const val APP_VERSION_CODE = "josh_app_version_code"
const val APP_VERSION_NAME = "josh_app_version_name"
const val APP_USER_AGENT = "josh_user_agent"
const val APP_ACCEPT_LANGUAGE = "josh_accept_language"
const val REMOTE_USER_MENTOR_ID = "josh_remote_user_mentor_id"

// Pref Keys
const val PREF_KEY_LOCAL_USER_AGORA_ID = "josh_local_user_id"

// Current Call Details
const val PREF_KEY_CURRENT_CALL_START_TIME = "josh_current_call_start_time"

// Last Call Details
//const val PREF_KEY_LAST_CALL_START_TIME = "josh_last_call_start_time"
const val PREF_KEY_LAST_REMOTE_USER_NAME = "josh_last_remote_user_name"
const val PREF_KEY_LAST_REMOTE_USER_IMAGE = "josh_last_remote_user_image"
const val PREF_KEY_LAST_CALL_ID = "josh_last_call_id"
const val PREF_KEY_LAST_CALL_TYPE = "josh_last_call_type"
const val PREF_KEY_LAST_REMOTE_USER_AGORA_ID = "josh_last_remote_user_agora_id"
const val PREF_KEY_LAST_CHANNEL_NAME = "josh_last_channel_name"
const val PREF_KEY_LAST_TOPIC_NAME = "josh_last_topic_name"
const val PREF_KEY_LAST_CALL_DURATION = "josh_last_call_duration"
const val PREF_KEY_LAST_REMOTE_USER_MENTOR_ID = "josh_last_remote_user_mentor_id"
const val PREF_KEY_LAST_DISCONNECT_SCREEN = "josh_last_disconnect_screen"

// FPP Flag Key
const val PREF_KEY_FPP_FLAG = "fpp_flag_state"

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
const val MENTOR_ID_COLUMN = "MENTOR_ID"
const val NOTIFICATION_TITLE_COLUMN = "NOTIFICATION_TITLE_COLUMN"
const val NOTIFICATION_SUBTITLE_COLUMN = "NOTIFICATION_SUBTITLE_COLUMN"
const val NOTIFICATION_LESSON_COLUMN = "NOTIFICATION_LESSON_COLUMN"
const val VIDEO_COLUMN = "VIDEO_COLUMN"
const val MENTOR_NAME_COLUMN = "MENTOR_NAME"
const val MENTOR_PROFILE_COLUMN = "MENTOR_PROFILE"
const val RECORDING_TEXT_COLUMN = "RECORDING_TEXT_COLUMN"
const val GAME_TEXT_COLUMN = "GAME_TEXT_COLUMN"
const val COURSE_ID_COLUMN = "COURSE_ID"
const val FREE_TRIAL_OR_COURSE_BOUGHT_COLUMN = "FREE_TRIAL_OR_COURSE_BOUGHT_COLUMN"
const val DEVICE_ID_COLUMN = "DEVICE_ID"
const val FT_ENDED_OR_BLOCKED_COLUMN = "FT_ENDED_OR_BLOCKED_COLUMN"
const val CURRENT_ACTIVITY_COLUMN = "CURRENT_ACTIVITY_COLUMN"

// Broadcast Receiver
const val SERVICE_BROADCAST_KEY = "service_broadcast_key"
const val START_SERVICE = true
const val STOP_SERVICE = false
const val CALLING_SERVICE_ACTION = "com.joshtalks.joshskills.premium.CALLING_SERVICE"

// Pending Intent
const val FROM_INCOMING_CALL = "josh_from_incoming_call"
const val FROM_ACTIVITY = "josh_from_activity"
const val INCOMING_CALL_ID = "josh_incoming_call_id"
const val FROM_CALL_BAR = "josh_from_call_bar"
const val STARTING_POINT = "josh_starting_point"
const val IS_EXPERT_CALLING = "expert_calling"

const val USER_NAME = "USER_NAME"
const val USER_PROFILE = "USER_PROFILE"
const val EXPERT_CALL_DURATION = "EXPERT_CALL_DURATION"
const val IS_FIRST_CALL = "IS_FIRST_CALL"
const val IS_FIRST_5MIN_CALL = "IS_FIRST_5MIN_CALL"
const val AUTO_CONNECT_CURRENT_TRY_COUNT = "AUTO_CONNECT_CURRENT_TRY_COUNT"

const val CALLING_ACTIVITY_REQUEST_CODE = 102