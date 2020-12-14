package com.joshtalks.joshskills.core

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import com.joshtalks.joshskills.BuildConfig
import com.joshtalks.joshskills.core.service.WorkManagerAdmin
import com.joshtalks.joshskills.repository.local.AppDatabase

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
const val BETTERY_OPTIMIZATION_ALREADY_ASKED = "bettery_optimization_asked"
const val RESUME_CERTIFICATION_EXAM = "resume_certification_exam_"
const val LAST_ACTIVE_API_TIME = "last_active_time_"
const val IS_LEADERBOARD_ACTIVE = "is_leaderboard_active"
const val MY_COLOR_CODE = "joshskills_my_color_code"
const val IS_PROFILE_FEATURE_ACTIVE = "is_leaderboard_active"
const val USER_SCORE = "user_score"

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

    fun getBoolValue(key: String, isConsistent: Boolean = false): Boolean {
        return if (isConsistent) prefManagerConsistent.getBoolean(key, false)
        else prefManagerCommon.getBoolean(key, false)
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


    fun getClientToken(): String {
        return BuildConfig.CLIENT_TOKEN

    }

    fun logoutUser() {
        prefManagerCommon.edit().clear().apply()
        WorkManagerAdmin.appStartWorker()
    }

    fun clearUser() {
        prefManagerCommon.edit().clear().apply()
        AppDatabase.clearDatabase()
        WorkManagerAdmin.appStartWorker()
    }


    fun removeKey(key: String, isConsistent: Boolean = false) {
        if (isConsistent) prefManagerConsistent.edit().remove(key).apply()
        else prefManagerCommon.edit().remove(key).apply()

    }

    fun getLastSyncTime(key: String): Pair<String, String> {
        return try {
            val time = getStringValue(key)
            if (time.isNotEmpty()) {
                Pair("createdmilisecond", time)
            } else {
                Pair("created", getLongValue(key).toString())
            }
        } catch (ex: Exception) {
            Pair("created", "0")
        }
    }
}
