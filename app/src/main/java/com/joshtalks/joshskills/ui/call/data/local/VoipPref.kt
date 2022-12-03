package com.joshtalks.joshskills.ui.call.data.local

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.util.Log
import androidx.fragment.app.FragmentActivity
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.base.constants.*
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.core.FirebaseRemoteConfigKey.Companion.AUTO_CALL
import com.joshtalks.joshskills.core.FirebaseRemoteConfigKey.Companion.AUTO_CONNECT_MAX_RETRY_PER_CALL
import com.joshtalks.joshskills.core.FirebaseRemoteConfigKey.Companion.AUTO_CONNECT_PRIMARY_CONDITION
import com.joshtalks.joshskills.core.FirebaseRemoteConfigKey.Companion.AUTO_CONNECT_STATUS
import com.joshtalks.joshskills.core.FirebaseRemoteConfigKey.Companion.CALL_RATING
import com.joshtalks.joshskills.core.FirebaseRemoteConfigKey.Companion.PURCHASE_POPUP
import com.joshtalks.joshskills.core.analytics.MarketingAnalytics
import com.joshtalks.joshskills.core.notification.NotificationCategory
import com.joshtalks.joshskills.core.notification.NotificationUtils
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.repository.server.PurchasePopupType
import com.joshtalks.joshskills.ui.callWithExpert.CallWithExpertActivity
import com.joshtalks.joshskills.ui.callWithExpert.repository.db.SkillsDatastore
import com.joshtalks.joshskills.ui.lesson.PurchaseDialog
import com.joshtalks.joshskills.ui.voip.new_arch.ui.call_rating.CallRatingsFragment
import com.joshtalks.joshskills.ui.voip.new_arch.ui.feedback.FeedbackDialogFragment
import com.joshtalks.joshskills.ui.voip.new_arch.ui.report.VoipReportDialogFragment
import com.joshtalks.joshskills.ui.voip.new_arch.ui.views.AutoCallActivity
import com.joshtalks.joshskills.ui.voip.new_arch.ui.views.UserInterestActivity
import com.joshtalks.joshskills.voip.BeepTimer
import com.joshtalks.joshskills.voip.constant.Category
import com.joshtalks.joshskills.voip.data.local.AGORA_CALL_ID
import com.joshtalks.joshskills.voip.data.local.LOCAL_USER_AGORA_ID
import com.joshtalks.joshskills.voip.inSeconds
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private const val TAG = "VoipPref"
private const val AUTO_CONNECT_DISABLED = 0L
private const val AUTO_CONNECT_ONCE = 1L
private const val AUTO_CONNECT_PER_CALL = 2L

object VoipPref {
    lateinit var preferenceManager: SharedPreferences

    val coroutineExceptionHandler = CoroutineExceptionHandler { _, e ->
        e.printStackTrace()
    }
    val mutex = Mutex(false)
    var isListenerActivated = false

    val expertDurationMutex = Mutex(false)

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

