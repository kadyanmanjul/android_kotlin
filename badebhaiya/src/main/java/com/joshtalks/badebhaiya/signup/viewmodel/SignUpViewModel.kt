package com.joshtalks.badebhaiya.signup.viewmodel

import android.app.Application
import android.os.Bundle
import android.util.Log
import androidx.databinding.ObservableBoolean
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.joshtalks.badebhaiya.R
import com.joshtalks.badebhaiya.core.*
import com.joshtalks.badebhaiya.core.io.AppDirectory
import com.joshtalks.badebhaiya.core.workers.WorkManagerAdmin
import com.joshtalks.badebhaiya.feed.model.Users
import com.joshtalks.badebhaiya.impressions.Impression
import com.joshtalks.badebhaiya.profile.ProfileFragment
import com.joshtalks.badebhaiya.profile.ProfileViewModel
import com.joshtalks.badebhaiya.repository.BBRepository
import com.joshtalks.badebhaiya.repository.CommonRepository
import com.joshtalks.badebhaiya.repository.model.User
import com.joshtalks.badebhaiya.repository.server.AmazonPolicyResponse
import com.joshtalks.badebhaiya.repository.service.RetrofitInstance
import com.joshtalks.badebhaiya.signup.request.VerifyOTPRequest
import com.joshtalks.badebhaiya.signup.response.LoginResponse
import com.joshtalks.badebhaiya.utils.TAG
import com.joshtalks.badebhaiya.utils.Utils
import com.truecaller.android.sdk.TrueProfile
import com.truecaller.android.sdk.TruecallerSDK
import id.zelory.compressor.Compressor
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import timber.log.Timber

data class BBtoFollow(
    val userId: String,
)

class SignUpViewModel(application: Application): AndroidViewModel(application) {
    val repository = BBRepository()
    val signUpStatus = MutableLiveData<SignUpStepStatus>()
    var mobileNumber = EMPTY
    val profilePicUploadApiCallStatus = MutableLiveData<ApiCallStatus>()
    var firstName = EMPTY
    var lastName = EMPTY
    var redirect:String=EMPTY
    var valid: ObservableBoolean = ObservableBoolean()
    val openProfile = MutableLiveData<String>()
    private var followedSpeakers = 0
    val isNextEnabled = MutableLiveData<Boolean>(false)

    fun sendPhoneNumberForOTP(phoneNumber: String, countryCode: String) {
        viewModelScope.launch {
            try {
                mobileNumber = phoneNumber
                val reqObj = mapOf("country_code" to countryCode, "mobile" to phoneNumber)
                 var resp=repository.sendPhoneNumberForOTP(reqObj)

               valid.set(resp.code()==500)
                //showToast(resp.toString())


            } catch (ex: Exception) {
                Log.d("sahil", "login via phone exception => ${ex.cause}")
                ex.printStackTrace()
            }
        }
    }

    fun followSpeaker(){
        followedSpeakers++
        if (followedSpeakers > 0){
            isNextEnabled.value = true
        }
    }

    fun unfollowSpeaker(){
        followedSpeakers--
        if(followedSpeakers <= 0){
            isNextEnabled.value = false
        }
    }

    fun openProfile(userId: String){
        openProfile.value = userId
    }



    val bbToFollow: Flow<PagingData<Users>> = Pager(
        config = PagingConfig(pageSize = 12, enablePlaceholders = false),
        pagingSourceFactory = { repository.bbToFollowPaginatedList() }
    )
        .flow
        .cachedIn(viewModelScope)


