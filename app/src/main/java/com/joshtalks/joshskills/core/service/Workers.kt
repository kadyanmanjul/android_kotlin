package com.joshtalks.joshskills.core.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import android.text.format.DateUtils
import android.util.Log
import android.widget.RemoteViews
import androidx.concurrent.futures.CallbackToFutureAdapter
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.identifier.AdvertisingIdClient
import com.google.android.gms.tasks.OnCompleteListener
import com.google.common.util.concurrent.ListenableFuture
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.installations.FirebaseInstallations
import com.google.firebase.messaging.FirebaseMessaging
import com.joshtalks.joshskills.BuildConfig
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.core.firestore.NotificationAnalytics
import com.joshtalks.joshskills.core.notification.FCM_ACTIVE
import com.joshtalks.joshskills.core.notification.FCM_TOKEN
import com.joshtalks.joshskills.core.notification.NotificationUtils
import com.joshtalks.joshskills.engage_notification.AppUsageModel
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.eventbus.DBInsertion
import com.joshtalks.joshskills.repository.server.UpdateDeviceRequest
import com.joshtalks.joshskills.track.CourseUsageSync
import com.joshtalks.joshskills.core.analytics.*
import com.joshtalks.joshskills.core.notification.HAS_NOTIFICATION
import com.joshtalks.joshskills.repository.local.model.*
import com.joshtalks.joshskills.ui.inbox.InboxActivity
import com.joshtalks.joshskills.ui.payment.new_buy_page_layout.BuyPageActivity
import com.joshtalks.joshskills.ui.payment.order_summary.PaymentSummaryActivity
import com.joshtalks.joshskills.ui.special_practice.utils.COUPON_CODE
import com.joshtalks.joshskills.ui.special_practice.utils.FLOW_FROM
import com.joshtalks.joshskills.util.ReminderUtil
import com.yariksoffice.lingver.Lingver
import io.branch.referral.Branch
import kotlinx.coroutines.*
import org.json.JSONObject
import java.util.Date
import java.util.concurrent.TimeUnit
import kotlin.streams.toList
import kotlin.system.exitProcess
import retrofit2.HttpException
import timber.log.Timber

const val INSTALL_REFERRER_SYNC = "install_referrer_sync"
const val CONVERSATION_ID = "conversation_id"
const val IS_ACTIVE = "is_active"
const val NOTIFICATION_TEXT = "notification_text"
const val NOTIFICATION_TITLE = "notification_title"
const val LANGUAGE_CODE = "language_code"
val NOTIFICATION_DELAY = arrayOf(3, 30, 60)
val NOTIFICATION_TEXT_TEXT = arrayOf(
    "Chalo speaking practice try karte hai",
    "Try speaking practice ",
    "Isko abhi complete kare"
)
val NOTIFICATION_TITLE_TEXT = arrayOf(
    "%name, %num students are online",
    "Meet people from across the country.",
    "Apka aaj ka goal hai Lesson 1 complete karna"
)

class UniqueIdGenerationWorker(var context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result {
        try {
            if (PrefManager.getStringValue(USER_UNIQUE_ID).isBlank()) {
                delay(200)
            }
            if (PrefManager.hasKey(USER_UNIQUE_ID).not()) {
                val response =
                    AppObjectController.signUpNetworkService.getGaid(mapOf("device_id" to Utils.getDeviceId()))
                if (response.isSuccessful && response.body() != null) {
                    PrefManager.put(USER_UNIQUE_ID, response.body()!!.gaID)
                    Branch.getInstance().setIdentity(response.body()!!.gaID)
                } else {
                    return Result.failure()
                }
            }
        } catch (ex: Throwable) {
            LogException.catchException(ex)
            return Result.failure()
        }
        return Result.success()
    }
}

