package com.joshtalks.joshskills.ui.day_wise_course

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.repository.local.entity.BASE_MESSAGE_TYPE
import com.joshtalks.joshskills.repository.local.entity.ChatModel
import com.joshtalks.joshskills.repository.local.entity.PdfType
import com.joshtalks.joshskills.repository.local.entity.Question
import com.joshtalks.joshskills.repository.local.entity.VideoType
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.repository.local.model.assessment.AssessmentQuestionWithRelations
import com.joshtalks.joshskills.repository.local.model.assessment.AssessmentWithRelations
import com.joshtalks.joshskills.repository.server.assessment.AssessmentResponse
import com.joshtalks.joshskills.repository.server.assessment.AssessmentStatus
import com.joshtalks.joshskills.repository.server.chat_message.UpdateQuestionStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CapsuleViewModel(application: Application) : AndroidViewModel(application) {

    var questions: MutableLiveData<List<Question>> = MutableLiveData()
    var pdfForQuestion: LiveData<List<PdfType>> = MutableLiveData()
    var videoForQuestion: LiveData<List<VideoType>> = MutableLiveData()
    val appDatabase = AppObjectController.appDatabase
    val chatDao = appDatabase.chatDao()

    var lessonId: Int = 0

    val chatObservableLiveData: MutableLiveData<List<ChatModel>> = MutableLiveData()

    val assessmentLiveData: MutableLiveData<AssessmentWithRelations> = MutableLiveData()

    val assessmentStatus: MutableLiveData<AssessmentStatus> =
        MutableLiveData(AssessmentStatus.NOT_STARTED)

    fun getQuestions(listOfChat: ArrayList<ChatModel>) {
//        syncQuestions(lessonId)
        val chatList: MutableList<ChatModel> = mutableListOf()

        viewModelScope.launch(Dispatchers.IO) {
            listOfChat.forEach { chat ->
                val question: Question? = appDatabase.chatDao().getQuestion(chat.chatId)
                question?.run {

                    question.lesson = appDatabase.lessonDao().getLesson(question.lesson_id)

                    when (this.material_type) {
                        BASE_MESSAGE_TYPE.IM -> question.imageList =
                            appDatabase.chatDao()
                                .getImagesOfQuestion(questionId = question.questionId)
                        BASE_MESSAGE_TYPE.VI -> question.videoList =
                            appDatabase.chatDao()
                                .getVideosOfQuestion(questionId = question.questionId)
                        BASE_MESSAGE_TYPE.AU -> question.audioList =
                            appDatabase.chatDao()
                                .getAudiosOfQuestion(questionId = question.questionId)
                        BASE_MESSAGE_TYPE.PD -> question.pdfList =
                            appDatabase.chatDao()
                                .getPdfOfQuestion(questionId = question.questionId)
                    }
                    if (this.parent_id.isNullOrEmpty().not()) {
                        chat.parentQuestionObject =
                            appDatabase.chatDao().getQuestionOnId(this.parent_id!!)
                    }
                    if (assessmentId != null) {
                        question.vAssessmentCount = AppObjectController.appDatabase.assessmentDao()
                            .countOfAssessment(assessmentId)
                    }
                    chat.question = question
                }

                if (chat.type == BASE_MESSAGE_TYPE.Q && question == null) {
                    return@forEach
                }

                chatList.add(chat)
            }
            chatObservableLiveData.postValue(chatList)
        }
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

    fun saveAssessmentQuestion(assessmentQuestion: AssessmentQuestionWithRelations) {
        CoroutineScope(Dispatchers.IO).launch {
            AppObjectController.appDatabase.assessmentDao()
                .insertAssessmentQuestion(assessmentQuestion)
        }
    }

    fun updateQuestionStatus(status: String, questionId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            AppObjectController.chatNetworkService.updateQuestionStatus(
                UpdateQuestionStatus(
                    status, lessonId, Mentor.getInstance().getId(), questionId
                )
            )
        }
    }

}