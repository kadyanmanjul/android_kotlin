package com.joshtalks.joshskills.ui.call.data.local

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.fragment.app.FragmentActivity
import com.google.gson.Gson
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.base.constants.*
import com.joshtalks.joshskills.base.model.VoipUIState
import com.joshtalks.joshskills.core.ActivityLifecycleCallback
import com.joshtalks.joshskills.core.IS_COURSE_BOUGHT
import com.joshtalks.joshskills.core.PrefManager
import com.joshtalks.joshskills.ui.voip.new_arch.ui.feedback.FeedbackDialogFragment
import com.joshtalks.joshskills.ui.voip.new_arch.ui.report.VoipReportDialogFragment
import com.joshtalks.joshskills.voip.constant.IDLE
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private const val TAG = "VoipPref"

object VoipPref {
        lateinit var preferenceManager: SharedPreferences
        val coroutineExceptionHandler = CoroutineExceptionHandler { _, e ->
            e.printStackTrace()
        }
        val scope = CoroutineScope(Dispatchers.IO + coroutineExceptionHandler)
        val mutex = Mutex(false)
        var isListenerActivated = false

        @Synchronized
        fun initVoipPref(context: Context) {
            preferenceManager = context.getSharedPreferences(
                context.getString(R.string.voip_shared_pref_file_name),
                Context.MODE_PRIVATE
            )
            startListener()
        }

        fun updateIncomingCallData(callId: Int, callType: Int) {
            val editor = preferenceManager.edit()
            editor.putInt(PREF_KEY_INCOMING_CALL_TYPE, callType)
            editor.putInt(PREF_KEY_INCOMING_CALL_ID, callId)
            editor.apply()
        }

        fun updateCurrentCallStartTime(startTime : Long) {
            val editor = preferenceManager.edit()
            editor.putLong(PREF_KEY_CURRENT_CALL_START_TIME, startTime)
            editor.commit()
        }

        fun updateLastCallDetails(
            duration: Long,
            remoteUserName: String,
            remoteUserImage: String?,
            callId: Int,
            callType: Int,
            remoteUserAgoraId: Int,
            localUserAgoraId: Int,
            channelName: String,
            topicName: String
        ) {
            val editor = preferenceManager.edit()
            editor.putLong(PREF_KEY_CURRENT_CALL_START_TIME, 0L)
            editor.putLong(PREF_KEY_LAST_CALL_DURATION, duration)
            editor.putString(PREF_KEY_LAST_REMOTE_USER_NAME, remoteUserName)
            editor.putString(PREF_KEY_LAST_REMOTE_USER_IMAGE, remoteUserImage)
            editor.putInt(PREF_KEY_LAST_CALL_ID, callId)
            editor.putInt(PREF_KEY_LAST_CALL_TYPE, callType)
            editor.putInt(PREF_KEY_LAST_REMOTE_USER_AGORA_ID, remoteUserAgoraId)
            editor.putString(PREF_KEY_LAST_CHANNEL_NAME, channelName)
            editor.putInt(PREF_KEY_LOCAL_USER_AGORA_ID, localUserAgoraId)
            editor.putString(PREF_KEY_LAST_TOPIC_NAME, topicName)
            editor.commit()

            // TODO: These logic shouldn't be here
            if (duration != 0L) {
                showDialogBox(duration)
            }
        }

        // TODO: These function shouldn't be here
        private fun showDialogBox(totalSecond: Long) {
                    scope.launch {
                        delay(500)
                        val currentActivity = ActivityLifecycleCallback.currentActivity
                        if (currentActivity != null) {
                            val newFragmentActivity = currentActivity as? FragmentActivity
                           withContext(Dispatchers.Main) {
                            newFragmentActivity?.showVoipDialog(totalSecond)

                    }
                }
            }
        }

        // TODO: These function shouldn't be here
        private fun FragmentActivity.showVoipDialog(totalSecond: Long) {
            if (totalSecond < 120L && PrefManager.getBoolValue(IS_COURSE_BOUGHT)) {
                showReportDialog(this)
            } else {
                showFeedBackDialog(this)
            }
        }

