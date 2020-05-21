package com.joshtalks.joshskills.ui.help

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.crashlytics.android.Crashlytics
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.core.io.AppDirectory
import com.joshtalks.joshskills.repository.server.AmazonPolicyResponse
import com.joshtalks.joshskills.repository.server.ComplaintResponse
import com.joshtalks.joshskills.repository.server.RequestComplaint
import com.joshtalks.joshskills.repository.server.TypeOfHelpModel
import id.zelory.compressor.Compressor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.HttpException
import java.io.File
import java.net.SocketTimeoutException
import java.net.UnknownHostException


class HelpViewModel(application: Application) : AndroidViewModel(application) {

    var context: JoshApplication = getApplication()
    val typeOfHelpModelLiveData: MutableLiveData<List<TypeOfHelpModel>> = MutableLiveData()
    val apiCallStatusLiveData: MutableLiveData<ApiCallStatus> = MutableLiveData()
    lateinit var complaintResponse: ComplaintResponse

    fun getAllHelpCategory() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response: List<TypeOfHelpModel> =
                    AppObjectController.commonNetworkService.getHelpCategory()
                typeOfHelpModelLiveData.postValue(response)
            } catch (ex: Exception) {
                when (ex) {
                    is HttpException -> {
                    }
                    is SocketTimeoutException, is UnknownHostException -> {
                        showToast(context.getString(R.string.internet_not_available_msz))
                    }
                    else -> {
                        Crashlytics.logException(ex)
                    }
                }
            }
        }
    }


    fun requestComplaint(requestComplaint: RequestComplaint) {
        viewModelScope.launch(Dispatchers.IO) {
            if (requestComplaint.imageUrl.isNullOrEmpty().not()) {
                val resp = uploadAttachmentMedia(requestComplaint.imageUrl)
                if (resp == null) {
                    apiCallStatusLiveData.postValue(ApiCallStatus.FAILED)
                } else {
                    requestComplaint.imageUrl = resp as String
                }
            }
            try {
                complaintResponse =
                    AppObjectController.commonNetworkService.submitComplaint(requestComplaint)
                apiCallStatusLiveData.postValue(ApiCallStatus.SUCCESS)
            } catch (ex: Exception) {
                apiCallStatusLiveData.postValue(ApiCallStatus.FAILED)
                when (ex) {
                    is HttpException -> {
                    }
                    is SocketTimeoutException, is UnknownHostException -> {
                        showToast(context.getString(R.string.internet_not_available_msz))
                    }
                    else -> {
                        Crashlytics.logException(ex)
                    }
                }
            }
        }

    }


    private suspend fun uploadAttachmentMedia(mediaPath: String?): Any? {
        return viewModelScope.async(Dispatchers.IO) {
            try {

                val obj = mapOf("media_path" to File(getCompressImage(mediaPath!!)).name)
                val responseObj =
                    AppObjectController.chatNetworkService.requestUploadMediaAsync(obj).await()
                val statusCode: Int = uploadOnS3Server(responseObj, mediaPath)
                if (statusCode in 200..210) {
                    return@async responseObj.url.plus(File.separator)
                        .plus(responseObj.fields["key"])
                } else {
                    return@async null
                }
            } catch (ex: Exception) {
                //  Crashlytics.logException(ex)
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
                    Compressor(getApplication()).setQuality(75).setMaxWidth(720).setMaxHeight(
                        1280
                    ).compressToFile(File(path)).absolutePath, path
                )
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
            return@async path
        }.await()
    }


}