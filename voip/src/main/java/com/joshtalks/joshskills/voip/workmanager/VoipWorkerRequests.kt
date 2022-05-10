//package com.joshtalks.joshskills.voip.workmanager
//
//import androidx.work.*
//import com.joshtalks.joshskills.voip.Utils
//
//private const val UPLOAD_ANALYTICS_WORKER_NAME="Upload_Analytics_Api"
//
//object VoipWorkerRequests {
//    fun uploadVoipAnalyticsWorker(){
//        val uploadDataConstraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
//        val workRequest = OneTimeWorkRequestBuilder<UploadAnalyticsWorker>().setConstraints(uploadDataConstraints).build()
//        WorkManager.getInstance(Utils.context!!.applicationContext).enqueueUniqueWork(
//            UPLOAD_ANALYTICS_WORKER_NAME,
//            ExistingWorkPolicy.REPLACE,
//            workRequest
//        )
//    }
//}