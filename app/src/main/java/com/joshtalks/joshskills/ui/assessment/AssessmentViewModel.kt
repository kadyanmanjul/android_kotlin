package com.joshtalks.joshskills.ui.assessment

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.joshtalks.joshskills.core.ApiCallStatus
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.repository.server.assessment.AssessmentResponse
import com.joshtalks.joshskills.util.showAppropriateMsg
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class AssessmentViewModel(application: Application) : AndroidViewModel(application) {
    private val jobs = arrayListOf<Job>()
    val apiCallStatusLiveData: MutableLiveData<ApiCallStatus> = MutableLiveData()
    val assessmentLiveData: MutableLiveData<AssessmentResponse> = MutableLiveData()

    fun fetchAssessmentDetails(assessmentId: Int) {
        jobs += viewModelScope.launch(Dispatchers.IO) {
            try {
                val response =
                    AppObjectController.chatNetworkService.getAssessmentId(assessmentId)
                if (response.isSuccessful) {
                    apiCallStatusLiveData.postValue(ApiCallStatus.SUCCESS)
                    assessmentLiveData.postValue(response.body())
                    // TODO Save to DB
                    return@launch
                }

            } catch (ex: Throwable) {
                ex.showAppropriateMsg()
            }
            apiCallStatusLiveData.postValue(ApiCallStatus.FAILED)
        }
    }
}
