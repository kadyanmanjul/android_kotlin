package com.joshtalks.joshskills.base.storage

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.preference.PreferenceManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.joshtalks.joshskills.BuildConfig
import com.joshtalks.joshskills.base.constants.CALLING_SERVICE_ACTION
import com.joshtalks.joshskills.base.constants.SERVICE_BROADCAST_KEY
import com.joshtalks.joshskills.base.constants.STOP_SERVICE
import com.joshtalks.joshskills.base.core.AppObjectController

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
        return context.getSharedPreferences(fileName, Context.MODE_PRIVATE)
    }

    fun clear() {
        prefManagerCommon.edit().clear().apply()
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

    fun getIntValue(key: String, isConsistent: Boolean = false, defValue: Int = 0): Int {
        return if (isConsistent) prefManagerConsistent.getInt(key, defValue)
        else prefManagerCommon.getInt(key, defValue)
    }

    fun getLongValue(key: String, isConsistent: Boolean = false, defValue: Long = 0L): Long {
        return if (isConsistent) prefManagerConsistent.getLong(key, defValue)
        else prefManagerCommon.getLong(key, defValue)
    }

    fun getFloatValue(key: String, isConsistent: Boolean = false, defValue: Float = 0F): Float {
        return if (isConsistent) prefManagerConsistent.getFloat(key, defValue)
        else prefManagerCommon.getFloat(key, defValue)
    }

    fun getSetValue(key: String, isConsistent: Boolean = false, defValue: Set<String> = setOf()): Set<String> {
        return if (isConsistent) prefManagerConsistent.getStringSet(key, defValue) ?: defValue
        else prefManagerCommon.getStringSet(key, defValue) ?: defValue
    }

    fun <T> getPrefObject(key: String, modelClass: Class<T>): T {
        val gson = Gson()
        val json: String = getStringValue(key = key)
        return gson.fromJson(json, modelClass)
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

    fun put(key: String, value: String, isConsistent: Boolean = false) {
        if (isConsistent) prefManagerConsistent.edit().putString(key, value).apply()
        else prefManagerCommon.edit().putString(key, value).apply()
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

    fun hasKey(key: String, isConsistent: Boolean = false): Boolean {
        return if (isConsistent) prefManagerConsistent.contains(key)
        else prefManagerCommon.contains(key)
    }

    fun isInSet(
        key: String,
        value: String,
        isConsistent: Boolean = false
    ): Boolean = getSetValue(key, isConsistent).contains(value)

    fun removeKey(key: String, isConsistent: Boolean = false) {
        if (isConsistent) prefManagerConsistent.edit().remove(key).apply()
        else prefManagerCommon.edit().remove(key).apply()
    }

    fun getPrefMap(key: String): MutableMap<String, Any?>? {
        val gson = Gson()
        val json: String = getStringValue(key = key, defaultValue = "") as String
        return gson.fromJson(json, object : TypeToken<MutableMap<String, Any?>>() {}.type)
    }

    fun getClientToken() = BuildConfig.CLIENT_TOKEN

    fun logoutUser() {
        sendBroadcast()
        prefManagerCommon.edit().clear().apply()
        LastSyncPrefManager.clear()
        /* TODO: missing WorkManagerAdmin
        WorkManagerAdmin.instanceIdGenerateWorker()
        WorkManagerAdmin.appInitWorker()
        WorkManagerAdmin.appStartWorker(true)*/
    }

    fun clearUser() {
        sendBroadcast()
        LastSyncPrefManager.clear()
        prefManagerCommon.edit().clear().apply()
        /* TODO: missing AppDatabase, WorkManagerAdmin
        AppDatabase.clearDatabase()
        WorkManagerAdmin.instanceIdGenerateWorker()
        WorkManagerAdmin.appInitWorker()
        WorkManagerAdmin.appStartWorker(true) */
    }

    fun clearDatabase() {
        LastSyncPrefManager.clear()
        //TODO: AppDatabase missing -- AppDatabase.clearDatabase()
    }

    private fun sendBroadcast() {
        val broadcastIntent = Intent().apply {
            action = CALLING_SERVICE_ACTION
            putExtra(SERVICE_BROADCAST_KEY, STOP_SERVICE)
        }
        LocalBroadcastManager.getInstance(AppObjectController.joshApplication).sendBroadcast(broadcastIntent)
    }
}
