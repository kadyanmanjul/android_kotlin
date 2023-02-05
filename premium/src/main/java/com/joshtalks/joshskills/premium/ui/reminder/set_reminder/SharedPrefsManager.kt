package com.joshtalks.joshskills.premium.ui.reminder.set_reminder

import android.content.Context

class SharedPrefsManager private constructor(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    companion object {
        private const val PREFERENCES = "sPrefs"
        const val IS_FIRST_REMINDER = "is_first_reminder"
        const val IS_REMINDER_SYNCED: String = "is_reminder_synced"

        @Synchronized
        fun newInstance(context: Context) = SharedPrefsManager(context)
    }


    fun putInt(key: String, value: Int) = preferences.edit().putInt(key, value).apply()

    fun putString(key: String, value: String) = preferences.edit().putString(key, value).apply()

    fun getBoolean(key: String, defValue: Boolean) = preferences.getBoolean(key, defValue)

    fun getInt(key: String, defValue: Int) = preferences.getInt(key, defValue)

    fun getString(key: String, defValue: String) = preferences.getString(key, defValue)

    fun putBoolean(key: String, value: Boolean) = preferences.edit().putBoolean(key, value).apply()
}