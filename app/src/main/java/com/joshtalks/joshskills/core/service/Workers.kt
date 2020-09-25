package com.joshtalks.joshskills.core.service

import android.content.Context
import android.text.format.DateUtils
import androidx.work.CoroutineWorker
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.identifier.AdvertisingIdClient
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.iid.FirebaseInstanceId
import com.joshtalks.joshskills.BuildConfig
import com.joshtalks.joshskills.core.API_TOKEN
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.COUNTRY_ISO
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.core.EXPLORE_TYPE
import com.joshtalks.joshskills.core.INSTANCE_ID
import com.joshtalks.joshskills.core.InstallReferralUtil
import com.joshtalks.joshskills.core.LOGIN_ON
import com.joshtalks.joshskills.core.PrefManager
import com.joshtalks.joshskills.core.RATING_DETAILS_KEY
import com.joshtalks.joshskills.core.RESTORE_ID
import com.joshtalks.joshskills.core.SERVER_GID_ID
import com.joshtalks.joshskills.core.SUBSCRIPTION_TEST_ID
import com.joshtalks.joshskills.core.USER_UNIQUE_ID
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.core.analytics.LogException
import com.joshtalks.joshskills.core.notification.FCM_TOKEN
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.entity.NPSEvent
import com.joshtalks.joshskills.repository.local.entity.NPSEventModel
import com.joshtalks.joshskills.repository.local.eventbus.NPSEventGenerateEventBus
import com.joshtalks.joshskills.repository.local.model.DeviceDetailsResponse
import com.joshtalks.joshskills.repository.local.model.ExploreCardType
import com.joshtalks.joshskills.repository.local.model.FCMResponse
import com.joshtalks.joshskills.repository.local.model.GaIDMentorModel
import com.joshtalks.joshskills.repository.local.model.InstallReferrerModel
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.repository.local.model.RequestRegisterGAId
import com.joshtalks.joshskills.repository.local.model.User
import com.joshtalks.joshskills.repository.server.ActiveUserRequest
import com.joshtalks.joshskills.repository.server.MessageStatusRequest
import com.joshtalks.joshskills.repository.server.UpdateDeviceRequest
import com.joshtalks.joshskills.repository.server.onboarding.VersionResponse
import com.joshtalks.joshskills.repository.server.signup.LoginResponse
import com.joshtalks.joshskills.repository.service.NetworkRequestHelper
import com.joshtalks.joshskills.repository.service.SyncChatService
import com.sinch.verification.PhoneNumberUtils
import io.branch.referral.Branch
import java.util.*
import retrofit2.HttpException

const val INSTALL_REFERRER_SYNC = "install_referrer_sync"
const val CONVERSATION_ID = "conversation_id"
const val IS_ACTIVE = "is_active"


class AppRunRequiredTaskWorker(var context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result {
        AppAnalytics.flush()
        AppObjectController.facebookEventLogger.flush()
        AppObjectController.firebaseAnalytics.resetAnalyticsData()
        AppObjectController.getFetchObject().awaitFinish()
        if (PrefManager.getStringValue(API_TOKEN).isEmpty()) {
            PrefManager.put(API_TOKEN, User.getInstance().token)
        }
        if (PrefManager.getStringValue(COUNTRY_ISO).isEmpty()) {
            PrefManager.put(COUNTRY_ISO, PhoneNumberUtils.getDefaultCountryIso(context))
        }
        if (PrefManager.getIntValue(SUBSCRIPTION_TEST_ID) == 0) {
            PrefManager.put(SUBSCRIPTION_TEST_ID, 122)
        }

        WorkManagerAdmin.readMessageUpdating()
        WorkManagerAdmin.deleteUnlockTypeQuestions()
        AppObjectController.getFirebaseRemoteConfig().fetchAndActivate().addOnCompleteListener {
            val npsEvent =
                AppObjectController.getFirebaseRemoteConfig().getString("NPS_EVENT_LIST")
            NPSEventModel.setNPSList(npsEvent)
            AppObjectController.firebaseAnalytics.setUserProperty(
                "App Version",
                BuildConfig.VERSION_CODE.toString()
            )
        }.addOnFailureListener { exception ->
            exception.printStackTrace()
        }
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
            if (PrefManager.hasKey(INSTANCE_ID, false).not()) {
                val gaid = PrefManager.getStringValue(USER_UNIQUE_ID)
                val res =
                    AppObjectController.signUpNetworkService.getInstanceIdAsync(mapOf("gaid" to gaid))
                if (res.instanceId.isEmpty().not())
                    PrefManager.put(INSTANCE_ID, res.instanceId, false)
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
                if (res.isSuccessful) {
                    res.body()?.let {
                        val sortedInterest =
                            it.courseInterestTags?.sortedBy { tag -> tag.sortOrder }
                        val sortedCategories =
                            it.courseCategories?.sortedBy { category -> category.sortOrder }
                        it.courseInterestTags = sortedInterest
                        it.courseCategories = sortedCategories
                        VersionResponse.update(it)
                    }
                }
            }

            return Result.success()
        } catch (ex: Throwable) {
            LogException.catchException(ex)
            return Result.retry()
        }
    }
}

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
                if (response.isSuccessful)
                    response.body()?.let {
                        updateFromLoginResponse(it)
                    }
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
    User.update(user.toString())
    PrefManager.put(API_TOKEN, loginResponse.token)
    Mentor.getInstance()
        .setId(loginResponse.mentorId)
        .setReferralCode(loginResponse.referralCode)
        .setUserId(loginResponse.userId)
        .update()
    AppAnalytics.updateUser()
    WorkManagerAdmin.requiredTaskAfterLoginComplete()
}

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


