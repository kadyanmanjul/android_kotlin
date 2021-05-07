package com.joshtalks.joshskills.ui.online_test

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.joshtalks.joshskills.core.ApiCallStatus
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.repository.local.entity.LessonQuestion
import com.joshtalks.joshskills.repository.local.model.assessment.AssessmentQuestionWithRelations
import com.joshtalks.joshskills.repository.server.assessment.OnlineTestRequest
import com.joshtalks.joshskills.repository.server.assessment.OnlineTestResponse
import com.joshtalks.joshskills.util.showAppropriateMsg
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber

class OnlineTestViewModel(application: Application) : AndroidViewModel(application) {

    val lessonQuestionsLiveData: MutableLiveData<List<LessonQuestion>> = MutableLiveData()
    val grammarAssessmentLiveData: MutableLiveData<OnlineTestResponse> = MutableLiveData()
    val message: MutableLiveData<String> = MutableLiveData()
    val apiStatus: MutableLiveData<ApiCallStatus> = MutableLiveData()


    fun fetchAssessmentDetails() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = getOnlineTestServer()
                if (response.isSuccessful) {
                    response.body()?.let {
                        response.body()?.let {
                            grammarAssessmentLiveData.postValue(it)
                        }
                    }
                }
            } catch (ex: Throwable) {
                ex.showAppropriateMsg()
            }
        }
    }

    private suspend fun getOnlineTestServer() =
        AppObjectController.chatNetworkService.getOnlineTestQuestion()


    fun postAnswerAndGetNewQuestion(assessmentWithRelations: AssessmentQuestionWithRelations) {
        postTestQuestionToServer(assessmentWithRelations)
    }

    fun postTestQuestionToServer(assessmentQuestion: AssessmentQuestionWithRelations) {
        viewModelScope.launch(Dispatchers.IO) {
            delay(1000)
            try {
                val choice = assessmentQuestion.choiceList.filter { it.isSelectedByUser }
                    .sortedBy { it.userSelectedOrder }
                var answerText: StringBuilder = StringBuilder(EMPTY)
                choice.forEach {
                    answerText = answerText.append(it.text)
                }
                val assessmentRequest = OnlineTestRequest(
                    assessmentQuestion, answerText.toString()
                )
                val response =
                    AppObjectController.chatNetworkService.postAndGetNextOnlineTestQuestion(
                        assessmentRequest
                    )
                if (response.isSuccessful) {
                    response.body()?.let {
                        grammarAssessmentLiveData.postValue(it)
                    }
                }
            } catch (ex: Throwable) {
                Timber.e(ex)
            }
        }
    }
}
