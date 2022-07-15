package com.joshtalks.joshskills.ui.call.data.local

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.fragment.app.FragmentActivity
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.base.constants.*
import com.joshtalks.joshskills.core.ActivityLifecycleCallback
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.base.local.model.Mentor
import com.joshtalks.joshskills.ui.voip.new_arch.ui.call_rating.CallRatingsFragment
import com.joshtalks.joshskills.ui.voip.new_arch.ui.feedback.FeedbackDialogFragment
import com.joshtalks.joshskills.ui.voip.new_arch.ui.report.VoipReportDialogFragment
import com.joshtalks.joshskills.voip.inSeconds
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex

private const val TAG = "VoipPref"

object VoipPref {
        lateinit var preferenceManager: SharedPreferences
        val coroutineExceptionHandler = CoroutineExceptionHandler { _, e ->
            e.printStackTrace()
        }
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
            topicName: String,
            showFpp : String,
            remoteUserMentorId : String
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
            editor.putString(PREF_KEY_FPP_FLAG, showFpp)
            editor.putString(PREF_KEY_LAST_REMOTE_USER_MENTOR_ID, remoteUserMentorId)
            editor.commit()

            // TODO: These logic shouldn't be here

            if (duration != 0L) {
                showDialogBox(duration)
            }
        }

        // TODO: These function shouldn't be here
        private fun showDialogBox(totalSecond: Long) {
            CoroutineScope(Dispatchers.IO + coroutineExceptionHandler).launch {
                        delay(500)
                        val currentActivity = ActivityLifecycleCallback.currentActivity
                        if(currentActivity.isDestroyed || currentActivity.isFinishing){
                            delay(500)
                            val newCurrentActivity = ActivityLifecycleCallback.currentActivity
                            val newFragmentActivity = newCurrentActivity as? FragmentActivity
                            withContext(Dispatchers.Main) {
                                newFragmentActivity?.showVoipDialog(totalSecond)
                            }
                        }
                        else if (currentActivity != null) {
                            val newFragmentActivity = currentActivity as? FragmentActivity
                           withContext(Dispatchers.Main) {
                            newFragmentActivity?.showVoipDialog(totalSecond)
                    }

                        }
            }
        }

        // TODO: These function shouldn't be here
        private fun FragmentActivity.showVoipDialog(totalSecond: Long) {
                showCallRatingDialog(this)
        }

    private fun showCallRatingDialog(fragmentActivity: FragmentActivity) {
        CallRatingsFragment.newInstance(
            getLastRemoteUserName(),
            getLastCallDurationInSec().toInt(),
            getLastCallId(),
            getLastProfileImage(),
            getLastRemoteUserAgoraId().toString(),
            getLocalUserAgoraId().toString()
        ).show(fragmentActivity.supportFragmentManager, "CallRatingsFragment")
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
            FeedbackDialogFragment.newInstance()
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
            return if(duration.inSeconds()>0){
                duration.inSeconds()
            }else{
                1
            }
        }

    fun getLastRemoteUserMentorId(): String {
        return preferenceManager.getString(PREF_KEY_LAST_REMOTE_USER_MENTOR_ID, "").toString()
    }

        fun startListener() {
            if(isListenerActivated.not()) {
                Log.d(TAG, "startListener: ")
                isListenerActivated = true
                preferenceManager.registerOnSharedPreferenceChangeListener(VoipPrefListener)
            }
        }

   private fun getFppFlag(): String {
        return preferenceManager.getString(PREF_KEY_FPP_FLAG, "true").toString()
    }

    fun getCurrentUserName(): String{
        return Mentor.getInstance().getUser()?.firstName?: ""
    }

    fun getCurrentUserImage(): String{
        return Mentor.getInstance().getUser()?.photo?: ""
    }
}