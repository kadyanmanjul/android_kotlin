package com.joshtalks.joshskills.ui.signup

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.github.razir.progressbutton.hideProgress
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.core.analytics.MarketingAnalytics
import com.joshtalks.joshskills.core.service.WorkManagerAdmin
import com.joshtalks.joshskills.repository.local.eventbus.LoginViaStatus
import com.joshtalks.joshskills.repository.local.model.DeviceDetailsResponse
import com.joshtalks.joshskills.repository.local.model.FCMResponse
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.repository.local.model.Mentor.Companion.updateFromLoginResponse
import com.joshtalks.joshskills.repository.local.model.User
import com.joshtalks.joshskills.repository.server.TrueCallerLoginRequest
import com.joshtalks.joshskills.repository.server.signup.LoginResponse
import com.joshtalks.joshskills.util.showAppropriateMsg
import com.truecaller.android.sdk.TrueProfile
import com.userexperior.UserExperior
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.HashMap

class FreeTrialOnBoardViewModel(application: Application) : AndroidViewModel(application) {
    private val _signUpStatus: MutableLiveData<SignUpStepStatus> = MutableLiveData()
    val signUpStatus: LiveData<SignUpStepStatus> = _signUpStatus
    val progressBarStatus: MutableLiveData<Boolean> = MutableLiveData()
    val service = AppObjectController.signUpNetworkService
    var fromVerificationScreen = MutableLiveData(false)
    var loginViaStatus: LoginViaStatus? = null
    val verificationStatus: MutableLiveData<VerificationStatus> = MutableLiveData()
    val apiStatus: MutableLiveData<ApiCallStatus> = MutableLiveData()
    fun saveImpression(eventName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val requestData = hashMapOf(
                    Pair("mentor_id", Mentor.getInstance().getId()),
                    Pair("event_name", eventName)
                )
                AppObjectController.commonNetworkService.saveImpression(requestData)
            } catch (ex: Exception) {
                Timber.e(ex)
            }
        }
    }

    suspend fun verifyUserTrueCaller(profile: TrueProfile) {
        progressBarStatus.postValue(true)
        withContext(Dispatchers.IO) {
            try {
                val trueCallerLoginRequest = TrueCallerLoginRequest(
                    profile.payload,
                    profile.signature,
                    profile.signatureAlgorithm,
                    PrefManager.getStringValue(INSTANCE_ID, false)
                )
                val response = service.verifyViaTrueCaller(trueCallerLoginRequest)
                if (response.isSuccessful) {
                    response.body()?.run { //when api calls
                        MarketingAnalytics.completeRegistrationAnalytics(
                            this.newUser,
                            RegistrationMethods.TRUE_CALLER
                        )
                        updateFromLoginResponse(this)
                    }
                }
            } catch (ex: Throwable) {
                ex.showAppropriateMsg()
            }
            _signUpStatus.postValue(SignUpStepStatus.ERROR)
        }
    }

    private fun updateFromLoginResponse(loginResponse: LoginResponse) {
        viewModelScope.launch {
            FCMResponse.removeOldFCM()
            DeviceDetailsResponse.removeOldDevice()
            deleteMentor(loginResponse.mentorId, Mentor.getInstance().getId())
            val user = User.getInstance()
            user.userId = loginResponse.userId
            user.isVerified = true
            user.token = loginResponse.token
            user.source = loginResponse.createdSource!!
            User.update(user)
            PrefManager.put(API_TOKEN, loginResponse.token)
            Mentor.getInstance()
                .setId(loginResponse.mentorId)
                .setReferralCode(loginResponse.referralCode)
                .setUserId(loginResponse.userId)
                .update()
            Mentor.getInstance().updateUser(user)
            UserExperior.setUserIdentifier(Mentor.getInstance().getId())
            AppAnalytics.updateUser()
            fetchMentor()
            WorkManagerAdmin.userActiveStatusWorker(true)
            WorkManagerAdmin.requiredTaskAfterLoginComplete()
        }
    }

    fun completingProfile(map: MutableMap<String, String?>, isUserVerified: Boolean = true) {
        progressBarStatus.postValue(true)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = service.updateUserProfile(Mentor.getInstance().getUserId(), map)
                if (response.isSuccessful) {
                    response.body()?.let {
                        it.isVerified = isUserVerified
                        User.getInstance().updateFromResponse(it)
                        _signUpStatus.postValue(SignUpStepStatus.ProfileCompleted)
                    }
                    return@launch
                }
            } catch (ex: Throwable) {
                ex.showAppropriateMsg()
            }
            _signUpStatus.postValue(SignUpStepStatus.ERROR)
        }
    }

    fun startFreeTrial(mentorId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                apiStatus.postValue(ApiCallStatus.START)
                val resp =
                    AppObjectController.commonNetworkService.enrollFreeTrialMentorWithCourse(
                        mapOf(
                            "mentor_id" to mentorId,
                            "gaid" to PrefManager.getStringValue(USER_UNIQUE_ID, false),
                            "event_name" to IMPRESSION_REGISTER_FREE_TRIAL
                        )
                    )


                if (resp.isSuccessful) {
                    PrefManager.put(IS_GUEST_ENROLLED, value = true)
                    apiStatus.postValue(ApiCallStatus.SUCCESS)
                    return@launch
                }
            } catch (ex: Throwable) {
                ex.showAppropriateMsg()
                apiStatus.postValue(ApiCallStatus.FAILED)
                ex.printStackTrace()
            }
            apiStatus.postValue(ApiCallStatus.FAILED)
        }
    }

    private fun deleteMentor(mentorId: String, oldMentorId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val requestParams: HashMap<String, String> = HashMap()
                requestParams["non_verified_mentor_id"] = oldMentorId
                requestParams["verified_mentor_id"] = mentorId

                AppObjectController.commonNetworkService.deleteMentor(requestParams)
                return@launch
            } catch (ex: Throwable) {
                ex.showAppropriateMsg()
            }
            _signUpStatus.postValue(SignUpStepStatus.ERROR)
        }
    }

    private fun fetchMentor() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = service.getPersonalProfileAsync(Mentor.getInstance().getId())
                Mentor.getInstance().updateFromResponse(response)
                response.getUser()?.let {
                    it.isVerified = true
                    User.update(it)
                }
                AppAnalytics.updateUser()
                return@launch
            } catch (ex: Throwable) {
                ex.showAppropriateMsg()
            }
            _signUpStatus.postValue(SignUpStepStatus.ERROR)
        }
    }
}
