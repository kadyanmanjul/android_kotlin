package com.joshtalks.joshskills.ui.voip.new_arch.ui.callbar

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.preference.PreferenceManager
import com.joshtalks.joshskills.base.constants.*
import com.joshtalks.joshskills.core.ActivityLifecycleCallback
import com.joshtalks.joshskills.core.IS_COURSE_BOUGHT
import com.joshtalks.joshskills.core.PrefManager
import com.joshtalks.joshskills.ui.voip.new_arch.ui.feedback.FeedbackDialogFragment
import com.joshtalks.joshskills.ui.voip.new_arch.ui.report.ReportDialogFragment
import com.joshtalks.joshskills.voip.constant.IDLE

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
            getDuration()
        }

        private fun getDuration() {
            val callTime = getLastCallStartTime()
            val second: Int = (callTime / 1000 % 60).toInt()
            val minute = (callTime / (1000 * 60) % 60).toInt()
            val totalSecond:Int=((minute*60)+second)

            val currentActivity = ActivityLifecycleCallback.currentActivity
            val fragmentActivity = currentActivity as FragmentActivity

            if(totalSecond < 120 && PrefManager.getBoolValue(IS_COURSE_BOUGHT) ){
                 showReportDialog(fragmentActivity)
            }else{
                showFeedBackDialog(fragmentActivity)
            }
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
        fun setListener(callTimeStampListener: CallTimeStampListener) {
            preferenceManager.registerOnSharedPreferenceChangeListener(callTimeStampListener)
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
}

object CallTimeStampListener : SharedPreferences.OnSharedPreferenceChangeListener {
    private val timerLiveData by lazy {
        MutableLiveData(checkTimestamp())
    }

    private val voipStateLiveData by lazy {
        MutableLiveData(checkVoipState())
    }

    fun observerVoipState(): LiveData<Int> {
        return voipStateLiveData
    }

    fun observerStartTime(): LiveData<Long> {
        return timerLiveData
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        Log.d(TAG, "onSharedPreferenceChanged: $key")
        when (key) {
            PREF_KEY_CURRENT_CALL_START_TIME -> timerLiveData.value = checkTimestamp()
            PREF_KEY_WEBRTC_CURRENT_STATE -> voipStateLiveData.postValue(checkVoipState())
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

}