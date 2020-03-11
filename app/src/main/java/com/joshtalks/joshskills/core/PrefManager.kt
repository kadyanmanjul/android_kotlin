package com.joshtalks.joshskills.core

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import com.joshtalks.joshskills.BuildConfig
import com.joshtalks.joshskills.repository.local.AppDatabase

const val USER_UNIQUE_ID = "user_unique_id"
const val FIRST_COURSE_BUY="first_course_buy"
const val FIRST_TIME_OFFER_SHOW="first_time_offer_show"
const val GID_SET_FOR_USER="gid_set_for_user"
const val SERVER_GID_ID="server_gid_id"

object PrefManager {

    @JvmStatic
    private var prefManager: SharedPreferences = this.getPref(AppObjectController.joshApplication)

    private const val PREF_NAME = "JoshSkills"
    private lateinit var sharedPreferences: SharedPreferences


    @SuppressLint("RestrictedApi")
    private fun getPref(context: Context): SharedPreferences {
        sharedPreferences = PreferenceManager(context).sharedPreferences
        PreferenceManager(context).sharedPreferencesName = PREF_NAME
        return sharedPreferences
    }

    fun clear() {
        prefManager.edit().clear().apply()
    }


    fun hasKey(key: String): Boolean {
        return prefManager.contains(key)
    }

    fun getBoolValue(key: String): Boolean {
        return prefManager.getBoolean(key, false)
    }
    fun getStringValue(key: String): String {
        return prefManager.getString(key, EMPTY) ?: EMPTY
    }

    fun getIntValue(key: String): Int {
        return prefManager.getInt(key, 0)

    }

    fun getLongValue(key: String): Long {
        return prefManager.getLong(key, 0)

    }

    fun put(key: String, value: String) {
        prefManager.edit().putString(key, value).apply()

    }

    fun put(key: String, value: Int) {
        prefManager.edit().putInt(key, value).apply()

    }

    fun put(key: String, value: Long) {
        prefManager.edit().putLong(key, value).apply()

    }

    fun put(key: String, value: Boolean) {
        prefManager.edit().putBoolean(key, value).apply()

    }


    fun getClientToken(): String {
        return BuildConfig.CLIENT_TOKEN

    }

    fun logoutUser() {
        prefManager.edit().clear().apply()
    }


    fun clearUser() {
        prefManager.edit().clear().apply()
        AppDatabase.clearDatabase()
    }

    fun removeKey(key: String) {
        prefManager.edit().remove(key).apply()

    }

}