package com.joshtalks.joshskills.core

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.preference.PreferenceManager
import com.google.gson.Gson
import com.joshtalks.joshskills.BuildConfig
import com.joshtalks.joshskills.base.constants.CALLING_SERVICE_ACTION
import com.joshtalks.joshskills.base.constants.SERVICE_BROADCAST_KEY
import com.joshtalks.joshskills.base.constants.STOP_SERVICE
import com.joshtalks.joshskills.core.io.LastSyncPrefManager
import com.joshtalks.joshskills.core.service.WorkManagerAdmin
import com.joshtalks.joshskills.repository.local.AppDatabase
import com.joshtalks.joshskills.ui.voip.new_arch.ui.report.model.VoipReportModel
import com.joshtalks.joshskills.ui.voip.voip_rating.model.ReportModel

const val USER_UNIQUE_ID = "user_unique_id"
const val GID_SET_FOR_USER = "gid_set_for_user"
const val SERVER_GID_ID = "server_gid_id"
const val CERTIFICATE_GENERATE = "_certificate_generate"
const val RATING_DETAILS_KEY = "_rating_details"
const val ONBOARDING_VERSION_KEY = "onboarding_version_keys"
const val CUSTOM_PERMISSION_ACTION_KEY = "joshskills_custom_permission"
const val LOGIN_ON = "_login_on"
const val INSTANCE_ID = "joshskills_instance_id"
const val IS_GUEST_ENROLLED = "is_guest_enrolled"
const val VERSION = "landing_page_version"
const val READ_WRITE_PERMISSION_GIVEN = "read_write_permission_given"
const val PAYMENT_MOBILE_NUMBER = "payment_mobile_number"
const val IS_PAYMENT_DONE = "is_payment_done"
const val API_TOKEN = "api_token"
const val COUNTRY_ISO = "country_iso"
const val REFERRED_REFERRAL_CODE = "referred_referral_code"
const val RESTORE_ID = "restore_id"
const val FRESH_CHAT_UNREAD_MESSAGES = "fresh_chat_unread_messages"
const val FRESH_CHAT_ID_RESTORED = "fresh_chat_id_restored"
const val EXPLORE_TYPE = "joshskills_explore_type"
const val IS_TRIAL_ENDED = "joshskills_is_trial_ended"
const val IS_TRIAL_STARTED = "joshskills_is_trial_started"
const val REMAINING_TRIAL_DAYS = "joshskills_remaining_trial_days"
const val IS_SUBSCRIPTION_ENDED = "joshskills_is_subscription_ended"
const val IS_SUBSCRIPTION_STARTED = "joshskills_is_subscription_started"
const val SUBSCRIPTION_TEST_ID = "joshskills_subscription_test_id"
const val REMAINING_SUBSCRIPTION_DAYS = "joshskills_remaining_subscription_days"
const val IS_PRACTISE_PARTNER_VIEWED = "joshskills_practise_partner_viewed"
const val SHOW_COURSE_DETAIL_TOOLTIP = "show_course_detail_tooltip"
const val IN_APP_REVIEW_COUNT = "in_app_review_count"
const val SELECTED_QUALITY = "selected_quality"
const val CLEAR_CACHE = "is_clear_cache"
const val USER_LOCALE = "user_locale"
const val USER_LOCALE_UPDATED = "user_locale_update"
const val IS_LOCALE_UPDATED_IN_SETTINGS="is_locale_updated_in_settings"
const val IS_LOCALE_UPDATED_IN_INBOX="is_locale_updated_in_inbox"
const val BETTERY_OPTIMIZATION_ALREADY_ASKED = "bettery_optimization_asked"
const val RESUME_CERTIFICATION_EXAM = "resume_certification_exam_"
const val LAST_ACTIVE_API_TIME = "last_active_time_"
const val IS_LEADERBOARD_ACTIVE = "is_leaderboard_active"
const val MY_COLOR_CODE = "joshskills_my_color_code"
const val COURSE_PROGRESS_OPENED = "course_progress_opened"
const val IS_GROUP_CHAT_HINT_SEEN = "joshskills_is_groupchat_hint_seen"
const val IS_PROFILE_FEATURE_ACTIVE = "is_leaderboard_active"
const val USER_SCORE = "user_score"
const val SPEAKING_POINTS = "speaking_points"
const val LESSON_TWO_OPENED = "lesson_two_opened"
const val GROUP_CHAT_LAST_READ_MESSAGE_ID = "GROUP_CHAT_LAST_READ_MESSAGE_ID"
const val CALL_RINGTONE_NOT_MUTE = "call_ringtone_not_mute"
const val IS_GROUP_NOTIFICATION_MUTED = "is_group_notification_muted"
const val DEMO_P2P_CALLEE_NAME = "demo_p2p_callee_name"
const val INTRODUCTION_LAST_POSITION = "introduction_page_last_position"
const val INTRODUCTION_IS_CONTINUE_CLICKED = "introduction_is_continue_clicked"
const val INTRODUCTION_YES_EXCITED_CLICKED = "introduction_yes_excited_clicked"
const val INTRODUCTION_START_NOW_CLICKED = "introduction_start_now_clicked"
const val DEMO_LESSON_TOPIC_ID = "demo_lesson_topic_id"
const val DEMO_LESSON_NUMBER = "demo_lesson_number"
const val LEADER_BOARD_OPEN_COUNT = "leader_board_open_count"
const val P2P_LAST_CALL = "has_p2p_last_call"
const val SEARCH_HINT_SHOW = "search_hint_show"
const val ONLINE_HINT_SHOW = "online_hint_show"
const val PREF_IS_CONVERSATION_ROOM_ACTIVE = "is_conversation_room_active"
const val IS_CONVERSATION_ROOM_ACTIVE_FOR_USER = "is_conversation_room_active_for_user"

