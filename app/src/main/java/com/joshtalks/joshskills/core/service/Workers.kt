package com.joshtalks.joshskills.core.service

import android.content.Context
import android.text.format.DateUtils
import androidx.concurrent.futures.CallbackToFutureAdapter
import androidx.work.*
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.identifier.AdvertisingIdClient
import com.google.android.gms.tasks.OnCompleteListener
import com.google.common.util.concurrent.ListenableFuture
import com.google.firebase.installations.FirebaseInstallations
import com.google.firebase.messaging.FirebaseMessaging
import com.joshtalks.joshskills.BuildConfig
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.core.analytics.LogException
import com.joshtalks.joshskills.core.analytics.MarketingAnalytics
import com.joshtalks.joshskills.core.notification.FCM_TOKEN
import com.joshtalks.joshskills.engage_notification.AppUsageModel
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.entity.NPSEvent
import com.joshtalks.joshskills.repository.local.entity.NPSEventModel
import com.joshtalks.joshskills.repository.local.eventbus.DBInsertion
import com.joshtalks.joshskills.repository.local.eventbus.NPSEventGenerateEventBus
import com.joshtalks.joshskills.repository.local.model.*
import com.joshtalks.joshskills.repository.server.ActiveUserRequest
import com.joshtalks.joshskills.repository.server.MessageStatusRequest
import com.joshtalks.joshskills.repository.server.UpdateDeviceRequest
import com.joshtalks.joshskills.repository.server.onboarding.VersionResponse
import com.joshtalks.joshskills.track.CourseUsageSync
import com.sinch.verification.PhoneNumberUtils
import com.yariksoffice.lingver.Lingver
import io.branch.referral.Branch
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.streams.toList
import kotlin.system.exitProcess
import timber.log.Timber

const val INSTALL_REFERRER_SYNC = "install_referrer_sync"
const val CONVERSATION_ID = "conversation_id"
const val IS_ACTIVE = "is_active"
const val LANGUAGE_CODE = "language_code"

class UniqueIdGenerationWorker(var context: Context, workerParams: WorkerParameters) :
    Worker(context, workerParams) {
    override fun doWork(): Result {
        try {
            if (PrefManager.hasKey(USER_UNIQUE_ID).not()) {
                val id = getGoogleAdId(context)
                PrefManager.put(USER_UNIQUE_ID, id)
                Branch.getInstance().setIdentity(id)
            }
        } catch (ex: Throwable) {
            LogException.catchException(ex)
        }
        return Result.success()
    }
}

