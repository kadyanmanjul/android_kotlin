package com.joshtalks.badebhaiya.core.workers

import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.joshtalks.badebhaiya.core.AppObjectController

object WorkManagerAdmin {

    fun appInitWorker() {
        WorkManager.getInstance(AppObjectController.joshApplication)
            .beginWith(
                mutableListOf(
/*
                    OneTimeWorkRequestBuilder<UniqueIdGenerationWorker>().build(),
*/
                    OneTimeWorkRequestBuilder<AppRunRequiredTaskWorker>().build()
                )
            ).enqueue()
    }

    fun appStartWorker() {
        WorkManager.getInstance(AppObjectController.joshApplication)
            .beginWith(
                mutableListOf(
/*
                    OneTimeWorkRequestBuilder<UniqueIdGenerationWorker>().build(),
*/
                    OneTimeWorkRequestBuilder<AppRunRequiredTaskWorker>().build()
                )
            )
            .then(
                mutableListOf(
                    OneTimeWorkRequestBuilder<RefreshFCMTokenWorker>().build(),
                    OneTimeWorkRequestBuilder<UpdateDeviceDetailsWorker>().build(),
                )
            )
            .enqueue()
    }

    fun requiredTaskAfterLoginComplete() {
        WorkManager.getInstance(AppObjectController.joshApplication)
            .beginWith(
                mutableListOf(
                    OneTimeWorkRequestBuilder<RefreshFCMTokenWorker>().build(),
                    OneTimeWorkRequestBuilder<JoshTalksInstallWorker>().build(),
                )
            )
            .then(OneTimeWorkRequestBuilder<UpdateDeviceDetailsWorker>().build())
            .enqueue()
    }

    fun requiredTaskInLandingPage() {
        WorkManager.getInstance(AppObjectController.joshApplication)
            .beginWith(OneTimeWorkRequestBuilder<UpdateDeviceDetailsWorker>().build())
            .enqueue()
    }

/*
    fun deviceIdGenerateWorker() {
        WorkManager.getInstance(AppObjectController.joshApplication).enqueueUniqueWork(
            "Unique_id_generate",
            ExistingWorkPolicy.KEEP, (OneTimeWorkRequestBuilder<UniqueIdGenerationWorker>().build())
        )
    }
*/

    fun forceRefreshFcmToken() {
        val workRequest = OneTimeWorkRequestBuilder<RefreshFCMTokenWorker>()
            .build()
        WorkManager.getInstance(AppObjectController.joshApplication).enqueue(workRequest)
    }

}
