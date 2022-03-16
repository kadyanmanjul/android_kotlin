package com.joshtalks.badebhaiya.signup.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.joshtalks.badebhaiya.core.API_TOKEN
import com.joshtalks.badebhaiya.core.PrefManager
import com.joshtalks.badebhaiya.core.SignUpStepStatus
import com.joshtalks.badebhaiya.repository.BBRepository
import com.joshtalks.badebhaiya.repository.model.User
import com.joshtalks.badebhaiya.signup.request.VerifyOTPRequest
import com.joshtalks.badebhaiya.signup.response.LoginResponse
import kotlinx.coroutines.launch

class SignUpViewModel(application: Application): AndroidViewModel(application) {
    val repository = BBRepository()
    val signUpStatus = MutableLiveData<SignUpStepStatus>()

    fun sendPhoneNumberForOTP(phoneNumber: String, countryCode: String) {
        viewModelScope.launch {
            try {
                val reqObj = mapOf("country_code" to countryCode, "mobile" to phoneNumber)
                repository.sendPhoneNumberForOTP(reqObj)
                signUpStatus.value = SignUpStepStatus.RequestForOTP
            } catch (ex: Exception) {

            }
        }
    }

    fun verifyOTP(otp: String, phoneNumber: String) {
        viewModelScope.launch {
            try {
                val reqObj = VerifyOTPRequest("+91", phoneNumber, otp)
                val response = repository.verifyOTP(reqObj)
                Log.i("ayushg", "verifyOTP: $response")
                if (response.isSuccessful) {
                    response.body()?.let {
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

            }
        }
    }

    private fun updateUserFromLoginResponse(loginResponse: LoginResponse) {
        try {
            viewModelScope.launch {
                val response = repository.getUserDetailsForSignUp(User.getInstance().userId)
                if (response.isSuccessful) {
                    response.body()?.let {
                        User.getInstance().updateFromResponse(it)
                    }
                    PrefManager.put(API_TOKEN, loginResponse.token)
                }
            }
        } catch (ex: Exception) {

        }
    }
}