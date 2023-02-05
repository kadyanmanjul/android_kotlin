package com.joshtalks.joshskills.premium.ui.help

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.joshtalks.joshskills.premium.core.ApiCallStatus
import com.joshtalks.joshskills.premium.core.AppObjectController
import com.joshtalks.joshskills.premium.core.JoshApplication
import com.joshtalks.joshskills.premium.core.Utils
import com.joshtalks.joshskills.premium.core.io.AppDirectory
import com.joshtalks.joshskills.premium.repository.server.*
import com.joshtalks.joshskills.premium.util.showAppropriateMsg
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import timber.log.Timber
import java.io.File
import id.zelory.compressor.Compressor
import id.zelory.compressor.constraint.quality
import id.zelory.compressor.constraint.resolution


class HelpViewModel(application: Application) : AndroidViewModel(application) {

    var context: JoshApplication = getApplication()
    val faqCategoryLiveData: MutableLiveData<List<FAQCategory>> = MutableLiveData()
    val apiCallStatusLiveData: MutableLiveData<ApiCallStatus> = MutableLiveData()
    val faqListLiveData: MutableLiveData<List<FAQ>> = MutableLiveData()
    private val jobs = arrayListOf<Job>()
    lateinit var complaintResponse: ComplaintResponse
    val apiCallStatusLiveDataComplaint: MutableLiveData<ApiCallStatus> = MutableLiveData()


    fun getAllHelpCategory() {
        jobs += viewModelScope.launch(Dispatchers.IO) {
            try {
                val response =
                    AppObjectController.commonNetworkService.getHelpCategoryV2()
                if (response.isSuccessful) {
                    apiCallStatusLiveData.postValue(ApiCallStatus.SUCCESS)
                    faqCategoryLiveData.postValue(response.body())
                    return@launch
                }

            } catch (ex: Exception) {
                ex.showAppropriateMsg()
            }
            apiCallStatusLiveData.postValue(ApiCallStatus.FAILED)
        }
    }

    fun getFaq() {
        jobs += viewModelScope.launch(Dispatchers.IO) {
            try {
                val response: List<FAQ> =
                    AppObjectController.commonNetworkService.getFaqList()
                faqListLiveData.postValue(response)
            } catch (ex: Exception) {
                ex.showAppropriateMsg()
            }
        }
    }

    fun postFaqFeedback(id: String, boolean: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val requestMap = mutableMapOf<String, String?>()
                if (boolean)
                    requestMap["yes_count"] = "1"
                else
                    requestMap["no_count"] = "1"
                AppObjectController.commonNetworkService.patchFaqFeedback(id, requestMap)
            } catch (ex: Exception) {
                Timber.tag("FAQ Feedback").e(ex)
            }
        }
    }

    fun requestComplaint(requestComplaint: RequestComplaint) {
        viewModelScope.launch(Dispatchers.IO) {
            if (requestComplaint.imageUrl.isNullOrEmpty().not()) {
                val resp = uploadAttachmentMedia(requestComplaint.imageUrl!!)
                if (resp == null) {
                    apiCallStatusLiveDataComplaint.postValue(ApiCallStatus.FAILED)
                    return@launch
                } else {
                    requestComplaint.imageUrl = resp as String
                }

            }
            try {
                val response = AppObjectController.commonNetworkService.submitComplaint(requestComplaint)
                if (response.isSuccessful){
                    complaintResponse = response.body()!!
                    apiCallStatusLiveDataComplaint.postValue(ApiCallStatus.SUCCESS)
                }
            } catch (e: Exception) {
                apiCallStatusLiveDataComplaint.postValue(ApiCallStatus.FAILED)

                e.printStackTrace()
            }

        }

    }


    private suspend fun uploadAttachmentMedia(mediaPath: String): Any? {
        return viewModelScope.async(Dispatchers.IO) {
            try {
                val obj = mapOf("media_path" to File(getCompressImage(mediaPath)).name)
                val responseObj =
                    AppObjectController.chatNetworkService.requestUploadMediaAsync(obj).await()
                val statusCode: Int = uploadOnS3Server(responseObj, mediaPath)
                if (statusCode in 200..210) {
                    val url: String =
                        responseObj.url.plus(File.separator).plus(responseObj.fields["key"])
                    return@async url
                } else {
                    return@async null
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
                apiCallStatusLiveData.postValue(ApiCallStatus.FAILED)
                return@async null
            }
        }.await() as Any

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

    private suspend fun getCompressImage(path: String): String {
        return viewModelScope.async(Dispatchers.IO) {
            try {
                AppDirectory.copy(
                    Compressor.compress(AppObjectController.joshApplication,File(path)){
                        quality(75)
                        resolution(720,1280)}.absolutePath, path
                )
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
            return@async path
        }.await()
    }


    override fun onCleared() {
        super.onCleared()
        jobs.forEach {
            try {
                it.cancel()
            } catch (e : Exception) {
                e.printStackTrace()
            }
        } // cancels the job and waits for its completion
    }
}
