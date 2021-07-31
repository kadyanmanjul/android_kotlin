package com.joshtalks.joshskills.ui.signup

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.joshtalks.joshskills.core.ApiCallStatus
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.IS_GUEST_ENROLLED
import com.joshtalks.joshskills.core.PrefManager
import com.joshtalks.joshskills.util.showAppropriateMsg
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FreeTrialViewModel(application: Application) : AndroidViewModel(application) {
    val apiStatus: MutableLiveData<ApiCallStatus> = MutableLiveData()

    fun startFreeTrial(mentorId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                apiStatus.postValue(ApiCallStatus.START)
                val resp =
                    AppObjectController.commonNetworkService.enrollFreeTrialMentorWithCourse(mapOf("mentor_id" to mentorId))

                Log.d("Manjul ", "startFreeTrial() called $resp")
                Log.d("Manjul ", "startFreeTrial() called ${resp.isSuccessful}")

                if (resp.isSuccessful){
                    PrefManager.put(IS_GUEST_ENROLLED, value = true)
                    apiStatus.postValue(ApiCallStatus.SUCCESS)
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
}