const val LAST_FIRESTORE_NOTIFICATION_TIME = "last_firestore_notification_time"
const val ONLINE_TEST_COMPLETED = "online_test_completed"
const val ONLINE_TEST_LAST_LESSON_COMPLETED = "online_test_last_lesson_completed"
const val ONLINE_TEST_LAST_LESSON_ATTEMPTED = "online_test_last_lesson_attempted"
const val ONLINE_TEST_LIST_OF_COMPLETED_RULES = "online_test_list_of_completed_rules"
const val ONLINE_TEST_LIST_OF_TOTAL_RULES = "online_test_list_of_total_rules"
const val INBOX_SCREEN_VISIT_COUNT = "inbox_screen_visit_count"
const val IS_FREE_TRIAL = "joshskills_is_free_trial"
const val FREE_TRIAL_TEST_SCORE = "free_trial_test_score"
const val HAS_ENTERED_NAME_IN_FREE_TRIAL = "has_entered_name_in_free_trial"
const val IS_ENTERED_NAME_IN_FREE_TRIAL = "is_entered_name_in_free_trial"
const val HAS_SEEN_LESSON_TOOLTIP = "joshskills_has_seen_lesson_tooltip"
const val HAS_SEEN_LEADERBOARD_TOOLTIP = "joshskills_has_seen_leaderboard_tooltip"
const val HAS_SEEN_GRAMMAR_TOOLTIP = "joshskills_has_seen_grammar_tooltip"
const val HAS_SEEN_VOCAB_TOOLTIP = "joshskills_has_seen_vocab_tooltip"
const val HAS_SEEN_VOCAB_SPEAKING_ANIMATION = "joshskills_has_seen_vocab_speaking_animation"
const val HAS_SEEN_VOCAB_HAND_TOOLTIP = "joshskills_has_seen_vocab_hand_tooltip"
const val HAS_SEEN_READING_HAND_TOOLTIP = "joshskills_has_seen_reading_hand_tooltip"
const val HAS_SEEN_READING_TOOLTIP = "joshskills_has_seen_reading_tooltip"
const val HAS_SEEN_GROUP_TOOLTIP = "joshskills_has_seen_group_tooltip"
const val HAS_SEEN_GROUP_CALL_TOOLTIP = "joshskills_has_seen_group_call_tooltip"
const val HAS_SEEN_READING_PLAY_ANIMATION = "joshskills_has_seen_reading_play_animation"
const val HAS_SEEN_SPEAKING_TOOLTIP = "joshskills_has_seen_speaking_tooltip"
const val LESSON_COMPLETE_SNACKBAR_TEXT_STRING = "lesson_complete_snackbar_text_string"
const val HAS_SEEN_LEADERBOARD_ANIMATION = "joshskills_has_seen_leaderboard_animation"
const val HAS_SEEN_LESSON_SPOTLIGHT = "joshskills_has_seen_lesson_spotlight"
const val HAS_SEEN_SPEAKING_SPOTLIGHT = "joshskills_has_seen_speaking_spotlight"
const val HAS_SEEN_CONVO_ROOM_SPOTLIGHT = "joshskills_has_seen_convo_room_spotlight"
const val HAS_SEEN_CONVO_ROOM_POINTS = "joshskills_has_seen_convo_room_points"
const val HAS_SEEN_LOCAL_NOTIFICATION = "has_seen_local_notification"
const val LOCAL_NOTIFICATION_INDEX = "local_notification_index"
const val CHAT_OPENED_FOR_NOTIFICATION = "chat_opened_for_notification"
const val LESSON_COMPLETED_FOR_NOTIFICATION = "lesson_complete_for_notification"
const val IS_COURSE_BOUGHT = "is_course_bought"
const val COURSE_EXPIRY_TIME_IN_MS = "course_expiry_time_in_ms"
const val ONBOARDING_STAGE = "onboarding_stage"
const val BLOCK_ISSUE = "BLOCK_ISSUE"
const val REPORT_ISSUE = "REPORT_ISSUE"
const val LAST_TIME_AUTOSTART_SHOWN = "LAST_TIME_AUTOSTART_SHOWN"
const val SHOULD_SHOW_AUTOSTART_POPUP = "SHOULD_SHOW_AUTOSTART_POPUP"
const val LAST_TIME_WORK_MANAGER_START = "LAST_TIME_WORK_MANAGER_START"
const val HAS_SEEN_COHORT_BASE_COURSE_TOOLTIP = "joshskills_has_seen_cohort_base_course_tooltip"

