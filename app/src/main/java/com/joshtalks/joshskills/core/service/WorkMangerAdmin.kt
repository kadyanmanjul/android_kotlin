package com.joshtalks.joshskills.core.service

import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.repository.local.model.ScreenEngagementModel


const val MEDIA_K_NAME = "kClassName"
const val MEDIA_OBJECT = "media_object"

object WorkMangerAdmin {

    fun installReferrerWorker(){
        WorkManager.getInstance(AppObjectController.joshApplication)
            .enqueue(OneTimeWorkRequestBuilder<JoshTalksInstallWorker>().build())
    }

    fun fineMoreEventWorker() {
        WorkManager.getInstance(AppObjectController.joshApplication)
            .enqueue(OneTimeWorkRequestBuilder<FindMoreEventWorker>().build())
    }
    fun buyNowEventWorker(courseName:String){
        val data = workDataOf("course_name" to courseName)
        val workRequest = OneTimeWorkRequestBuilder<BuyNowEventWorker>()
            .setInputData(data)
            .build()
        WorkManager.getInstance(AppObjectController.joshApplication).enqueue(workRequest)
    }
    fun screenAnalyticsWorker(screenEngagementModel: ScreenEngagementModel){
        val imageData = workDataOf(SCREEN_ENGAGEMENT_OBJECT to AppObjectController.gsonMapper.toJson(screenEngagementModel))
        val uploadWorkRequest = OneTimeWorkRequestBuilder<ScreenEngagementWorker>()
            .setInputData(imageData)
            .build()
        WorkManager.getInstance(AppObjectController.joshApplication).enqueue(uploadWorkRequest)
    }

    fun buyNowImageEventWorker(courseName:String){
        val data = workDataOf("course_name" to courseName)
        val workRequest = OneTimeWorkRequestBuilder<BuyNowImageEventWorker>()
            .setInputData(data)
            .build()
        WorkManager.getInstance(AppObjectController.joshApplication).enqueue(workRequest)
    }

    /* fun startUploadProfileInWorker(imageModel: ImageModel){
         val imageData = workDataOf(MEDIA_OBJECT to AppObjectController.gsonMapper.toJson(imageModel))
         val uploadWorkRequest = OneTimeWorkRequestBuilder<UploadMediaWorker>()
             .setInputData(imageData)
             .build()
         WorkManager.getInstance(AppObjectController.joshApplication).enqueue(uploadWorkRequest)
     }

     fun startDeviceDetailsUpdate(){
         val jobRequest = OneTimeWorkRequestBuilder<UploadDeviceDetailsWorker>()
             .build()
         WorkManager.getInstance(AppObjectController.joshApplication).enqueue(jobRequest)

     }
     fun downloadAudioWorker(listAudioData: List<AudioType>) {

         val imageData = workDataOf(MEDIA_OBJECT to AppObjectController.gsonMapper.toJson(listAudioData))

         val jobRequest = OneTimeWorkRequestBuilder<DownloadAudioFileWorker>()
             .setInputData(imageData)

             .build()
         WorkManager.getInstance(AppObjectController.joshApplication).enqueue(jobRequest)

     }*/

}
