package com.joshtalks.joshskills.ui.userprofile

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.ApiCallStatus
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.IS_PROFILE_FEATURE_ACTIVE
import com.joshtalks.joshskills.core.PrefManager
import com.joshtalks.joshskills.core.USER_SCORE
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.joshskills.core.io.AppDirectory
import com.joshtalks.joshskills.core.showToast
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.repository.local.model.User
import com.joshtalks.joshskills.repository.server.AmazonPolicyResponse
import com.joshtalks.joshskills.repository.server.AwardCategory
import com.joshtalks.joshskills.repository.server.UserProfileResponse
import com.joshtalks.joshskills.util.showAppropriateMsg
import id.zelory.compressor.Compressor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

class UserProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val jobs = arrayListOf<Job>()
    val apiCallStatusLiveData: MutableLiveData<ApiCallStatus> = MutableLiveData()
    val userData: MutableLiveData<UserProfileResponse> = MutableLiveData()
    val userProfileUrl: MutableLiveData<String?> = MutableLiveData()
    val apiCallStatus: MutableLiveData<ApiCallStatus> = MutableLiveData()

    fun getUserProfileUrl() = userProfileUrl.value

    fun uploadMedia(mediaPath: String) {
        viewModelScope.launch(Dispatchers.IO) {
            apiCallStatus.postValue(ApiCallStatus.START)
            val compressImagePath = getCompressImage(mediaPath)
            uploadCompressedMedia(compressImagePath)
        }
    }

    private fun uploadCompressedMedia(
        mediaPath: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val obj = mapOf("media_path" to File(mediaPath).name)
                val responseObj =
                    AppObjectController.chatNetworkService.requestUploadMediaAsync(obj).await()
                val statusCode: Int = uploadOnS3Server(responseObj, mediaPath)
                if (statusCode in 200..210) {
                    val url = responseObj.url.plus(File.separator).plus(responseObj.fields["key"])
                    completingProfile(url)
                } else {
                    apiCallStatus.postValue(ApiCallStatus.FAILED)
                }

            } catch (ex: Exception) {
                apiCallStatus.postValue(ApiCallStatus.FAILED)
                ex.printStackTrace()
            }
        }
    }

    fun completingProfile(map: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val requestMap = mutableMapOf<String, String?>()
                requestMap["photo_url"] = map
                userProfileUrl.postValue(map)
                val response =
                    AppObjectController.signUpNetworkService.updateUserProfile(
                        Mentor.getInstance().getUserId(), requestMap
                    )
                if (response.isSuccessful) {
                    response.body()?.let {
                        it.isVerified = true
                        User.getInstance().updateFromResponse(it)
                    }
                    apiCallStatus.postValue(ApiCallStatus.SUCCESS)
                    return@launch
                } else {
                    apiCallStatus.postValue(ApiCallStatus.FAILED)

                }
            } catch (ex: Throwable) {
                ex.showAppropriateMsg()
                apiCallStatus.postValue(ApiCallStatus.FAILED)
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

    fun getProfileData(mentorId: String) {
        apiCallStatusLiveData.postValue(ApiCallStatus.START)
        jobs += viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = AppObjectController.commonNetworkService.getUserProfileData(mentorId)
                if (response.isSuccessful && response.body() != null) {
                    apiCallStatusLiveData.postValue(ApiCallStatus.SUCCESS)

                    val newList: List<AwardCategory>? =
                        response.body()?.awardCategory?.sortedWith(compareBy { it.sortOrder })
                    newList?.forEach {
                        it.awards = it.awards?.sortedBy { it.sortOrder }
                    }
                    response.body()?.awardCategory = newList
                    userData.postValue(response.body()!!)
                    if (mentorId.equals(Mentor.getInstance().getId()))
                    PrefManager.put(IS_PROFILE_FEATURE_ACTIVE, response.body()?.isPointsActive ?: false
                    )
                    if (mentorId.equals(Mentor.getInstance().getId()))
                        PrefManager.put(USER_SCORE, response.body()!!.points.toString())
                    userProfileUrl.postValue(response.body()!!.photoUrl)
                    return@launch
                } else if (response.errorBody() != null
                    && response.errorBody()!!.string().contains("mentor_id is not valid")
                ) {
                    apiCallStatusLiveData.postValue(ApiCallStatus.FAILED)
                    showToast(AppObjectController.joshApplication.getString(R.string.user_does_not_exist))
                } else {
                    apiCallStatusLiveData.postValue(ApiCallStatus.FAILED)
                    showToast(AppObjectController.joshApplication.getString(R.string.something_went_wrong))
                }

            } catch (ex: Throwable) {
                ex.showAppropriateMsg()
            }
            apiCallStatusLiveData.postValue(ApiCallStatus.FAILED)
        }
    }
}
