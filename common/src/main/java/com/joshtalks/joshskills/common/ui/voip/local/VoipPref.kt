package com.joshtalks.joshskills.common.ui.voip.local

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.util.Log
import androidx.fragment.app.FragmentActivity
import com.joshtalks.joshskills.common.R
import com.joshtalks.joshskills.common.core.*
import com.joshtalks.joshskills.common.core.FirebaseRemoteConfigKey.Companion.AUTO_CALL
import com.joshtalks.joshskills.common.core.FirebaseRemoteConfigKey.Companion.AUTO_CONNECT_MAX_RETRY_PER_CALL
import com.joshtalks.joshskills.common.core.FirebaseRemoteConfigKey.Companion.AUTO_CONNECT_PRIMARY_CONDITION
import com.joshtalks.joshskills.common.core.FirebaseRemoteConfigKey.Companion.AUTO_CONNECT_STATUS
import com.joshtalks.joshskills.common.core.FirebaseRemoteConfigKey.Companion.CALL_RATING
import com.joshtalks.joshskills.common.core.FirebaseRemoteConfigKey.Companion.PURCHASE_POPUP
import com.joshtalks.joshskills.common.core.FirebaseRemoteConfigKey.Companion.SCRATCH_POPUP
import com.joshtalks.joshskills.common.core.analytics.MarketingAnalytics
import com.joshtalks.joshskills.common.core.notification.NotificationCategory
import com.joshtalks.joshskills.common.core.notification.client_side.ClientNotificationUtils
import com.joshtalks.joshskills.common.repository.local.SkillsDatastore
import com.joshtalks.joshskills.common.repository.local.model.Mentor
import com.joshtalks.joshskills.common.repository.server.PurchasePopupType
//import com.joshtalks.joshskills.lesson.PurchaseDialog
import com.joshtalks.joshskills.common.ui.voip.new_arch.ui.call_rating.CallRatingsFragment
import com.joshtalks.joshskills.common.ui.voip.new_arch.ui.feedback.FeedbackDialogFragment
import com.joshtalks.joshskills.common.ui.voip.new_arch.ui.views.AutoCallActivity
import com.joshtalks.joshskills.common.ui.voip.new_arch.ui.views.UserInterestActivity
import com.joshtalks.joshskills.voip.BeepTimer
import com.joshtalks.joshskills.voip.base.constants.AUTO_CONNECT_CURRENT_TRY_COUNT
import com.joshtalks.joshskills.voip.base.constants.*
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
            ClientNotificationUtils(AppObjectController.joshApplication).removeScheduledNotification(
                NotificationCategory.AFTER_LOGIN
            )
            ClientNotificationUtils(AppObjectController.joshApplication).removeScheduledNotification(
                NotificationCategory.AFTER_FIRST_CALL
            )
            ClientNotificationUtils(AppObjectController.joshApplication).updateNotificationDb(
                NotificationCategory.AFTER_FIVE_MIN_CALL
            )
            MarketingAnalytics.callComplete5MinForFirstTime()
        } else if (duration != 0L && preferenceManager.getBoolean(IS_FIRST_CALL, true)) {
            editor.putBoolean(IS_FIRST_CALL, false)
            ClientNotificationUtils(AppObjectController.joshApplication).removeScheduledNotification(
                NotificationCategory.AFTER_LOGIN
            )
            ClientNotificationUtils(AppObjectController.joshApplication).updateNotificationDb(
                NotificationCategory.AFTER_FIRST_CALL
            )
        }
        editor.commit()

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
        deductAmountAfterCall(getLastCallDurationInSec().toString(), remoteUserMentorId, callType)
    }

    private fun showPopUp(duration: Long, callType: Int) {
        CoroutineScope(Dispatchers.IO + coroutineExceptionHandler).launch {
            when (getPopUpType(duration, callType)) {
                POPUP.AUTO_CONNECT -> showDialogBox(duration.inSeconds(), AUTO_CALL)
                POPUP.INTEREST -> startLevelAndInterestForm()
                POPUP.PURCHASE -> showDialogBox(duration, PURCHASE_POPUP)
                POPUP.CALL_RATING -> showDialogBox(duration, CALL_RATING)
                POPUP.SCRATCH_CARD -> showDialogBox(duration, SCRATCH_POPUP)
                POPUP.ERROR -> {}
            }
        }
    }

    private suspend fun getPopUpType(duration: Long, callType: Int): POPUP {
        try {
            if (shouldAutoConnect(duration))
                return POPUP.AUTO_CONNECT

            val resp = AppObjectController.commonNetworkService.getPopupType()
            return if ((resp.body()?.get("show_screen") == true))
                POPUP.INTEREST
            else if ((resp.body()?.get("show_scratch_card") == true) && callType == Category.PEER_TO_PEER.ordinal)
                POPUP.SCRATCH_CARD
            else if ((resp.body()?.get("show_payment_popup") == true) && callType != Category.EXPERT.ordinal)
                POPUP.PURCHASE
            else if (callType != Category.EXPERT.ordinal)
                POPUP.CALL_RATING
            else
                POPUP.ERROR
        } catch (e: Exception) {
            e.printStackTrace()
            return POPUP.ERROR
        }
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
        //TODO: Replace AppObjectController -- Sukesh
        AppObjectController.navigator.with(ActivityLifecycleCallback.currentActivity).navigate(
            object : ExpertCallContract {
                override val navigator = AppObjectController.navigator
                override val openUpgradePage = true
                override val flags = arrayOf(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
        )
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
                        CALL_RATING -> newFragmentActivity?.let { showCallRatingDialog(it) }
                        SCRATCH_POPUP -> newFragmentActivity?.let { showScratchCard(it, totalSecond) }
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
                        else -> newFragmentActivity?.let { showPurchaseDialog(it, totalSecond) }
                    }
                }
            } else {
                val newFragmentActivity = currentActivity as? FragmentActivity
                withContext(Dispatchers.Main) {
                    when (type) {
                        CALL_RATING -> newFragmentActivity?.let { showCallRatingDialog(it) }
                        SCRATCH_POPUP -> newFragmentActivity?.let { showScratchCard(it, totalSecond) }
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
                        else -> newFragmentActivity?.let { showPurchaseDialog(it, totalSecond) }
                    }
                }
            }
        }
    }

    // TODO: These function shouldn't be here
    fun showScratchCard(fragmentActivity: FragmentActivity, duration: Long) {
        CoroutineScope(Dispatchers.IO + coroutineExceptionHandler).launch {
            try {
                val resp = AppObjectController.commonNetworkService.getCoursePopUpData(
                    courseId = PrefManager.getStringValue(CURRENT_COURSE_ID)
                        .ifEmpty { DEFAULT_COURSE_ID },
                    popupName = PurchasePopupType.SCRATCH_CARD.name,
                    callCount = PrefManager.getIntValue(FT_CALLS_LEFT),
                    callDuration = duration
                )
                resp.body().let {
                    ScratchCardDialog.newInstance(it).show(fragmentActivity.supportFragmentManager, "ScratchCardDialog")
                }
            } catch (ex: Exception) {
                Log.d(TAG, "showScratchDialog: ${ex.message}")
            }
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
                val resp = AppObjectController.commonNetworkService.getCoursePopUpData(
                    courseId = PrefManager.getStringValue(CURRENT_COURSE_ID)
                        .ifEmpty { DEFAULT_COURSE_ID },
                    popupName = PurchasePopupType.SPEAKING_COMPLETED.name,
                    callCount = PrefManager.getIntValue(FT_CALLS_LEFT),
                    callDuration = duration
                )
                //TODO Create a navigation to open PurchaseDialog
//                resp.body()?.let {
//                    if (it.couponCode != null && it.couponExpiryTime != null)
//                        PrefManager.put(COUPON_EXPIRY_TIME, it.couponExpiryTime.time)
//                    com.joshtalks.joshskills.lesson.PurchaseDialog.newInstance(it)
//                        .show(fragmentActivity.supportFragmentManager, "PurchaseDialog")
//                }
            } catch (ex: Exception) {
                Log.d(TAG, "showPurchaseDialog: ${ex.message}")
            }
        }
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
    INTEREST,
    AUTO_CONNECT,
    PURCHASE,
    CALL_RATING,
    SCRATCH_CARD,
    ERROR
}