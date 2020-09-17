package com.joshtalks.joshskills.ui.signup

import android.app.Application
import androidx.databinding.ObservableField
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.phone.SmsRetriever
import com.google.android.gms.tasks.Task
import com.google.gson.reflect.TypeToken
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.core.analytics.MarketingAnalytics
import com.joshtalks.joshskills.core.service.WorkManagerAdmin
import com.joshtalks.joshskills.repository.local.eventbus.CreatedSource
import com.joshtalks.joshskills.repository.local.eventbus.LoginViaStatus
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.repository.local.model.User
import com.joshtalks.joshskills.repository.server.RequestVerifyOTP
import com.joshtalks.joshskills.repository.server.TrueCallerLoginRequest
import com.joshtalks.joshskills.repository.server.onboarding.FreeTrialData
import com.joshtalks.joshskills.repository.server.onboarding.VersionResponse
import com.joshtalks.joshskills.repository.server.signup.LoginResponse
import com.joshtalks.joshskills.repository.server.signup.RequestSocialSignUp
import com.joshtalks.joshskills.repository.server.signup.RequestUserVerification
import com.joshtalks.joshskills.repository.server.signup.request.SocialSignUpRequest
import com.joshtalks.joshskills.util.showAppropriateMsg
import com.truecaller.android.sdk.TrueProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SignUpViewModel(application: Application) : AndroidViewModel(application) {
    private val _signUpStatus: MutableLiveData<SignUpStepStatus> = MutableLiveData()
    val signUpStatus: LiveData<SignUpStepStatus> = _signUpStatus
    val progressBarStatus: MutableLiveData<Boolean> = MutableLiveData()
    var resendAttempt: Int = 1
    var incorrectAttempt: Int = 0
    var currentTime: Long = 0
    var fromVerificationScreen = MutableLiveData(false)
    val verificationStatus: MutableLiveData<VerificationStatus> = MutableLiveData()

    val otpField = ObservableField<String>()
    var context: JoshApplication = getApplication()
    var phoneNumber = String()
    var countryCode = String()
    var loginViaStatus: LoginViaStatus? = null

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
                    instanceId = PrefManager.getStringValue(INSTANCE_ID, false)
                )
                    .name(name)
                    .email(email)
                    .photoUrl(profilePicture)
                val response =
                    AppObjectController.signUpNetworkService.socialLogin(
                        getPath(loginViaStatus),
                        reqObj.build()
                    )
                if (response.isSuccessful) {
                    response.body()?.run {
                        MarketingAnalytics.completeRegistrationAnalytics(
                            this.newUser,
                            RegistrationMethods.FACEBOOK
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

    fun signUpUsingSMS(cCode: String, mNumber: String) {
        progressBarStatus.postValue(true)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                loginAnalyticsEvent(VerificationVia.SMS.name)
                val reqObj = mapOf("country_code" to cCode, "mobile" to mNumber)
                val response = AppObjectController.signUpNetworkService.getOtpForNumberAsync(reqObj)
                if (response.isSuccessful) {
                    countryCode = cCode
                    phoneNumber = mNumber
                    _signUpStatus.postValue(SignUpStepStatus.RequestForOTP)
                    registerSMSReceiver()
                    return@launch
                }
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
                val response = AppObjectController.signUpNetworkService.getOtpForNumberAsync(reqObj)
                if (response.isSuccessful) {
                    _signUpStatus.postValue(SignUpStepStatus.ReGeneratedOTP)
                    return@launch
                }
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
                    PrefManager.getStringValue(INSTANCE_ID, false)
                )
                val response =
                    AppObjectController.signUpNetworkService.verifyViaTrueCaller(
                        trueCallerLoginRequest
                    )
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
                    PrefManager.getStringValue(INSTANCE_ID, false),
                    countryCode,
                    mNumber
                )

                val response = AppObjectController.signUpNetworkService.userVerification(reqObj)
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
                    PrefManager.getStringValue(INSTANCE_ID, false),
                    countryCode,
                    phoneNumber,
                    otp ?: otpField.get()!!
                )
                val response = AppObjectController.signUpNetworkService.verifyOTP(reqObj)
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
        val user = User.getInstance()
        user.userId = loginResponse.userId
        user.isVerified = true
        user.token = loginResponse.token
        User.update(user.toString())
        PrefManager.put(API_TOKEN, loginResponse.token)
        Mentor.getInstance()
            .setId(loginResponse.mentorId)
            .setReferralCode(loginResponse.referralCode)
            .setUserId(loginResponse.userId)
            .update()
        AppAnalytics.updateUser()
        WorkManagerAdmin.requiredTaskAfterLoginComplete()
        fetchMentor()
    }

    private fun fetchMentor() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response =
                    AppObjectController.signUpNetworkService.getPersonalProfileAsync(
                        Mentor.getInstance().getId()
                    )
                if (response.isSuccessful) {
                    response.body()?.run {
                        Mentor.getInstance().updateFromResponse(this)
                        this.getUser()?.let {
                            it.isVerified = true
                            User.update(it.toString())
                        }
                        AppAnalytics.updateUser()
                        analyzeUserProfile()
                    }
                    return@launch
                }
            } catch (ex: Throwable) {
                ex.showAppropriateMsg()
            }
            _signUpStatus.postValue(SignUpStepStatus.ERROR)
        }
    }

    private fun analyzeUserProfile() {
        val user = User.getInstance()
        if (user.phoneNumber.isNotEmpty() && user.firstName.isEmpty()) {
            return _signUpStatus.postValue(SignUpStepStatus.ProfileInCompleted)
        }
        if (user.firstName.isEmpty()) {
            return _signUpStatus.postValue(SignUpStepStatus.ProfileInCompleted)
        }
        _signUpStatus.postValue(SignUpStepStatus.SignUpCompleted)
        if (loginViaStatus == LoginViaStatus.SMS_VERIFY)
            fromVerificationScreen.postValue(true)
    }


    private fun getPath(loginViaStatus: LoginViaStatus): String {
        if (LoginViaStatus.FACEBOOK == loginViaStatus) {
            return "facebook"
        } else if (LoginViaStatus.GMAIL == loginViaStatus) {
            return "gmail"
        }
        return EMPTY
    }

    fun completingProfile(map: MutableMap<String, String?>) {
        progressBarStatus.postValue(true)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response =
                    AppObjectController.signUpNetworkService.updateUserProfile(
                        Mentor.getInstance().getUserId(), map
                    )
                if (response.isSuccessful) {
                    response.body()?.run {
                        this.isVerified = true
                        User.getInstance().updateFromResponse(this)
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

    fun verifyUser(
        request: SocialSignUpRequest
    ) {
        if (request.createdSource == CreatedSource.OTP.name) {
            request.countryCode = countryCode
            request.mobile = phoneNumber
        }
        progressBarStatus.postValue(true)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response =
                    AppObjectController.signUpNetworkService.verifyGuestUser(
                        request
                    )
                if (response.isSuccessful) {
                    response.body()?.run {
                        updateSubscriptionStatus()
                        when (request.createdSource) {
                            CreatedSource.FB.name -> {
                                MarketingAnalytics.completeRegistrationAnalytics(
                                    this.newUser,
                                    RegistrationMethods.FACEBOOK
                                )
                            }
                            CreatedSource.GML.name -> {
                                MarketingAnalytics.completeRegistrationAnalytics(
                                    this.newUser,
                                    RegistrationMethods.GOOGLE
                                )
                            }
                            CreatedSource.OTP.name -> {
                                MarketingAnalytics.completeRegistrationAnalytics(
                                    this.newUser,
                                    RegistrationMethods.MOBILE_NUMBER
                                )
                            }
                            CreatedSource.TC.name -> {
                                MarketingAnalytics.completeRegistrationAnalytics(
                                    this.newUser,
                                    RegistrationMethods.TRUE_CALLER
                                )
                            }
                        }
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

    fun updateSubscriptionStatus() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response =
                    AppObjectController.signUpNetworkService.getOnBoardingStatus(
                        PrefManager.getStringValue(INSTANCE_ID, false),
                        Mentor.getInstance().getId(),
                        PrefManager.getStringValue(USER_UNIQUE_ID)
                    )
                if (response.isSuccessful) {
                    response.body()?.run {
                        // Update Version Data in local
                        val versionData = AppObjectController.gsonMapper.fromJson<VersionResponse>(
                            PrefManager.getStringValue(ONBOARDING_VERSION_KEY),
                            object : TypeToken<VersionResponse>() {}.type
                        )
                        versionData?.version?.let {
                            it.name = this.version.name
                            it.id = this.version.id
                        }
                        versionData?.let {
                            PrefManager.put(
                                ONBOARDING_VERSION_KEY,
                                AppObjectController.gsonMapper.toJson(versionData)
                            )
                        }

                        // save Free trial data
                        FreeTrialData.update(this.freeTrialData)

                        PrefManager.put(EXPLORE_TYPE, this.exploreType)
                        PrefManager.put(
                            IS_SUBSCRIPTION_STARTED,
                            this.subscriptionData.isSubscriptionBought ?: false
                        )
                        PrefManager.put(
                            REMAINING_SUBSCRIPTION_DAYS,
                            this.subscriptionData.remainingDays
                        )

                        PrefManager.put(IS_TRIAL_STARTED, this.freeTrialData.is7DFTBought ?: false)
                        PrefManager.put(REMAINING_TRIAL_DAYS, this.freeTrialData.remainingDays)
                        PrefManager.put(SHOW_COURSE_DETAIL_TOOLTIP, this.showTooltip4)

                    }
                }
            } catch (ex: Throwable) {
                ex.showAppropriateMsg()
            }
        }
    }

}