const val USER_ACTIVE_IN_GAME = "game_active"
const val USER_LEAVE_THE_GAME = "game_left"
const val USER_MUTE_OR_NOT = "mute_un_mute"
const val HAS_SEEN_QUIZ_VIDEO_TOOLTIP = "has_seen_quiz_video_tooltip"
const val LAST_SEEN_VIDEO_ID = "last_seen_video_id"
const val IS_CALL_BTN_CLICKED_FROM_NEW_SCREEN = "is_call_btn_clicked_from_new_screen"
const val LAST_FAKE_CALL_INVOKE_TIME = "last_fake_call_invoke_time"
const val IS_LOGIN_VIA_TRUECALLER = "is_login_via_truecaller"
const val IS_ENGLISH_SYLLABUS_PDF_OPENED = "is_english_syllabus_pdf_opened"
const val IS_FREE_TRIAL_ENDED = "is_free_trial_ended"
const val CURRENT_COURSE_ID = "course_id"
const val DEFAULT_COURSE_ID = "151"
const val PAID_COURSE_TEST_ID = "PAID_COURSE_TEST_ID"
const val IS_FREE_TRIAL_CAMPAIGN_ACTIVE = "is_free_trial_campaign_active"
const val IS_EFT_VARIENT_ENABLED = "is_eft_varient_enabled"
const val IS_VOIP_NEW_ARCH_ENABLED = "is_voip_new_arch_enabled"
const val IS_TWENTY_MIN_CALL_ENABLED = "is_twenty_min_call_enabled"
const val REMOVE_TOOLTIP_FOR_TWENTY_MIN_CALL = "remove_toolpit_for_twenty_min_call"
const val TWENTY_MIN_CALL_GOAL_POSTED = "twenty_min_call_goal_posted"
const val SPEAKING_SCREEN_SEEN_GOAL_POSTED = "speaking_screen_seen_goal_posted"
const val TWENTY_MIN_CALL_ATTEMPTED_GOAL_POSTED = "twenty_min_call_attempted_goal_posted"
const val IS_SPEAKING_SCREEN_CLICKED = "is_speaking_screen_clicked"
const val CALL_BTN_CLICKED = "call_btn_clicked"
const val IS_APP_OPENED_FOR_FIRST_TIME = "is_app_opened_for_first_time"
const val IS_HINDI_SELECTED = "is_hindi_selected"
const val IS_HINGLISH_SELECTED = "is_hinglish_selected"
const val SERVER_TIME_OFFSET = "server_time_offset"
const val IS_A2_C1_RETENTION_ENABLED = "is_a2_c1_retention_enabled"
const val ONE_GROUP_REQUEST_SENT = "ONE_GROUP_REQUEST_SENT"

