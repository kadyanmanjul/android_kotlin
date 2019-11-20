package com.joshtalks.joshskills.core.service

import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.repository.local.entity.AudioType
import com.joshtalks.joshskills.repository.local.model.ImageModel

const val MEDIA_K_NAME="kClassName"
const val MEDIA_OBJECT="media_object"

object WorkMangerPapa {


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