class AppRunRequiredTaskWorker(var context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result {

        FirebaseAnalytics.getInstance(AppObjectController.joshApplication).resetAnalyticsData()
        AppObjectController.getFetchObject().awaitFinish()
        AppObjectController.isSettingUpdate = false
        if (PrefManager.getStringValue(API_TOKEN).isEmpty()) {
            PrefManager.put(API_TOKEN, User.getInstance().token)
        }
        if (PrefManager.getStringValue(COUNTRY_ISO).isEmpty()) {
            PrefManager.put(COUNTRY_ISO, getDefaultCountryIso(context))
        }
        if (PrefManager.getIntValue(SUBSCRIPTION_TEST_ID) == 0) {
            PrefManager.put(SUBSCRIPTION_TEST_ID, 122)
        }
        AppObjectController.getFirebaseRemoteConfig().setDefaultsAsync(R.xml.remote_config_defaults)
        AppObjectController.getFirebaseRemoteConfig().fetchAndActivate()
            .addOnCompleteListener {
                val disabledVersions =
                AppObjectController.getFirebaseRemoteConfig()
                    .getString(FirebaseRemoteConfigKey.DISABLED_VERSION_CODES)

            val disabledVersionsArr = disabledVersions.split(",")
            disabledVersionsArr.forEach {
                if (it == BuildConfig.VERSION_CODE.toString()) {
                    exitProcess(0)
                }
            }
        }.addOnFailureListener { exception ->
            exception.printStackTrace()
        }
        if (PrefManager.hasKey(CALL_RINGTONE_NOT_MUTE).not()) {
            PrefManager.put(CALL_RINGTONE_NOT_MUTE, true)
        }
        PrefManager.put(P2P_LAST_CALL, false)
        AppObjectController.initialiseFreshChat()
        Log.i("Workers", "doWork: referrer")
        InstallReferralUtil.installReferrer(context)
        return Result.success()
    }
}

class JoshTalksInstallWorker(context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result {
        if (PrefManager.hasKey(INSTALL_REFERRER_SYNC)) {
            return Result.success()
        }

        val obj = InstallReferrerModel.getPrefObject()
        obj?.apply {
            this.mentor = Mentor.getInstance().getId()
        }

        if (obj != null) {
            try {
                AppObjectController.signUpNetworkService.getInstallReferrerAsync(obj)
                PrefManager.put(INSTALL_REFERRER_SYNC, true)
            } catch (ex: Throwable) {
                LogException.catchException(ex)
            }
        }
        return Result.success()
    }
}

class CheckFCMTokenInServerWorker(context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result {
        try {
            val fcmToken = PrefManager.getStringValue(FCM_TOKEN)
            if (fcmToken.isBlank()) {
                WorkManagerAdmin.regenerateFCMWorker()
                return Result.success()
            }

            val result = AppObjectController.signUpNetworkService.checkFCMInServer(
                mapOf(
                    "user_id" to Mentor.getInstance().getId(),
                    "registration_id" to fcmToken
                )
            )
            if (result["message"] != FCM_ACTIVE)
                WorkManagerAdmin.regenerateFCMWorker()

            return Result.success()
        } catch (ex: Exception) {
            return if (ex is HttpException && ex.code() == 500) {
                WorkManagerAdmin.regenerateFCMWorker()
                Result.success()
            } else
                Result.failure()
        }
    }
}

