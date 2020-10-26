package com.joshtalks.joshskills.ui.day_wise_course

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.repository.local.entity.PdfType
import com.joshtalks.joshskills.repository.local.entity.Question
import com.joshtalks.joshskills.repository.local.entity.VideoType
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.repository.local.model.assessment.AssessmentWithRelations
import com.joshtalks.joshskills.repository.server.assessment.AssessmentResponse
import com.joshtalks.joshskills.repository.server.assessment.AssessmentStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CapsuleViewModel(application: Application) : AndroidViewModel(application) {

    var questions: MutableLiveData<List<Question>> = MutableLiveData()
    var pdfForQuestion: LiveData<List<PdfType>> = MutableLiveData()
    var videoForQuestion: LiveData<List<VideoType>> = MutableLiveData()
    val chatDao = AppObjectController.appDatabase.chatDao()

    val assessmentLiveData: MutableLiveData<AssessmentWithRelations> = MutableLiveData()

    val assessmentStatus: MutableLiveData<AssessmentStatus> =
        MutableLiveData(AssessmentStatus.NOT_STARTED)

    fun getQuestions(lessonId: String) {
        syncQuestions(lessonId)
        /*viewModelScope.launch {
            questions = chatDao.getQuestionsForLesson(lessonId)
            return@launch*//*
        }*/
    }
/*
    fun getPdfForQuestions(questionId: String) {
        viewModelScope.launch {
            pdfForQuestion = chatDao.getPdfOfQuestion(questionId)
            return@launch
        }
    }

    fun getVideoForQuestions(questionId: String) {
        viewModelScope.launch {
            videoForQuestion = chatDao.getVideosOfQuestion(questionId)
            return@launch
        }
    }*/

    fun syncQuestions(lessonId: String) {
        viewModelScope.launch {
            val response = AppObjectController.chatNetworkService.getQuestionsForLesson(
                Mentor.getInstance().getId(), lessonId
            )
            if (response.success) {
                questions.postValue(response.responseData)
                chatDao.insertChatQuestions(response.responseData)
            }
            return@launch
        }
    }

    fun fetchAssessmentDetails(assessmentId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val response = getAssessmentFromServer(assessmentId)
            if (response.isSuccessful) {
//                apiCallStatusLiveData.postValue(ApiCallStatus.SUCCESS)
                response.body()?.let {
                    if (it.status == AssessmentStatus.COMPLETED) {
                        assessmentStatus.postValue(it.status)
                    } else {
                        insertAssessmentToDB(it)
                        var assessmentWithRelations = getAssessmentFromDB(assessmentId)
                        assessmentLiveData.postValue(assessmentWithRelations)
                    }
                }

            }
        }
    }

    private suspend fun getAssessmentFromServer(assessmentId: Int) =
        AppObjectController.chatNetworkService.getAssessmentId(assessmentId)


    private suspend fun insertAssessmentToDB(assessmentResponse: AssessmentResponse) =
        AppObjectController.appDatabase.assessmentDao()
            .insertAssessmentFromResponse(assessmentResponse)

    private fun getAssessmentFromDB(assessmentId: Int) =
        AppObjectController.appDatabase.assessmentDao().getAssessmentById(assessmentId)

}