package com.joshtalks.joshskills.ui.signup

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.core.abTest.ABTestCampaignData
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.core.analytics.MarketingAnalytics
import com.joshtalks.joshskills.core.service.WorkManagerAdmin
import com.joshtalks.joshskills.repository.local.model.DeviceDetailsResponse
import com.joshtalks.joshskills.repository.local.model.FCMResponse
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.repository.local.model.User
import com.joshtalks.joshskills.repository.server.ChooseLanguages
import com.joshtalks.joshskills.repository.server.TrueCallerLoginRequest
import com.joshtalks.joshskills.repository.server.signup.LoginResponse
import com.joshtalks.joshskills.ui.group.repository.ABTestRepository
import com.joshtalks.joshskills.util.showAppropriateMsg
import com.truecaller.android.sdk.TrueProfile
import com.userexperior.UserExperior
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

class FreeTrialOnBoardViewModel(application: Application) : AndroidViewModel(application) {

    val signUpStatus: MutableLiveData<SignUpStepStatus> = MutableLiveData()
    val progressBarStatus: MutableLiveData<Boolean> = MutableLiveData()
    val service = AppObjectController.signUpNetworkService
    val verificationStatus: MutableLiveData<VerificationStatus> = MutableLiveData()
    val apiStatus: MutableLiveData<ApiCallStatus> = MutableLiveData()
    val availableLanguages: MutableLiveData<List<ChooseLanguages>> = MutableLiveData()
    var userName: String? = null
    var isVerified: Boolean = false
    var isUserExist: Boolean = false
    val points100ABtestLiveData = MutableLiveData<ABTestCampaignData?>()
    val eftABtestLiveData = MutableLiveData<ABTestCampaignData?>()

    val repository: ABTestRepository by lazy { ABTestRepository() }
    fun get100PCampaignData(campaign: String, campaignEft: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try{
                repository.getCampaignData(campaignEft)?.let { campaign ->
                    eftABtestLiveData.postValue(campaign)
                }
                val response = repository.getCampaignData(campaign)
                if(response != null ){
                    points100ABtestLiveData.postValue(response)
                }else {
                    points100ABtestLiveData.postValue(null)
                }
            }catch (ex : Exception){
                ex.printStackTrace()
                points100ABtestLiveData.postValue(null)
            }
        }
    }
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

    fun saveTrueCallerImpression(eventName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val requestData = hashMapOf(
                    Pair("mentor_id", Mentor.getInstance().getId()),
                    Pair("event_name", eventName)
                )
                AppObjectController.commonNetworkService.saveTrueCallerImpression(requestData)
            } catch (ex: Exception) {
                Timber.e(ex)
            }
        }
    }

    suspend fun verifyUserViaTrueCaller(profile: TrueProfile) {
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
                if (response.isSuccessful && response.body() != null) {
                    isUserExist = response.body()!!.isUserExist
                    response.body()?.run {
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
            signUpStatus.postValue(SignUpStepStatus.ERROR)
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
//            WorkManagerAdmin.userActiveStatusWorker(true)
            WorkManagerAdmin.requiredTaskAfterLoginComplete()
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
            signUpStatus.postValue(SignUpStepStatus.ERROR)
        }
    }

    private fun fetchMentor() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = service.getPersonalProfileAsync(Mentor.getInstance().getId())
                if (response != null) {
                    Mentor.getInstance().updateFromResponse(response)
                    response.getUser()?.let {
                        it.isVerified = true
                        User.update(it)
                    }
                    AppAnalytics.updateUser()
                    analyzeUserProfile()
                    return@launch
                }
            } catch (ex: Throwable) {
                ex.showAppropriateMsg()
            }
            signUpStatus.postValue(SignUpStepStatus.ERROR)
        }
    }

    private fun analyzeUserProfile() {
        val user = User.getInstance()
        if (user.phoneNumber.isNullOrEmpty() || user.firstName.isNullOrEmpty()) {
            return signUpStatus.postValue(SignUpStepStatus.ProfileInCompleted)
        }
        signUpStatus.postValue(SignUpStepStatus.SignUpCompleted)
    }

    fun getAvailableLanguages() {
        viewModelScope.launch {
            try {
                val response = service.getAvailableLanguageCourses()
                if (response.isSuccessful && response.code() in 200..203)
                    availableLanguages.value = response.body()
            } catch (ex: Throwable) {
                ex.showAppropriateMsg()
            }
        }
    }
}