    fun updateCurrentCallStartTime(startTime: Long) {
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
        showFpp: String,
        remoteUserMentorId: String,
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
        if (duration.inSeconds() >= AppObjectController.getFirebaseRemoteConfig().getLong(AUTO_CONNECT_PRIMARY_CONDITION))
            resetAutoCallCount()
        showPopUp(duration, callType)
        if (preferenceManager.getBoolean(IS_FIRST_5MIN_CALL, true) &&
            duration.inSeconds() >= 300 && PrefManager.getBoolValue(IS_FREE_TRIAL)
        ) {
            editor.putBoolean(IS_FIRST_CALL, false)
            editor.putBoolean(IS_FIRST_5MIN_CALL, false)
            NotificationUtils(AppObjectController.joshApplication).removeScheduledNotification(
                NotificationCategory.AFTER_LOGIN
            )
            NotificationUtils(AppObjectController.joshApplication).removeScheduledNotification(
                NotificationCategory.AFTER_FIRST_CALL
            )
            NotificationUtils(AppObjectController.joshApplication).updateNotificationDb(
                NotificationCategory.AFTER_FIVE_MIN_CALL
            )
            MarketingAnalytics.callComplete5MinForFirstTime()
        } else if (duration != 0L && preferenceManager.getBoolean(IS_FIRST_CALL, true)) {
            editor.putBoolean(IS_FIRST_CALL, false)
            NotificationUtils(AppObjectController.joshApplication).removeScheduledNotification(
                NotificationCategory.AFTER_LOGIN
            )
            NotificationUtils(AppObjectController.joshApplication).updateNotificationDb(
                NotificationCategory.AFTER_FIRST_CALL
            )
        }

        if (duration.inSeconds() >= 300) {
            MarketingAnalytics.callComplete5Min()
        }

        if (duration.inSeconds() >= 1200) {
            MarketingAnalytics.callComplete20Min()
            if (PrefManager.getBoolValue(IS_FREE_TRIAL)) {
                MarketingAnalytics.callComplete20MinForFreeTrial()
            }
        }

        if (duration.inSeconds() >= 600) {
            if (PrefManager.getBoolValue(IS_FREE_TRIAL)) {
                MarketingAnalytics.callComplete10MinForFreeTrial()
            }
        }

        // TODO: These logic shouldn't be here
//        ExpertListRepo().deductAmountAfterCall(getLastCallDurationInSec().toString(), remoteUserMentorId, callType)
        deductAmountAfterCall(getLastCallDurationInSec().toString(), remoteUserMentorId, callType)
    }

    fun showPopUp(duration: Long, callType: Int) {
        CoroutineScope(Dispatchers.IO + coroutineExceptionHandler).launch {
            when (getPopUpType(duration)) {
                POPUP.DEFAULT -> showDefaultDialog(callType, duration)
                POPUP.INTEREST -> startLevelAndInterestForm()
                POPUP.AUTO_CONNECT -> showDialogBox(duration.inSeconds(), AUTO_CALL)
            }
        }
    }

    private fun showDefaultDialog(callType: Int, duration: Long) {
        if (PrefManager.getBoolValue(IS_FREE_TRIAL) && callType != Category.EXPERT.ordinal) {
            showDialogBox(duration, PURCHASE_POPUP)
        } else if (callType != Category.EXPERT.ordinal) {
            showDialogBox(duration, CALL_RATING)
        }
    }

    private suspend fun getPopUpType(duration: Long): POPUP {
        return try {
            if(shouldAutoConnect(duration))
                return POPUP.AUTO_CONNECT

            val resp = AppObjectController.p2pNetworkService.getFormSubmitStatus()
            if (duration == 0L || resp.isSuccessful.not() || isLevelAndInterestEnabled().not())
                POPUP.DEFAULT
            else {
                val isInterestFormEnabled = resp.body()?.get("show_screen") ?: 0
                if (isInterestFormEnabled == 1)
                    POPUP.INTEREST
                else
                    POPUP.DEFAULT
            }
        } catch (e: Exception) {
            e.printStackTrace()
            POPUP.DEFAULT
        }
    }

    private fun isLevelAndInterestEnabled(): Boolean {
        return PrefManager.getIntValue(IS_LEVEL_DETAILS_ENABLED) == 1 && PrefManager.getIntValue(
            IS_INTEREST_FORM_ENABLED
        ) == 1
    }

    private fun shouldAutoConnect(duration: Long): Boolean {
        return when(AppObjectController.getFirebaseRemoteConfig().getLong(AUTO_CONNECT_STATUS)) {
            AUTO_CONNECT_DISABLED -> false
            AUTO_CONNECT_ONCE, AUTO_CONNECT_PER_CALL -> {
                PrefManager.getBoolValue(IS_FREE_TRIAL) && duration > 0L &&
                        duration.inSeconds() < AppObjectController.getFirebaseRemoteConfig().getLong(
                    AUTO_CONNECT_PRIMARY_CONDITION) &&
                        preferenceManager.getInt(AUTO_CONNECT_CURRENT_TRY_COUNT, 0) < AppObjectController.getFirebaseRemoteConfig().getLong(AUTO_CONNECT_MAX_RETRY_PER_CALL) &&
                        com.joshtalks.joshskills.voip.data.local.PrefManager.getLastDisconnectScreenName() == "VoiceCallActivity"
            }
            else -> false
        }
    }


