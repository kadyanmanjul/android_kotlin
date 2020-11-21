package com.joshtalks.joshskills.ui.day_wise_course

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.repository.local.entity.BASE_MESSAGE_TYPE
import com.joshtalks.joshskills.repository.local.entity.ChatModel
import com.joshtalks.joshskills.repository.local.entity.LESSON_STATUS
import com.joshtalks.joshskills.repository.local.entity.PdfType
import com.joshtalks.joshskills.repository.local.entity.Question
import com.joshtalks.joshskills.repository.local.entity.VideoType
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.repository.local.model.assessment.AssessmentQuestionWithRelations
import com.joshtalks.joshskills.repository.local.model.assessment.AssessmentWithRelations
import com.joshtalks.joshskills.repository.server.assessment.AssessmentResponse
import com.joshtalks.joshskills.repository.server.assessment.AssessmentStatus
import com.joshtalks.joshskills.repository.server.assessment.AssessmentType
import com.joshtalks.joshskills.repository.server.chat_message.UpdateQuestionStatus
import com.joshtalks.joshskills.util.showAppropriateMsg
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CapsuleViewModel(application: Application) : AndroidViewModel(application) {

    var questions: MutableLiveData<List<Question>> = MutableLiveData()
    var pdfForQuestion: LiveData<List<PdfType>> = MutableLiveData()
    var videoForQuestion: LiveData<List<VideoType>> = MutableLiveData()
    val appDatabase = AppObjectController.appDatabase
    val chatDao = appDatabase.chatDao()
    val lessonDao = appDatabase.lessonDao()

    val chatObservableLiveData: MutableLiveData<List<ChatModel>> = MutableLiveData()

    val assessmentLiveData: MutableLiveData<AssessmentWithRelations> = MutableLiveData()

    val assessmentStatus: MutableLiveData<AssessmentStatus> =
        MutableLiveData(AssessmentStatus.NOT_STARTED)
    val lessonStatusLiveData: MutableLiveData<String> = MutableLiveData()

    fun getQuestions(lessonId: Int) {
        val chatList: MutableList<ChatModel> = mutableListOf()

        viewModelScope.launch(Dispatchers.IO) {
            val listOfChat = appDatabase.chatDao().getChatsForLessonId(lessonId)
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

    fun syncQuestions(lessonId: Int) {
        //Note: it is required to be called for some backend logic reason.
        viewModelScope.launch(Dispatchers.IO) {
            AppObjectController.chatNetworkService.getQuestionsForLesson(
                Mentor.getInstance().getId(), lessonId
            )
            return@launch
        }
    }

    fun fetchAssessmentDetails(assessmentId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
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
//                        apiCallStatusLiveData.postValue(ApiCallStatus.SUCCESS)
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
//            apiCallStatusLiveData.postValue(ApiCallStatus.FAILED)
            /*
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

            }*/
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

    fun updateQuestionStatus(status: String, questionId: Int, courseId: Int, lessonId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val resp = AppObjectController.chatNetworkService.updateQuestionStatus(
                    UpdateQuestionStatus(
                        status, lessonId, Mentor.getInstance().getId(), questionId, courseId
                    )
                )
                if (resp.success) {
                    lessonStatusLiveData.postValue(resp.responseData)
                    return@launch
                }
            } catch (e: Exception) {
                e.printStackTrace()
                return@launch
            }
        }
    }

    fun updateQuestionInLocal(question: Question) {
        viewModelScope.launch(Dispatchers.IO) {
            chatDao.updateQuestionObject(question)
        }
    }

    fun updateQuestionLessonStatus(lessonId:Int) {
        viewModelScope.launch(Dispatchers.IO) {
            lessonDao.updateFeedbackStatus(lessonId,LESSON_STATUS.CO)
        }
    }
}