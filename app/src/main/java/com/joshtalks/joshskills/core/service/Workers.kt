package com.joshtalks.joshskills.core.service

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.crashlytics.android.Crashlytics
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.identifier.AdvertisingIdClient
import com.google.firebase.database.*
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.repository.local.model.*
import com.joshtalks.joshskills.repository.server.MessageStatusRequest
import com.joshtalks.joshskills.repository.service.NetworkRequestHelper
import java.util.*


const val INSTALL_REFERRER_SYNC = "install_referrer_sync"
const val CONVERSATION_ID = "conversation_id"


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
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
        return Result.success()
    }
}

const val FIND_MORE_FIREBASE_DATABASE = "find_more_event"

class FindMoreEventWorker(context: Context, workerParams: WorkerParameters) :
    Worker(context, workerParams) {

    override fun doWork(): Result {
        val database = FirebaseDatabase.getInstance()
        val myRef = database.getReference(FIND_MORE_FIREBASE_DATABASE)
        myRef.child(System.currentTimeMillis().toString()).setValue(Mentor.getInstance().getId())
        return Result.success()
    }

}


const val BUY_NOW_FIREBASE_DATABASE = "buy_now_event"

class BuyNowEventWorker(context: Context, private val workerParams: WorkerParameters) :
    Worker(context, workerParams) {

    override fun doWork(): Result {
        val courseName = workerParams.inputData.getString("course_name")
        val database = FirebaseDatabase.getInstance()
        val myRef = database.getReference(BUY_NOW_FIREBASE_DATABASE)
        myRef.child(courseName + "_" + System.currentTimeMillis().toString())
            .setValue(Mentor.getInstance().getId())
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
                MobileAds.initialize(
                    context,
                    context.getString(com.joshtalks.joshskills.R.string.ads_id)
                )
                val adInfo = AdvertisingIdClient.getAdvertisingIdInfo(context)
                adInfo.id?.let {
                    PrefManager.put(USER_UNIQUE_ID, it)
                }
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
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
                PrefManager.getLongValue(this).let { time ->
                    if (time > 0) {
                        arguments["created"] = (time / 1000).toString()
                    }
                }
                NetworkRequestHelper.getUpdatedChat(this, queryMap = arguments)
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
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


        } catch (ex: Exception) {
            ex.printStackTrace()
            return Result.retry()

        }
    }

}


class ReferralCodeRefreshWorker(context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result {
        return try {
            if (Mentor.getInstance().referralCode.isEmpty()) {
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
        } catch (ex: Exception) {
            ex.printStackTrace()
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

        val courseName = workerParams.inputData.getString("course_name")
        val courseID = workerParams.inputData.getString("course_id")
        val isBuy = workerParams.inputData.getBoolean("buy_course", false)
        val uniqueId = PrefManager.getStringValue(USER_UNIQUE_ID)
        val buyInitialize = workerParams.inputData.getBoolean("buy_initialize", false)

        val myRef = database.getReference(NEW_COURSE_SCREEN_FIREBASE_DATABASE)
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

                    key.setValue(courseTrackModel)
                } catch (ex: Exception) {
                    ex.printStackTrace()
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                databaseError.toException()
                Crashlytics.logException(databaseError.toException())
            }
        })

        return Result.success()
    }

}