class RegenerateFCMTokenWorker(context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result {
        try {
            FirebaseInstallations.getInstance().delete().addOnCompleteListener {
                FirebaseMessaging.getInstance().deleteToken().addOnCompleteListener {
                    regenerateFCM()
                }
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
            return Result.failure()
        }
        return Result.success()
    }

    private fun regenerateFCM() {
        FirebaseInstallations.getInstance().getToken(true).addOnCompleteListener {
            FirebaseMessaging.getInstance().token.addOnCompleteListener(
                OnCompleteListener { task ->
                    Timber.d("FCMToken : Refreshed")
                    if (!task.isSuccessful) {
                        task.exception?.run {
                            LogException.catchException(this)
                        }
                        task.exception?.printStackTrace()
                        return@OnCompleteListener
                    }
                    task.result.let {
                        PrefManager.put(FCM_TOKEN, it)
                    }
                }
            )
        }
    }
}

class WorkerAfterLoginInApp(context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result {
        PrefManager.put(LOGIN_ON, Date().time)
        return Result.success()
    }
}

class WorkerInLandingScreen(context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result {
        WorkManagerAdmin.syncNotificationEngagement()
        AppObjectController.clearDownloadMangerCallback()
        WorkManagerAdmin.syncAppCourseUsage()
        AppAnalytics.updateUser()
        return Result.success()
    }
}

class SyncEngageVideo(context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result {
        val syncEngageVideoList = mutableListOf<Long>()
        val chatIdList = AppObjectController.appDatabase.videoEngageDao().getAllUnSyncVideo()
        if (chatIdList.isNullOrEmpty().not()) {
            chatIdList.forEach {
                try {
                    if (it.isSharableVideo) {
                        AppObjectController.chatNetworkService.engageSharableVideoApi(it)
                    } else {
                        AppObjectController.chatNetworkService.engageVideoApiV2(it)
                    }
                    syncEngageVideoList.add(it.id)
                } catch (ex: Throwable) {
                    ex.printStackTrace()
                }
            }
            AppObjectController.appDatabase.videoEngageDao()
                .updateVideoSyncStatus(syncEngageVideoList)
        }

        return Result.success()
    }
}

class FeedbackRatingWorker(context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result {
        try {
            val response =
                AppObjectController.commonNetworkService.getFeedbackRatingDetailsAsync()
            PrefManager.put(RATING_DETAILS_KEY, AppObjectController.gsonMapper.toJson(response))
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        return Result.success()
    }
}

class BackgroundNotificationWorker(val context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            buildNotification()
            ReminderUtil(context).setAlarmNotificationWorker()
            NotificationAnalytics().fetchMissedNotification(context)
            NotificationAnalytics().pushToServer()
            removeNotification()
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure()
        }
    }

    private fun buildNotification() {
        val notificationIntent = Intent(context, InboxActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(context, 0, notificationIntent, 0)
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notificationBuilder = NotificationCompat.Builder(context, NOTIF_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_status_bar_notification)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText("Fetching all your notifications!")
            .setContentIntent(pendingIntent)
            .setDefaults(Notification.FLAG_ONGOING_EVENT)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            notificationBuilder.priority = NotificationManager.IMPORTANCE_LOW
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                NOTIF_CHANNEL_ID,
                NOTIF_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            )

            notificationBuilder.setChannelId(NOTIF_CHANNEL_ID)
            notificationManager.createNotificationChannel(notificationChannel)
        }

        val notification = notificationBuilder.build()
        notification.flags = Notification.FLAG_NO_CLEAR

        notificationManager.notify(NOTIF_ID, notification)
    }

    private fun removeNotification() {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIF_ID)
    }
}

class StickyNotificationWorker(val context: Context, val workerParams: WorkerParameters) : CoroutineWorker(context, workerParams) {

    companion object {
        const val notificationId = 10206
    }
    private var shouldUpdate = true
    private var job: Job? = null
    private var endTime: Long = 0
    private lateinit var notificationBuilder: NotificationCompat.Builder

    override suspend fun doWork(): Result {
        return try {
            buildNotification(getPendingIntent())

            val couponCode = workerParams.inputData.getString("coupon_code") ?: "ENG10"
            endTime = workerParams.inputData.getLong("expiry_time", 3600000L)
            val title = workerParams.inputData.getString("sticky_title") ?: "Do you know?"
            val body = workerParams.inputData.getString("sticky_body") ?: "You'll miss an offer if you don't click on this notification"

            updateJob(title, body, couponCode, endTime)

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val response = AppObjectController.utilsAPIService.updateNotificationStatus(
                        mapOf(Pair("coupon_code", couponCode))
                    )
                    shouldUpdate = true
                    endTime = (response["expiry_time"] as Double).toLong() * 1000L
                    updatePrefValue(endTime)
                    updateJob(title, body, couponCode, endTime)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure()
        }
    }

