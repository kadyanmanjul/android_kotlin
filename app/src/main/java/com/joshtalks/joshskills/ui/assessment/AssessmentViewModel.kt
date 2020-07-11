package com.joshtalks.joshskills.ui.assessment

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.joshtalks.joshskills.core.ApiCallStatus
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.loadJSONFromAsset
import com.joshtalks.joshskills.repository.local.model.assessment.Assessment
import com.joshtalks.joshskills.repository.local.model.assessment.AssessmentQuestion
import com.joshtalks.joshskills.repository.local.model.assessment.Choice
import com.joshtalks.joshskills.repository.server.assessment.AssessmentResponse
import com.joshtalks.joshskills.util.showAppropriateMsg
import kotlinx.coroutines.CoroutineScope
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
                mockData()
            }
            apiCallStatusLiveData.postValue(ApiCallStatus.FAILED)
        }
    }

    private fun mockData() {
        val assessmentResponse = AppObjectController.gsonMapperForLocal.fromJson(
            loadJSONFromAsset("assessmentJson.json"),
            AssessmentResponse::class.java
        )
        Log.d("Assessment123", "AssessmentResponse = $assessmentResponse")
        CoroutineScope(Dispatchers.IO).launch {
            AppObjectController.appDatabase.assessmentDao()
                .insertAssessment(Assessment(assessmentResponse))
            assessmentResponse.questions.forEach { questionResponse ->
                AppObjectController.appDatabase.assessmentDao()
                    .insertAssessmentQuestion(
                        AssessmentQuestion(
                            questionResponse,
                            assessmentResponse.id
                        )
                    )
                questionResponse.choices.forEach { choiceResponse ->
                    AppObjectController.appDatabase.assessmentDao()
                        .insertAssessmentChoice(Choice(choiceResponse, questionResponse.id))
                }

            }
            val questions = AppObjectController.appDatabase.assessmentDao().loadAssesment(1)
            val choices = AppObjectController.appDatabase.assessmentDao().loadChoice(1)
        }
    }
}