const val MOENGAGE_USER_CREATED = "MOENGAGE_USER_CREATED"

object PrefManager {

    private const val PREF_NAME_COMMON = "JoshSkills"
    private const val PREF_NAME_CONSISTENT = "com.joshtalks.joshskills.JoshSkillsConsistentPref"

    @JvmStatic
    private val prefManagerCommon by lazy {
        this.getPref(AppObjectController.joshApplication)
    }

    @JvmStatic
    private val prefManagerConsistent: SharedPreferences =
        this.getPref(AppObjectController.joshApplication, PREF_NAME_CONSISTENT)


    @SuppressLint("RestrictedApi")
    private fun getPref(context: Context): SharedPreferences {
        val sharedPreferences = PreferenceManager(context).sharedPreferences
        PreferenceManager(context).sharedPreferencesName = PREF_NAME_COMMON
        return sharedPreferences
    }

    @SuppressLint("RestrictedApi")
    private fun getPref(context: Context, fileName: String): SharedPreferences {
        return context.getSharedPreferences(
            fileName, Context.MODE_PRIVATE
        )
    }

    fun clear() {
        prefManagerCommon.edit().clear().apply()
    }


    fun hasKey(key: String, isConsistent: Boolean = false): Boolean {
        return if (isConsistent) prefManagerConsistent.contains(key)
        else prefManagerCommon.contains(key)
    }

    @JvmStatic
    fun getBoolValue(
        key: String,
        isConsistent: Boolean = false,
        defValue: Boolean = false
    ): Boolean {
        return if (isConsistent) prefManagerConsistent.getBoolean(key, defValue)
        else prefManagerCommon.getBoolean(key, defValue)
    }

    fun getStringValue(
        key: String,
        isConsistent: Boolean = false,
        defaultValue: String = EMPTY
    ): String {
        return if (isConsistent) prefManagerConsistent.getString(key, EMPTY) ?: EMPTY
        else prefManagerCommon.getString(key, defaultValue) ?: EMPTY
    }

    fun getIntValue(key: String, isConsistent: Boolean = false): Int {
        return if (isConsistent) prefManagerConsistent.getInt(key, 0)
        else prefManagerCommon.getInt(key, 0)

    }

    fun getIntValue(key: String, isConsistent: Boolean = false, defValue: Int): Int {
        return if (isConsistent) prefManagerConsistent.getInt(key, defValue)
        else prefManagerCommon.getInt(key, defValue)
    }

    fun getSetValue(key: String, isConsistent: Boolean = false, defValue: Set<String> = setOf()): Set<String> {
        return if (isConsistent) prefManagerConsistent.getStringSet(key, defValue) ?: defValue
        else prefManagerCommon.getStringSet(key, defValue) ?: defValue
    }

    fun appendToSet(
        key: String,
        value: String,
        isConsistent: Boolean = false,
    ) {
        getSetValue(key, isConsistent).toMutableSet().also {
            it.add(value)
            put(key, it, isConsistent)
        }
    }

    fun isInSet(
        key: String,
        value: String,
        isConsistent: Boolean = false
    ): Boolean = getSetValue(key, isConsistent).contains(value)

    fun getLongValue(key: String, isConsistent: Boolean = false): Long {
        return if (isConsistent) prefManagerConsistent.getLong(key, 0)
        else prefManagerCommon.getLong(key, 0)
    }

