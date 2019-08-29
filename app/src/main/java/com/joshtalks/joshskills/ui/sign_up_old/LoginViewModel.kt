package com.joshtalks.joshskills.ui.sign_up_old

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.PrefManager
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.joshskills.repository.local.model.User
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.repository.server.AccountKitRequest
import com.joshtalks.joshskills.repository.server.CreateAccountResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext

class LoginViewModel(application: Application) : AndroidViewModel(application) {

    var loginStatusCallback = MutableLiveData<Boolean>()


    fun onAuthorizationCodeFetched(authorizationCode: String) {
        viewModelScope.async(Dispatchers.IO) {
            var reqObj = AccountKitRequest(
                authorizationCode,
                Utils.getDeviceId(),
                PrefManager.getClientToken()
            )
            try {
                val createAccountResponse: CreateAccountResponse =
                    AppObjectController.networkService.accountKitAuthorizationAsync(reqObj).await()


                val user = User.getInstance()
                user.id = createAccountResponse.user_id
                user.source = "OTP"
                user.token = createAccountResponse.token
                User.update(user.toString())

                Mentor.getInstance()
                    .setId(createAccountResponse.mentor_id)
                    .update()

                fetchMentor()

            } catch (e: Exception) {
                e.printStackTrace()
                //showError("Something went wrong! Please try again!")
                loginStatusCallback.postValue(false)
            }

        }

    }

    private fun fetchMentor() {

        viewModelScope.async(Dispatchers.IO) {
            try {
                val mentor: Mentor =
                    AppObjectController.networkService.getPersonalProfileAsync(Mentor.getInstance().getId())
                        .await()

                Mentor.getInstance().updateFromResponse(mentor)
                mentor.getUser()?.let {
                    val user = User.getInstance()
                    user.phoneNumber=it.phoneNumber
                    user.username=it.username
                    user.userType=it.userType
                    User.update(user.toString())
                }
                withContext(Dispatchers.Main){
                    loginStatusCallback.postValue(true)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                //showError("Something went wrong! Please try again!")
                loginStatusCallback.postValue(false)
            }

        }

    }


}