package com.joshtalks.joshskills.core.service

import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
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
import com.joshtalks.joshskills.repository.local.model.User
import java.util.UUID
import java.util.concurrent.TimeUnit
import timber.log.Timber

object WorkManagerAdmin {

    fun appInitWorker() {
        WorkManager.getInstance(AppObjectController.joshApplication)
            .beginWith(
                mutableListOf(
                    OneTimeWorkRequestBuilder<UniqueIdGenerationWorker>().build(),
                    OneTimeWorkRequestBuilder<AppRunRequiredTaskWorker>().build()
                )
            ).enqueue()
    }

    fun appStartWorker() {
        WorkManager.getInstance(AppObjectController.joshApplication)
            .beginWith(
                mutableListOf(
                    OneTimeWorkRequestBuilder<UniqueIdGenerationWorker>().build(),
                    OneTimeWorkRequestBuilder<AppRunRequiredTaskWorker>().build()
                )
            )
            .then(
                mutableListOf(
                    OneTimeWorkRequestBuilder<UploadFCMTokenOnServer>().build(),
                    OneTimeWorkRequestBuilder<UpdateDeviceDetailsWorker>().build(),
                    //  OneTimeWorkRequestBuilder<InstanceIdGenerationWorker>().build(),
                )
            )
            /*  .then(
                  OneTimeWorkRequestBuilder<GenerateGuestUserMentorWorker>().build(),
              )*/
            .then(
                mutableListOf(
                    OneTimeWorkRequestBuilder<AppUsageSyncWorker>().build(),
                    OneTimeWorkRequestBuilder<DeleteUnlockTypeQuestion>().build()
                )
            )
            .then(OneTimeWorkRequestBuilder<GenerateRestoreIdWorker>().build())
            .enqueue()
    }

    fun requiredTaskAfterLoginComplete() {
        WorkManager.getInstance(AppObjectController.joshApplication)
            .beginWith(OneTimeWorkRequestBuilder<WorkerAfterLoginInApp>().build())
            // .then(OneTimeWorkRequestBuilder<PatchUserIdToGAIdV2>().build())
            .then(OneTimeWorkRequestBuilder<MergeMentorWithGAIDWorker>().build())
            .then(
                mutableListOf(
                    OneTimeWorkRequestBuilder<UploadFCMTokenOnServer>().build(),
                    OneTimeWorkRequestBuilder<JoshTalksInstallWorker>().build(),
                )
            )
            .then(OneTimeWorkRequestBuilder<UpdateDeviceDetailsWorker>().build())
            .enqueue()
    }

    fun requiredTaskInLandingPage() {
        WorkManager.getInstance(AppObjectController.joshApplication)
            .beginWith(OneTimeWorkRequestBuilder<WorkerInLandingScreen>().build())
            .then(OneTimeWorkRequestBuilder<UserActiveWorker>().build())
            .then(
                mutableListOf(
                    //  OneTimeWorkRequestBuilder<ReferralCodeRefreshWorker>().build(),
                    OneTimeWorkRequestBuilder<SyncEngageVideo>().build(),
                    OneTimeWorkRequestBuilder<FeedbackRatingWorker>().build(),
                    OneTimeWorkRequestBuilder<LogAchievementLevelEventWorker>().build(),
                )
            )
            .then(OneTimeWorkRequestBuilder<UpdateDeviceDetailsWorker>().build())
            .then(OneTimeWorkRequestBuilder<SyncFavoriteCaller>().build())
            .then(OneTimeWorkRequestBuilder<CourseUsageSyncWorker>().build())
            .enqueue()
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

    fun readMessageUpdating() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val workRequest = PeriodicWorkRequest.Builder(
            MessageReadPeriodicWorker::class.java,
            30,
            TimeUnit.MINUTES,
            PeriodicWorkRequest.MIN_PERIODIC_FLEX_MILLIS,
            TimeUnit.MILLISECONDS
        )
            .setConstraints(constraints)
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

    fun syncAppCourseUsage() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val workRequest = PeriodicWorkRequest.Builder(
            CourseUsageSyncWorker::class.java,
            1,
            TimeUnit.HOURS,
            PeriodicWorkRequest.MIN_PERIODIC_FLEX_MILLIS,
            TimeUnit.MILLISECONDS
        )
            .setConstraints(constraints)
            .setInitialDelay(10, TimeUnit.MINUTES)
            .addTag(CourseUsageSyncWorker::class.java.simpleName)
            .build()

        WorkManager.getInstance(AppObjectController.joshApplication)
            .enqueueUniquePeriodicWork(
                CourseUsageSyncWorker::class.java.simpleName,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
    }

    fun refreshFcmToken() {
        val workRequest = PeriodicWorkRequest.Builder(
            RefreshFCMTokenWorker::class.java,
            15,
            TimeUnit.MINUTES,
            PeriodicWorkRequest.MIN_PERIODIC_FLEX_MILLIS,
            TimeUnit.MILLISECONDS
        ).setInitialDelay(1, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(AppObjectController.joshApplication).enqueueUniquePeriodicWork(
            "fcm_refresh",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }

    fun forceRefreshFcmToken() {
        val workRequest = OneTimeWorkRequestBuilder<RefreshFCMTokenWorker>()
            .build()
        WorkManager.getInstance(AppObjectController.joshApplication).enqueue(workRequest)
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

    fun setRepeatingNotificationWorker(notificationIndex: Int) {

        val delay = NOTIFICATION_DELAY.get(notificationIndex)
        val text = NOTIFICATION_TEXT_TEXT.get(notificationIndex)
        var title: String? = null
        if (notificationIndex == 0) {
            title =
                NOTIFICATION_TITLE_TEXT.get(notificationIndex)
                    .replace("%num", (24..78).random().toString())
                    .replace("%name", User.getInstance().firstName.toString())
        } else {
            title =
                NOTIFICATION_TITLE_TEXT.get(notificationIndex)
        }
        Timber.d(
            "Local Notification Set LOCAL_NOTIFICATION_INDEX: ${notificationIndex}"
        )
        val data = workDataOf(NOTIFICATION_TEXT to text, NOTIFICATION_TITLE to title)
        val workRequest = OneTimeWorkRequestBuilder<SetLocalNotificationWorker>()
            .setInputData(data)
            .setInitialDelay(delay, TimeUnit.MINUTES)
            .addTag(SetLocalNotificationWorker::class.java.name)
            .build()

        WorkManager.getInstance(AppObjectController.joshApplication).enqueueUniqueWork(
            "set_notification",
            ExistingWorkPolicy.KEEP,
            workRequest
        )
    }

    fun removeRepeatingNotificationWorker() {
        WorkManager.getInstance(AppObjectController.joshApplication)
            .cancelAllWorkByTag(SetLocalNotificationWorker::class.java.name)
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

    fun syncFavoriteCaller() {
        val workRequest = OneTimeWorkRequestBuilder<SyncFavoriteCaller>()
            .build()
        WorkManager.getInstance(AppObjectController.joshApplication).enqueueUniqueWork(
            "SyncFavoriteCaller_Api",
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }

    fun instanceIdGenerateWorker() {
        val workRequest = OneTimeWorkRequestBuilder<InstanceIdGenerationWorker>()
            .build()
        WorkManager.getInstance(AppObjectController.joshApplication).enqueueUniqueWork(
            "Instance_id_Api",
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }

}
