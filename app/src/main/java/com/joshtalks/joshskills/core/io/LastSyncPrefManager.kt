package com.joshtalks.joshskills.core.io

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.EMPTY


object LastSyncPrefManager {

    private const val PREF_NAME_COMMON = "JoshSkills_Last_Sync"

    private val lastSyncPrefManager by lazy {
        getPref(AppObjectController.joshApplication)
    }

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
        lastSyncPrefManager.edit().clear().apply()
    }


    fun hasKey(key: String) = lastSyncPrefManager.contains(key)

    fun getBoolValue(key: String, defValue: Boolean = false) =
        lastSyncPrefManager.getBoolean(key, defValue)

    fun getStringValue(key: String, defaultValue: String = EMPTY) =
        lastSyncPrefManager.getString(key, defaultValue) ?: defaultValue

    fun getIntValue(key: String) = lastSyncPrefManager.getInt(key, 0)


    fun getLongValue(key: String) = lastSyncPrefManager.getLong(key, 0)


    private fun getFloatValue(key: String) = lastSyncPrefManager.getFloat(key, 0F)

    fun put(key: String, value: String) = lastSyncPrefManager.edit().putString(key, value).apply()

    fun put(key: String, value: Int) = lastSyncPrefManager.edit().putInt(key, value).apply()

    fun put(key: String, value: Long) = lastSyncPrefManager.edit().putLong(key, value).apply()

    fun put(key: String, value: Float) = lastSyncPrefManager.edit().putFloat(key, value).apply()

    fun put(key: String, value: Boolean) = lastSyncPrefManager.edit().putBoolean(key, value).apply()

    fun removeKey(key: String) = lastSyncPrefManager.edit().remove(key).apply()

    fun removeAll() = lastSyncPrefManager.edit().clear().apply()


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
