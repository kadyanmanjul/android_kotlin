package com.joshtalks.joshskills.ui.call.data.local

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.fragment.app.FragmentActivity
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

        fun updateCallDetails(
            timestamp: Long,
            remoteUserName: String = "",
            remoteUserImage: String? = null,
            callId: Int = -1,
            callType: Int = -1,
            remoteUserAgoraId: Int = -1,
            currentUserAgoraId: Int = -1,
            channelName: String = "",
            topicName: String = ""
        ) {
            val editor = preferenceManager.edit()
            Log.d(TAG, "updateCallDetails: timestamp --> $timestamp")
            updateUserDetails(
                remoteUserImage = remoteUserImage,
                remoteUserName = remoteUserName,
                remoteUserAgoraId = remoteUserAgoraId,
                callId = callId,
                callType = callType,
                currentUserAgoraId = currentUserAgoraId,
                channelName = channelName,
                topicName = topicName
            )
            editor.putLong(PREF_KEY_CURRENT_CALL_START_TIME, timestamp)
            editor.apply()
        }

        fun updateUserDetails(
            remoteUserName: String = "",
                              remoteUserImage: String? = null,
                              callId: Int = -1,
                              callType: Int = -1,
                              remoteUserAgoraId: Int = -1,
                              currentUserAgoraId: Int = -1,
                              channelName: String = "",
                              topicName: String = "") {
            val editor = preferenceManager.edit()
            editor.putString(PREF_KEY_CURRENT_REMOTE_USER_NAME, remoteUserName)
            editor.putString(PREF_KEY_CURRENT_REMOTE_USER_IMAGE, remoteUserImage)
            editor.putInt(PREF_KEY_CURRENT_CALL_ID, callId)
            editor.putInt(PREF_KEY_CURRENT_CALL_TYPE, callType)
            editor.putInt(PREF_KEY_CURRENT_REMOTE_USER_AGORA_ID, remoteUserAgoraId)
            editor.putString(PREF_KEY_CURRENT_CHANNEL_NAME, channelName)
            editor.putInt(PREF_KEY_CURRENT_USER_AGORA_ID, currentUserAgoraId)
            editor.putString(PREF_KEY_CURRENT_TOPIC_NAME, topicName)
            Log.d(TAG, "updateUserDetails: ")
            editor.apply()
        }

        fun resetCurrentCallState() {
            val editor = preferenceManager.edit()
            editor.putBoolean(PREF_KEY_CURRENT_USER_ON_MUTE, false)
            editor.putBoolean(PREF_KEY_CURRENT_USER_ON_HOLD, false)
            editor.putBoolean(PREF_KEY_CURRENT_USER_SPEAKER_ON, false)
            editor.putBoolean(PREF_KEY_CURRENT_REMOTE_USER_ON_MUTE, false)
            editor.apply()
        }

        fun currentUserMuteState(userOnMute: Boolean = false) {
            Log.d(TAG, "currentUserMuteState: $userOnMute")
            val editor = preferenceManager.edit()
            editor.putBoolean(PREF_KEY_CURRENT_USER_ON_MUTE, userOnMute)
            editor.apply()
        }

        fun currentUserHoldState(userOnHold: Boolean = false) {
            Log.d(TAG, "currentUserHoldState: $userOnHold")
            val editor = preferenceManager.edit()
            editor.putBoolean(PREF_KEY_CURRENT_USER_ON_HOLD, userOnHold)
            editor.apply()
        }

        fun currentUserSpeakerState(userSpeakerOn: Boolean = false) {
            Log.d(TAG, "currentUserSpeakerState: $userSpeakerOn")
            val editor = preferenceManager.edit()
            editor.putBoolean(PREF_KEY_CURRENT_USER_SPEAKER_ON, userSpeakerOn)
            editor.apply()
        }

        fun currentRemoteUserMuteState(remoteUserOnMute: Boolean = false) {
            Log.d(TAG, "currentRemoteUserMuteState: $remoteUserOnMute")
            val editor = preferenceManager.edit()
            editor.putBoolean(PREF_KEY_CURRENT_REMOTE_USER_ON_MUTE, remoteUserOnMute)
            editor.apply()
        }

        fun updateIncomingCallData(callId: Int, callType: Int) {
            val editor = preferenceManager.edit()
            editor.putInt(PREF_KEY_INCOMING_CALL_TYPE, callType)
            editor.putInt(PREF_KEY_INCOMING_CALL_ID, callId)
            editor.apply()
        }

        fun updateLastCallDetails(duration: Long) {
            val editor = preferenceManager.edit()
            editor.putLong(PREF_KEY_LAST_CALL_DURATION, duration)

            editor.putString(
                PREF_KEY_LAST_REMOTE_USER_NAME, preferenceManager.getString(
                    PREF_KEY_CURRENT_REMOTE_USER_NAME, ""
                )
            )
            editor.putString(
                PREF_KEY_LAST_REMOTE_USER_IMAGE, preferenceManager.getString(
                    PREF_KEY_CURRENT_REMOTE_USER_IMAGE, null
                )
            )
            editor.putInt(
                PREF_KEY_LAST_CALL_ID, preferenceManager.getInt(
                    PREF_KEY_CURRENT_CALL_ID, -1
                )
            )
            editor.putInt(
                PREF_KEY_LAST_CALL_TYPE, preferenceManager.getInt(
                    PREF_KEY_CURRENT_CALL_TYPE, -1
                )
            )
            editor.putInt(
                PREF_KEY_LAST_REMOTE_USER_AGORA_ID, preferenceManager.getInt(
                    PREF_KEY_CURRENT_REMOTE_USER_AGORA_ID, -1
                )
            )
            editor.putLong(
                PREF_KEY_LAST_CALL_START_TIME, preferenceManager.getLong(
                    PREF_KEY_CURRENT_CALL_START_TIME, 0L
                )
            )
            editor.putString(
                PREF_KEY_LAST_CHANNEL_NAME, preferenceManager.getString(
                    PREF_KEY_CURRENT_CHANNEL_NAME, ""
                )
            )

            editor.apply()

            // TODO: These logic shouldn't be here
            if (duration != 0L) {
                showDialogBox(duration)
            }
        }

        // TODO: These function shouldn't be here
        private fun showDialogBox(totalSecond: Long) {
            val currentActivity = ActivityLifecycleCallback.currentActivity
            val voiceCallClassName = "com.joshtalks.joshskills." + currentActivity.localClassName
            val fragmentActivity = currentActivity as? FragmentActivity
            if (currentActivity != null) {
                if (voiceCallClassName != "com.joshtalks.joshskills.ui.voip.new_arch.ui.views.VoiceCallActivity") {
                    fragmentActivity?.showVoipDialog(totalSecond)
                } else {
                    scope.launch {
                        delay(1000)
                        val newCurrentActivity = ActivityLifecycleCallback.currentActivity
                        val newFragmentActivity = newCurrentActivity as? FragmentActivity
                        withContext(Dispatchers.Main) {
                            newFragmentActivity?.showVoipDialog(totalSecond)
                        }
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
                getCurrentUserAgoraId(), "REPORT", getLastCallChannelName(), function
            )
                .show(fragmentActivity.supportFragmentManager, "ReportDialogFragment")
        }

        private fun showFeedBackDialog(fragmentActivity: FragmentActivity) {
            val function = fun() {}
            FeedbackDialogFragment.newInstance(function)
                .show(fragmentActivity.supportFragmentManager, "FeedBackDialogFragment")
        }

        fun updateVoipState(state: Int) {
            scope.launch {
                try {
                    mutex.withLock {
                        Log.d(TAG, "update Webrtc State : $state")
                        val editor = preferenceManager.edit()
                        editor.putInt(PREF_KEY_WEBRTC_CURRENT_STATE, state)
                        editor.commit()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        fun getVoipState(): Int {
            return preferenceManager.getInt(PREF_KEY_WEBRTC_CURRENT_STATE, IDLE)
        }

        fun getVoipUIState(): VoipUIState {
            return VoipUIState(
                isMute = preferenceManager.getBoolean(PREF_KEY_CURRENT_USER_ON_MUTE, false),
                isSpeakerOn = preferenceManager.getBoolean(PREF_KEY_CURRENT_USER_SPEAKER_ON, false),
                isOnHold = preferenceManager.getBoolean(PREF_KEY_CURRENT_USER_ON_HOLD, false),
                isRemoteUserMuted = preferenceManager.getBoolean(
                    PREF_KEY_CURRENT_REMOTE_USER_ON_MUTE, false
                )
            )
        }

        fun getStartTimeStamp(): Long {
            val startTime = preferenceManager.getLong(PREF_KEY_CURRENT_CALL_START_TIME, 0)
            Log.d(TAG, "getStartTimeStamp: $startTime")
            return startTime
        }

        fun getTopicName(): String {
            return preferenceManager.getString(PREF_KEY_CURRENT_TOPIC_NAME, "").toString()
        }

        fun getCallerName(): String {
            return preferenceManager.getString(PREF_KEY_CURRENT_REMOTE_USER_NAME, "").toString()
        }

        fun getLastCallerName(): String {
            return preferenceManager.getString(PREF_KEY_LAST_REMOTE_USER_NAME, "").toString()
        }

        fun getCallType(): Int {
            return preferenceManager.getInt(PREF_KEY_CURRENT_CALL_TYPE, -1)
        }

        fun getIncomingCallId(): Int {
            return preferenceManager.getInt(PREF_KEY_INCOMING_CALL_ID, -1)
        }

        fun getProfileImage(): String {
            return preferenceManager.getString(PREF_KEY_CURRENT_REMOTE_USER_IMAGE, "").toString()
        }

        fun getLastRemoteUserAgoraId(): Int {
            return preferenceManager.getInt(PREF_KEY_LAST_REMOTE_USER_AGORA_ID, -1)
        }

        fun getCurrentUserAgoraId(): Int {
            return preferenceManager.getInt(PREF_KEY_CURRENT_USER_AGORA_ID, -1)
        }

        fun getLastCallId(): Int {
            return preferenceManager.getInt(PREF_KEY_LAST_CALL_ID, -1)
        }

        fun getLastCallChannelName(): String {
            return preferenceManager.getString(PREF_KEY_LAST_CHANNEL_NAME, "").toString()
        }

        fun getLastCallStartTime(): Long {
            return preferenceManager.getLong(PREF_KEY_LAST_CALL_START_TIME, 0L)
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
}