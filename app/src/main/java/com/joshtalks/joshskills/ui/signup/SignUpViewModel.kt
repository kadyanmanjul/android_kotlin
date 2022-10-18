package com.joshtalks.joshskills.ui.signup

import android.app.Application
import android.content.Intent
import androidx.databinding.ObservableField
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.gms.auth.api.phone.SmsRetriever
import com.google.android.gms.tasks.Task
import com.joshtalks.joshskills.base.constants.CALLING_SERVICE_ACTION
import com.joshtalks.joshskills.base.constants.SERVICE_BROADCAST_KEY
import com.joshtalks.joshskills.base.constants.START_SERVICE
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.core.abTest.GoalKeys
import com.joshtalks.joshskills.core.abTest.VariantKeys
import com.joshtalks.joshskills.core.abTest.repository.ABTestRepository
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.core.analytics.MarketingAnalytics
import com.joshtalks.joshskills.core.notification.NotificationCategory
import com.joshtalks.joshskills.core.notification.NotificationUtils
import com.joshtalks.joshskills.core.service.WorkManagerAdmin
import com.joshtalks.joshskills.repository.local.eventbus.LoginViaStatus
import com.joshtalks.joshskills.repository.local.minimalentity.InboxEntity
import com.joshtalks.joshskills.repository.local.model.DeviceDetailsResponse
import com.joshtalks.joshskills.repository.local.model.FCMResponse
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.repository.local.model.User
import com.joshtalks.joshskills.repository.server.RequestVerifyOTP
import com.joshtalks.joshskills.repository.server.TrueCallerLoginRequest
import com.joshtalks.joshskills.repository.server.UpdateDeviceRequest
import com.joshtalks.joshskills.repository.server.onboarding.FreeTrialData
import com.joshtalks.joshskills.repository.server.onboarding.SpecificOnboardingCourseData
import com.joshtalks.joshskills.repository.server.onboarding.VersionResponse
import com.joshtalks.joshskills.repository.server.signup.LoginResponse
import com.joshtalks.joshskills.repository.server.signup.RequestSocialSignUp
import com.joshtalks.joshskills.repository.server.signup.RequestUserVerification
import com.joshtalks.joshskills.util.showAppropriateMsg
import com.truecaller.android.sdk.TrueProfile
import com.userexperior.UserExperior
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.*

class SignUpViewModel(application: Application) : AndroidViewModel(application) {
    private val _signUpStatus: MutableLiveData<SignUpStepStatus> = MutableLiveData()
    val signUpStatus: LiveData<SignUpStepStatus> = _signUpStatus
    val progressBarStatus: MutableLiveData<Boolean> = MutableLiveData()
    var resendAttempt: Int = 1
    var incorrectAttempt: Int = 0
    var currentTime: Long = 0
    var fromVerificationScreen = MutableLiveData(false)
    val verificationStatus: MutableLiveData<VerificationStatus> = MutableLiveData()
    val apiStatus: MutableLiveData<ApiCallStatus> = MutableLiveData()

    val otpField = ObservableField<String>()
    var context: JoshApplication = getApplication()
    var phoneNumber = String()
    var countryCode = String()
    var loginViaStatus: LoginViaStatus? = null
    val service = AppObjectController.signUpNetworkService
    val abTestRepository by lazy { ABTestRepository() }
    val freeTrialEntity: MutableLiveData<InboxEntity> = MutableLiveData()
    var shouldStartFreeTrial: Boolean = false