    fun verifyOTP(otp: String, phoneNumber: String) {
        viewModelScope.launch {
            try {
                val reqObj = VerifyOTPRequest("+91", phoneNumber, otp, Utils.getDeviceId())
                val response = repository.verifyOTP(reqObj)
                Log.i(TAG, "verifyOTP: $response")
                if (response.isSuccessful) {
                    response.body()?.let {
                        PrefManager.put(IS_NEW_USER, it.isUserExist.not())
                        updateUserFromLoginResponse(it)
                    }
                    return@launch
                } else {
                    if (response.code() == 400) {
                        signUpStatus.postValue(SignUpStepStatus.WRONG_OTP)
                        return@launch
                    }
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    private fun updateUserFromLoginResponse(loginResponse: LoginResponse) {
        val user = User.getInstance()
        user.userId = loginResponse.userId
        user.token = loginResponse.token
        PrefManager.put(API_TOKEN, loginResponse.token)
        user.update()
        fetchUser()
    }

    private fun fetchUser() {
        viewModelScope.launch {
            try {
                val response = repository.getUserDetailsForSignUp(User.getInstance().userId)
                if (response.isSuccessful) {
                    response.body()?.let {
                        User.getInstance().updateFromResponse(it)
                        WorkManagerAdmin.requiredTaskAfterLoginComplete()
                        analyzeUserProfile()
                    }
                }
                return@launch
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    private fun analyzeUserProfile() {
        val user = User.getInstance()
        if (user.firstName.isNullOrEmpty()) {
            return signUpStatus.postValue(SignUpStepStatus.NameMissing)
        }
        if (user.profilePicUrl.isNullOrEmpty()) {
            return signUpStatus.postValue(SignUpStepStatus.ProfilePicMissing)
        }
        signUpStatus.postValue(SignUpStepStatus.ProfileCompleted)
    }

    fun completeProfile(requestMap: MutableMap<String, String?>) {
        viewModelScope.launch {
            try {
                val response = repository.updateUserProfile(User.getInstance().userId, requestMap)
                if (response.isSuccessful) {
                    response.body()?.let {
                        User.getInstance().updateFromResponse(it)
                        signUpStatus.postValue(SignUpStepStatus.NameEntered)
                    }
                }
            } catch (ex: Exception) {

            }
        }
    }

    fun changeSignUpStepStatusToSkip() {
        signUpStatus.postValue(SignUpStepStatus.ProfilePicSkipped)
    }

    fun uploadMedia(mediaPath: String) {
        viewModelScope.launch(Dispatchers.IO) {
            profilePicUploadApiCallStatus.postValue(ApiCallStatus.START)
            val compressImagePath = getCompressImage(mediaPath)
            uploadCompressedMedia(compressImagePath)
        }
    }

    private suspend fun getCompressImage(path: String): String {
        return viewModelScope.async(Dispatchers.IO) {
            try {
                AppDirectory.copy(
                    Compressor(getApplication()).setQuality(75).setMaxWidth(720).setMaxHeight(1280)
                        .compressToFile(File(path)).absolutePath, path)
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
            return@async path
        }.await()
    }


    private fun uploadCompressedMedia(mediaPath: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                profilePicUploadApiCallStatus.postValue(ApiCallStatus.START)
                val obj = mapOf("media_path" to File(mediaPath).name)
                val responseObj =
                    CommonRepository().requestUploadMediaAsync(obj).await()
                val statusCode: Int = uploadOnS3Server(responseObj, mediaPath)
                if (statusCode in 200..210) {
                    val url = responseObj.url.plus(File.separator).plus(responseObj.fields["key"])
                    saveProfileInfo(url)
                } else {
                    profilePicUploadApiCallStatus.postValue(ApiCallStatus.FAILED)
                }

            } catch (ex: Exception) {
                profilePicUploadApiCallStatus.postValue(ApiCallStatus.FAILED)
                ex.printStackTrace()
            }
        }
    }

    private suspend fun uploadOnS3Server(responseObj: AmazonPolicyResponse, mediaPath: String): Int {
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
            val responseUpload = RetrofitInstance.mediaDUNetworkService.uploadMediaAsync(
                responseObj.url,
                parameters,
                body
            ).execute()
            return@async responseUpload.code()
        }.await()
    }

    private fun saveProfileInfo(url: String?) {
        viewModelScope.launch {
            try {
                val requestMap = mutableMapOf<String, String?>()
                requestMap["photo_url"] = url
                val response = repository.updateUserProfile(User.getInstance().userId, requestMap)
                if (response.isSuccessful) {
                    response.body()?.let {
                        User.getInstance().updateFromResponse(it)
                        signUpStatus.postValue(SignUpStepStatus.ProfileCompleted)
                    }
                }
            } catch (ex: Exception) {

            }
        }
    }

    fun trueCallerLogin(user: TrueProfile) {
        viewModelScope.launch {
            try {
                val requestTrueUser = mutableMapOf<String, String>()
                requestTrueUser["payload"] = user.payload
                requestTrueUser["signature"] = user.signature
                requestTrueUser["signature_algorithm"] = user.signatureAlgorithm
                requestTrueUser["device_id"] = Utils.getDeviceId()
                val response = repository.trueCallerLogin(requestTrueUser)
                if(response.isSuccessful) {
                    response.body()?.let {
                        firstName = user.firstName
                        lastName = user.lastName
                        updateUserFromLoginResponse(it)
                    }
                }
                else
                    showToast("API Failed")

            }
            catch (ex: Exception) {

            }
        }
    }
}
