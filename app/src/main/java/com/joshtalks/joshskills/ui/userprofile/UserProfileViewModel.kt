package com.joshtalks.joshskills.ui.userprofile

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.ApiCallStatus
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.core.IS_PROFILE_FEATURE_ACTIVE
import com.joshtalks.joshskills.core.JoshApplication
import com.joshtalks.joshskills.core.PrefManager
import com.joshtalks.joshskills.core.USER_SCORE
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.joshskills.core.io.AppDirectory
import com.joshtalks.joshskills.core.showToast
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.eventbus.SaveProfileClickedEvent
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.repository.local.model.User
import com.joshtalks.joshskills.repository.server.AmazonPolicyResponse
import com.joshtalks.joshskills.repository.server.AnimatedLeaderBoardResponse
import com.joshtalks.joshskills.repository.server.AwardCategory
import com.joshtalks.joshskills.repository.server.PreviousProfilePictures
import com.joshtalks.joshskills.repository.server.UserProfileResponse
import com.joshtalks.joshskills.util.showAppropriateMsg
import id.zelory.compressor.Compressor
import java.io.File
import java.util.ArrayList
import java.util.HashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody

class UserProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val jobs = arrayListOf<Job>()
    val apiCallStatusLiveData: MutableLiveData<ApiCallStatus> = MutableLiveData()
    val userData: MutableLiveData<UserProfileResponse> = MutableLiveData()
    val userProfileUrl: MutableLiveData<String?> = MutableLiveData()
    val apiCallStatus: MutableLiveData<ApiCallStatus> = MutableLiveData()
    val animatedLeaderBoardData: MutableLiveData<AnimatedLeaderBoardResponse> = MutableLiveData()
    val previousProfilePics: MutableLiveData<PreviousProfilePictures> = MutableLiveData()

    var context: JoshApplication = getApplication()

    fun getMentorData(mentorId: String) {
        jobs += viewModelScope.launch(Dispatchers.IO) {
            try {
                val response =
                    AppObjectController.commonNetworkService.getAnimatedLeaderBoardData(mentorId)
                if (response.isSuccessful && response.body() != null) {
                    animatedLeaderBoardData.postValue(response.body())
                }

            } catch (ex: Throwable) {
                ex.showAppropriateMsg()
            }
        }
    }

    fun patchAwardDetails(awardIds: ArrayList<Int>) {
        jobs += viewModelScope.launch(Dispatchers.IO) {
            try {
                val extras: HashMap<String, List<Int>> = HashMap()
                extras["award_mentor_list"] = awardIds
                AppObjectController.commonNetworkService.patchAwardDetails(extras)

            } catch (ex: Exception) {
                //ex.showAppropriateMsg()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        jobs.forEach {
            try {
                it.cancel()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

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
                    saveProfileInfo(url)
                } else {
                    apiCallStatus.postValue(ApiCallStatus.FAILED)
                }

            } catch (ex: Exception) {
                apiCallStatus.postValue(ApiCallStatus.FAILED)
                ex.printStackTrace()
            }
        }
    }

    fun saveProfileInfo(
        profilePicUrl: String?,
        newName: String = EMPTY,
        dobStr: String = EMPTY,
        homeTown: String = EMPTY,
        isSaveBtnClicked: Boolean = false
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                apiCallStatus.postValue(ApiCallStatus.START)
                val requestMap = mutableMapOf<String, String?>()
                requestMap["photo_url"] = profilePicUrl
                if (newName.isNotEmpty()) {
                    requestMap["first_name"] = newName
                }
                if (dobStr.isNotEmpty()) {
                    requestMap["date_of_birth"] = dobStr
                }
                if (homeTown.isNotEmpty()) {
                    requestMap["hometown"] = homeTown
                }
                val response =
                    AppObjectController.signUpNetworkService.updateUserProfile(
                        Mentor.getInstance().getUserId(), requestMap
                    )
                if (response.isSuccessful) {
                    response.body()?.let {
                        userProfileUrl.postValue(profilePicUrl)
                        if (isSaveBtnClicked) {
                            RxBus2.publish(SaveProfileClickedEvent(true))
                        }
                        it.isVerified = User.getInstance().isVerified
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

    fun getProfileData(mentorId: String, intervalType: String?, previousPage: String?) {
        apiCallStatusLiveData.postValue(ApiCallStatus.START)
        jobs += viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = AppObjectController.commonNetworkService.getUserProfileDataV3(
                    mentorId,
                    intervalType,
                    previousPage
                )
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
                        PrefManager.put(
                            IS_PROFILE_FEATURE_ACTIVE, response.body()?.isPointsActive ?: false
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

    fun engageUserProfileTime(impressionId: String, startTime: Long) {
        jobs += viewModelScope.launch(Dispatchers.IO) {
            try {
                if (impressionId.isNullOrBlank())
                    return@launch

                AppObjectController.commonNetworkService.engageUserProfileTime(
                    impressionId,
                    mapOf("time_spent" to startTime)
                )

            } catch (ex: Throwable) {
                ex.printStackTrace()
            }
        }
    }

    fun getUrlFor3DWebView(awardMentorId: String) {
        jobs += viewModelScope.launch(Dispatchers.IO) {
            try {
                if (awardMentorId.isBlank())
                    return@launch

                val response = AppObjectController.commonNetworkService.get3DWebView(awardMentorId)

            } catch (ex: Throwable) {
                ex.printStackTrace()
            }
        }
    }

    fun getPreviousProfilePics() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                apiCallStatus.postValue(ApiCallStatus.START)
                val response =
                    AppObjectController.signUpNetworkService.getPreviousProfilePics()
                if (response.isSuccessful) {
                    response.body()?.let {
                        previousProfilePics.postValue(it)
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

}
