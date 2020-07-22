package com.joshtalks.joshskills.ui.assessment

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.joshtalks.joshskills.core.ApiCallStatus
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.loadJSONFromAsset
import com.joshtalks.joshskills.repository.local.model.assessment.AssessmentQuestionWithRelations
import com.joshtalks.joshskills.repository.local.model.assessment.AssessmentWithRelations
import com.joshtalks.joshskills.repository.server.assessment.AssessmentResponse
import com.joshtalks.joshskills.util.showAppropriateMsg
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class AssessmentViewModel(application: Application) : AndroidViewModel(application) {
    private val jobs = arrayListOf<Job>()
    val apiCallStatusLiveData: MutableLiveData<ApiCallStatus> = MutableLiveData()
    val assessmentLiveData: MutableLiveData<AssessmentWithRelations> = MutableLiveData()

    fun fetchAssessmentDetails(assessmentId: Int) {
        jobs += viewModelScope.launch(Dispatchers.IO) {
            try {
                val assessmentWithRelations = getAssessmentFromDB(assessmentId)

                if (assessmentWithRelations != null) {
                    assessmentLiveData.postValue(assessmentWithRelations)
                } else {
                    val response = getAssessmentFromServer(assessmentId)
                    if (response.isSuccessful) {
                        apiCallStatusLiveData.postValue(ApiCallStatus.SUCCESS)
                        response.body()?.let {
                            insertAssessmentToDB(it)
                            assessmentLiveData.postValue(AssessmentWithRelations(it))
                        }
                        return@launch
                    }
                }

            } catch (ex: Throwable) {
                ex.showAppropriateMsg()
                mockData()
            }
            apiCallStatusLiveData.postValue(ApiCallStatus.FAILED)
        }
    }

    private suspend fun insertAssessmentToDB(assessmentResponse: AssessmentResponse) =
        AppObjectController.appDatabase.assessmentDao()
            .insertAssessmentFromResponse(assessmentResponse)

    private fun getAssessmentFromDB(assessmentId: Int) =
        AppObjectController.appDatabase.assessmentDao().getAssessmentById(assessmentId)

    private suspend fun getAssessmentFromServer(assessmentId: Int) =
        AppObjectController.chatNetworkService.getAssessmentId(assessmentId)

    private fun mockData() {
        val assessmentResponse = AppObjectController.gsonMapperForLocal.fromJson(
            loadJSONFromAsset("assessmentJson.json"),
            AssessmentResponse::class.java
        )
        CoroutineScope(Dispatchers.IO).launch {
            AppObjectController.appDatabase.assessmentDao()
                .insertAssessment(AssessmentWithRelations(assessmentResponse))
            val assessmentWithRelations = getAssessmentFromDB(1)
            assessmentLiveData.postValue(assessmentWithRelations)

        }
    }

    fun saveAssessmentQuestion(assessmentQuestion: AssessmentQuestionWithRelations) {
        CoroutineScope(Dispatchers.IO).launch {
            AppObjectController.appDatabase.assessmentDao()
                .insertAssessmentQuestion(assessmentQuestion)
        }
    }

    fun postTestData(assessmentWithRelations: AssessmentWithRelations) {
        jobs += viewModelScope.launch(Dispatchers.IO) {
            try {

                val resp = AppObjectController.chatNetworkService.submitTestAsync(assessmentWithRelations)
                if (resp.isSuccessful && resp.body() != null) {
                    apiCallStatusLiveData.postValue(ApiCallStatus.SUCCESS)
                    return@launch
                }
            } catch (ex: Throwable) {
                ex.showAppropriateMsg()
                mockData()
            }
            apiCallStatusLiveData.postValue(ApiCallStatus.FAILED)
        }
    }
}
