package com.joshtalks.badebhaiya.core

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager

const val API_TOKEN = "api_token"

object PrefManager {
    private const val PREF_NAME_COMMON = "JoshBadeBhaiya"
    private const val PREF_NAME_CONSISTENT = "com.joshtalks.badebhaiya.JoshBadeBhaiyaConsistentPref"

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
        return context.getSharedPreferences(fileName, Context.MODE_PRIVATE)
    }

    fun clear() {
        prefManagerCommon.edit().clear().apply()
    }

    fun hasKey(key: String, isConsistent: Boolean = false): Boolean {
        return if (isConsistent) prefManagerConsistent.contains(key)
        else prefManagerCommon.contains(key)
    }

    @JvmStatic
    fun getBoolValue(key: String, isConsistent: Boolean = false, defValue: Boolean = false): Boolean {
        return if (isConsistent) prefManagerConsistent.getBoolean(key, defValue)
        else prefManagerCommon.getBoolean(key, defValue)
    }

    fun getStringValue(key: String, isConsistent: Boolean = false, defaultValue: String = EMPTY): String {
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

    fun appendToSet(key: String, value: String, isConsistent: Boolean = false) {
        getSetValue(key, isConsistent).toMutableSet().also {
            it.add(value)
            put(key, it, isConsistent)
        }
    }

    fun isInSet(key: String, value: String, isConsistent: Boolean = false): Boolean = getSetValue(key, isConsistent).contains(value)

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

    fun logoutUser() {

    }

    fun clearUser() {

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
                Pair("createdmilisecond", getLongValue(key).toString())
            }
        } catch (ex: Exception) {
            Pair("createdmilisecond", "0")
        }
    }
}
