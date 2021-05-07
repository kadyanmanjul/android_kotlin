package com.joshtalks.joshskills.ui.online_test

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.repository.local.entity.*
import com.joshtalks.joshskills.repository.local.model.assessment.AssessmentQuestionWithRelations
import com.joshtalks.joshskills.repository.server.assessment.OnlineTestRequest
import com.joshtalks.joshskills.util.showAppropriateMsg
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

class OnlineTestViewModel(application: Application) : AndroidViewModel(application) {

    val lessonQuestionsLiveData: MutableLiveData<List<LessonQuestion>> = MutableLiveData()

    val grammarAssessmentLiveData: MutableLiveData<AssessmentQuestionWithRelations> =
        MutableLiveData()
    val apiStatus: MutableLiveData<ApiCallStatus> = MutableLiveData()


    private suspend fun getOnlineTest(): AssessmentQuestionWithRelations? {
        return withContext(viewModelScope.coroutineContext + Dispatchers.IO) {
            try {
                var assessmentRelations: AssessmentQuestionWithRelations
                val response = getOnlineTestServer()
                if (response.isSuccessful) {
                    response.body()?.let {
                        assessmentRelations = AssessmentQuestionWithRelations(it, 10)
                        return@withContext assessmentRelations
                    }
                }
            } catch (ex: Throwable) {
                ex.showAppropriateMsg()
                return@withContext null
            }
            return@withContext null
        }
    }

    private suspend fun getOnlineTestServer() =
        AppObjectController.chatNetworkService.getOnlineTestQuestion()

    fun fetchAssessmentDetails() {
        viewModelScope.launch(Dispatchers.IO) {
            getOnlineTest()?.let {
                grammarAssessmentLiveData.postValue(it)
            }
        }
    }

    fun postAnswerAndGetNewQuestion(assessmentWithRelations: AssessmentQuestionWithRelations) {
        postTestQuestionToServer(assessmentWithRelations)
    }

    fun postTestQuestionToServer(assessmentQuestion: AssessmentQuestionWithRelations) {
        viewModelScope.launch(Dispatchers.IO) {
            delay(1000)
            try {
                assessmentQuestion?.let {
                    val assessmentRequest = OnlineTestRequest(
                        assessmentQuestion.question.remoteId,
                        assessmentQuestion.question.status
                    )
                    val response =
                        AppObjectController.chatNetworkService.postAndGetNextOnlineTestQuestion(
                            assessmentRequest
                        )
                    if (response.isSuccessful) {
                        var assessmentRelations: AssessmentQuestionWithRelations
                        response.body()?.let {
                            assessmentRelations = AssessmentQuestionWithRelations(it, 10)
                            grammarAssessmentLiveData.postValue(assessmentRelations)
                        }
                    }
                }
            } catch (ex: Throwable) {
                Timber.e(ex)
            }
        }
    }
}
