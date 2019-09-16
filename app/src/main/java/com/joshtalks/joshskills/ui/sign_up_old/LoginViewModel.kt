package com.joshtalks.joshskills.ui.sign_up_old

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.PrefManager
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.repository.local.model.User
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.repository.server.AccountKitRequest
import com.joshtalks.joshskills.repository.server.CreateAccountResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginViewModel(application: Application) : AndroidViewModel(application) {

    var loginStatusCallback = MutableLiveData<Boolean>()

    fun onAuthorizationCodeFetched(authorizationCode: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val reqObj = AccountKitRequest(
                authorizationCode,
                Utils.getDeviceId(),
                PrefManager.getClientToken()
            )
            try {
                val createAccountResponse: CreateAccountResponse =
                    AppObjectController.signUpNetworkService.accountKitAuthorizationAsync(reqObj).await()

                val user = User.getInstance()
                user.id = createAccountResponse.user_id
                user.source = "OTP"
                user.token = createAccountResponse.token
                User.update(user.toString())

                Mentor.getInstance()
                    .setId(createAccountResponse.mentor_id)
                    .update()

                fetchMentor()

                AppAnalytics.updateUser()

            } catch (e: Exception) {
                e.printStackTrace()
                loginStatusCallback.postValue(false)
            }

        }

    }

    private fun fetchMentor() {

        viewModelScope.launch (Dispatchers.IO) {
            try {
                val mentor: Mentor =
                    AppObjectController.signUpNetworkService.getPersonalProfileAsync(Mentor.getInstance().getId())
                        .await()

                Mentor.getInstance().updateFromResponse(mentor)
                mentor.getUser()?.let {
                    User.getInstance().updateFromResponse(it)
                }

                AppAnalytics.updateUser()

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