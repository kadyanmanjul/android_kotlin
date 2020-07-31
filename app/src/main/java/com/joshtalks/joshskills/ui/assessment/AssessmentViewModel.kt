package com.joshtalks.joshskills.ui.assessment

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.joshtalks.joshskills.core.ApiCallStatus
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.repository.local.model.assessment.AssessmentQuestionWithRelations
import com.joshtalks.joshskills.repository.local.model.assessment.AssessmentWithRelations
import com.joshtalks.joshskills.repository.server.assessment.AssessmentResponse
import com.joshtalks.joshskills.repository.server.assessment.AssessmentStatus
import com.joshtalks.joshskills.repository.server.assessment.AssessmentType
import com.joshtalks.joshskills.util.showAppropriateMsg
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class AssessmentViewModel(application: Application) : AndroidViewModel(application) {
    private val jobs = arrayListOf<Job>()
    val apiCallStatusLiveData: MutableLiveData<ApiCallStatus> = MutableLiveData()
    val assessmentLiveData: MutableLiveData<AssessmentWithRelations> = MutableLiveData()
    val assessmentStatus: MutableLiveData<AssessmentStatus> =
        MutableLiveData(AssessmentStatus.NOT_STARTED)

    fun fetchAssessmentDetails(assessmentId: Int) {
        jobs += viewModelScope.launch(Dispatchers.IO) {
            try {
                var assessmentWithRelations = getAssessmentFromDB(assessmentId)

                if (assessmentWithRelations != null) {
                    if (assessmentWithRelations.assessment.type == AssessmentType.TEST)
                        when (assessmentWithRelations.assessment.status) {
                            AssessmentStatus.COMPLETED -> {
                                assessmentStatus.postValue(assessmentWithRelations.assessment.status)
                            }
                            AssessmentStatus.NOT_STARTED, AssessmentStatus.STARTED -> {
                                assessmentLiveData.postValue(
                                    assessmentWithRelations
                                )
                                assessmentStatus.postValue(assessmentWithRelations.assessment.status)
                            }
                        }
                    else {
                        assessmentLiveData.postValue(assessmentWithRelations)
                    }
                } else {
                    val response = getAssessmentFromServer(assessmentId)
                    if (response.isSuccessful) {
                        apiCallStatusLiveData.postValue(ApiCallStatus.SUCCESS)
                        response.body()?.let {
                            if(it.status==AssessmentStatus.COMPLETED){
                                assessmentStatus.postValue(it.status)
                            }
                            else {
                                insertAssessmentToDB(it)
                                assessmentWithRelations = getAssessmentFromDB(assessmentId)
                                assessmentLiveData.postValue(assessmentWithRelations)
                            }
                        }
                        return@launch
                    }
                }

            } catch (ex: Throwable) {
                ex.showAppropriateMsg()
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

    private suspend fun getAssessmentResponse(assessmentWithRelations: AssessmentWithRelations) =
        AssessmentResponse((assessmentWithRelations))

    fun saveAssessmentQuestion(assessmentQuestion: AssessmentQuestionWithRelations) {
        CoroutineScope(Dispatchers.IO).launch {
            AppObjectController.appDatabase.assessmentDao()
                .insertAssessmentQuestion(assessmentQuestion)
        }
    }

    fun updateAssessmentStatus(assessmentId: Int) {
        if (assessmentId == -1)
            return
        CoroutineScope(Dispatchers.IO).launch {
            AppObjectController.appDatabase.assessmentDao()
                .updateAssessmentStatus(assessmentId, AssessmentStatus.COMPLETED)
        }
    }

    fun postTestData(assessmentId: Int) {
        val assessmentWithRelations = assessmentLiveData.value ?: return
        updateAssessmentStatus(assessmentId)
        jobs += viewModelScope.launch(Dispatchers.IO) {
            try {
                val assessmentResponse = getAssessmentResponse(assessmentWithRelations)
                AppObjectController.chatNetworkService.submitTestAsync(assessmentResponse)

            } catch (ex: Throwable) {
                ex.showAppropriateMsg()
            }
            apiCallStatusLiveData.postValue(ApiCallStatus.FAILED)
        }
    }

    fun getTestReport(assessmentId: Int) {

        jobs += viewModelScope.launch(Dispatchers.IO) {
            try {
                val resp =
                    AppObjectController.chatNetworkService.getTestReport(assessmentId)
                if (resp.isSuccessful && resp.body() != null) {
                    apiCallStatusLiveData.postValue(ApiCallStatus.SUCCESS)
                    assessmentLiveData.postValue(AssessmentWithRelations(resp.body()!!))
                    return@launch
                }
            } catch (ex: Throwable) {
                ex.showAppropriateMsg()
            }
            apiCallStatusLiveData.postValue(ApiCallStatus.FAILED)
        }
    }
}