    private fun getPendingIntent(code: String = EMPTY): PendingIntent {
        val notificationIntent = Intent(context, BuyPageActivity::class.java).apply {
            putExtra(FLOW_FROM, "Sticky Notification")
            putExtra(COUPON_CODE, code)
            putExtra(HAS_NOTIFICATION, true)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        return PendingIntent.getActivity(context, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private fun buildNotification(pendingIntent: PendingIntent) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannelData.UPDATES
        notificationBuilder = NotificationCompat.Builder(context, channel.id)
            .setSmallIcon(R.drawable.ic_status_bar_notification)
            .setOngoing(true)
            .setAutoCancel(false)
            .setOnlyAlertOnce(true)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText("Claim your coupon by clicking this notification")
            .setContentIntent(pendingIntent)
            .setDefaults(Notification.FLAG_ONGOING_EVENT)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            notificationBuilder.priority = NotificationCompat.PRIORITY_HIGH
        }

        val remoteView = getRemoteView()

        notificationBuilder.setCustomContentView(remoteView)
        if (Build.VERSION.SDK_INT >= 29) {
            notificationBuilder.apply {
                setCustomHeadsUpContentView(remoteView)
                setCustomBigContentView(remoteView)
                setCustomContentView(remoteView)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                channel.id,
                channel.type,
                NotificationManager.IMPORTANCE_LOW
            )

            notificationBuilder.setChannelId(channel.id)
            notificationManager.createNotificationChannel(notificationChannel)
        }

        val notification = notificationBuilder.build()
        notification.flags = Notification.FLAG_NO_CLEAR

        notificationManager.notify(notificationId, notification)
    }

    private fun updateNotification(title: String, body: String, coupon: String, time: Float, timeDiff: Long) {
        if (shouldUpdate) {
            notificationBuilder.setContentIntent(getPendingIntent(coupon))
            notificationBuilder.contentView.setTextViewText(R.id.notification_title, title)
            notificationBuilder.contentView.setTextViewText(R.id.notification_body, body)
            notificationBuilder.contentView.setChronometer(
                R.id.notification_timer,
                SystemClock.elapsedRealtime() + timeDiff,
                null,
                true
            )
            shouldUpdate = false
        }
        if ((time) <= 0)
            stopWorker()
        notificationBuilder.contentView.setProgressBar(R.id.notification_progress, 100, (100 - (time * 100)).toInt(), false)
        NotificationManagerCompat.from(context).notify(notificationId, notificationBuilder.build())
    }

    private fun stopWorker() {
        val nMgr = context.getSystemService(Service.NOTIFICATION_SERVICE) as NotificationManager
        nMgr.cancel(notificationId)
        WorkManagerAdmin.removeStickyNotificationWorker()
    }

    private fun updateJob(title: String, body: String, couponCode: String, endTime: Long) {
        job?.cancel()
        val offsetTime = PrefManager.getLongValue(SERVER_TIME_OFFSET, true)
        val timeDiff = endTime - System.currentTimeMillis().plus(offsetTime)
        job = CoroutineScope(Dispatchers.Main).launch {
            while (System.currentTimeMillis().plus(offsetTime) < endTime) {
                updateNotification(
                    title = title,
                    body = body,
                    coupon = couponCode,
                    ((endTime - System.currentTimeMillis().plus(offsetTime)).toFloat() / timeDiff),
                    timeDiff
                )
                delay(10000)
            }
            stopWorker()
        }
    }

    private fun updatePrefValue(endTime: Long) {
        try {
            val jsonObject = JSONObject(PrefManager.getStringValue(STICKY_COUPON_DATA))
            jsonObject.put("expiry_time", endTime.div(1000))
            PrefManager.put(STICKY_COUPON_DATA, jsonObject.toString())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getRemoteView(): RemoteViews {
        val remoteView = RemoteViews(context.packageName, R.layout.coupon_code_notification)
        remoteView.setTextViewText(R.id.notification_title, "Sticky Notification")
        remoteView.setTextViewText(R.id.notification_body, "This is the notification body")
        remoteView.setChronometerCountDown(R.id.notification_timer, true)
        remoteView.setProgressBar(R.id.notification_progress, 100, 0, false)
        return remoteView
    }
}

class UpdateDeviceDetailsWorker(context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result {
        try {
            val device = DeviceDetailsResponse.getInstance()
            val status = device?.apiStatus ?: ApiRespStatus.EMPTY
            val deviceId = device?.id ?: 0
            if (ApiRespStatus.PATCH == status) {
                //return Result.success()
                if (deviceId > 0) {
                    val details = AppObjectController.signUpNetworkService.patchDeviceDetails(
                        deviceId,
                        UpdateDeviceRequest()
                    )
                    // TODO no need to send UpdateDeviceRequest object in patch request
                    details.apiStatus = ApiRespStatus.PATCH
                    details.update()
                }
            } else if (ApiRespStatus.POST == status) {
                if (deviceId > 0) {
                    val details = AppObjectController.signUpNetworkService.patchDeviceDetails(
                        deviceId,
                        UpdateDeviceRequest()
                    )
                    // TODO no need to send UpdateDeviceRequest object in patch request
                    details.apiStatus = ApiRespStatus.PATCH
                    details.update()
                }
            } else {
                val details =
                    AppObjectController.signUpNetworkService.postDeviceDetails(
                        UpdateDeviceRequest()
                    )
                details.apiStatus = ApiRespStatus.POST
                details.update()
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        return Result.success()
    }
}

class GenerateRestoreIdWorker(context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result {
        try {
            if (PrefManager.getStringValue(RESTORE_ID).isBlank()) {
                val id = PrefManager.getStringValue(USER_UNIQUE_ID)
                val details =
                    AppObjectController.commonNetworkService.getFreshChatRestoreIdAsync(id)
                if (details.restoreId.isNullOrBlank().not()) {
                    PrefManager.put(RESTORE_ID, details.restoreId!!)
                    AppObjectController.restoreUser(details.restoreId!!)
                } else AppObjectController.restoreUser(null)
            } else AppObjectController.restoreUser(PrefManager.getStringValue(RESTORE_ID))
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        return Result.success()
    }
}

class MergeMentorWithGAIDWorker(context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result {
        try {
            val id = PrefManager.getIntValue(SERVER_GID_ID)
            if (id == 0) {
                return Result.success()
            }
            val data = mapOf("mentor" to Mentor.getInstance().getId())
            AppObjectController.commonNetworkService.mergeMentorWithGAId(id.toString(), data)
            // PrefManager.removeKey(SERVER_GID_ID)
        } catch (ex: Throwable) {
            LogException.catchException(ex)
        }
        return Result.success()
    }
}

class DeleteUnlockTypeQuestion(context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result {
        try {
            val listConversationId =
                AppObjectController.appDatabase.courseDao().getAllConversationId()
            listConversationId.forEach { conversationId ->
                val listChatModel = AppObjectController.appDatabase.chatDao()
                    .getUnlockChatModel(conversationId)

                if (listChatModel.isNullOrEmpty()) {
                    return@forEach
                }

                listChatModel.forEach { chatModel ->
                    chatModel?.let {
                        if (DateUtils.isToday(it.created.time).not()) {
                            AppObjectController.appDatabase.chatDao()
                                .deleteChatMessage(chatModel)
                        }
                    }
                }
            }
        } catch (ex: Throwable) {
            LogException.catchException(ex)
        }
        return Result.success()
    }
}

class LogAchievementLevelEventWorker(context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result {
        try {
            val videoEngageEntity =
                AppObjectController.appDatabase.videoEngageDao().getWatchTime()
            videoEngageEntity?.totalTime?.run {
                val time = TimeUnit.MILLISECONDS.toMinutes(this).div(10).toInt()
                if (time > 7 || time == 0) {
                    return Result.success()
                }
                MarketingAnalytics.logAchievementLevelEvent(time)
            }
        } catch (ex: Throwable) {
            LogException.catchException(ex)
        }
        return Result.success()
    }
}

class LanguageChangeWorker(var context: Context, private var workerParams: WorkerParameters) :
    ListenableWorker(context, workerParams) {
    override fun startWork(): ListenableFuture<Result> {
        return CallbackToFutureAdapter.getFuture { completer ->
            if (runAttemptCount > 2) {
                completer.set(Result.failure())
            }

            val language = workerParams.inputData.getString(LANGUAGE_CODE) ?: "en"
            val defaultLanguage = PrefManager.getStringValue(USER_LOCALE)
            Lingver.getInstance().setLocale(context, language)
            context.changeLocale(language)
            AppObjectController.isSettingUpdate = true
            AppObjectController.getFirebaseRemoteConfig().reset()
            AppObjectController.getFirebaseRemoteConfig().setDefaultsAsync(R.xml.remote_config_defaults)
            AppObjectController.getFirebaseRemoteConfig().fetch(0)
            AppObjectController.getFirebaseRemoteConfig()
                .fetchAndActivate()
                .addOnSuccessListener {
                    if (it) {
                        PrefManager.put(USER_LOCALE, language)
                        PrefManager.put(USER_LOCALE_UPDATED, true)
                        completer.set(Result.success())
                    } else {
                        onErrorToFetch(defaultLanguage)
                        completer.set(Result.retry())
                    }
                }
                .addOnFailureListener {
                    it.printStackTrace()
                    onErrorToFetch(defaultLanguage)
                    completer.setCancelled()
                }
        }
    }

    private fun onErrorToFetch(defaultLanguage: String) {
        PrefManager.put(USER_LOCALE, defaultLanguage)
        AppObjectController.isSettingUpdate = false
        Lingver.getInstance().setLocale(context, defaultLanguage)
    }
}

class AppUsageWorker(context: Context, private var workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result {
        try {
            val active = workerParams.inputData.getBoolean(IS_ACTIVE, false)
            if (active) {
                AppObjectController.appUsageStartTime = System.currentTimeMillis()
                return Result.success()
            } else {
                val cTime = System.currentTimeMillis()
                val uTime = AppObjectController.appUsageStartTime
                if (uTime > cTime || uTime <= 0) {
                    AppObjectController.appUsageStartTime = 0L
                    return Result.success()
                }
                val time = cTime - uTime
                AppObjectController.appDatabase.appUsageDao()
                    .insertIntoAppUsage(AppUsageModel(time))
            }
        } catch (ex: Throwable) {
            ex.printStackTrace()
        }
        AppObjectController.appUsageStartTime = 0L
        return Result.success()
    }
}

class AppUsageSyncWorker(context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result {
        try {
            val list = AppObjectController.appDatabase.appUsageDao().getAllSession()
            if (list.isNotEmpty()) {
                val mentorId = Mentor.getInstance().getId()
                val gaid = PrefManager.getStringValue(USER_UNIQUE_ID)
                list.listIterator().forEach {
                    if (mentorId.isNotBlank()) {
                        it.mentorId = mentorId
                    }
                    it.gaidId = gaid
                }
                val body: HashMap<String, List<AppUsageModel>> = HashMap()
                body["data"] = list
                val resp = AppObjectController.commonNetworkService.engageUserSession(body)
                if (resp.isSuccessful) {
                    AppObjectController.appDatabase.appUsageDao().deleteAllSyncSession()
                }
            }
        } catch (ex: Throwable) {
            LogException.catchException(ex)
        }
        return Result.success()
    }
}

class PatchUserIdToGAIdV2(context: Context, private val workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result {
        try {
            val obj = GaIDMentorModel.getMapObject()
            if (obj != null) {
                val extras: HashMap<String, List<String>> = HashMap()
                extras["mentors"] = listOf(Mentor.getInstance().getId())
                val resp = AppObjectController.commonNetworkService.patchMentorWithGAIdAsync(
                    obj.gaidServerDbId,
                    extras
                )
                if (resp.isSuccessful || resp.code() == 400) {
                    GaIDMentorModel.update(obj)
                }
            }
        } catch (ex: Throwable) {
            // LogException.catchException(ex)
        }
        return Result.success()
    }
}

class SyncFavoriteCaller(context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result {
        try {
            val response = AppObjectController.p2pNetworkService.getFavoriteCallerList(
                Mentor.getInstance().getId()
            )
            AppObjectController.appDatabase.favoriteCallerDao().also {
                it.removeAllFavorite()
                it.insertFavoriteCallers(response)
                RxBus2.publish(DBInsertion("Favorite_caller"))
            }
        } catch (ex: Throwable) {
            LogException.catchException(ex)
        }
        return Result.success()
    }
}

class CourseUsageSyncWorker(context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result {
        try {
            val db = AppObjectController.appDatabase
            val sessionList = db.courseUsageDao().getAllSession()
            val keys = sessionList.groupBy { it.conversationId }.keys
            val courseIdList = db.courseDao().getCourseIdsFromConversationId(keys.toList())

            val list = sessionList.parallelStream()
                .map { courseUsageModel ->
                    CourseUsageSync(
                        courseId = courseIdList.findLast { it.conversationId == courseUsageModel.conversationId }?.courseId
                            ?: -1,
                        conversationId = courseUsageModel.conversationId,
                        screenName = courseUsageModel.screenName ?: EMPTY,
                        startTime = courseUsageModel.startTime,
                        endTime = courseUsageModel.endTime ?: 0
                    )
                }.toList()
            if (list.isEmpty()) {
                return Result.success()
            }
            val body: HashMap<String, List<CourseUsageSync>> = HashMap()
            body["data"] = list
            val resp = AppObjectController.commonNetworkService.engageCourseUsageSession(body)
            if (resp.isSuccessful) {
                AppObjectController.appDatabase.courseUsageDao().deleteAllSyncSession()
            }
        } catch (ex: Throwable) {
            ex.printStackTrace()
        }
        return Result.success()
    }
}

class NotificationEngagementSyncWorker(val context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {

    val oneDayMillis = 24 * 60 * 60 * 1000L
    val oneWeekMillis = 7 * oneDayMillis

    override suspend fun doWork(): Result {
        try {
            NotificationAnalytics().fetchMissedNotification(context)
            if (shouldFetchClientData()) {
                val response = AppObjectController.utilsAPIService.getFTScheduledNotifications(
                    PrefManager.getStringValue(
                        FREE_TRIAL_TEST_ID,
                        false,
                        "None"
                    ),
                    PrefManager.getIntValue(DAILY_NOTIFICATION_COUNT).toString()
                )
                if (response.isNotEmpty()) {
                    NotificationUtils(context).removeAllNotificationAsync()
                    AppObjectController.appDatabase.scheduleNotificationDao().insertAllNotifications(response)
                    NotificationUtils(context).updateNotificationDb()
                }
                PrefManager.put(LAST_TIME_FETCHED_NOTIFICATION, System.currentTimeMillis())
                PrefManager.put(DAILY_NOTIFICATION_COUNT, PrefManager.getIntValue(DAILY_NOTIFICATION_COUNT, defValue = 0) + 1)
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        return try {
            if (shouldSendEnabledAnalytics()) {
                val response = AppObjectController.commonNetworkService.saveImpression(
                    mapOf(
                        Pair("mentor_id", Mentor.getInstance().getId()),
                        Pair("event_name", notificationEnabledEvent())
                    )
                )
                if (response.isSuccessful) {
                    val count = PrefManager.getIntValue(NOTIFICATION_STATUS_COUNT)
                    PrefManager.put(NOTIFICATION_STATUS_COUNT, count + 1)
                    PrefManager.put(NOTIFICATION_LAST_TIME_STATUS, System.currentTimeMillis())
                }
            }
            when(NotificationAnalytics().pushToServer()) {
                true -> Result.success()
                false -> Result.failure()
            }
        } catch (ex: Throwable) {
            ex.printStackTrace()
            Result.failure()
        }
    }

    private fun shouldFetchClientData(): Boolean {
        val lastTime = PrefManager.getLongValue(LAST_TIME_FETCHED_NOTIFICATION)
        if (lastTime < dateStartOfDay().time && PrefManager.getBoolValue(IS_FREE_TRIAL))
            return true
        return false
    }

    private fun shouldSendEnabledAnalytics(): Boolean {
        val count = PrefManager.getIntValue(NOTIFICATION_STATUS_COUNT)
        val timeDifference = System.currentTimeMillis() - PrefManager.getLongValue(NOTIFICATION_LAST_TIME_STATUS)
        if (count < 7 && timeDifference > oneDayMillis)
            return true
        else if (count >= 7 && timeDifference > oneWeekMillis)
            return true
        return false
    }

    private fun notificationEnabledEvent(): String {
        if (NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            return AnalyticsEvent.NOTIFICATION_ENABLED.name
        }
        return AnalyticsEvent.NOTIFICATION_DISABLED.name
    }
}

class FakeCallNotificationWorker(
    val context: Context,
    workerParams: WorkerParameters
) :
    CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result {
        try {
            if (PrefManager.getBoolValue(IS_COURSE_BOUGHT).not() &&
                PrefManager.getLongValue(COURSE_EXPIRY_TIME_IN_MS) != 0L &&
                PrefManager.getLongValue(COURSE_EXPIRY_TIME_IN_MS) < System.currentTimeMillis() &&
                AppObjectController.currentActivityClass != PaymentSummaryActivity::class.java.simpleName &&
                AppObjectController.currentActivityClass != BuyPageActivity::class.java.simpleName
            ) {
                try {
                    val resp = AppObjectController.p2pNetworkService.getFakeCall()
                    val nc = resp.toNotificationObject(null)
                    NotificationUtils(context).sendNotification(nc)
                }catch (ex:Exception){}
            }
        } catch (ex: Throwable) {
            ex.printStackTrace()
        }
        return Result.success()
    }
}

fun getGoogleAdId(context: Context): String? {
    try {
        MobileAds.initialize(context)
        val adInfo = AdvertisingIdClient.getAdvertisingIdInfo(context)
        return adInfo.id
    } catch (e: Exception) {

    }
    return null
}

class UpdateServerTimeWorker(context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result {
        try {
            val serverTimeResponse = AppObjectController.utilsAPIService.getServerTime()
            if (serverTimeResponse.isSuccessful && serverTimeResponse.body()!=null){
                val offset = serverTimeResponse.body()!!.minus(System.currentTimeMillis())
                PrefManager.put(SERVER_TIME_OFFSET, isConsistent = true, value = offset)
                return Result.success()
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        return Result.success()
    }
}
