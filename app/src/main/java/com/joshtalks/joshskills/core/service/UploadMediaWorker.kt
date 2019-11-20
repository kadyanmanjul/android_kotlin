package com.joshtalks.joshskills.core.service

import android.content.Context
import android.util.Log
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
                val filePart: MultipartBody.Part = createMultipartBody(imageObject.imageLocalPath)
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