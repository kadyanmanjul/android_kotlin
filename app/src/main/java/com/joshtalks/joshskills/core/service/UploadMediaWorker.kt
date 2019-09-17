package com.joshtalks.joshskills.core.service

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.gson.reflect.TypeToken
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.repository.local.entity.AudioType
import com.joshtalks.joshskills.repository.local.model.ImageModel
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.repository.local.model.User
import com.joshtalks.joshskills.repository.server.UpdateDeviceRequest
import kotlinx.coroutines.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import java.io.File
import java.util.HashMap
import kotlin.reflect.KClass
import okhttp3.RequestBody
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody


object UploadWorker {

    fun uploadProfile(imageObject: ImageModel) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                var filePart: MultipartBody.Part = createMultipartBody(imageObject.imageLocalPath)
                val uploadProfile: Any =
                    AppObjectController.signUpNetworkService.uploadProfilePicture(User.getInstance().id,filePart)
                AppAnalytics.create(AnalyticsEvent.PROFILE_IMAGE_UPLOAD.NAME)
                    .push()
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    private fun createMultipartBody(filePath: String): MultipartBody.Part {
        val file = File(filePath)
        val requestBody = createRequestBody(file)
        return MultipartBody.Part.createFormData("file", file.name, requestBody)
    }

    private fun createRequestBody(file: File): RequestBody {
        return file.asRequestBody("image/*".toMediaTypeOrNull())
    }
}

class UploadProfileWorker(context: Context, var params: WorkerParameters) :
    CoroutineWorker(context, params) {


    private fun createMultipartBody(filePath: String): MultipartBody.Part {
        val file = File(filePath)
        val requestBody = createRequestBody(file)
        return MultipartBody.Part.createFormData("file", file.name, requestBody)
    }

    private fun createRequestBody(file: File): RequestBody {
        return file.asRequestBody("image/*".toMediaTypeOrNull())
    }

    override suspend fun doWork(): Result = coroutineScope {
        val obj = params.inputData
        val imageObject: ImageModel = AppObjectController.gsonMapper.fromJson(
            obj.getString(MEDIA_OBJECT),
            ImageModel::class.java
        )
        val job = async(Dispatchers.IO) {
            var filePart: MultipartBody.Part = createMultipartBody(imageObject.imageLocalPath)
            val uploadProfile: Any =
                AppObjectController.signUpNetworkService.uploadProfilePicture(User.getInstance().id,filePart)
            Log.e("dd", uploadProfile.toString())


        }

        job.await()
        Result.success()
    }

    companion object {

        @JvmStatic
        private val supportedMediaClass = HashMap<String, KClass<*>>()

        init {
            supportedMediaClass["ImageModel"] = ImageModel::class
        }

    }
}


class UploadMediaWorker(context: Context, var params: WorkerParameters) :
    CoroutineWorker(context, params) {


    override suspend fun doWork(): Result = coroutineScope {
        val obj = params.inputData
        val kClassName = obj.getString(MEDIA_K_NAME)
        val turnsType = object : TypeToken<ImageModel>() {}.type

        //var kClassObj=AppObjectController.gsonMapper.fromJson("",turnsType)

        val job = async {


        }

        job.await()
        Result.success()
    }

    companion object {

        @JvmStatic
        private val supportedMediaClass = HashMap<String, KClass<*>>()

        init {
            supportedMediaClass["ImageModel"] = ImageModel::class
        }

    }

}


class UploadDeviceDetailsWorker(context: Context, var params: WorkerParameters) :
    CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = coroutineScope {

        val job = async {
            if (Mentor.getInstance().hasId()) {
                val updateDetails: Any =
                    AppObjectController.signUpNetworkService.updateDeviceDetails(UpdateDeviceRequest())

            }

        }

        job.await()
        Result.success()
    }
}

class DownloadAudioFileWorker(
    context: Context, var params: WorkerParameters,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {
    override fun doWork(): Result {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val obj = params.inputData

                val typeToken = object : TypeToken<List<AudioType>>() {}.type
                var listOfAudios: List<AudioType> =
                    AppObjectController.gsonMapper.fromJson<List<AudioType>>(
                        obj.getString(MEDIA_OBJECT), typeToken
                    )
                for (audioType in listOfAudios) {
                    val meta: Deferred<Any> =
                        AppObjectController.chatNetworkService.downloadFileAsync(audioType.audio_url)
                }

            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
        return Result.success();
    }

}