    private fun startLevelAndInterestForm() {
        val intent = Intent(ActivityLifecycleCallback.currentActivity, UserInterestActivity::class.java)
        intent.putExtra("isEditCall", false)
        ActivityLifecycleCallback.currentActivity.startActivity(intent)
    }

    private fun openExpertUpgradeScreen() {
        val intent = Intent(ActivityLifecycleCallback.currentActivity, CallWithExpertActivity::class.java)
        intent.putExtra("open_upgrade_page", true)
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        ActivityLifecycleCallback.currentActivity.startActivity(intent)
    }

    private fun deductAmountAfterCall(duration: String, remoteUserMentorId: String, callType: Int) {
        BeepTimer.stopBeepSound()
        if (callType == Category.EXPERT.ordinal) {
            setExpertCallDuration(duration)

            CoroutineScope(Dispatchers.IO + coroutineExceptionHandler).launch {
                expertDurationMutex.withLock {
                    try {
                        val map = HashMap<String, String>()
                        map["time_spoken_in_seconds"] = duration
                        map["connected_user_id"] = remoteUserMentorId
                        map["agora_call_id"] = getLastCallId().toString()
                        val response = AppObjectController.commonNetworkService.deductAmountAfterCall(map)
                        when (response.code()) {
                            200 -> {
                                setExpertCallDuration("")
                                SkillsDatastore.updateWalletAmount(response.body()?.amount ?: 0)
                                SkillsDatastore.updateExpertCredits(response.body()?.credits ?: -1)
                                if (response.body()?.credits == -1) {
                                    openExpertUpgradeScreen()
                                }
                            }
                            406 -> { }
                        }
                    } catch (ex: Exception) {
                    }
                }
            }
        }
    }

    fun resetAutoCallCount() {
        if (AppObjectController.getFirebaseRemoteConfig().getLong(AUTO_CONNECT_STATUS) == AUTO_CONNECT_PER_CALL) {
            val editor = preferenceManager.edit()
            editor.putInt(AUTO_CONNECT_CURRENT_TRY_COUNT, 0)
            editor.commit()
        }
    }

    // TODO: These function shouldn't be here
    private fun showDialogBox(totalSecond: Long, type: String) {
        CoroutineScope(Dispatchers.IO + coroutineExceptionHandler).launch {
            delay(500)
            val currentActivity = ActivityLifecycleCallback.currentActivity
            if (currentActivity == null || currentActivity.isDestroyed || currentActivity.isFinishing) {
                delay(500)
                val newCurrentActivity = ActivityLifecycleCallback.currentActivity
                val newFragmentActivity = newCurrentActivity as? FragmentActivity
                withContext(Dispatchers.Main) {
                    when (type) {
                        CALL_RATING -> newFragmentActivity?.showVoipDialog(totalSecond, CALL_RATING)
                        AUTO_CALL -> {
                            val count = preferenceManager.getInt(AUTO_CONNECT_CURRENT_TRY_COUNT, 0) + 1
                            val editor = preferenceManager.edit()
                            editor.putInt(AUTO_CONNECT_CURRENT_TRY_COUNT, count)
                            editor.commit()
                            newFragmentActivity?.startActivity(
                                Intent(
                                    newFragmentActivity,
                                    AutoCallActivity::class.java
                                )
                            )
                        }
                        else -> newFragmentActivity?.showVoipDialog(totalSecond, PURCHASE_POPUP)
                    }
                }
            } else {
                val newFragmentActivity = currentActivity as? FragmentActivity
                withContext(Dispatchers.Main) {
                    when (type) {
                        CALL_RATING -> newFragmentActivity?.showVoipDialog(totalSecond, CALL_RATING)
                        AUTO_CALL -> {
                            val count = preferenceManager.getInt(AUTO_CONNECT_CURRENT_TRY_COUNT, 0) + 1
                            val editor = preferenceManager.edit()
                            editor.putInt(AUTO_CONNECT_CURRENT_TRY_COUNT, count)
                            editor.commit()
                            newFragmentActivity?.startActivity(
                                Intent(
                                    newFragmentActivity,
                                    AutoCallActivity::class.java
                                )
                            )
                        }
                        else -> newFragmentActivity?.showVoipDialog(totalSecond, PURCHASE_POPUP)
                    }
                }
            }
        }
    }

