package com.joshtalks.joshskills.core

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import com.joshtalks.joshskills.BuildConfig
import com.joshtalks.joshskills.core.service.WorkMangerAdmin
import com.joshtalks.joshskills.repository.local.AppDatabase

const val USER_UNIQUE_ID = "user_unique_id"
const val GID_SET_FOR_USER = "gid_set_for_user"
const val SERVER_GID_ID = "server_gid_id"
const val CERTIFICATE_GENERATE = "_certificate_generate"
const val RATING_DETAILS_KEY = "_rating_details"
const val CUSTOM_PERMISSION_ACTION_KEY = "joshskills_custom_permission"
const val LOGIN_ON = "_login_on"
const val INSTANCE_ID = "joshskills_instance_id"
const val VERSION = "landing_page_version"
const val READ_WRITE_PERMISSION_GIVEN = "read_write_permission_given"
const val PAYMENT_MOBILE_NUMBER = "payment_mobile_number"
const val API_TOKEN = "api_token"
const val COUNTRY_ISO = "country_iso"
const val REFERRED_REFERRAL_CODE = "referred_referral_code"
const val RESTORE_ID = "restore_id"
const val FRESH_CHAT_UNREAD_MESSAGES = "fresh_chat_unread_messages"
const val FRESH_CHAT_ID_RESTORED = "fresh_chat_id_restored"
const val EXPLORE_TYPE = "joshskills_explore_type"

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

    fun getStringValue(key: String, isConsistent: Boolean = false): String {
        return if (isConsistent) prefManagerConsistent.getString(key, EMPTY) ?: EMPTY
        else prefManagerCommon.getString(key, EMPTY) ?: EMPTY
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
        WorkMangerAdmin.appStartWorker()
    }

    fun clearUser() {
        prefManagerCommon.edit().clear().apply()
        AppDatabase.clearDatabase()
        WorkMangerAdmin.appStartWorker()
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
