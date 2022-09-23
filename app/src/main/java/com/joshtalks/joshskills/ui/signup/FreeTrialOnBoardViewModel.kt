package com.joshtalks.joshskills.ui.signup

import android.os.Message
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.joshtalks.joshskills.base.BaseViewModel
import com.joshtalks.joshskills.base.EventLiveData
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.core.abTest.GoalKeys
import com.joshtalks.joshskills.core.abTest.repository.ABTestRepository
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.core.analytics.MarketingAnalytics
import com.joshtalks.joshskills.core.notification.NotificationCategory
import com.joshtalks.joshskills.core.notification.NotificationUtils
import com.joshtalks.joshskills.core.service.WorkManagerAdmin
import com.joshtalks.joshskills.repository.local.model.DeviceDetailsResponse
import com.joshtalks.joshskills.repository.local.model.FCMResponse
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.repository.local.model.User
import com.joshtalks.joshskills.repository.server.ChooseLanguages
import com.joshtalks.joshskills.repository.server.GoalList
import com.joshtalks.joshskills.repository.server.GoalSelectionResponse
import com.joshtalks.joshskills.repository.server.TrueCallerLoginRequest
import com.joshtalks.joshskills.repository.server.signup.LoginResponse
import com.joshtalks.joshskills.ui.activity_feed.utils.IS_USER_EXIST
import com.joshtalks.joshskills.ui.signup.adapters.ChooseGoalAdapter
import com.joshtalks.joshskills.ui.special_practice.utils.CLICK_CONTINUE
import com.joshtalks.joshskills.ui.special_practice.utils.CLICK_GOAL_CARD
import com.joshtalks.joshskills.ui.special_practice.utils.CLICK_ON_BACK_PRESS
import com.joshtalks.joshskills.util.showAppropriateMsg
import com.truecaller.android.sdk.TrueProfile
import com.userexperior.UserExperior
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

class FreeTrialOnBoardViewModel : BaseViewModel() {

    val signUpStatus: MutableLiveData<SignUpStepStatus> = MutableLiveData()
    val progressBarStatus: MutableLiveData<Boolean> = MutableLiveData()
    val service = AppObjectController.signUpNetworkService
    val verificationStatus: MutableLiveData<VerificationStatus> = MutableLiveData()
    val apiStatus: MutableLiveData<ApiCallStatus> = MutableLiveData()
    val availableLanguages: MutableLiveData<List<ChooseLanguages>> = MutableLiveData()
    val availableGoals: MutableLiveData<GoalSelectionResponse> = MutableLiveData()
    val liveEvent = EventLiveData
    var userName: String? = null
    var isVerified: Boolean = false
    var isUserExist: Boolean = false
    val isLanguageFragment = ObservableBoolean(false)
    val abTestRepository by lazy { ABTestRepository() }
    var goalListAdapter =  ChooseGoalAdapter()
    val goalText = ObservableField(EMPTY)

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

    fun verifyUserViaTrueCaller(profile: TrueProfile) {
        progressBarStatus.postValue(true)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val trueCallerLoginRequest = TrueCallerLoginRequest(
                    profile.payload,
                    profile.signature,
                    profile.signatureAlgorithm,
                    PrefManager.getStringValue(USER_UNIQUE_ID)
                )
                val response = service.verifyViaTrueCaller(trueCallerLoginRequest)
                if (response.isSuccessful && response.body() != null) {
                    isUserExist = response.body()?.isUserExist ?: false
                    if (isUserExist) {
                        val msg = Message()
                        msg.what = IS_USER_EXIST
                        liveEvent.postValue(msg)
                    }
                    response.body()?.run {
                        MarketingAnalytics.completeRegistrationAnalytics(
                            this.newUser,
                            RegistrationMethods.TRUE_CALLER
                        )
                        updateFromLoginResponse(this)
                    }
                } else {
                    signUpStatus.postValue(SignUpStepStatus.ERROR)
                }
            } catch (ex: Throwable) {
                ex.showAppropriateMsg()
                signUpStatus.postValue(SignUpStepStatus.ERROR)
            }
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
            NotificationUtils(AppObjectController.joshApplication).removeScheduledNotification(NotificationCategory.APP_OPEN)
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
                    if (isUserExist.not()) {
                        liveEvent.postValue(Message().apply { what = USER_CREATED_SUCCESSFULLY })
                    }
                    return@launch
                } else {
                    signUpStatus.postValue(SignUpStepStatus.ERROR)
                }
            } catch (ex: Throwable) {
                signUpStatus.postValue(SignUpStepStatus.ERROR)
                ex.showAppropriateMsg()
            }
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
                apiStatus.postValue(ApiCallStatus.START)
                val response = service.getAvailableLanguageCourses()
                if (response.isSuccessful && response.code() in 200..203) {
                    availableLanguages.value = response.body()
                    apiStatus.postValue(ApiCallStatus.SUCCESS)
                } else {
                    apiStatus.postValue(ApiCallStatus.FAILED)
                }
            } catch (ex: Throwable) {
                apiStatus.postValue(ApiCallStatus.FAILED)
                ex.showAppropriateMsg()
            }
        }
    }

    fun getAvailableCourseGoals(testId:String) {
        viewModelScope.launch {
            try {
                apiStatus.postValue(ApiCallStatus.START)
                val response = service.getAvailableGoals(testId)
                if (response.isSuccessful && response.code() in 200..203) {
                    response.body()?.data?.let { goalListAdapter.setData(it) }
                    goalListAdapter.count = 1
                    goalText.set(response.body()?.title)
                    apiStatus.postValue(ApiCallStatus.SUCCESS)
                } else {
                    apiStatus.postValue(ApiCallStatus.FAILED)
                }
            } catch (ex: Throwable) {
                apiStatus.postValue(ApiCallStatus.FAILED)
                ex.showAppropriateMsg()
            }
        }
    }

    val onItemGoalClick: (GoalList, Int, Int, String) -> Unit = { it, position,type, cardType ->
        when (type) {
            CLICK_GOAL_CARD -> {
                message.what = CLICK_GOAL_CARD
                message.obj = it
                message.arg1 = position
                singleLiveEvent.value = message
            }
        }
    }

    fun onClickContinue(){
        message.what = CLICK_CONTINUE
        singleLiveEvent.value = message
    }

    fun postGoal(goal: GoalKeys) {
        viewModelScope.launch {
            abTestRepository.postGoal(goal.NAME)
        }
    }

    fun onBackPress(){
        message.what = CLICK_ON_BACK_PRESS
        singleLiveEvent.value = message
    }
}
