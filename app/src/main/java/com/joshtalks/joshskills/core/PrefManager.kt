package com.joshtalks.joshskills.core

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import com.joshtalks.joshskills.BuildConfig

const val COURSE_STARTED_FB_EVENT = "course_started_event"
const val USER_UNIQUE_ID = "user_unique_id"


object PrefManager {

    private const val PREF_NAME = "JoshSkills"
    private lateinit var sharedPreferences: SharedPreferences
    @JvmStatic
    private var prefManager: SharedPreferences = getPref(AppObjectController.joshApplication)


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

    fun getStringValue(key: String): String {
        return prefManager.getString(key, "").toString()
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





}