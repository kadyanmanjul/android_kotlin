package com.joshtalks.joshskills.ui.special_practice.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.joshskills.core.showToast
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.repository.server.AmazonPolicyResponse
import com.joshtalks.joshskills.ui.special_practice.model.SaveVideoModel
import com.joshtalks.joshskills.ui.special_practice.model.SpecialPractice
import com.joshtalks.joshskills.ui.special_practice.repo.ViewAndShareRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

class ViewAndShareViewModel (application: Application) :
    AndroidViewModel(application){
    val specialIdData = MutableLiveData<SpecialPractice>()

    fun submitPractise(localPath:String,specialId:Int) {
        var videoUrl = ""
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (localPath.isNullOrEmpty().not()) {
                    val obj = mapOf("media_path" to File(localPath).name)
                    val responseObj =
                        AppObjectController.chatNetworkService.requestUploadMediaAsync(obj).await()
                    val statusCode: Int = uploadOnS3Server(responseObj, localPath)
                    if (statusCode in 200..210) {
                        val url = responseObj.url.plus(File.separator).plus(responseObj.fields["key"])
                        videoUrl = url
                    } else {
                        return@launch
                    }
                }

                val resp = ViewAndShareRepo().saveRecordedVideo(SaveVideoModel(Mentor.getInstance().getId(),videoUrl,specialId))
                if (resp.isSuccessful && resp.body() != null) {
                    showToast("$resp")
                } else {
                    showToast("$resp")
                }
            } catch (ex: Exception) {
                showToast("${ex.message}")

            }
        }
    }

    private suspend fun uploadOnS3Server(
        responseObj: AmazonPolicyResponse,
        mediaPath: String
    ): Int {
        return viewModelScope.async(Dispatchers.IO) {
            val parameters = emptyMap<String, RequestBody>().toMutableMap()
            for (entry in responseObj.fields) {
                parameters[entry.key] = Utils.createPartFromString(entry.value)
            }

            val requestFile = File(mediaPath).asRequestBody("*".toMediaTypeOrNull())
            val body = MultipartBody.Part.createFormData(
                "file",
                responseObj.fields["key"],
                requestFile
            )
            val responseUpload = AppObjectController.mediaDUNetworkService.uploadMediaAsync(
                responseObj.url,
                parameters,
                body
            ).execute()
            return@async responseUpload.code()
        }.await()
    }

    fun getSpecialId(){
        viewModelScope.launch (Dispatchers.IO){
            specialIdData.postValue(AppObjectController.appDatabase.specialDao().getSpecialPracticeFromChatId())
        }
    }
}