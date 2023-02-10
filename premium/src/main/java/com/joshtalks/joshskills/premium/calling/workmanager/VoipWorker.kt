//package com.joshtalks.joshskills.voip.workmanager
//
//import android.content.Context
//import androidx.work.CoroutineWorker
//import androidx.work.WorkerParameters
//import com.joshtalks.joshskills.premium.calling.voipanalytics.CallAnalytics
//
//
//class UploadAnalyticsWorker(context: Context, workerParams: WorkerParameters) : CoroutineWorker(context, workerParams)  {
//    override suspend fun doWork(): Result {
//        uploadAnalyticsToServer()
//        return Result.success()
//    }
//
//    fun uploadAnalyticsToServer() {
//        CallAnalytics.uploadAnalyticsToServer()
//    }
//}