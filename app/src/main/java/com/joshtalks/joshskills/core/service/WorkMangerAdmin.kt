package com.joshtalks.joshskills.core.service

import androidx.work.*
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.repository.local.entity.NPSEvent
import com.joshtalks.joshskills.repository.local.model.ScreenEngagementModel
import java.util.*
import java.util.concurrent.TimeUnit

object WorkMangerAdmin {

    fun appStartWorker() {

        WorkManager.getInstance(AppObjectController.joshApplication)
            .beginWith(
                mutableListOf(
                    OneTimeWorkRequestBuilder<AppRunRequiredTaskWorker>().build(),
                    OneTimeWorkRequestBuilder<RefreshFCMTokenWorker>().build()
                )
            )
            .then(OneTimeWorkRequestBuilder<InstanceIdGenerationWorker>().build())
            .then(OneTimeWorkRequestBuilder<UniqueIdGenerationWorker>().build())
            .then(OneTimeWorkRequestBuilder<MappingGaIDWithMentor>().build())
            .then(
                mutableListOf(
                    OneTimeWorkRequestBuilder<UploadFCMTokenOnServer>().build(),
                    OneTimeWorkRequestBuilder<UpdateDeviceDetailsWorker>().build()
                )
            )
            .then(OneTimeWorkRequestBuilder<GenerateRestoreIdWorker>().build())
            .enqueue()

    }

    fun requiredTaskAfterLoginComplete() {
        WorkManager.getInstance(AppObjectController.joshApplication)
            .beginWith(OneTimeWorkRequestBuilder<WorkerAfterLoginInApp>().build())
            .then(OneTimeWorkRequestBuilder<MappingGaIDWithMentor>().build())
            .then(OneTimeWorkRequestBuilder<MergeMentorWithGAIDWorker>().build())
            .then(
                mutableListOf(
                    OneTimeWorkRequestBuilder<UploadFCMTokenOnServer>().build(),
                    OneTimeWorkRequestBuilder<UpdateDeviceDetailsWorker>().build(),
                    OneTimeWorkRequestBuilder<JoshTalksInstallWorker>().build()

                )
            ).enqueue()
    }


    fun requiredTaskInLandingPage() {
        WorkManager.getInstance(AppObjectController.joshApplication)
            .beginWith(OneTimeWorkRequestBuilder<WorkerInLandingScreen>().build())
            .then(
                mutableListOf(
                    OneTimeWorkRequestBuilder<UserActiveWorker>().build(),
                    OneTimeWorkRequestBuilder<ReferralCodeRefreshWorker>().build(),
                    OneTimeWorkRequestBuilder<SyncEngageVideo>().build(),
                    OneTimeWorkRequestBuilder<FeedbackRatingWorker>().build()
                )
            ).enqueue()
    }

    fun installReferrerWorker() {
        WorkManager.getInstance(AppObjectController.joshApplication)
            .enqueue(OneTimeWorkRequestBuilder<JoshTalksInstallWorker>().build())
    }

    fun fineMoreEventWorker() {
        WorkManager.getInstance(AppObjectController.joshApplication)
            .enqueue(OneTimeWorkRequestBuilder<FindMoreEventWorker>().build())
    }

    fun buyNowEventWorker(courseName: String) {
        val data = workDataOf("course_name" to courseName)
        val workRequest = OneTimeWorkRequestBuilder<BuyNowEventWorker>()
            .setInputData(data)
            .build()
        WorkManager.getInstance(AppObjectController.joshApplication).enqueue(workRequest)
    }

    fun screenAnalyticsWorker(screenEngagementModel: ScreenEngagementModel) {
        val imageData = workDataOf(
            SCREEN_ENGAGEMENT_OBJECT to AppObjectController.gsonMapper.toJson(screenEngagementModel)
        )
        val uploadWorkRequest = OneTimeWorkRequestBuilder<ScreenEngagementWorker>()
            .setInputData(imageData)
            .build()
        WorkManager.getInstance(AppObjectController.joshApplication).enqueue(uploadWorkRequest)
    }

    fun buyNowImageEventWorker(courseName: String) {
        val data = workDataOf("course_name" to courseName)
        val workRequest = OneTimeWorkRequestBuilder<BuyNowImageEventWorker>()
            .setInputData(data)
            .build()
        WorkManager.getInstance(AppObjectController.joshApplication).enqueue(workRequest)
    }

    fun deviceIdGenerateWorker() {
        WorkManager.getInstance(AppObjectController.joshApplication)
            .enqueue(OneTimeWorkRequestBuilder<UniqueIdGenerationWorker>().build())
    }

    fun updatedCourseForConversation(conversationId: String) {
        val data = workDataOf(CONVERSATION_ID to conversationId)
        val workRequest = OneTimeWorkRequestBuilder<GetUserConversationWorker>()
            .setInputData(data)
            .build()
        WorkManager.getInstance(AppObjectController.joshApplication).enqueue(workRequest)
    }

