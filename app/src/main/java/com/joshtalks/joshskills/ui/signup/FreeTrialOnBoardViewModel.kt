package com.joshtalks.joshskills.ui.signup

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.core.analytics.MarketingAnalytics
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.repository.server.TrueCallerLoginRequest
import com.joshtalks.joshskills.util.showAppropriateMsg
import com.truecaller.android.sdk.TrueProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

class FreeTrialOnBoardViewModel(application: Application) : AndroidViewModel(application) {

    val signUpStatus: MutableLiveData<SignUpStepStatus> = MutableLiveData()
    val progressBarStatus: MutableLiveData<Boolean> = MutableLiveData()
    val service = AppObjectController.signUpNetworkService
    var userName: String? = null
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
                if (response.isSuccessful) {
                    response.body()?.run {
                        MarketingAnalytics.completeRegistrationAnalytics(
                            this.newUser,
                            RegistrationMethods.TRUE_CALLER
                        )
                    }
                }
            } catch (ex: Throwable) {
                ex.showAppropriateMsg()
            }
        }
    }
}