package com.joshtalks.joshskills.ui.assessment

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.joshtalks.joshskills.core.ApiCallStatus
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.JoshApplication
import com.joshtalks.joshskills.repository.local.model.assessment.AssessmentWithRelations
import com.joshtalks.joshskills.repository.server.FAQ
import com.joshtalks.joshskills.util.showAppropriateMsg
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class AssessmentTestViewModel(application: Application) :
    AndroidViewModel(application) { // TODO - Remove this Class
    var context: JoshApplication = getApplication()
    val assessmentLiveData: MutableLiveData<AssessmentWithRelations> =
        MutableLiveData()
    val apiCallStatusLiveData: MutableLiveData<ApiCallStatus> = MutableLiveData()
    val choiceListLiveData: MutableLiveData<List<FAQ>> = MutableLiveData()
    private val jobs = arrayListOf<Job>()

    fun getAssessmentFromDao(assessmentid: Int) {
        jobs += viewModelScope.launch(Dispatchers.IO) {
            try {
                val response =
                    AppObjectController.appDatabase.assessmentDao().getAssessmentById(assessmentid)
                assessmentLiveData.postValue(response)
                return@launch

            } catch (ex: Exception) {
                ex.showAppropriateMsg()
            }
        }
    }

    fun getAssessmentFromAPI(assesmentId: Int) {
        jobs += viewModelScope.launch(Dispatchers.IO) {
            try {
                val response =
                    AppObjectController.chatNetworkService.getTestReport(assesmentId)
                if (response.isSuccessful) {
                    apiCallStatusLiveData.postValue(ApiCallStatus.SUCCESS)
                    assessmentLiveData.postValue(response.body())
                    return@launch
                }

            } catch (ex: Exception) {
                ex.showAppropriateMsg()
            }
            apiCallStatusLiveData.postValue(ApiCallStatus.FAILED)
        }
    }

    fun postAssessment(assessmentWithRelations: AssessmentWithRelations) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                AppObjectController.chatNetworkService.submitTestAsync(assessmentWithRelations)
            } catch (ex: Exception) {
                ex.showAppropriateMsg()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        jobs.forEach { it.cancel() } // cancels the job and waits for its completion
    }
}
