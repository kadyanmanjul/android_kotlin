package com.joshtalks.joshskills.ui.signup

import android.app.Application
import androidx.databinding.ObservableField
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.facebook.appevents.AppEventsConstants.EVENT_NAME_COMPLETED_REGISTRATION
import com.google.android.gms.auth.api.phone.SmsRetriever
import com.google.android.gms.tasks.Task
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.core.service.WorkMangerAdmin
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.repository.local.model.User
import com.joshtalks.joshskills.repository.server.LoginResponse
import com.joshtalks.joshskills.repository.server.RequestVerifyOTP
import com.joshtalks.joshskills.repository.server.TrueCallerLoginRequest
import com.joshtalks.joshskills.util.BindableString
import com.truecaller.android.sdk.TrueProfile
import io.branch.referral.Branch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.HttpException


class SignUpViewModel(application: Application) : AndroidViewModel(application) {
    var context: JoshApplication = getApplication()
    var text = BindableString()
    var phoneNumber = EMPTY
    var countryCode = EMPTY
    val signUpStatus = MutableLiveData<SignUpStepStatus>()
    val progressDialogStatus = MutableLiveData<Boolean>()
    val otpVerifyStatus = MutableLiveData<Boolean>()

    val otpField = ObservableField<String>()


    fun networkCallForOtp(country_code: String, mobileNumber: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val reqObj = mapOf("mobile" to mobileNumber)
                AppObjectController.signUpNetworkService.getOtpForNumberAsync(reqObj).await()
                phoneNumber = mobileNumber
                countryCode = country_code
                progressDialogStatus.postValue(false)
                registerSMSReceiver()
                signUpStatus.postValue(SignUpStepStatus.SignUpStepSecond)

            } catch (ex: HttpException) {
                progressDialogStatus.postValue(false)
            } catch (e: Exception) {
                e.printStackTrace()
                progressDialogStatus.postValue(false)
            }
        }
    }

    fun resendOTP(mobileNumber: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val reqObj = mapOf("mobile" to mobileNumber)
                AppObjectController.signUpNetworkService.getOtpForNumberAsync(reqObj).await()
                progressDialogStatus.postValue(false)
                signUpStatus.postValue(SignUpStepStatus.SignUpResendOTP)
            } catch (ex: HttpException) {
                progressDialogStatus.postValue(false)
            } catch (e: Exception) {
                e.printStackTrace()
                progressDialogStatus.postValue(false)
            }
        }
    }

    fun verifyOTP(otp: String?) {

        viewModelScope.launch(Dispatchers.IO) {
            try {

                val reqObj = RequestVerifyOTP(phoneNumber, otp ?: otpField.get()!!)
                val response: LoginResponse =
                    AppObjectController.signUpNetworkService.verifyOTP(reqObj).await()
                AppAnalytics.create(AnalyticsEvent.LOGIN_WITH_OTP.NAME).push()
                val user = User.getInstance()
                user.id = response.userId
                user.source = "OTP"
                user.token = response.token
                User.update(user.toString())
                if (phoneNumber.isEmpty().not()) {
                    Branch.getInstance().setIdentity(user.phoneNumber)
                }
                Mentor.getInstance()
                    .setId(response.mentorId)
                    .setReferralCode(response.referralCode)
                    .update()

                AppAnalytics.updateUser()
                mergeMentorWithGId(response.mentorId)
                fetchMentor()
                WorkMangerAdmin.mappingGIDWithMentor()

            } catch (ex: HttpException) {
                if (ex.code() == 400) {
                    otpVerifyStatus.postValue(true)
                } else {
                    progressDialogStatus.postValue(false)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                progressDialogStatus.postValue(false)
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

                AppAnalytics.create(AnalyticsEvent.LOGIN_WITH_TRUECALLER.NAME).push()

                val user = User.getInstance()
                user.id = response.userId
                user.source = "OTP"
                user.token = response.token

                User.update(user.toString())
                if (phoneNumber.isEmpty().not()) {
                    Branch.getInstance().setIdentity(user.phoneNumber)
                }
                Mentor.getInstance()
                    .setId(response.mentorId)
                    .setReferralCode(response.referralCode)
                    .update()
                AppAnalytics.updateUser()
                mergeMentorWithGId(response.mentorId)
                fetchMentor()
                WorkMangerAdmin.mappingGIDWithMentor()
            } catch (ex: HttpException) {
                progressDialogStatus.postValue(false)
                ex.printStackTrace()
            } catch (e: Exception) {
                e.printStackTrace()
                progressDialogStatus.postValue(false)
            }
        }
    }


    private fun fetchMentor() {
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
                AppObjectController.facebookEventLogger.logEvent(EVENT_NAME_COMPLETED_REGISTRATION)
                if (User.getInstance().phoneNumber.isEmpty().not()) {
                    Branch.getInstance().setIdentity(User.getInstance().phoneNumber)
                }
                AppAnalytics.updateUser()
                signUpStatus.postValue(SignUpStepStatus.SignUpCompleted)

            } catch (e: Exception) {
                e.printStackTrace()
                progressDialogStatus.postValue(false)
                //showError("Something went wrong! Please try again!")
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

    private fun getCourseFromServer() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val courseList = AppObjectController.chatNetworkService.getRegisterCourses()
                if (courseList.isNullOrEmpty()) {
                    signUpStatus.postValue(SignUpStepStatus.CoursesNotExist)

                } else {
                    AppObjectController.appDatabase.courseDao().insertRegisterCourses(courseList)
                    signUpStatus.postValue(SignUpStepStatus.SignUpCompleted)

                }
            } catch (ex: Exception) {
                signUpStatus.postValue(SignUpStepStatus.SignUpCompleted)
                ex.printStackTrace()
            }

        }
    }

    private fun mergeMentorWithGId(mentorId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val id = PrefManager.getIntValue(SERVER_GID_ID)
                if (id == 0) {
                    return@launch
                }
                val data = mapOf("mentor" to mentorId)
                AppObjectController.chatNetworkService.mergeMentorWithGId(id.toString(), data)
                PrefManager.removeKey(SERVER_GID_ID)
            } catch (ex: Exception) {
                ex.printStackTrace()
            }

        }
    }


}