    fun readMessageUpdating() {

        val workRequest = PeriodicWorkRequest.Builder(
            MessageReadPeriodicWorker::class.java,
            30,
            TimeUnit.MINUTES,
            PeriodicWorkRequest.MIN_PERIODIC_FLEX_MILLIS,
            TimeUnit.MILLISECONDS
        )
            .setInitialDelay(1, TimeUnit.MINUTES)
            .addTag(MessageReadPeriodicWorker::class.java.simpleName)
            .build()


        WorkManager.getInstance(AppObjectController.joshApplication)
            .enqueueUniquePeriodicWork(
                MessageReadPeriodicWorker::class.java.simpleName,
                ExistingPeriodicWorkPolicy.REPLACE,
                workRequest
            )
    }

    fun getUserReferralCodeWorker() {
        WorkManager.getInstance(AppObjectController.joshApplication)
            .enqueue(OneTimeWorkRequestBuilder<ReferralCodeRefreshWorker>().build())
    }


    fun referralEventTracker(event: REFERRAL_EVENT) {
        val data = workDataOf(
            REFERRAL_EVENT_OBJECT to AppObjectController.gsonMapper.toJson(event)
        )
        val workRequest = OneTimeWorkRequestBuilder<ReferralEventWorker>()
            .setInputData(data)
            .build()
        WorkManager.getInstance(AppObjectController.joshApplication).enqueue(workRequest)
    }


    fun newCourseScreenEventWorker(
        courseName: String?,
        courseId: String?,
        buyInitialize: Boolean = false,
        buyCourse: Boolean = false
    ) {
        val data = workDataOf(
            "course_name" to courseName,
            "course_id" to courseId,
            "buy_course" to buyCourse,
            "buy_initialize" to buyInitialize
        )
        val workRequest = OneTimeWorkRequestBuilder<NewCourseScreenEventWorker>()
            .setInputData(data)
            .build()
        WorkManager.getInstance(AppObjectController.joshApplication).enqueue(workRequest)
    }

    fun registerUserGIDWithTestId(testId: String) {
        if (testId.isEmpty() || PrefManager.getBoolValue(GID_SET_FOR_USER)) {
            return
        }
        val data = workDataOf("test_id" to testId)
        val workRequest = OneTimeWorkRequestBuilder<RegisterUserGId>()
            .setInputData(data)
            .build()
        WorkManager.getInstance(AppObjectController.joshApplication).enqueue(workRequest)
    }

    fun mappingGIDWithMentor() {
        WorkManager.getInstance(AppObjectController.joshApplication)
            .enqueue(OneTimeWorkRequestBuilder<MappingGaIDWithMentor>().build())
    }

    fun refreshFCMToken() {
        WorkManager.getInstance(AppObjectController.joshApplication)
            .enqueue(OneTimeWorkRequestBuilder<RefreshFCMTokenWorker>().build())
    }

    fun pushTokenToServer() {
        WorkManager.getInstance(AppObjectController.joshApplication)
            .enqueue(OneTimeWorkRequestBuilder<UploadFCMTokenOnServer>().build())
    }


    fun syncVideoEngage() {
        WorkManager.getInstance(AppObjectController.joshApplication)
            .enqueue(OneTimeWorkRequestBuilder<SyncEngageVideo>().build())
    }

    fun fetchFeedbackRating() {
        WorkManager.getInstance(AppObjectController.joshApplication)
            .enqueue(OneTimeWorkRequestBuilder<FeedbackRatingWorker>().build())
    }

    fun getQuestionFeedback(questionId: String): UUID {
        val data = workDataOf("question_id" to questionId)
        val workRequest = OneTimeWorkRequestBuilder<FeedbackStatusForQuestionWorker>()
            .setInputData(data)
            .build()
        WorkManager.getInstance(AppObjectController.joshApplication).enqueue(workRequest)
        return workRequest.id
    }

    fun getQuestionNPA(eventName: String): UUID {
        val data = workDataOf("event" to eventName)
        val workRequest = OneTimeWorkRequestBuilder<NPAQuestionViaEventWorker>()
            .setInputData(data)
            .build()
        WorkManager.getInstance(AppObjectController.joshApplication).enqueue(workRequest)
        return workRequest.id
    }

    fun determineNPAEvent(
        event: NPSEvent = NPSEvent.STANDARD_TIME_EVENT,
        interval: Int = -1,
        id: String? = EMPTY
    ) {
        val data =
            workDataOf(
                "event" to AppObjectController.gsonMapper.toJson(event),
                "day" to interval,
                "id" to id
            )
        val workRequest = OneTimeWorkRequestBuilder<DeterminedNPSEvent>()
            .setInputData(data)
            .build()
        WorkManager.getInstance(AppObjectController.joshApplication).enqueue(workRequest)
    }

}