class GetUserConversationWorker(var context: Context, private var workerParams: WorkerParameters) :
    Worker(context, workerParams) {

    override fun doWork(): Result {
        try {
            val conversationId = workerParams.inputData.getString(CONVERSATION_ID)
            conversationId?.run {
                val arguments = mutableMapOf<String, String>()
                val (key, value) = PrefManager.getLastSyncTime(this)
                arguments[key] = value
                NetworkRequestHelper.getUpdatedChat(this, queryMap = arguments)
            }
        } catch (ex: Throwable) {
            LogException.catchException(ex)
        }
        return Result.success()
    }

}

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


class ReferralCodeRefreshWorker(context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result {
        return try {
            if (Mentor.getInstance().hasId() && Mentor.getInstance().referralCode.isEmpty()) {
                val reqObj = mapOf("mentor" to Mentor.getInstance().getId())
                val response =
                    AppObjectController.signUpNetworkService.validateOrGetAndReferralOrCouponAsync(
                        reqObj
                    ).await()
                response.getOrNull(0)?.code?.let {
                    Mentor.getInstance().setReferralCode(it).update()
                }
            }
            Result.success()
        } catch (ex: Throwable) {
            LogException.catchException(ex)
            Result.retry()
        }
    }

}

class RegisterUserGAId(context: Context, private val workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result {
        try {
            val requestRegisterGAId = RequestRegisterGAId()
            requestRegisterGAId.gaid = PrefManager.getStringValue(USER_UNIQUE_ID)
            requestRegisterGAId.installOn =
                InstallReferrerModel.getPrefObject()?.installOn ?: Date().time
            requestRegisterGAId.test =
                workerParams.inputData.getString("test_id")?.split("_")?.get(1)?.toInt()
            requestRegisterGAId.utmMedium = InstallReferrerModel.getPrefObject()?.utmMedium ?: EMPTY
            requestRegisterGAId.utmSource = InstallReferrerModel.getPrefObject()?.utmSource ?: EMPTY
            val exploreType = workerParams.inputData.getString("explore_type")
            requestRegisterGAId.exploreCardType =
                if (exploreType?.isNotBlank() == true) ExploreCardType.valueOf(exploreType) else null
            val resp =
                AppObjectController.commonNetworkService.registerGAIdAsync(requestRegisterGAId)
                    .await()
            PrefManager.put(SERVER_GID_ID, resp.id)
            PrefManager.put(EXPLORE_TYPE, resp.exploreCardType!!.name, false)
        } catch (ex: Throwable) {
            //LogException.catchException(ex)
        }
        return Result.success()
    }
}


class RefreshFCMTokenWorker(context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result {
        FirebaseInstanceId.getInstance().instanceId
            .addOnCompleteListener(OnCompleteListener { task ->
                if (!task.isSuccessful) {
                    task.exception?.run {
                        LogException.catchException(this)
                    }
                    task.exception?.printStackTrace()
                    return@OnCompleteListener
                }
                task.result?.token?.run {
                    PrefManager.put(FCM_TOKEN, this)
                }
            })
        return Result.success()
    }
}