    private fun getFloatValue(key: String, isConsistent: Boolean = false): Float {
        return if (isConsistent) prefManagerConsistent.getFloat(key, 0F)
        else prefManagerCommon.getFloat(key, 0F)

    }

    fun put(key: String, value: String, isConsistent: Boolean = false) {
        if (isConsistent) prefManagerConsistent.edit().putString(key, value).apply()
        else prefManagerCommon.edit().putString(key, value).apply()

    }

    fun put(key: String, value: Int, isConsistent: Boolean = false) {
        if (isConsistent) prefManagerConsistent.edit().putInt(key, value).apply()
        else prefManagerCommon.edit().putInt(key, value).apply()

    }

    fun put(key: String, value: Long, isConsistent: Boolean = false) {
        if (isConsistent) prefManagerConsistent.edit().putLong(key, value).apply()
        else prefManagerCommon.edit().putLong(key, value).apply()

    }

    fun put(key: String, value: Float, isConsistent: Boolean = false) {
        if (isConsistent) prefManagerConsistent.edit().putFloat(key, value).apply()
        else prefManagerCommon.edit().putFloat(key, value).apply()

    }

    fun put(key: String, value: Boolean, isConsistent: Boolean = false) {
        if (isConsistent) prefManagerConsistent.edit().putBoolean(key, value).apply()
        else prefManagerCommon.edit().putBoolean(key, value).apply()
    }

    fun put(key: String, value: Set<String>, isConsistent: Boolean = false) {
        if (isConsistent) prefManagerConsistent.edit().putStringSet(key, value).apply()
        else prefManagerCommon.edit().putStringSet(key, value).apply()
    }

    fun putPrefObject(key: String, objects: Any) {
        val gson = Gson()
        val jsonString = gson.toJson(objects)
        put(key = key, value = jsonString)
    }

    fun getPrefObject(key: String): ReportModel? {
        val gson = Gson()
        val json: String = getStringValue(key = key, defaultValue = "") as String
        return gson.fromJson(json, ReportModel::class.java)
    }

    fun getVoipPrefObject(key: String): VoipReportModel? {
        val gson = Gson()
        val json: String = getStringValue(key = key, defaultValue = "") as String
        return gson.fromJson(json, VoipReportModel::class.java)
    }

    fun getClientToken(): String {
        return BuildConfig.CLIENT_TOKEN
    }

    fun logoutUser() {
        sendBroadcast()
        prefManagerCommon.edit().clear().apply()
        LastSyncPrefManager.clear()
        WorkManagerAdmin.instanceIdGenerateWorker()
        WorkManagerAdmin.appInitWorker()
        WorkManagerAdmin.appStartWorker(true)
    }

    fun clearUser() {
        sendBroadcast()
        LastSyncPrefManager.clear()
        prefManagerCommon.edit().clear().apply()
        AppDatabase.clearDatabase()
        WorkManagerAdmin.instanceIdGenerateWorker()
        WorkManagerAdmin.appInitWorker()
        WorkManagerAdmin.appStartWorker(true)
    }

    fun clearDatabase() {
        LastSyncPrefManager.clear()
        AppDatabase.clearDatabase()
    }


    fun removeKey(key: String, isConsistent: Boolean = false) {
        if (isConsistent) prefManagerConsistent.edit().remove(key).apply()
        else prefManagerCommon.edit().remove(key).apply()

    }

    private fun sendBroadcast() {
        val broadcastIntent = Intent().apply {
            action = CALLING_SERVICE_ACTION
            putExtra(SERVICE_BROADCAST_KEY, STOP_SERVICE)
        }
        LocalBroadcastManager.getInstance(AppObjectController.joshApplication).sendBroadcast(broadcastIntent)
    }

    fun getLastSyncTime(key: String): Pair<String, String> {
        return try {
            val time = getStringValue(key)
            if (time.isNotEmpty()) {
                Pair("createdmilisecond", time)
            } else {
                Pair("createdmilisecond", getLongValue(key).toString())
            }
        } catch (ex: Exception) {
            Pair("createdmilisecond", "0")
        }
    }
}
