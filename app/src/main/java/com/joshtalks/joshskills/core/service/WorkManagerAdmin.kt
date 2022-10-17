package com.joshtalks.joshskills.core.service

import android.app.NotificationManager
import android.content.Context
import androidx.work.*
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.core.memory.MemoryManagementWorker
import com.joshtalks.joshskills.core.memory.RemoveMediaWorker
import java.util.*
import java.util.concurrent.TimeUnit

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
        val workerList = mutableListOf(
            OneTimeWorkRequestBuilder<UniqueIdGenerationWorker>().build(),
            OneTimeWorkRequestBuilder<AppRunRequiredTaskWorker>().build(),
            OneTimeWorkRequestBuilder<UpdateServerTimeWorker>().build()
        )
        WorkManager.getInstance(AppObjectController.joshApplication)
            .beginWith(workerList)
            .then(OneTimeWorkRequestBuilder<UpdateDeviceDetailsWorker>().build())
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
            .beginWith(
                mutableListOf(
                    OneTimeWorkRequestBuilder<WorkerAfterLoginInApp>().build(),
                )
            )
            .then(OneTimeWorkRequestBuilder<RegenerateFCMTokenWorker>().build())
            .then(OneTimeWorkRequestBuilder<MergeMentorWithGAIDWorker>().build())
            .then(OneTimeWorkRequestBuilder<JoshTalksInstallWorker>().build())
            .then(OneTimeWorkRequestBuilder<UpdateDeviceDetailsWorker>().build())
            .enqueue()
    }

    fun requiredTaskInLandingPage() {
        WorkManager.getInstance(AppObjectController.joshApplication)
            .beginWith(OneTimeWorkRequestBuilder<WorkerInLandingScreen>().build())
            .then(
                mutableListOf(
                    OneTimeWorkRequestBuilder<SyncEngageVideo>().build(),
                    OneTimeWorkRequestBuilder<FeedbackRatingWorker>().build(),
                    OneTimeWorkRequestBuilder<LogAchievementLevelEventWorker>().build(),
                )
            )
            .then(OneTimeWorkRequestBuilder<CheckFCMTokenInServerWorker>().build())
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

    fun regenerateFCMWorker() {
        WorkManager.getInstance(AppObjectController.joshApplication).enqueue(
            OneTimeWorkRequestBuilder<RegenerateFCMTokenWorker>().build()
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

    fun syncNotificationEngagement() {
        WorkManager.getInstance(AppObjectController.joshApplication)
            .enqueue(OneTimeWorkRequestBuilder<NotificationEngagementSyncWorker>().build())
    }

    fun setBackgroundNotificationWorker() {
        removeBackgroundNotificationWorker()
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val delay = AppObjectController.getFirebaseRemoteConfig().getLong(FirebaseRemoteConfigKey.NOTIFICATION_API_TIME)

        val workRequest = OneTimeWorkRequestBuilder<BackgroundNotificationWorker>()
            .setConstraints(constraints)
            .setInitialDelay(delay, TimeUnit.HOURS)
            .addTag(BackgroundNotificationWorker::class.java.name)
            .build()

        WorkManager.getInstance(AppObjectController.joshApplication).enqueue(workRequest)
    }

    fun removeBackgroundNotificationWorker() {
        WorkManager.getInstance(AppObjectController.joshApplication)
            .cancelAllWorkByTag(BackgroundNotificationWorker::class.java.name)
    }

    fun setStickyNotificationWorker(title: String?, body: String?, coupon: String, expiry: Long) {
        val data = workDataOf(
            "sticky_title" to title,
            "sticky_body" to body,
            "coupon_code" to coupon,
            "expiry_time" to expiry,
        )
        removeStickyNotificationWorker()
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<StickyNotificationWorker>()
            .setInputData(data)
            .setConstraints(constraints)
            .addTag(StickyNotificationWorker::class.java.name)
            .build()

        WorkManager.getInstance(AppObjectController.joshApplication).enqueue(workRequest)
    }

    fun removeStickyNotificationWorker(context: Context? = null) {
        WorkManager.getInstance(AppObjectController.joshApplication)
            .cancelAllWorkByTag(StickyNotificationWorker::class.java.name)
        context?.let {
            val notificationManager = it.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(StickyNotificationWorker.notificationId)
        }
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

    fun setFakeCallNotificationWorker() {
        WorkManager.getInstance(AppObjectController.joshApplication)
            .cancelAllWorkByTag(FakeCallNotificationWorker::class.java.name)
        if (PrefManager.getBoolValue(IS_COURSE_BOUGHT).not() &&
            PrefManager.getLongValue(COURSE_EXPIRY_TIME_IN_MS) != 0L &&
            PrefManager.getLongValue(COURSE_EXPIRY_TIME_IN_MS) < System.currentTimeMillis()
        ) {
            val delay = (10L..45L).random()
            val workRequest = OneTimeWorkRequestBuilder<FakeCallNotificationWorker>()
                .setInputData(workDataOf())
                .setInitialDelay(delay, TimeUnit.SECONDS)
                .addTag(FakeCallNotificationWorker::class.java.name)
                .build()
            WorkManager.getInstance(AppObjectController.joshApplication).enqueue(workRequest)
        }
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
}