class MappingGaIDWithMentor(var context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result {
        if (GaIDMentorModel.getMapObject() == null) {
            val extras: HashMap<String, String> = HashMap()
            val obj = GaIDMentorModel()
            obj.gaID = getGoogleAdId(context)
            try {
                extras["id"] = obj.gaID
                val resp = AppObjectController.commonNetworkService.registerGAIdDetailsAsync(extras)
                    .await()
                GaIDMentorModel.update(resp)
            } catch (ex: HttpException) {
                if (ex.code() == 400) {
                    GaIDMentorModel.update(obj)
                }
                ex.printStackTrace()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            val obj = GaIDMentorModel.getMapObject()
            if (obj != null && obj.mapMentorList.isNullOrEmpty() && Mentor.getInstance().hasId()) {
                try {
                    val extras: HashMap<String, List<String>> = HashMap()
                    extras["mentors"] = listOf(Mentor.getInstance().getId())
                    AppObjectController.commonNetworkService.patchMentorWithGAIdAsync(
                        obj.gaID,
                        extras
                    ).await()
                    obj.mapMentorList = extras["mentors"]
                    GaIDMentorModel.update(obj)
                } catch (ex: HttpException) {
                    GaIDMentorModel.update(obj)
                    ex.printStackTrace()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

        }
        return Result.success()
    }
}


class UploadFCMTokenOnServer(context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result {
        try {
            val token = PrefManager.getStringValue(FCM_TOKEN)
            if (token.isEmpty())
                return Result.success()
            val data = mutableMapOf(
                "registration_id" to token,
                "name" to Utils.getDeviceName(),
                "device_id" to Utils.getDeviceId(),
                "active" to "true",
                "type" to "android"
            )
            if (PrefManager.getStringValue(USER_UNIQUE_ID).isNotEmpty()) {
                data["gaid"] = PrefManager.getStringValue(USER_UNIQUE_ID)
            }
            val fcmResponse = FCMResponse.getInstance()
            if (Mentor.getInstance().hasId() && fcmResponse != null) {
                fcmResponse.userId = Mentor.getInstance().getId()
                AppObjectController.signUpNetworkService.updateFCMToken(
                    fcmResponse.id, fcmResponse
                ).await()
            } else {
                if (fcmResponse == null) {
                    val response =
                        AppObjectController.signUpNetworkService.uploadFCMToken(data).await()
                    response.update()
                }
            }
        } catch (ex: Throwable) {
            //  LogException.catchException(ex)
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
        InstallReferralUtil.installReferrer(applicationContext)
        //DatabaseUtils.updateUserMessageSeen()
        AppObjectController.clearDownloadMangerCallback()
        AppAnalytics.updateUser()
        SyncChatService.syncChatWithServer()
        AppObjectController.startSinchCallingService()
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
                } catch (ex: Exception) {
                }
            }
            AppObjectController.appDatabase.videoEngageDao().deleteVideos(syncEngageVideoList)
        }

        return Result.success()
    }
}

class FeedbackRatingWorker(context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result {
        try {
            val response = AppObjectController.commonNetworkService.getFeedbackRatingDetailsAsync()
            PrefManager.put(RATING_DETAILS_KEY, AppObjectController.gsonMapper.toJson(response))
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        return Result.success()
    }
}

class FeedbackStatusForQuestionWorker(
    context: Context,
    private var workerParams: WorkerParameters
) :
    CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result {
        try {
            val questionId = workerParams.inputData.getString("question_id")
            if (questionId.isNullOrEmpty().not()) {
                val response =
                    AppObjectController.commonNetworkService.getQuestionFeedbackStatus(questionId!!)
                if (response.isSuccessful) {
                    val id = response.body()?.submittedData?.id ?: 0
                    var feedbackRequire = response.body()?.feedbackRequire
                    if (id > 0) {
                        feedbackRequire = false
                    }
                    AppObjectController.appDatabase.chatDao()
                        .updateFeedbackStatus(questionId, feedbackRequire)
                }
            }
            val outputData = workDataOf("question_id" to questionId, "status" to 1)
            return Result.success(outputData)
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        return Result.failure()
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
            val details =
                AppObjectController.signUpNetworkService.postDeviceDetails(
                    UpdateDeviceRequest()
                )
            details.update()
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        return Result.success()
    }
}

class PatchDeviceDetailsWorker(context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result {
        try {
            if (Mentor.getInstance().hasId()) {
                val id = DeviceDetailsResponse.getInstance()?.id!!
                val details =
                    AppObjectController.signUpNetworkService.patchDeviceDetails(
                        id,
                        UpdateDeviceRequest()
                    )
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
            AppObjectController.signUpNetworkService.userActive(Mentor.getInstance().getId(), Any())
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
            AppObjectController.chatNetworkService.mergeMentorWithGAId(id.toString(), data)
            PrefManager.removeKey(SERVER_GID_ID)
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
                if (conversationId.isNotBlank()) {

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
            if (Mentor.getInstance().hasId()) {
                val active = workerParams.inputData.getBoolean(IS_ACTIVE, false)
                val data = ActiveUserRequest(Mentor.getInstance().getId(), active)
                AppObjectController.signUpNetworkService.activeUser(data)
            }
        } catch (ex: Exception) {
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
