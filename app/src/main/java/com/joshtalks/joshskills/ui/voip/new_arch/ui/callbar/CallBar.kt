package com.joshtalks.joshskills.ui.voip.new_arch.ui.callbar

import android.content.Context
import android.content.SharedPreferences
import android.os.SystemClock
import android.util.Log
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.preference.PreferenceManager
import com.joshtalks.joshskills.base.constants.*
import com.joshtalks.joshskills.base.model.VoipUIState
import com.joshtalks.joshskills.voip.constant.IDLE
import com.joshtalks.joshskills.core.ActivityLifecycleCallback
import com.joshtalks.joshskills.core.IS_COURSE_BOUGHT
import com.joshtalks.joshskills.core.PrefManager
import com.joshtalks.joshskills.ui.voip.new_arch.ui.feedback.FeedbackDialogFragment
import com.joshtalks.joshskills.ui.voip.new_arch.ui.report.ReportDialogFragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

/**
 * Require DataBinding in targeted xml with following instruction ->
 * 1. import @View
 * 2. add variable @callBar of type CallBar class
 * 3. add @CallBarLayout in xml with required attributes
 * 4. use binding adapters setters @onCallBarClick (callBar::intentToCallActivity)
 * 5. toggle visibility using @isCallONGoing ObservableBoolean
 *  Required to set Variable @callBar in Activity as "binding.callBar= CallBar(this)"
 */

private const val TAG = "CallBar"

class VoipPref {

    companion object {
        lateinit var preferenceManager: SharedPreferences

        @Synchronized
        fun initVoipPref(context: Context) {
            preferenceManager = PreferenceManager.getDefaultSharedPreferences(context)
        }

        fun updateCallDetails(
            timestamp: Long,
            remoteUserName: String = "",
            remoteUserImage: String? = null,
            callId: Int = -1,
            callType: Int = -1,
            remoteUserAgoraId: Int = -1,
            currentUserAgoraId:Int = -1,
            channelName : String = "",
            topicName : String = ""
        ) {
            val editor = preferenceManager.edit()
            editor.putString(PREF_KEY_CURRENT_REMOTE_USER_NAME, remoteUserName)
            editor.putString(PREF_KEY_CURRENT_REMOTE_USER_IMAGE, remoteUserImage)
            editor.putInt(PREF_KEY_CURRENT_CALL_ID, callId)
            editor.putInt(PREF_KEY_CURRENT_CALL_TYPE, callType)
            editor.putInt(PREF_KEY_CURRENT_REMOTE_USER_AGORA_ID, remoteUserAgoraId)
            editor.putLong(PREF_KEY_CURRENT_CALL_START_TIME, timestamp)
            editor.putString(PREF_KEY_CURRENT_CHANNEL_NAME, channelName)
            editor.putInt(PREF_KEY_CURRENT_USER_AGORA_ID, currentUserAgoraId)
            editor.putString(PREF_KEY_CURRENT_TOPIC_NAME, topicName)
            Log.d(TAG, "updateCallDetails: timestamp --> $timestamp")
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

        fun updateLastCallDetails() {
            val editor = preferenceManager.edit()
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

            if(preferenceManager.getLong(PREF_KEY_CURRENT_CALL_START_TIME, 0L).toInt() != 0){
                showDialogBox()
            }
        }

        private fun showDialogBox() {
            val currentActivity = ActivityLifecycleCallback.currentActivity
            val voiceCallClassName= "com.joshtalks.joshskills."+currentActivity.localClassName
            val fragmentActivity = currentActivity as FragmentActivity
            if(currentActivity!=null) {
                if (voiceCallClassName != "com.joshtalks.joshskills.ui.voip.new_arch.ui.views.VoiceCallActivity") {
                    getDuration(fragmentActivity)
                } else {
                    CoroutineScope(Dispatchers.IO).launch {
                        delay(1000)
                        val newCurrentActivity = ActivityLifecycleCallback.currentActivity
                        val newFragmentActivity = newCurrentActivity as FragmentActivity
                        getDuration(newFragmentActivity)
                    }
                }
            }
        }

        fun getDuration(fragmentActivity: FragmentActivity) {
            val startTime =   getLastCallStartTime()
            val currentTime = SystemClock.elapsedRealtime()
            val totalSecond = TimeUnit.MILLISECONDS.toSeconds(currentTime - startTime).toInt()
            val editor=preferenceManager.edit()
            editor.putInt(PREF_KEY_LAST_CALL_DURATION,totalSecond)
            editor.apply()

            if(totalSecond < 120 && PrefManager.getBoolValue(IS_COURSE_BOUGHT) ){
                showReportDialog(fragmentActivity)
            }else{
                showFeedBackDialog(fragmentActivity)
            }
        }
        private fun showReportDialog(fragmentActivity: FragmentActivity) {

            val function = fun(){
                showFeedBackDialog(fragmentActivity)
            }
            ReportDialogFragment.newInstance(getLastRemoteUserAgoraId(),
                getCurrentUserAgoraId(),"REPORT", getLastCallChannelName(),function)
                .show(fragmentActivity.supportFragmentManager, "ReportDialogFragment")
        }

        private fun showFeedBackDialog(fragmentActivity: FragmentActivity) {
            val function = fun(){}
            FeedbackDialogFragment.newInstance(function)
                .show(fragmentActivity.supportFragmentManager, "FeedBackDialogFragment")
        }
        fun updateVoipState(state: Int) {
            Log.d(TAG, "update Webrtc State : $state")
            val editor = preferenceManager.edit()
            editor.putInt(PREF_KEY_WEBRTC_CURRENT_STATE, state)
            editor.apply()
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
             return preferenceManager.getString(PREF_KEY_CURRENT_TOPIC_NAME,"").toString()
        }
        fun getCallerName(): String {
            return preferenceManager.getString(PREF_KEY_CURRENT_REMOTE_USER_NAME,"").toString()
        }
        fun getLastCallerName(): String {
            return preferenceManager.getString(PREF_KEY_LAST_REMOTE_USER_NAME,"").toString()
        }
        fun getCallType(): Int {
            return preferenceManager.getInt(PREF_KEY_CURRENT_CALL_TYPE,-1)
        }
        fun getIncomingCallId(): Int {
            return preferenceManager.getInt(PREF_KEY_INCOMING_CALL_ID,-1)
        }
        fun getProfileImage(): String {
            return preferenceManager.getString(PREF_KEY_CURRENT_REMOTE_USER_IMAGE,"").toString()
        }
        fun getLastRemoteUserAgoraId():Int{
            return preferenceManager.getInt(PREF_KEY_LAST_REMOTE_USER_AGORA_ID,-1)
        }
        fun getCurrentUserAgoraId():Int{
            return preferenceManager.getInt(PREF_KEY_CURRENT_USER_AGORA_ID,-1)
        }
        fun getLastCallId():Int{
           return preferenceManager.getInt(PREF_KEY_LAST_CALL_ID,-1)
        }
        fun getLastCallChannelName():String{
            return preferenceManager.getString(PREF_KEY_LAST_CHANNEL_NAME,"").toString()
        }
        fun getLastCallStartTime():Long{
            return preferenceManager.getLong(PREF_KEY_LAST_CALL_START_TIME,0L)
        }
        fun getLastCallDurationInSec():Int{
            return preferenceManager.getInt(PREF_KEY_LAST_CALL_DURATION,0)
        }
        fun setListener(callTimeStampListener: CallTimeStampListener) {
            preferenceManager.registerOnSharedPreferenceChangeListener(callTimeStampListener)
        }
    }
}

class CallBar {
    val prefListener by lazy { CallTimeStampListener }

