package com.joshtalks.joshskills.core.service

import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.core.memory.MemoryManagementWorker
import com.joshtalks.joshskills.core.memory.RemoveMediaWorker
import com.joshtalks.joshskills.repository.local.entity.NPSEvent
import java.util.UUID
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
            .then(OneTimeWorkRequestBuilder<UniqueIdGenerationWorker>().build())
            .then(OneTimeWorkRequestBuilder<MappingGaIDWithMentor>().build())
            .then(OneTimeWorkRequestBuilder<InstanceIdGenerationWorker>().build())
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
            .then(OneTimeWorkRequestBuilder<RegisterUserGAId>().build())
            .then(OneTimeWorkRequestBuilder<MappingGaIDWithMentor>().build())
            .then(OneTimeWorkRequestBuilder<MergeMentorWithGAIDWorker>().build())
            .then(
                mutableListOf(
                    OneTimeWorkRequestBuilder<UploadFCMTokenOnServer>().build(),
                    OneTimeWorkRequestBuilder<PatchDeviceDetailsWorker>().build(),
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
        WorkManager.getInstance(AppObjectController.joshApplication).enqueue(workRequest)

    }


    fun deleteUnlockTypeQuestions() {
        val workRequest = OneTimeWorkRequestBuilder<DeleteUnlockTypeQuestion>()
            .addTag(DeleteUnlockTypeQuestion::class.java.simpleName)
            .build()

        WorkManager.getInstance(AppObjectController.joshApplication)
            .enqueue(workRequest)
    }

}
