package com.joshtalks.joshskills.core.service

import androidx.work.*
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.core.memory.MemoryManagementWorker
import com.joshtalks.joshskills.core.memory.RemoveMediaWorker
import com.joshtalks.joshskills.repository.local.entity.NPSEvent
import java.util.*
import java.util.concurrent.TimeUnit

object WorkManagerAdmin {

    fun appStartWorker() {
        WorkManager.getInstance(AppObjectController.joshApplication)
            .beginWith(
                mutableListOf(
                    OneTimeWorkRequestBuilder<AppRunRequiredTaskWorker>().build(),
                    OneTimeWorkRequestBuilder<UniqueIdGenerationWorker>().build()
                )
            )
            //  .then(OneTimeWorkRequestBuilder<EngageToUseAppNotificationWorker>().build())
            //    .then(OneTimeWorkRequestBuilder<UniqueIdGenerationWorker>().build())
//            .then(OneTimeWorkRequestBuilder<MappingGaIDWithMentor>().build())
            //      .then(OneTimeWorkRequestBuilder<InstanceIdGenerationWorker>().build())
            //     .then(OneTimeWorkRequestBuilder<RegisterUserGAId>().build())
            .then(OneTimeWorkRequestBuilder<GetVersionAndFlowDataWorker>().build())
            //   .then(OneTimeWorkRequestBuilder<GenerateGuestUserMentorWorker>().build())
            .then(
                mutableListOf(
                    OneTimeWorkRequestBuilder<UploadFCMTokenOnServer>().build(),
                    OneTimeWorkRequestBuilder<UpdateDeviceDetailsWorker>().build()
                )
            )
            .then(OneTimeWorkRequestBuilder<AppUsageSyncWorker>().build())
            .then(OneTimeWorkRequestBuilder<GenerateRestoreIdWorker>().build())
            .enqueue()

    }

    fun initGaid(testId: String?, exploreType: String? = null): UUID {
        val data =
            when {
                testId?.isNotBlank() == true -> workDataOf("test_id" to testId)
                exploreType?.isNotBlank() == true -> workDataOf("explore_type" to exploreType)
                else -> workDataOf()
            }
        val workRequest = OneTimeWorkRequestBuilder<RegisterGaidV2>()
            .setInputData(data)
            .build()
        WorkManager.getInstance(AppObjectController.joshApplication).enqueue(workRequest)
        return workRequest.id
    }


    fun requiredTaskAfterLoginComplete() {
        WorkManager.getInstance(AppObjectController.joshApplication)
            .beginWith(OneTimeWorkRequestBuilder<WorkerAfterLoginInApp>().build())
            .then(OneTimeWorkRequestBuilder<RegisterUserGAId>().build())
            .then(OneTimeWorkRequestBuilder<MappingGaIDWithMentor>().build())
            .then(OneTimeWorkRequestBuilder<MergeMentorWithGAIDWorker>().build())
            .then(
                mutableListOf(
                    OneTimeWorkRequestBuilder<UploadFCMTokenOnServer>().build(),
                    OneTimeWorkRequestBuilder<PatchDeviceDetailsWorker>().build(),
                    OneTimeWorkRequestBuilder<JoshTalksInstallWorker>().build(),
                )
            )
            .enqueue()

    }


    fun requiredTaskInLandingPage() {
        WorkManager.getInstance(AppObjectController.joshApplication)
            .beginWith(OneTimeWorkRequestBuilder<WorkerInLandingScreen>().build())
            .then(OneTimeWorkRequestBuilder<UserActiveWorker>().build())
            .then(
                mutableListOf(
                    OneTimeWorkRequestBuilder<ReferralCodeRefreshWorker>().build(),
                    OneTimeWorkRequestBuilder<SyncEngageVideo>().build(),
                    OneTimeWorkRequestBuilder<FeedbackRatingWorker>().build(),
                    OneTimeWorkRequestBuilder<LogAchievementLevelEventWorker>().build(),
                )
            ).enqueue()
    }

    fun syncEngageVideoTask() {
        WorkManager.getInstance(AppObjectController.joshApplication)
            .beginWith(OneTimeWorkRequestBuilder<SyncEngageVideo>().build())
            .then(OneTimeWorkRequestBuilder<LogAchievementLevelEventWorker>().build()).enqueue()
    }