class AppRunRequiredTaskWorker(var context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result {
        AppAnalytics.flush()
        AppObjectController.facebookEventLogger.flush()
        AppObjectController.firebaseAnalytics.resetAnalyticsData()
        AppObjectController.getFetchObject().awaitFinish()
        AppObjectController.isSettingUpdate = false
        if (PrefManager.getStringValue(API_TOKEN).isEmpty()) {
            PrefManager.put(API_TOKEN, User.getInstance().token)
        }
        if (PrefManager.getStringValue(COUNTRY_ISO).isEmpty()) {
            PrefManager.put(COUNTRY_ISO, PhoneNumberUtils.getDefaultCountryIso(context))
        }
        if (PrefManager.getIntValue(SUBSCRIPTION_TEST_ID) == 0) {
            PrefManager.put(SUBSCRIPTION_TEST_ID, 122)
        }
        AppObjectController.getFirebaseRemoteConfig().fetchAndActivate().addOnCompleteListener {
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

class InstanceIdGenerationWorker(var context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        try {
            if (PrefManager.hasKey(INSTANCE_ID, true)) {
                PrefManager.put(INSTANCE_ID, PrefManager.getStringValue(INSTANCE_ID, true), false)
            }
            if (PrefManager.hasKey(INSTANCE_ID, false).not()) {
                val gaid = PrefManager.getStringValue(USER_UNIQUE_ID)
                val res =
                    AppObjectController.signUpNetworkService.getInstanceIdAsync(mapOf("gaid" to gaid))
                if (res.instanceId.isEmpty().not()) {
                    PrefManager.put(INSTANCE_ID, res.instanceId, false)
                    PrefManager.put(INSTANCE_ID, res.instanceId, true)
                }
            }
        } catch (ex: Throwable) {
            LogException.catchException(ex)
        }
        return Result.success()
    }
}

class GetVersionAndFlowDataWorker(var context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        try {
            if (PrefManager.hasKey(INSTANCE_ID, false) &&
                VersionResponse.getInstance().hasVersion().not()
            ) {
                val instanceId = PrefManager.getStringValue(INSTANCE_ID)
                val res =
                    AppObjectController.commonNetworkService.getOnBoardingVersionDetails(mapOf("instance_id" to instanceId))
                val sortedInterest =
                    res.courseInterestTags?.sortedBy { tag -> tag.sortOrder }
                val sortedCategories =
                    res.courseCategories?.sortedBy { category -> category.sortOrder }
                res.courseInterestTags = sortedInterest
                res.courseCategories = sortedCategories
                VersionResponse.update(res)
            }
        } catch (ex: Throwable) {
            LogException.catchException(ex)
            return Result.retry()
        }
        return Result.success()
    }
}
/*
class GenerateGuestUserMentorWorker(var context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        try {
            if (PrefManager.hasKey(INSTANCE_ID, false) && PrefManager.getStringValue(API_TOKEN)
                    .isBlank()
                && PrefManager.getStringValue(INSTANCE_ID).isBlank().not()
            ) {
                val instanceId = PrefManager.getStringValue(INSTANCE_ID)
                val response =
                    AppObjectController.signUpNetworkService.createGuestUser(mapOf("instance_id" to instanceId))
                updateFromLoginResponse(response)
            }
        } catch (ex: Throwable) {
            LogException.catchException(ex)
        }
        return Result.success()
    }
}
private fun updateFromLoginResponse(loginResponse: LoginResponse) {
    val user = User.getInstance()
    user.userId = loginResponse.userId
    user.isVerified = false
    user.token = loginResponse.token
    User.update(user)
    PrefManager.put(API_TOKEN, loginResponse.token)
    Mentor.getInstance()
        .setId(loginResponse.mentorId)
        .setReferralCode(loginResponse.referralCode)
        .setUserId(loginResponse.userId)
        .update()
    AppAnalytics.updateUser()
    WorkManagerAdmin.requiredTaskAfterLoginComplete()
}
*/

class MessageReadPeriodicWorker(context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result {
        try {
            val chatIdList = AppObjectController.appDatabase.chatDao().getSeenByUserMessages()
            if (chatIdList.isNullOrEmpty().not()) {
                val messageStatusRequestList = mutableListOf<MessageStatusRequest>()
                chatIdList.forEach {
                    messageStatusRequestList.add(MessageStatusRequest(it))
                }
                AppObjectController.chatNetworkService.updateMessagesStatus(messageStatusRequestList)
            }
            return Result.success()
        } catch (ex: Throwable) {
            LogException.catchException(ex)
            return Result.retry()
        }
    }
}

class RefreshFCMTokenWorker(context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result {
        if (User.getInstance().isVerified) {
            regenerateFCM()
        } else {
            FirebaseInstallations.getInstance().delete().addOnCompleteListener {
                FirebaseMessaging.getInstance().deleteToken().addOnCompleteListener {
                    regenerateFCM()
                }
            }
        }
        return Result.success()
    }

    private fun regenerateFCM() {
        FirebaseInstallations.getInstance().getToken(true).addOnCompleteListener {
            FirebaseMessaging.getInstance().token.addOnCompleteListener(
                OnCompleteListener { task ->
                    if (!task.isSuccessful) {
                        task.exception?.run {
                            LogException.catchException(this)
                        }
                        task.exception?.printStackTrace()
                        return@OnCompleteListener
                    }
                    task.result?.run {
                        PrefManager.put(FCM_TOKEN, this)
                        val fcmResponse = FCMResponse.getInstance()
                        fcmResponse?.apiStatus = ApiRespStatus.POST
                        fcmResponse?.update()
                    }
                }
            )
        }
    }
}

class UploadFCMTokenOnServer(context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result {
        try {
            val token = PrefManager.getStringValue(FCM_TOKEN)
            if (token.isEmpty()) {
                WorkManagerAdmin.forceRefreshFcmToken()
                return Result.success()
            }
            val fcmResponse = FCMResponse.getInstance()
            val status = fcmResponse?.apiStatus ?: ApiRespStatus.EMPTY

            if (ApiRespStatus.PATCH == status) {
                return Result.success()
            } else if (ApiRespStatus.POST == status) {
                val userId = Mentor.getInstance().getId()
                if (fcmResponse != null && userId.isNotBlank()) {
                    val data = mutableMapOf("user_id" to userId, "registration_id" to token)
                    val resp =
                        AppObjectController.signUpNetworkService.patchFCMToken(
                            fcmResponse.id,
                            data
                        )
                    if (resp.isSuccessful) {
                        fcmResponse.userId = userId
                        fcmResponse.apiStatus = ApiRespStatus.PATCH
                        fcmResponse.update()
                    }
                }
            } else {
                val data = mutableMapOf(
                    "registration_id" to token,
                    "name" to Utils.getDeviceName(),
                    "device_id" to Utils.getDeviceId(),
                    "active" to "true",
                    "type" to "android",
                    "gaid" to PrefManager.getStringValue(USER_UNIQUE_ID)
                )
                val userId = Mentor.getInstance().getId()
                if (userId.isNotEmpty()) {
                    data["user_id"] = userId
                }
                val response = AppObjectController.signUpNetworkService.postFCMToken(data)
                response.apiStatus = if (userId.isEmpty()) {
                    ApiRespStatus.POST
                } else {
                    ApiRespStatus.PATCH
                }
                response.update()
            }
        } catch (ex: Throwable) {
            ex.printStackTrace()
        }
        return Result.success()
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
        AppObjectController.clearDownloadMangerCallback()
        // SyncChatService.syncChatWithServer()
        WorkManagerAdmin.readMessageUpdating()
        WorkManagerAdmin.refreshFcmToken()
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
                    AppObjectController.chatNetworkService.engageVideoApiV2(it)
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

class NPAQuestionViaEventWorker(
    context: Context,
    private var workerParams: WorkerParameters
) :
    CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result {
        try {
            val event = workerParams.inputData.getString("event")
            if (event.isNullOrEmpty().not()) {
                val response =
                    AppObjectController.commonNetworkService.getQuestionNPSEvent(event!!)
                if (response.isSuccessful) {
                    val outputData = workDataOf(
                        "nps_question_list" to AppObjectController.gsonMapperForLocal.toJson(
                            response.body()
                        )
                    )
                    return Result.success(outputData)
                }
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        return Result.failure()
    }
}

class DeterminedNPSEvent(context: Context, private var workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result {
        try {
            val conversationId = workerParams.inputData.getString("id") ?: EMPTY
            var day: Int = workerParams.inputData.getInt("day", -1)

            val event: NPSEvent = AppObjectController.gsonMapperForLocal.fromJson(
                workerParams.inputData.getString("event"),
                NPSEvent::class.java
            )

            if (day < 0) {
                val time = PrefManager.getLongValue(LOGIN_ON)
                if (time > 0) {
                    val temp = Utils.diffFromToday(Date(time))
                    day = if (temp == 0) {
                        -1
                    } else {
                        temp
                    }
                }
            }
            NPSEventModel.getAllNpaList()?.filter { it.enable }
                ?.find { it.day == day }?.run {
                    if (event == NPSEvent.STANDARD_TIME_EVENT && this.day == 0) {
                        return@run
                    }
                    val exist = AppObjectController.appDatabase.npsEventModelDao()
                        .isEventExist(this.eventName, this.day, conversationId)
                    if (exist == 0L) {
                        NPSEventModel.setCurrentNPA(this.event)
                        RxBus2.publish(NPSEventGenerateEventBus())
                    }
                }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        return Result.success()
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
                return Result.success()
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

class UserActiveWorker(context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result {
        try {
            val instanceId = when {
                PrefManager.hasKey(INSTANCE_ID, true) -> {
                    PrefManager.getStringValue(INSTANCE_ID, true)
                }
                PrefManager.hasKey(INSTANCE_ID, false) -> {
                    PrefManager.getStringValue(INSTANCE_ID, false)
                }
                else -> {
                    null
                }
            }
            val response = AppObjectController.signUpNetworkService.userActive(
                Mentor.getInstance().getId(),
                mapOf("instance_id" to instanceId, "device_id" to Utils.getDeviceId())
            )

            if (response.isSuccessful && response.body()?.isLatestLoginDevice == false) {
                Mentor.deleteUserCredentials()
                Mentor.deleteUserData()
            }
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

class IsUserActiveWorker(context: Context, private var workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result {
        try {
            if (Mentor.getInstance().hasId() && User.getInstance().isVerified) {
                val active = workerParams.inputData.getBoolean(IS_ACTIVE, false)
                val minTimeToApiFire = AppObjectController.getFirebaseRemoteConfig()
                    .getLong(FirebaseRemoteConfigKey.INTERVAL_TO_FIRE_ACTIVE_API)
                val lastTimeOfFireApi = PrefManager.getLongValue(LAST_ACTIVE_API_TIME)

                val secDiff =
                    TimeUnit.SECONDS.convert(
                        Date().time - lastTimeOfFireApi,
                        TimeUnit.MILLISECONDS
                    )
                Timber.tag("Workers").e("= %s", secDiff)
                if (secDiff >= minTimeToApiFire || active.not()) {
                    val data = ActiveUserRequest(Mentor.getInstance().getId(), active)
                    AppObjectController.signUpNetworkService.activeUser(data)
                    PrefManager.put(LAST_ACTIVE_API_TIME, Date().time)
                }
                if (active.not()) {
                    PrefManager.put(LAST_ACTIVE_API_TIME, 0L)
                }
            }
        } catch (ex: Throwable) {
            ex.printStackTrace()
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
            AppObjectController.getFirebaseRemoteConfig().fetch(0)
            AppObjectController.getFirebaseRemoteConfig()
                .fetchAndActivate()
                .addOnSuccessListener {
                    if (it) {
                        PrefManager.put(USER_LOCALE, language)
                        PrefManager.put(USER_LOCALE_UPDATED, true)
                        WorkManagerAdmin.startVersionAndFlowWorker()
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

fun getGoogleAdId(context: Context): String {
    MobileAds.initialize(context)
    val adInfo = AdvertisingIdClient.getAdvertisingIdInfo(context)
    return adInfo.id
}