        private fun showReportDialog(fragmentActivity: FragmentActivity) {

            val function = fun() {
                showFeedBackDialog(fragmentActivity)
            }
            VoipReportDialogFragment.newInstance(
                getLastRemoteUserAgoraId(),
                getLocalUserAgoraId(), "REPORT", getLastCallChannelName(), function
            )
                .show(fragmentActivity.supportFragmentManager, "ReportDialogFragment")
        }

        private fun showFeedBackDialog(fragmentActivity: FragmentActivity) {
            val function = fun() {}
            FeedbackDialogFragment.newInstance(function)
                .show(fragmentActivity.supportFragmentManager, "FeedBackDialogFragment")
        }

        fun getStartTimeStamp(): Long {
            val startTime = preferenceManager.getLong(PREF_KEY_CURRENT_CALL_START_TIME, 0)
            Log.d(TAG, "getStartTimeStamp: $startTime")
            return startTime
        }

        fun getLastCallTopicName(): String {
            return preferenceManager.getString(PREF_KEY_LAST_TOPIC_NAME, "").toString()
        }

        fun getLastRemoteUserName(): String {
            return preferenceManager.getString(PREF_KEY_LAST_REMOTE_USER_NAME, "").toString()
        }

        fun getLastCallType(): Int {
            return preferenceManager.getInt(PREF_KEY_LAST_CALL_TYPE, -1)
        }

        fun getIncomingCallId(): Int {
            return preferenceManager.getInt(PREF_KEY_INCOMING_CALL_ID, -1)
        }

        fun getLastProfileImage(): String {
            return preferenceManager.getString(PREF_KEY_LAST_REMOTE_USER_IMAGE, "").toString()
        }

        fun getLastRemoteUserAgoraId(): Int {
            return preferenceManager.getInt(PREF_KEY_LAST_REMOTE_USER_AGORA_ID, -1)
        }

        fun getLocalUserAgoraId(): Int {
            return preferenceManager.getInt(PREF_KEY_LOCAL_USER_AGORA_ID, -1)
        }

        fun getLastCallId(): Int {
            return preferenceManager.getInt(PREF_KEY_LAST_CALL_ID, -1)
        }

        fun getLastCallChannelName(): String {
            return preferenceManager.getString(PREF_KEY_LAST_CHANNEL_NAME, "").toString()
        }

        fun getLastCallDurationInSec(): Long {
            val duration = preferenceManager.getLong(PREF_KEY_LAST_CALL_DURATION, 0)
            Log.d(TAG, "getLastCallDurationInSec: $duration")
            return duration
        }

        fun startListener() {
            if(isListenerActivated.not()) {
                Log.d(TAG, "startListener: ")
                isListenerActivated = true
                preferenceManager.registerOnSharedPreferenceChangeListener(VoipPrefListener)
            }
        }

    fun getCurrentVoipState(): String {
       return preferenceManager.getString(PREF_KEY_CURRENT_VOIP_STATE, IDLE_STATE)?: IDLE_STATE
    }

    fun updateCurrentVoipState(currentVoipState: String? = IDLE_STATE) {
        Log.d(TAG, "updateCurrentVoipState: Updated")
        val editor = preferenceManager.edit()
        editor.putString(PREF_KEY_CURRENT_VOIP_STATE,currentVoipState)
        editor.apply()
    }

    fun getCurrentVoipStateStack(): Array<String> {
        val gson = Gson()
        val jsonText: String? = preferenceManager.getString(PREF_KEY_CURRENT_VOIP_STATE_STACK, null)
        return gson.fromJson(
            jsonText,
            Array<String>::class.java
        )
    }

    fun updateCurrentVoipStateStack(currentVoipStateStack: Any? = arrayOf("")) {
        val editor = preferenceManager.edit()
        val gson = Gson()
        val textList: Any? = currentVoipStateStack
        val jsonText = gson.toJson(textList)
        editor.putString(PREF_KEY_CURRENT_VOIP_STATE_STACK, jsonText)
        editor.apply()
    }
}