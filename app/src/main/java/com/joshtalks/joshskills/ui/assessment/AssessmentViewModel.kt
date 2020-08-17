package com.joshtalks.joshskills.ui.assessment

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.joshtalks.joshskills.core.ApiCallStatus
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.repository.local.model.assessment.AssessmentQuestionWithRelations
import com.joshtalks.joshskills.repository.local.model.assessment.AssessmentWithRelations
import com.joshtalks.joshskills.repository.server.assessment.AssessmentRequest
import com.joshtalks.joshskills.repository.server.assessment.AssessmentResponse
import com.joshtalks.joshskills.repository.server.assessment.AssessmentStatus
import com.joshtalks.joshskills.repository.server.assessment.AssessmentType
import com.joshtalks.joshskills.repository.server.assessment.ChoiceType
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
                    if (assessmentWithRelations.assessment.type == AssessmentType.TEST) {
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
                    } else {
                        assessmentLiveData.postValue(assessmentWithRelations)
                    }
                } else {
                    val response = getAssessmentFromServer(assessmentId)
                    if (response.isSuccessful) {
                        apiCallStatusLiveData.postValue(ApiCallStatus.SUCCESS)
                        response.body()?.let {
                            if (it.status == AssessmentStatus.COMPLETED) {
                                assessmentStatus.postValue(it.status)
                            } else {
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

    fun getAssessmentType() = assessmentLiveData.value?.assessment?.type

    private suspend fun insertAssessmentToDB(assessmentResponse: AssessmentResponse) =
        AppObjectController.appDatabase.assessmentDao()
            .insertAssessmentFromResponse(assessmentResponse)

    private fun getAssessmentFromDB(assessmentId: Int) =
        AppObjectController.appDatabase.assessmentDao().getAssessmentById(assessmentId)

    private suspend fun getAssessmentFromServer(assessmentId: Int) =
        AppObjectController.chatNetworkService.getAssessmentId(assessmentId)

    private fun getAssessmentResponse(assessmentWithRelations: AssessmentWithRelations) =
        AssessmentRequest((assessmentWithRelations))


    fun saveAssessmentQuestion(assessmentQuestion: AssessmentQuestionWithRelations) {
        CoroutineScope(Dispatchers.IO).launch {
            AppObjectController.appDatabase.assessmentDao()
                .insertAssessmentQuestion(assessmentQuestion)
        }
    }

    fun postTestData(assessmentId: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            val assessmentWithRelations = assessmentLiveData.value ?: return@launch
            updateAllQuestionsAttemptedStatus(assessmentWithRelations)
            updateAssessmentStatus(assessmentId)
            jobs += viewModelScope.launch(Dispatchers.IO) {
                try {
                    val assessmentRequest = getAssessmentResponse(assessmentWithRelations)
                    AppObjectController.chatNetworkService.submitTestAsync(assessmentRequest)

                } catch (ex: Throwable) {
                    ex.showAppropriateMsg()
                }
                apiCallStatusLiveData.postValue(ApiCallStatus.FAILED)
            }
        }
    }

    suspend fun updateAssessmentStatus(assessmentId: Int) {
        if (assessmentId == -1) {
            return
        }
        AppObjectController.appDatabase.assessmentDao()
            .updateAssessmentStatus(assessmentId, AssessmentStatus.COMPLETED)
        assessmentStatus.postValue(AssessmentStatus.COMPLETED)
        assessmentLiveData.postValue(assessmentLiveData.value?.apply {
            this.assessment.status = AssessmentStatus.COMPLETED
        })

    }

    private suspend fun updateAllQuestionsAttemptedStatus(assessmentWithRelations: AssessmentWithRelations) {
        assessmentWithRelations.questionList.forEach { questionWithRelations ->
            when (questionWithRelations.question.choiceType) {
                ChoiceType.SINGLE_SELECTION_TEXT,
                ChoiceType.MULTI_SELECTION_TEXT,
                ChoiceType.SINGLE_SELECTION_IMAGE,
                ChoiceType.MULTI_SELECTION_IMAGE -> {
                    questionWithRelations.question.isAttempted = false

                    run loop@{
                        questionWithRelations.choiceList.forEach {
                            if (it.isSelectedByUser) {
                                questionWithRelations.question.isAttempted = true
                                return@loop
                            }
                        }
                    }
                }
                ChoiceType.FILL_IN_THE_BLANKS_TEXT, ChoiceType.MATCH_TEXT -> {
                    questionWithRelations.question.isAttempted = true

                    questionWithRelations.choiceList.forEach {
                        it.userSelectedOrder = it.userSelectedOrder.plus(1)
                    }

                    run loop1@{
                        questionWithRelations.choiceList.forEach {
                            if (it.isSelectedByUser.not()) {
                                questionWithRelations.question.isAttempted = false
                                return@loop1
                            }
                        }
                    }
                }
            }
            AppObjectController.appDatabase.assessmentDao()
                .updateAssessmentQuestionWithoutRelation(questionWithRelations.question)
        }
    }

    fun getTestReport(assessmentId: Int) {

        jobs += viewModelScope.launch(Dispatchers.IO) {
            try {
                val resp =
                    AppObjectController.chatNetworkService.getTestReport(assessmentId)
                if (resp.isSuccessful && resp.body() != null) {
                    apiCallStatusLiveData.postValue(ApiCallStatus.SUCCESS)
                    resp.body()?.questions?.filter { it.choiceType == ChoiceType.MATCH_TEXT }
                        ?.forEach {
                            it.choices.forEach {
                                it.userSelectedOrder = it.userSelectedOrder.minus(1)
                            }
                        }
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