    // TODO: These function shouldn't be here
    private fun FragmentActivity.showVoipDialog(totalSecond: Long, type: String) {
        if (type == CALL_RATING) {
            showCallRatingDialog(this)
        } else {
            showPurchaseDialog(this, totalSecond)
        }
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

    private fun showPurchaseDialog(fragmentActivity: FragmentActivity, duration: Long) {
        CoroutineScope(Dispatchers.IO + coroutineExceptionHandler).launch {
            try {
                val resp =
                    AppObjectController.commonNetworkService.getCoursePopUpData(
                        courseId = PrefManager.getStringValue(CURRENT_COURSE_ID)
                            .ifEmpty { DEFAULT_COURSE_ID },
                        popupName = PurchasePopupType.SPEAKING_COMPLETED.name,
                        callCount = PrefManager.getIntValue(FT_CALLS_LEFT),
                        callDuration = duration
                    )
                resp.body()?.let {
                    if (it.couponCode != null && it.couponExpiryTime != null)
                        PrefManager.put(COUPON_EXPIRY_TIME, it.couponExpiryTime.time)
                    PurchaseDialog.newInstance(it)
                        .show(fragmentActivity.supportFragmentManager, "PurchaseDialog")
                }
            } catch (ex: Exception) {
                Log.d("sagar", "showPurchaseDialog: ${ex.message}")
            }
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
        FeedbackDialogFragment.newInstance()
            .show(fragmentActivity.supportFragmentManager, "FeedBackDialogFragment")
    }

    fun getStartTimeStamp(): Long {
        val startTime = preferenceManager.getLong(PREF_KEY_CURRENT_CALL_START_TIME, 0)
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
        return if (duration.inSeconds() > 0) {
            duration.inSeconds()
        } else {
            1
        }
    }

    fun getLastRemoteUserMentorId(): String {
        return preferenceManager.getString(PREF_KEY_LAST_REMOTE_USER_MENTOR_ID, "").toString()
    }

    fun startListener() {
        if (isListenerActivated.not()) {
            Log.d(TAG, "startListener: ")
            isListenerActivated = true
            preferenceManager.registerOnSharedPreferenceChangeListener(VoipPrefListener)
        }
    }

    private fun getFppFlag(): String {
        return preferenceManager.getString(PREF_KEY_FPP_FLAG, "true").toString()
    }

    fun getCurrentUserName(): String {
        return Mentor.getInstance().getUser()?.firstName ?: ""
    }

    fun getCurrentUserImage(): String {
        return Mentor.getInstance().getUser()?.photo ?: ""
    }

    fun setLocalUserAgoraIdAndCallId(localUserAgoraId: Int, callId: Int) {
        val editor = preferenceManager.edit()
        editor.putInt(LOCAL_USER_AGORA_ID, localUserAgoraId)
        editor.putInt(AGORA_CALL_ID, callId)
        editor.commit()
    }

    fun setExpertCallDuration(duration: String) {
        com.joshtalks.joshskills.voip.data.local.PrefManager.setExpertCallDuration(duration)
    }

    fun getExpertCallDuration(): String? {
        return com.joshtalks.joshskills.voip.data.local.PrefManager.getExpertCallDuration()
    }
}

enum class POPUP {
    DEFAULT,
    INTEREST,
    AUTO_CONNECT,
}