    fun deviceIdGenerateWorker() {
        WorkManager.getInstance(AppObjectController.joshApplication).enqueueUniqueWork(
            "Unique_id_generate",
            ExistingWorkPolicy.KEEP, (OneTimeWorkRequestBuilder<UniqueIdGenerationWorker>().build())
        )
    }

    fun updatedCourseForConversation(conversationId: String) {
        val data = workDataOf(CONVERSATION_ID to conversationId)
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()
        val workRequest = OneTimeWorkRequestBuilder<GetUserConversationWorker>()
            .setInputData(data)
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(AppObjectController.joshApplication).enqueueUniqueWork(
            "Update_Chat",
            ExistingWorkPolicy.APPEND_OR_REPLACE,
            workRequest
        )
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
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
    }

    fun refreshFcmToken() {
        val workRequest = PeriodicWorkRequestBuilder<RefreshFCMTokenWorker>(2, TimeUnit.DAYS)
            .setInitialDelay(5, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(AppObjectController.joshApplication).enqueueUniquePeriodicWork(
            "fcm_refresh",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }


    fun registerUserGAID(testId: String?, exploreType: String? = null) {
        val data =
            when {
                testId?.isNotBlank() == true -> workDataOf("test_id" to testId)
                exploreType?.isNotBlank() == true -> workDataOf("explore_type" to exploreType)
                else -> workDataOf()
            }

        val workRequest = OneTimeWorkRequestBuilder<RegisterUserGAId>()
            .setInputData(data)
            .build()
        WorkManager.getInstance(AppObjectController.joshApplication).enqueue(workRequest)
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

    fun getLanguageChangeWorker(language: String): UUID {
        val data = workDataOf(LANGUAGE_CODE to language)
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val workRequest = OneTimeWorkRequestBuilder<LanguageChangeWorker>()
            .setInputData(data)
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.LINEAR, 11, TimeUnit.SECONDS)
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

    fun clearMediaOfConversation(conversationId: String, isTimeDelete: Boolean = false): UUID {
        val data = workDataOf("conversation_id" to conversationId, "time_delete" to isTimeDelete)
        val workRequest = OneTimeWorkRequestBuilder<RemoveMediaWorker>()
            .setInputData(data)
            .build()
        WorkManager.getInstance(AppObjectController.joshApplication).enqueue(workRequest)
        return workRequest.id
    }

    fun runMemoryManagementWorker() {
        val workRequest = PeriodicWorkRequestBuilder<MemoryManagementWorker>(24, TimeUnit.HOURS)
            .addTag("cleanup")
            .build()
        WorkManager.getInstance(AppObjectController.joshApplication).enqueueUniquePeriodicWork(
            "memoryManagerWorker",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }


    fun deleteUnlockTypeQuestions() {
        val workRequest = OneTimeWorkRequestBuilder<DeleteUnlockTypeQuestion>()
            .addTag(DeleteUnlockTypeQuestion::class.java.simpleName)
            .build()

        WorkManager.getInstance(AppObjectController.joshApplication)
            .enqueue(workRequest)
    }

    fun userActiveStatusWorker(status: Boolean) {
        val data = workDataOf(IS_ACTIVE to status)
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val workRequest = OneTimeWorkRequestBuilder<IsUserActiveWorker>()
            .setInputData(data)
            .setInitialDelay(1, TimeUnit.SECONDS)
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(AppObjectController.joshApplication).enqueueUniqueWork(
            "Active_Api",
            ExistingWorkPolicy.KEEP,
            workRequest
        )
    }

    fun startVersionAndFlowWorker() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val workRequest = OneTimeWorkRequestBuilder<GetVersionAndFlowDataWorker>()
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(AppObjectController.joshApplication).enqueue(workRequest)
    }

    fun userAppUsage(status: Boolean) {
        val data = workDataOf(IS_ACTIVE to status)
        val constraints =
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                Constraints.Builder()
                    .setRequiresDeviceIdle(false)
                    .build()
            } else {
                Constraints.Builder()
                    .build()
            }
        val workRequest = OneTimeWorkRequestBuilder<AppUsageWorker>()
            .setInputData(data)
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(AppObjectController.joshApplication).enqueueUniqueWork(
            "AppUsage_Api",
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }
}
