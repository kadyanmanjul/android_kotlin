package com.joshtalks.joshskills.core.service

import androidx.work.*
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.GID_SET_FOR_USER
import com.joshtalks.joshskills.core.PrefManager
import com.joshtalks.joshskills.core.REFERRAL_EVENT
import com.joshtalks.joshskills.repository.local.model.ScreenEngagementModel
import java.util.concurrent.TimeUnit


object WorkMangerAdmin {

    fun appStartWorker(){
        WorkManager.getInstance(AppObjectController.joshApplication)
            .enqueue(OneTimeWorkRequestBuilder<AppRunRequiredTaskWorker>().build())
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


    fun mappingGIDWithMentor(){
        WorkManager.getInstance(AppObjectController.joshApplication)
            .enqueue(OneTimeWorkRequestBuilder<MappingGaIDWithMentor>().build())
    }

}
