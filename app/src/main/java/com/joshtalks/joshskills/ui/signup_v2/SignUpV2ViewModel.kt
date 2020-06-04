package com.joshtalks.joshskills.ui.signup_v2

import android.app.Application
import androidx.databinding.ObservableField
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.phone.SmsRetriever
import com.google.android.gms.tasks.Task
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.core.analytics.MarketingAnalytics
import com.joshtalks.joshskills.core.service.WorkMangerAdmin
import com.joshtalks.joshskills.repository.local.eventbus.LoginViaStatus
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.repository.local.model.User
import com.joshtalks.joshskills.repository.server.RequestVerifyOTP
import com.joshtalks.joshskills.repository.server.TrueCallerLoginRequest
import com.joshtalks.joshskills.repository.server.signup.LoginResponse
import com.joshtalks.joshskills.repository.server.signup.RequestSocialSignUp
import com.joshtalks.joshskills.repository.server.signup.RequestUserVerification
import com.truecaller.android.sdk.TrueProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SignUpV2ViewModel(application: Application) :
    AndroidViewModel(application) {
    private val _signUpStatus: MutableLiveData<SignUpStepStatus> = MutableLiveData()
    val signUpStatus: LiveData<SignUpStepStatus> = _signUpStatus
    val progressBarStatus: MutableLiveData<Boolean> = MutableLiveData()

    val otpField = ObservableField<String>()
    var context: JoshApplication = getApplication()
    var phoneNumber = String()
    var countryCode = String()


    fun signUpUsingSocial(
        loginViaStatus: LoginViaStatus,
        id: String,
        name: String?,
        email: String?,
        profilePicture: String? = null
    ) {
        progressBarStatus.postValue(true)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val reqObj = RequestSocialSignUp.Builder(
                    id = id,
                    instanceId = PrefManager.getStringValue(INSTANCE_ID)
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
                ex.printStackTrace()
            }
            _signUpStatus.postValue(SignUpStepStatus.ERROR)
        }
    }

    fun signUpUsingSMS(cCode: String, mNumber: String) {
        progressBarStatus.postValue(true)
        viewModelScope.launch(Dispatchers.IO) {
            try {
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
                ex.printStackTrace()
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
                ex.printStackTrace()
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
                    PrefManager.getStringValue(INSTANCE_ID)
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
                        AppAnalytics.create(AnalyticsEvent.LOGIN_WITH_TRUECALLER.NAME)
                            .addParam(AnalyticsEvent.VERIFIED_VIA_TRUECALLER.NAME, true).push()
                    }
                    return@launch
                }
            } catch (ex: Throwable) {
                ex.printStackTrace()
            }
            _signUpStatus.postValue(SignUpStepStatus.ERROR)
        }
    }

    fun signUpAfterPhoneVerify(countryCode: String, mNumber: String) {
        progressBarStatus.postValue(true)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val reqObj = RequestUserVerification(
                    PrefManager.getStringValue(INSTANCE_ID),
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
                        AppAnalytics.create(AnalyticsEvent.LOGIN_WITH_TRUECALLER.NAME)
                            .addParam(AnalyticsEvent.VERIFIED_VIA_TRUECALLER.NAME, true).push()
                    }
                    return@launch
                }
            } catch (ex: Throwable) {
                ex.printStackTrace()
            }
            _signUpStatus.postValue(SignUpStepStatus.ERROR)
        }
    }

    fun verifyOTP(otp: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val reqObj = RequestVerifyOTP(
                    PrefManager.getStringValue(INSTANCE_ID),
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
                        AppAnalytics.create(AnalyticsEvent.OTP_VERIFIED.NAME)
                            .addParam(AnalyticsEvent.OTP_VERIFIED.NAME, true).push()
                    }
                    return@launch
                } else {
                    if (response.code() == 400) {
                        _signUpStatus.postValue(SignUpStepStatus.WRONG_OTP)
                        return@launch
                    }
                }
            } catch (ex: Throwable) {
                ex.printStackTrace()
            }
            _signUpStatus.postValue(SignUpStepStatus.ERROR)
        }

    }


    private fun updateFromLoginResponse(loginResponse: LoginResponse) {
        val user = User.getInstance()
        user.userId = loginResponse.userId
        user.source = loginResponse.createdSource ?: EMPTY
        user.token = loginResponse.token
        User.update(user.toString())
        PrefManager.put(API_TOKEN, loginResponse.token)
        Mentor.getInstance()
            .setId(loginResponse.mentorId)
            .setReferralCode(loginResponse.referralCode)
            .setUserId(loginResponse.userId)
            .update()
        AppAnalytics.updateUser()
        WorkMangerAdmin.requiredTaskAfterLoginComplete()
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
                            User.update(it.toString())
                        }
                        AppAnalytics.updateUser()
                        analyzeUserProfile()
                    }
                    return@launch
                }
            } catch (ex: Throwable) {
                ex.printStackTrace()
            }
            _signUpStatus.postValue(SignUpStepStatus.ERROR)
        }
    }

    private fun analyzeUserProfile() {
        val user = User.getInstance()
        if (user.email.isNotEmpty() && user.phoneNumber.isNotEmpty()) {
            if (user.firstName.isEmpty() || user.dateOfBirth.isNullOrEmpty()) {
                _signUpStatus.postValue(SignUpStepStatus.ProfileInCompleted)
            } else {
                _signUpStatus.postValue(SignUpStepStatus.SignUpCompleted)
            }
        } else {
            _signUpStatus.postValue(SignUpStepStatus.ProfileInCompleted)
        }
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
                        User.getInstance().updateFromResponse(this)
                        _signUpStatus.postValue(SignUpStepStatus.ProfileCompleted)
                    }
                    return@launch
                }
            } catch (ex: Throwable) {
                ex.printStackTrace()
            }
            _signUpStatus.postValue(SignUpStepStatus.ERROR)
        }
    }


    private fun registerSMSReceiver() {
        val client = SmsRetriever.getClient(context)
        val task: Task<Void> = client.startSmsRetriever()
        task.addOnSuccessListener {

        }
        task.addOnFailureListener {

        }
        task.addOnCompleteListener {

        }
    }


}