    init {
        VoipPref.setListener(prefListener)
    }

    fun getTimerLiveData(): LiveData<Long> {
        return prefListener.observerStartTime()
    }

    fun observerVoipState(): LiveData<Int> {
        return prefListener.observerVoipState()
    }

    fun observerVoipUIState(): StateFlow<VoipUIState> {
        return prefListener.observerVoipUIState()
    }
}

object CallTimeStampListener : SharedPreferences.OnSharedPreferenceChangeListener {
    private val UI_STATE_UPDATED = setOf(
        PREF_KEY_CURRENT_USER_ON_HOLD,
        PREF_KEY_CURRENT_USER_ON_MUTE,
        PREF_KEY_CURRENT_USER_SPEAKER_ON,
        PREF_KEY_CURRENT_REMOTE_USER_ON_MUTE
    )

    private val timerLiveData by lazy {
        MutableLiveData(checkTimestamp())
    }

    private val voipStateLiveData by lazy {
        MutableLiveData(checkVoipState())
    }

    private val voipUIStateLiveData by lazy {
        MutableStateFlow(checkUIState())
    }

    fun observerVoipState(): LiveData<Int> {
        return voipStateLiveData
    }

    fun observerVoipUIState(): StateFlow<VoipUIState> {
        return voipUIStateLiveData
    }

    fun observerStartTime(): LiveData<Long> {
        return timerLiveData
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        Log.d(TAG, "onSharedPreferenceChanged: $key")
        when (key) {
            PREF_KEY_CURRENT_CALL_START_TIME -> timerLiveData.value = checkTimestamp()
            PREF_KEY_WEBRTC_CURRENT_STATE -> voipStateLiveData.value = checkVoipState()
            in UI_STATE_UPDATED -> {
                Log.d(TAG, "onSharedPreferenceChanged: $key")
                voipUIStateLiveData.value = checkUIState()
            }
        }
    }

    private fun checkTimestamp(): Long {
        return VoipPref.getStartTimeStamp()
    }

    private fun checkVoipState(): Int {
        val state = VoipPref.getVoipState()
        Log.d(TAG, "checkVoipState: $state")
        return state
    }

    private fun checkUIState(): VoipUIState {
        val state = VoipPref.getVoipUIState()
        Log.d(TAG, "checkUIState: $state")
        return state
    }

}