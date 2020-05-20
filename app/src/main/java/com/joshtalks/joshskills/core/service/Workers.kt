package com.joshtalks.joshskills.core.service

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.identifier.AdvertisingIdClient
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.database.*
import com.google.firebase.iid.FirebaseInstanceId
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.core.analytics.LogException
import com.joshtalks.joshskills.core.notification.FCM_TOKEN
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.DatabaseUtils
import com.joshtalks.joshskills.repository.local.entity.NPSEvent
import com.joshtalks.joshskills.repository.local.entity.NPSEventModel
import com.joshtalks.joshskills.repository.local.eventbus.NPSEventGenerateEventBus
import com.joshtalks.joshskills.repository.local.model.*
import com.joshtalks.joshskills.repository.server.MessageStatusRequest
import com.joshtalks.joshskills.repository.server.UpdateDeviceRequest
import com.joshtalks.joshskills.repository.service.NetworkRequestHelper
import com.joshtalks.joshskills.repository.service.SyncChatService
import io.branch.referral.Branch
import retrofit2.HttpException
import java.util.*

const val INSTALL_REFERRER_SYNC = "install_referrer_sync"
const val CONVERSATION_ID = "conversation_id"

class AppRunRequiredTaskWorker(context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result {
        AppAnalytics.flush()
        AppObjectController.facebookEventLogger.flush()
        AppObjectController.firebaseAnalytics.resetAnalyticsData()
        AppObjectController.getFetchObject().awaitFinish()
        WorkMangerAdmin.readMessageUpdating()
        AppObjectController.getFirebaseRemoteConfig().fetchAndActivate().addOnCompleteListener {
            val npsEvent =
                AppObjectController.getFirebaseRemoteConfig().getString("NPS_EVENT_LIST")
            NPSEventModel.setNPSList(npsEvent)
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

const val FIND_MORE_FIREBASE_DATABASE = "find_more_event"

class FindMoreEventWorker(context: Context, workerParams: WorkerParameters) :
    Worker(context, workerParams) {

    override fun doWork(): Result {
        try {
            val database = FirebaseDatabase.getInstance()
            val myRef = database.getReference(FIND_MORE_FIREBASE_DATABASE)
            myRef.child(System.currentTimeMillis().toString())
                .setValue(Mentor.getInstance().getId())
        } catch (ex: Throwable) {
            LogException.catchException(ex)
        }
        return Result.success()
    }

}


const val BUY_NOW_FIREBASE_DATABASE = "buy_now_event"

class BuyNowEventWorker(context: Context, private val workerParams: WorkerParameters) :
    Worker(context, workerParams) {

    override fun doWork(): Result {
        try {
            val courseName = workerParams.inputData.getString("course_name")
            val database = FirebaseDatabase.getInstance()
            val myRef = database.getReference(BUY_NOW_FIREBASE_DATABASE)
            myRef.child(courseName + "_" + System.currentTimeMillis().toString())
                .setValue(Mentor.getInstance().getId())
        } catch (ex: Throwable) {
            LogException.catchException(ex)
        }
        return Result.success()
    }

}

const val BUY_IMAGE_NOW_FIREBASE_DATABASE = "course_image_select_event"

class BuyNowImageEventWorker(context: Context, private val workerParams: WorkerParameters) :
    Worker(context, workerParams) {

    override fun doWork(): Result {
        val courseName = workerParams.inputData.getString("course_name")
        val database = FirebaseDatabase.getInstance()
        val myRef = database.getReference(BUY_IMAGE_NOW_FIREBASE_DATABASE)
        myRef.child(courseName + "_" + Date().time.toString())
            .setValue(Mentor.getInstance().getId())
        return Result.success()
    }

}


const val SCREEN_ENGAGEMENT_OBJECT = "screen_engagement"
const val SCREEN_ENGAGEMENT_FIREBASE_DATABASE = "screen_engagement"

class ScreenEngagementWorker(context: Context, private val workerParams: WorkerParameters) :
    Worker(context, workerParams) {

    override fun doWork(): Result {
        val obj = AppObjectController.gsonMapperForLocal.fromJson(
            workerParams.inputData.getString(SCREEN_ENGAGEMENT_OBJECT),
            ScreenEngagementModel::class.java
        )
        val database = FirebaseDatabase.getInstance()
        val ref: DatabaseReference = database.getReference(SCREEN_ENGAGEMENT_FIREBASE_DATABASE)
        val postsRef: DatabaseReference = ref.child(Mentor.getInstance().getId())
        obj.totalSpendTime = obj.endTime - obj.startTime
        postsRef.push().setValue(obj)
        return Result.success()
    }

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


const val REFERRAL_EVENT_OBJECT = "referral_event_object"

class ReferralEventWorker(context: Context, private val workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result {
        val obj = AppObjectController.gsonMapperForLocal.fromJson(
            workerParams.inputData.getString(REFERRAL_EVENT_OBJECT),
            REFERRAL_EVENT::class.java
        )
        val database = FirebaseDatabase.getInstance()
        val myRef = database.getReference("REFERRAL_EVENT")
        myRef.child(obj.type).child(Date().time.toString())
            .setValue(Mentor.getInstance().getId())
        return Result.success()
    }

}


const val NEW_COURSE_SCREEN_FIREBASE_DATABASE = "new_course_screen_event"

class NewCourseScreenEventWorker(context: Context, private val workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {

        val database = FirebaseDatabase.getInstance()
        val myRef = database.getReference(NEW_COURSE_SCREEN_FIREBASE_DATABASE)

        val courseName = workerParams.inputData.getString("course_name")
        val courseID = workerParams.inputData.getString("course_id")
        val isBuy = workerParams.inputData.getBoolean("buy_course", false)
        val uniqueId = PrefManager.getStringValue(USER_UNIQUE_ID)
        val buyInitialize = workerParams.inputData.getBoolean("buy_initialize", false)

        val key: DatabaseReference = if (myRef.child(uniqueId).key == null) {
            myRef.child(uniqueId).push()
        } else {
            myRef.child(uniqueId)
        }

        key.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                try {
                    val courseTrackModel: CourseTrackModel? = if (dataSnapshot.value != null) {
                        dataSnapshot.getValue(CourseTrackModel::class.java)
                    } else {
                        CourseTrackModel(uniqueId = uniqueId)
                    }
                    courseTrackModel?.mentorId = Mentor.getInstance().getId()
                    courseTrackModel?.mobileNumber = User.getInstance().phoneNumber

                    when {
                        isBuy -> {
                            courseTrackModel?.courseBuy?.add(
                                CourseDetailModel(
                                    courseName = courseName ?: EMPTY, courseId = courseID ?: EMPTY
                                )
                            )
                        }
                        buyInitialize -> {
                            courseTrackModel?.courseBuyInitialize?.add(
                                CourseDetailModel(
                                    courseName = courseName ?: EMPTY, courseId = courseID ?: EMPTY
                                )
                            )
                        }
                        else -> {
                            courseTrackModel?.courseWatch?.add(
                                CourseDetailModel(
                                    courseName = courseName ?: EMPTY, courseId = courseID ?: EMPTY
                                )
                            )
                        }
                    }

                    key.setValue(courseTrackModel).addOnFailureListener {
                        it.printStackTrace()
                    }
                } catch (ex: Throwable) {
                    LogException.catchException(ex)
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                databaseError.toException().printStackTrace()
            }
        })

        return Result.success()
    }

}

class RegisterUserGId(context: Context, private val workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result {
        try {
            val requestRegisterGId = RequestRegisterGId()
            requestRegisterGId.gaid = PrefManager.getStringValue(USER_UNIQUE_ID)
            requestRegisterGId.installOn =
                InstallReferrerModel.getPrefObject()?.installOn ?: Date().time
            requestRegisterGId.test =
                workerParams.inputData.getString("test_id")?.split("_")?.get(1)?.toInt() ?: 0
            requestRegisterGId.utmMedium = InstallReferrerModel.getPrefObject()?.utmMedium ?: EMPTY
            requestRegisterGId.utmSource = InstallReferrerModel.getPrefObject()?.utmSource ?: EMPTY
            val resp =
                AppObjectController.commonNetworkService.registerGAIdAsync(requestRegisterGId)
                    .await()
            PrefManager.put(SERVER_GID_ID, resp.id)
            PrefManager.put(GID_SET_FOR_USER, true)
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
        DatabaseUtils.updateUserMessageSeen()
        AppObjectController.clearDownloadMangerCallback()
        AppAnalytics.updateUser()
        SyncChatService.syncChatWithServer()
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
            if (Mentor.getInstance().hasId()) {
                /*  val id = DeviceDetailsResponse.getInstance()?.id!!
                  val details =
                      AppObjectController.signUpNetworkService.patchDeviceDetails(
                          id,
                          UpdateDeviceRequest()
                      )
                  details.update()
              } else {*/
                val details =
                    AppObjectController.signUpNetworkService.postDeviceDetails(UpdateDeviceRequest())
                details.update()
            }
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
            AppObjectController.chatNetworkService.mergeMentorWithGId(id.toString(), data)
            PrefManager.removeKey(SERVER_GID_ID)
        } catch (ex: Throwable) {
            LogException.catchException(ex)
        }
        return Result.success()
    }
}


fun getGoogleAdId(context: Context): String {
    MobileAds.initialize(context)
    val adInfo = AdvertisingIdClient.getAdvertisingIdInfo(context)
    return adInfo.id
}

