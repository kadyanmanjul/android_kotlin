package com.joshtalks.joshskills.ui.signup

import android.app.Application
import androidx.databinding.ObservableField
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.phone.SmsRetriever
import com.google.android.gms.tasks.Task
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.core.analytics.*
import com.joshtalks.joshskills.core.service.WorkMangerAdmin
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.repository.local.model.User
import com.joshtalks.joshskills.repository.server.LoginResponse
import com.joshtalks.joshskills.repository.server.RequestVerifyOTP
import com.joshtalks.joshskills.repository.server.TrueCallerLoginRequest
import com.joshtalks.joshskills.util.BindableString
import com.truecaller.android.sdk.TrueProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class SignUpViewModel(application: Application) : AndroidViewModel(application) {
    var context: JoshApplication = getApplication()
    var text = BindableString()
    var phoneNumber = EMPTY
    var countryCode = EMPTY
    var resendAttempt: Int =1
    var incorrectAttempt: Int = 0
    var currentTime: Long = 0
    val signUpStatus = MutableLiveData<SignUpStepStatus>()
    val progressDialogStatus = MutableLiveData<Boolean>()
    val otpVerifyStatus = MutableLiveData<Boolean>()
    val otpField = ObservableField<String>()


    fun networkCallForOtp(country_code: String, mobileNumber: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val reqObj = mapOf("mobile" to mobileNumber)
                val resp = AppObjectController.signUpNetworkService.getOtpForNumberAsync(reqObj)
                progressDialogStatus.postValue(false)
                if (resp.isSuccessful) {
                    phoneNumber = mobileNumber
                    countryCode = country_code
                    registerSMSReceiver()
                    signUpStatus.postValue(SignUpStepStatus.SignUpStepSecond)
                } else {
                    LogException.catchError(ErrorTag.OTP_REQUEST, resp.message())
                    showToast(context.getString(R.string.generic_message_for_error))
                }
            } catch (ex: Throwable) {
                LogException.catchException(ex)
                progressDialogStatus.postValue(false)
                when (ex) {
                    is HttpException -> {
                    }
                    is SocketTimeoutException, is UnknownHostException -> {
                        showToast(context.getString(R.string.internet_not_available_msz))
                    }
                    else -> {
                    }
                }
            }
        }
    }

    fun resendOTP(mobileNumber: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val reqObj = mapOf("mobile" to mobileNumber)
                val resp = AppObjectController.signUpNetworkService.getOtpForNumberAsync(reqObj)
                progressDialogStatus.postValue(false)
                if (resp.isSuccessful) {
                    signUpStatus.postValue(SignUpStepStatus.SignUpResendOTP)
                } else {
                    LogException.catchError(ErrorTag.OTP_REQUEST, resp.message())
                    showToast(context.getString(R.string.generic_message_for_error))
                }
            } catch (ex: Throwable) {
                LogException.catchException(ex)
                progressDialogStatus.postValue(false)
                when (ex) {
                    is HttpException -> {
                    }
                    is SocketTimeoutException, is UnknownHostException -> {
                        showToast(context.getString(R.string.internet_not_available_msz))
                    }
                    else -> {
                    }
                }
            }
        }
    }

    fun verifyOTP(otp: String?) {

        viewModelScope.launch(Dispatchers.IO) {
            try {

                val reqObj = RequestVerifyOTP(phoneNumber, otp ?: otpField.get()!!)
                val response: LoginResponse =
                    AppObjectController.signUpNetworkService.verifyOTP(reqObj).await()
                MarketingAnalytics.completeRegistrationAnalytics(
                    response.isUserExist,
                    RegistrationMethods.MOBILE_NUMBER
                )
                val user = User.getInstance()
                user.id = response.userId
                user.source = "OTP"
                user.token = response.token
                User.update(user.toString())
                Mentor.getInstance()
                    .setId(response.mentorId)
                    .setReferralCode(response.referralCode)
                    .update()
                AppAnalytics.updateUser()
                AppAnalytics.create(AnalyticsEvent.OTP_VERIFIED.NAME).push()
                fetchMentor()

            } catch (ex: Throwable) {
                LogException.catchException(ex)
                progressDialogStatus.postValue(false)
                when (ex) {
                    is HttpException -> {
                        if (ex.code() == 400) {
                            otpVerifyStatus.postValue(true)
                        }
                    }
                    is SocketTimeoutException, is UnknownHostException -> {
                        showToast(context.getString(R.string.internet_not_available_msz))
                    }
                    else -> {
                    }
                }
            }
        }

    }

    fun verifyUserViaTrueCaller(profile: TrueProfile) {
        viewModelScope.launch(Dispatchers.IO) {
            try {

                val trueCallerLoginRequest = TrueCallerLoginRequest(
                    profile.payload,
                    profile.signature,
                    profile.signatureAlgorithm
                )
                val response: LoginResponse =
                    AppObjectController.signUpNetworkService.verifyViaTrueCaller(
                        trueCallerLoginRequest
                    ).await()
                MarketingAnalytics.completeRegistrationAnalytics(
                    response.isUserExist,
                    RegistrationMethods.MOBILE_NUMBER
                )

                val user = User.getInstance()
                user.id = response.userId
                user.source = "OTP"
                user.token = response.token

                User.update(user.toString())
                Mentor.getInstance()
                    .setId(response.mentorId)
                    .setReferralCode(response.referralCode)
                    .update()

                AppAnalytics.updateUser()
                AppAnalytics.create(AnalyticsEvent.LOGIN_WITH_TRUECALLER.NAME)
                    .addParam(AnalyticsEvent.VERIFIED_VIA_TRUECALLER.NAME, true).push()
                fetchMentor()

            } catch (ex: Throwable) {
                LogException.catchException(ex)
                progressDialogStatus.postValue(false)
                when (ex) {
                    is HttpException -> {
                    }
                    is SocketTimeoutException, is UnknownHostException -> {
                        showToast(context.getString(R.string.internet_not_available_msz))
                    }
                    else -> {
                    }
                }
            }
        }
    }


    private fun fetchMentor() {
        WorkMangerAdmin.requiredTaskAfterLoginComplete()
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val mentor: Mentor =
                    AppObjectController.signUpNetworkService.getPersonalProfileAsync(
                        Mentor.getInstance().getId()
                    )
                        .await()
                Mentor.getInstance().updateFromResponse(mentor)
                mentor.getUser()?.let {
                    User.getInstance().updateFromResponse(it)
                }
                AppAnalytics.updateUser()
                signUpStatus.postValue(SignUpStepStatus.SignUpCompleted)

            } catch (ex: Throwable) {
                LogException.catchException(ex)
                progressDialogStatus.postValue(false)
                when (ex) {
                    is HttpException -> {
                    }
                    is SocketTimeoutException, is UnknownHostException -> {
                        showToast(context.getString(R.string.internet_not_available_msz))
                    }
                    else -> {
                    }
                }
            }
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

    fun incrementResendAttempts() {
        resendAttempt = resendAttempt + 1
    }

    fun incrementIncorrectAttempts() {
        incorrectAttempt = incorrectAttempt + 1
    }
}