    fun signUpUsingSocial(
        loginViaStatus: LoginViaStatus,
        id: String,
        name: String?,
        email: String?,
        profilePicture: String? = null
    ) {
        this.loginViaStatus = loginViaStatus
        progressBarStatus.postValue(true)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val reqObj = RequestSocialSignUp.Builder(
                    id = id,
                    gaid = PrefManager.getStringValue(USER_UNIQUE_ID, false)
                )
                    .name(name)
                    .email(email)
                    .photoUrl(profilePicture)
                val response = service.socialLogin(getPath(loginViaStatus), reqObj.build())
                MarketingAnalytics.completeRegistrationAnalytics(
                    response.newUser,
                    RegistrationMethods.FACEBOOK
                )
                updateFromLoginResponse(response)
                return@launch
            } catch (ex: Throwable) {
                ex.showAppropriateMsg()
            }
            _signUpStatus.postValue(SignUpStepStatus.ERROR)
        }
    }

    fun signUpUsingSMS(cCode: String, mNumber: String) {
        progressBarStatus.postValue(true)
        postGoal(GoalKeys.PHONE_NUMBER_SUBMITTED)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                loginAnalyticsEvent(VerificationVia.SMS.name)
                val reqObj = mapOf("country_code" to cCode, "mobile" to mNumber)
                service.getOtpForNumberAsync(reqObj)
                countryCode = cCode
                phoneNumber = mNumber
                delay(500)
                _signUpStatus.postValue(SignUpStepStatus.RequestForOTP)
                registerSMSReceiver()
                return@launch
            } catch (ex: Throwable) {
                ex.showAppropriateMsg()
            }
            _signUpStatus.postValue(SignUpStepStatus.ERROR)
        }
    }

    fun regeneratedOTP() {
        progressBarStatus.postValue(true)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val reqObj = mapOf("country_code" to countryCode, "mobile" to phoneNumber)
                service.getOtpForNumberAsync(reqObj)
                _signUpStatus.postValue(SignUpStepStatus.ReGeneratedOTP)
                return@launch
            } catch (ex: Throwable) {
                ex.showAppropriateMsg()
            }
            _signUpStatus.postValue(SignUpStepStatus.ERROR)
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
                if (response.isSuccessful) {
                    response.body()?.run {
                        MarketingAnalytics.completeRegistrationAnalytics(
                            this.newUser,
                            RegistrationMethods.MOBILE_NUMBER
                        )
                        updateFromLoginResponse(this)
                    }
                    return@launch
                }
            } catch (ex: Throwable) {
                ex.showAppropriateMsg()
            }
            _signUpStatus.postValue(SignUpStepStatus.ERROR)
        }
    }

    fun signUpAfterPhoneVerify(countryCode: String, mNumber: String) {
        progressBarStatus.postValue(true)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val reqObj = RequestUserVerification(
                    PrefManager.getStringValue(USER_UNIQUE_ID),
                    countryCode,
                    mNumber
                )

                val response = service.userVerification(reqObj)
                if (response.isSuccessful) {
                    response.body()?.run {
                        MarketingAnalytics.completeRegistrationAnalytics(
                            this.newUser,
                            RegistrationMethods.MOBILE_NUMBER
                        )
                        updateFromLoginResponse(this)
                    }
                    return@launch
                }
            } catch (ex: Throwable) {
                ex.showAppropriateMsg()
            }
            _signUpStatus.postValue(SignUpStepStatus.ERROR)
        }
    }

    fun verifyOTP(otp: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val reqObj = RequestVerifyOTP(
                    PrefManager.getStringValue(USER_UNIQUE_ID),
                    countryCode,
                    phoneNumber,
                    otp ?: otpField.get()!!
                )
                val response = service.verifyOTP(reqObj)
                if (response.isSuccessful) {
                    response.body()?.run {
                        MarketingAnalytics.completeRegistrationAnalytics(
                            this.newUser,
                            RegistrationMethods.MOBILE_NUMBER
                        )
                        updateFromLoginResponse(this)
                    }
                    return@launch
                } else {
                    if (response.code() == 400) {
                        _signUpStatus.postValue(SignUpStepStatus.WRONG_OTP)
                        return@launch
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
            fetchMentor(isNewUser = loginResponse.isUserExist.not())
            WorkManagerAdmin.requiredTaskAfterLoginComplete()
            ABTestRepository().updateAllCampaigns()
            NotificationUtils(context).removeScheduledNotification(NotificationCategory.APP_OPEN)
            PrefManager.put(IS_USER_LOGGED_IN, value = true, isConsistent = true)
            val isCourseBought = PrefManager.getBoolValue(IS_COURSE_BOUGHT, false)
            val courseExpiryTime =
                PrefManager.getLongValue(com.joshtalks.joshskills.core.COURSE_EXPIRY_TIME_IN_MS)
            if ((isCourseBought && User.getInstance().isVerified) || courseExpiryTime != 0L &&
                courseExpiryTime >= System.currentTimeMillis()
            ) {
                val broadcastIntent = Intent().apply {
                    action = CALLING_SERVICE_ACTION
                    putExtra(SERVICE_BROADCAST_KEY, START_SERVICE)
                }
                LocalBroadcastManager.getInstance(context).sendBroadcast(broadcastIntent)
            }
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

    private fun fetchMentor(isNewUser: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = service.getPersonalProfileAsync(Mentor.getInstance().getId())
                Mentor.getInstance().updateFromResponse(response)
                response.getUser()?.let {
                    it.isVerified = true
                    User.update(it)
                }
                AppAnalytics.updateUser()
                analyzeUserProfile(isNewUser)
                return@launch
            } catch (ex: Throwable) {
                ex.showAppropriateMsg()
            }
            _signUpStatus.postValue(SignUpStepStatus.ERROR)
        }
    }

    private fun analyzeUserProfile(isNewUser: Boolean) {
        val user = User.getInstance()
        if (shouldStartFreeTrial) {
            if (isNewUser && abTestRepository.isVariantActive(VariantKeys.NEW_LOGIN_BEFORE_NAME)) {
                return updateFTSignUpStatus(SignUpStepStatus.LanguageSelection)
            }
            if (isNewUser && abTestRepository.isVariantActive(VariantKeys.NEW_LOGIN_AFTER_NAME)) {
                return updateFTSignUpStatus(SignUpStepStatus.StartTrial)
            }
        }
        if (user.phoneNumber.isNullOrEmpty() && user.firstName.isNullOrEmpty()) {
            return _signUpStatus.postValue(SignUpStepStatus.ProfileInCompleted)
        }
        if (user.firstName.isNullOrEmpty() || user.dateOfBirth.isNullOrEmpty() || user.gender.isNullOrEmpty()) {
            return _signUpStatus.postValue(SignUpStepStatus.ProfileInCompleted)
        }
        _signUpStatus.postValue(SignUpStepStatus.SignUpCompleted)
        if (loginViaStatus == LoginViaStatus.SMS_VERIFY)
            fromVerificationScreen.postValue(true)
    }

    private fun getPath(loginViaStatus: LoginViaStatus): String {
        if (LoginViaStatus.GMAIL == loginViaStatus) {
            return "gmail"
        }
        return EMPTY
    }

    suspend fun hasMentorPaid(): Boolean {
        try {
            val map = mapOf(Pair("mentor_id", Mentor.getInstance().getId()))
            val response = AppObjectController.commonNetworkService.checkMentorPayStatus(map)
            return response["payment"] as? Boolean ?: false
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    fun completingProfile(map: MutableMap<String, String?>, isUserVerified: Boolean = true) {
        progressBarStatus.postValue(true)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (map["is_free_trial"] == "Y" && hasMentorPaid()) {
                    _signUpStatus.postValue(SignUpStepStatus.SignUpCompleted)
                    return@launch
                }
                val response = service.updateUserProfile(Mentor.getInstance().getUserId(), map)
                val phoneNumberComingFromTrueCaller = User.getInstance().phoneNumber ?: EMPTY
                if (response.isSuccessful) {
                    response.body()?.let {
                        it.isVerified = isUserVerified
                        if (!phoneNumberComingFromTrueCaller.isNullOrEmpty() && it.phoneNumber.isNullOrEmpty()) {
                            it.phoneNumber = phoneNumberComingFromTrueCaller
                        }
                        User.getInstance().updateFromResponse(it)
                        if (map.getOrDefault("is_free_trial", EMPTY) == "Y")
                            if (abTestRepository.isVariantActive(VariantKeys.NEW_LOGIN_AFTER_NAME))
                                updateFTSignUpStatus(SignUpStepStatus.NameSubmitted)
                            else
                                updateFTSignUpStatus(SignUpStepStatus.StartTrial)
                        else
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

    fun registerSMSReceiver() {
        val client = SmsRetriever.getClient(context)
        val task: Task<Void> = client.startSmsRetriever()
        task.addOnSuccessListener {
        }
        task.addOnFailureListener {
        }
        task.addOnCompleteListener {
        }
    }

    fun loginAnalyticsEvent(viaStatus: String) {
        AppAnalytics.create(AnalyticsEvent.LOGIN_WITH.NAME)
            .addUserDetails()
            .addBasicParam()
            .addParam(AnalyticsEvent.ACTION.NAME, viaStatus)
            .push()
    }

    fun incrementResendAttempts() {
        resendAttempt += 1
    }

    fun incrementIncorrectAttempts() {
        incorrectAttempt += 1
    }

    fun updateSubscriptionStatus() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response =
                    AppObjectController.signUpNetworkService.getOnBoardingStatus(
                        Mentor.getInstance().getId(),
                        PrefManager.getStringValue(USER_UNIQUE_ID)
                    )
                if (response.isSuccessful) {
                    response.body()?.run {
                        // Update Version Data in local
                        PrefManager.put(SUBSCRIPTION_TEST_ID, this.subscriptionTestId)
                        val versionData = VersionResponse.getInstance()
                        versionData.version.let {
                            it.name = this.version.name
                            it.id = this.version.id
                            VersionResponse.update(versionData)
                        }

                        // save Free trial data
                        FreeTrialData.update(this.freeTrialData)

                        PrefManager.put(EXPLORE_TYPE, this.exploreType)
                        PrefManager.put(
                            IS_SUBSCRIPTION_STARTED,
                            this.subscriptionData.isSubscriptionBought
                        )
                        PrefManager.put(
                            REMAINING_SUBSCRIPTION_DAYS,
                            this.subscriptionData.remainingDays
                        )

                        PrefManager.put(IS_TRIAL_STARTED, this.freeTrialData.is7DFTBought)
                        PrefManager.put(REMAINING_TRIAL_DAYS, this.freeTrialData.remainingDays)
                        PrefManager.put(SHOW_COURSE_DETAIL_TOOLTIP, this.showTooltip5)
                    }
                }
            } catch (ex: Throwable) {
                ex.showAppropriateMsg()
            }
        }
    }

    fun changeSignupStatusToProfilePicUploaded() {
        _signUpStatus.postValue(SignUpStepStatus.ProfilePicUploaded)
    }

    fun changeSignupStatusToStartAfterPicUploaded() {
        _signUpStatus.postValue(SignUpStepStatus.StartAfterPicUploaded)
    }

    fun changeSignupStatusToProfilePicSkipped() {
        _signUpStatus.postValue(SignUpStepStatus.ProfilePicSkipped)
    }

    fun startFreeTrial(mentorId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                apiStatus.postValue(ApiCallStatus.START)
                val resp =
                    AppObjectController.commonNetworkService.enrollFreeTrialMentorWithCourse(
                        mapOf(
                            "is_verified" to User.getInstance().isVerified.toString(),
                            "mentor_id" to mentorId,
                            "gaid" to PrefManager.getStringValue(USER_UNIQUE_ID, false),
                            "event_name" to IMPRESSION_REGISTER_FREE_TRIAL,
                            "test_id" to PrefManager.getStringValue(
                                FREE_TRIAL_TEST_ID,
                                false,
                                FREE_TRIAL_DEFAULT_TEST_ID
                            )
                        )
                    )
                if (resp.isSuccessful) {
                    postGoal(GoalKeys.FREE_TRIAL_STARTED)
                    getFreeTrialNotifications()
                    PrefManager.put(IS_GUEST_ENROLLED, value = true)
                    PrefManager.put(IS_USER_LOGGED_IN, value = true, isConsistent = true)
                    getRegisteredFreeTrialCourse()
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

    fun getRegisteredFreeTrialCourse() {
        viewModelScope.launch {
            try {
                val response = AppObjectController.chatNetworkService.getRegisteredCourses()
                if (response.isEmpty().not()) {
                    patchDeviceDetails()
                    AppObjectController.appDatabase.courseDao().insertRegisterCourses(response).let {
                        AppObjectController.appDatabase.courseDao().getRegisterCourseMinimal().let {
                            freeTrialEntity.postValue(it.firstOrNull())
                        }
                    }
                }
            } catch (e: Exception) {
                apiStatus.postValue(ApiCallStatus.SUCCESS)
            }
        }
    }

    private suspend fun patchDeviceDetails() {
        try {
            val device = DeviceDetailsResponse.getInstance()
            val status = device?.apiStatus ?: ApiRespStatus.EMPTY
            val deviceId = device?.id ?: 0
            if (ApiRespStatus.PATCH == status) {
                //return Result.success()
                if (deviceId > 0) {
                    val details = AppObjectController.signUpNetworkService.patchDeviceDetails(
                        deviceId,
                        UpdateDeviceRequest()
                    )
                    // TODO no need to send UpdateDeviceRequest object in patch request
                    details.apiStatus = ApiRespStatus.PATCH
                    details.update()
                }
            } else if (ApiRespStatus.POST == status) {
                if (deviceId > 0) {
                    val details = AppObjectController.signUpNetworkService.patchDeviceDetails(
                        deviceId,
                        UpdateDeviceRequest()
                    )
                    // TODO no need to send UpdateDeviceRequest object in patch request
                    details.apiStatus = ApiRespStatus.PATCH
                    details.update()
                }
            } else {
                val details =
                    AppObjectController.signUpNetworkService.postDeviceDetails(
                        UpdateDeviceRequest()
                    )
                details.apiStatus = ApiRespStatus.POST
                details.update()
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
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

    fun registerSpecificCourse() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val courseData = AppObjectController.gsonMapper.fromJson(
                    PrefManager.getStringValue(
                        SPECIFIC_ONBOARDING,
                        isConsistent = true
                    ), SpecificOnboardingCourseData::class.java
                )
                apiStatus.postValue(ApiCallStatus.START)
                val requestData = hashMapOf(
                    "mentor_id" to Mentor.getInstance().getId(),
                    "course_id" to courseData.courseId,
                    "plan_id" to courseData.planId
                )
                val response = AppObjectController.signUpNetworkService.registerCourse(requestData)
                apiStatus.postValue(
                    if (response.isSuccessful) ApiCallStatus.SUCCESS
                    else ApiCallStatus.FAILED
                )
            } catch (ex: Exception) {
                apiStatus.postValue(ApiCallStatus.FAILED)
                Timber.e(ex)
            }
        }
    }

    fun getFreeTrialNotifications() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = AppObjectController.utilsAPIService.getFTScheduledNotifications(
                    PrefManager.getStringValue(
                        FREE_TRIAL_TEST_ID,
                        false,
                        "None"
                    )
                )
                AppObjectController.appDatabase.scheduleNotificationDao().insertAllNotifications(response)
                NotificationUtils(context).removeScheduledNotification(NotificationCategory.APP_OPEN)
                NotificationUtils(context).updateNotificationDb(NotificationCategory.AFTER_LOGIN)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun updateFTSignUpStatus(signUpStepStatus: SignUpStepStatus) {
        if (signUpStepStatus == SignUpStepStatus.StartTrial) {
            PrefManager.removeKey(FT_ONBOARDING_NEXT_STEP)
        } else {
            PrefManager.put(
                FT_ONBOARDING_NEXT_STEP,
                value = signUpStepStatus.name
            )
        }
        _signUpStatus.postValue(signUpStepStatus)
    }

    fun postGoal(goalKeys: GoalKeys) {
        viewModelScope.launch {
            if (PrefManager.getStringValue(API_TOKEN).isNotEmpty())
                abTestRepository.postGoal(goalKeys.NAME)
        }
    }
}
