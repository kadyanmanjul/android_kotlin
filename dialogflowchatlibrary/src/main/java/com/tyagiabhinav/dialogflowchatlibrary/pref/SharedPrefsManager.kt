package com.tyagiabhinav.dialogflowchatlibrary.pref

import android.content.Context
import java.util.StringTokenizer

class SharedPrefsManager private constructor(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    companion object {
        private const val PREFERENCES = "josh_skills_dailog"
        const val COURSE_TEST_IDS = "course_test_ids"

        @Synchronized
        fun newInstance(context: Context) = SharedPrefsManager(context)
    }


    fun putInt(key: String, value: Int) = preferences.edit().putInt(key, value).apply()

    fun putString(key: String, value: String) = preferences.edit().putString(key, value).apply()

    fun getBoolean(key: String, defValue: Boolean) = preferences.getBoolean(key, defValue)

    fun getInt(key: String, defValue: Int) = preferences.getInt(key, defValue)

    fun getString(key: String, defValue: String) = preferences.getString(key, defValue)

    fun putBoolean(key: String, value: Boolean) = preferences.edit().putBoolean(key, value).apply()

    fun putArrayList(key: String, value: ArrayList<Int>) {
        val str = StringBuilder()
        for (i in 0 until value.size) {
            str.append(value.get(i)).append(",")
        }
        preferences.edit().putString(key, str.toString()).apply()
    }

    fun getArrayList(key: String, defValue: String): ArrayList<Int> {
        val savedString: String? = preferences.getString(key, defValue)
        if (savedString.isNullOrBlank()) {
            return arrayListOf()
        }
        val st = StringTokenizer(savedString, ",")
        val result = ArrayList<Int>()
        while (st.hasMoreTokens()) {
            result.add(st.nextToken().toInt())
        }
        return result
    }
}