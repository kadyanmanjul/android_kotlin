package com.joshtalks.joshskills.ui.lesson

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.AppObjectController.Companion.appDatabase
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.core.PrefManager
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.joshskills.core.custom_ui.m4aRecorder.M4ABaseAudioRecording
import com.joshtalks.joshskills.core.custom_ui.recorder.OnAudioRecordListener
import com.joshtalks.joshskills.core.custom_ui.recorder.RecordingItem
import com.joshtalks.joshskills.core.io.AppDirectory
import com.joshtalks.joshskills.core.showToast
import com.joshtalks.joshskills.repository.local.entity.DOWNLOAD_STATUS
import com.joshtalks.joshskills.repository.local.entity.LESSON_STATUS
import com.joshtalks.joshskills.repository.local.entity.LessonMaterialType
import com.joshtalks.joshskills.repository.local.entity.LessonModel
import com.joshtalks.joshskills.repository.local.entity.LessonQuestion
import com.joshtalks.joshskills.repository.local.entity.LessonQuestionType
import com.joshtalks.joshskills.repository.local.entity.PendingTask
import com.joshtalks.joshskills.repository.local.entity.PendingTaskModel
import com.joshtalks.joshskills.repository.local.entity.PracticeEngagement
import com.joshtalks.joshskills.repository.local.entity.PracticeFeedback2
import com.joshtalks.joshskills.repository.local.entity.QUESTION_STATUS
import com.joshtalks.joshskills.repository.local.entity.practise.PointsListResponse
import com.joshtalks.joshskills.repository.local.model.assessment.AssessmentQuestionWithRelations
import com.joshtalks.joshskills.repository.local.model.assessment.AssessmentWithRelations
import com.joshtalks.joshskills.repository.server.RequestEngage
import com.joshtalks.joshskills.repository.server.assessment.AssessmentResponse
import com.joshtalks.joshskills.repository.server.assessment.AssessmentStatus
import com.joshtalks.joshskills.repository.server.chat_message.UpdateQuestionStatus
import com.joshtalks.joshskills.repository.server.engage.Graph
import com.joshtalks.joshskills.repository.server.voip.SpeakingTopic
import com.joshtalks.joshskills.repository.service.NetworkRequestHelper
import com.joshtalks.joshskills.util.AudioRecording
import com.joshtalks.joshskills.util.FileUploadService
import com.joshtalks.joshskills.util.showAppropriateMsg
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LessonViewModel(application: Application) : AndroidViewModel(application) {

    val lessonQuestionsLiveData: MutableLiveData<List<LessonQuestion>> = MutableLiveData()
    val lessonLiveData: MutableLiveData<LessonModel> = MutableLiveData()
    val pointsSnackBarText: MutableLiveData<PointsListResponse> = MutableLiveData()
    val requestStatusLiveData: MutableLiveData<Boolean> = MutableLiveData()

    val grammarAssessmentLiveData: MutableLiveData<AssessmentWithRelations> = MutableLiveData()
    val grammarAssessmentStatus: MutableLiveData<AssessmentStatus> =
        MutableLiveData(AssessmentStatus.NOT_STARTED)
    val grammarVideoInterval: MutableLiveData<Graph?> = MutableLiveData()

    val vocabAssessmentData: MutableLiveData<ArrayList<AssessmentWithRelations>> = MutableLiveData()
    private var isRecordingStarted = false
    private val mAudioRecording: M4ABaseAudioRecording = M4ABaseAudioRecording()
    var recordFile: File? = null

    val practiceFeedback2LiveData: MutableLiveData<PracticeFeedback2> = MutableLiveData()
    val practiceEngagementData: MutableLiveData<PracticeEngagement> = MutableLiveData()
    val courseId: MutableLiveData<String> = MutableLiveData()
    val speakingTopicLiveData: MutableLiveData<SpeakingTopic?> = MutableLiveData()

    fun getLesson(lessonId: Int) {
        viewModelScope.launch {
            val lesson = getLessonFromDB(lessonId)

            if (lesson != null) {
                lessonLiveData.postValue(lesson)
            } else {
                showToast(AppObjectController.joshApplication.getString(R.string.generic_message_for_error))
            }
        }
    }

    private suspend fun getLessonFromDB(lessonId: Int): LessonModel? {
        return withContext(viewModelScope.coroutineContext + Dispatchers.IO) {
            return@withContext appDatabase.lessonDao().getLesson(lessonId)
        }
    }

    fun getQuestions(lessonId: Int) {
        viewModelScope.launch {
            val lessonQuestions = if (Utils.isInternetAvailable()) {
                getQuestionsFromAPI(lessonId)
            } else {
                getQuestionsFromDB(lessonId)
            }

            if (lessonQuestions.isNotEmpty()) {
                lessonQuestionsLiveData.postValue(lessonQuestions)
            } else {
                showToast(AppObjectController.joshApplication.getString(R.string.generic_message_for_error))
            }
        }
    }

    private suspend fun getQuestionsFromDB(lessonId: Int): List<LessonQuestion> {
        return withContext(viewModelScope.coroutineContext + Dispatchers.IO) {
            try {
                val lessonQuestions =
                    appDatabase.lessonQuestionDao().getQuestionsForLesson(lessonId)
                lessonQuestions.forEach { lessonQuestion ->
                    when (lessonQuestion.materialType) {

                        LessonMaterialType.IM ->
                            lessonQuestion.imageList =
                                appDatabase.chatDao().getImagesOfQuestion(lessonQuestion.id)

                        LessonMaterialType.VI ->
                            lessonQuestion.videoList =
                                appDatabase.chatDao().getVideosOfQuestion(lessonQuestion.id)

                        LessonMaterialType.AU ->
                            lessonQuestion.audioList =
                                appDatabase.chatDao().getAudiosOfQuestion(lessonQuestion.id)

                        LessonMaterialType.PD ->
                            lessonQuestion.pdfList =
                                appDatabase.chatDao().getPdfOfQuestion(lessonQuestion.id)
                    }
                }
                lessonQuestions
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    private suspend fun getQuestionsFromAPI(lessonId: Int): List<LessonQuestion> {
        return withContext(viewModelScope.coroutineContext + Dispatchers.IO) {
            try {
                var lastSyncTime = PrefManager.getStringValue(lessonId.toString())
                if (lastSyncTime.isBlank()) {
                    lastSyncTime = "0"
                }
                val response = AppObjectController.chatNetworkService.getQuestionsForLesson(
                    lastSyncTime,
                    lessonId
                )

                response.data.forEach {
                    it.lessonId = lessonId
                    saveQuestionToDB(it)
                }
                response.data.maxByOrNull { it.modified }?.let {
                    PrefManager.put(
                        it.lessonId.toString(),
                        it.modified.time.div(1000).toString()
                    )
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
            getQuestionsFromDB(lessonId)
        }
    }

    private suspend fun saveQuestionToDB(question: LessonQuestion) {
        return withContext(viewModelScope.coroutineContext + Dispatchers.IO) {
            appDatabase.lessonQuestionDao().insertQuestionForLesson(question)

            question.audioList?.let {
                it.listIterator().forEach { audioType ->
                    audioType.questionId = question.id
                    //  DownloadUtils.downloadAudioFile(it)
                }
                appDatabase.chatDao().insertAudioMessageList(it)
            }

            question.imageList?.let {
                it.listIterator().forEach { imageType ->
                    imageType.questionId = question.id
                }
                appDatabase.chatDao().insertImageTypeMessageList(it)
            }

            question.optionsList?.let {
                it.listIterator().forEach { optionType ->
                    optionType.questionId = question.id
                }
                appDatabase.chatDao().insertOptionTypeMessageList(it)
            }

            question.pdfList?.let {
                it.listIterator().forEach { pdfType ->
                    pdfType.questionId = question.id
                }
                appDatabase.chatDao().insertPdfMessageList(it)

            }
            question.videoList?.let {
                it.listIterator().forEach { videoType ->
                    videoType.questionId = question.id
                    videoType.downloadStatus = DOWNLOAD_STATUS.NOT_START
                    videoType.interval = question.interval
                }
                appDatabase.chatDao().insertVideoMessageList(it)
            }

            try {
                if (question.practiseEngagementV2.isNullOrEmpty().not()) {
                    question.practiceEngagement =
                        AppObjectController.gsonMapper.fromJson(
                            question.practiseEngagementV2?.toString(),
                            NetworkRequestHelper.practiceEnagagement
                        )
                    question.practiseEngagementV2?.forEach { pe ->
                        pe.questionForId = question.id
                        appDatabase.practiceEngagementDao()
                            .insertPractise(pe)
                    }
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
            }

        }
    }

    fun updateSectionStatus(lessonId: Int, status: LESSON_STATUS, tabPosition: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            when (tabPosition) {
                0 -> {
                    appDatabase.lessonDao().updateGrammarSectionStatus(lessonId, status)
                    lessonLiveData.postValue(
                        lessonLiveData.value?.apply {
                            this.grammarStatus = status
                        }
                    )
                }
                1 -> {
                    appDatabase.lessonDao().updateVocabularySectionStatus(lessonId, status)
                    lessonLiveData.postValue(
                        lessonLiveData.value?.apply {
                            this.vocabStatus = status
                        }
                    )
                }
                2 -> {
                    appDatabase.lessonDao().updateReadingSectionStatus(lessonId, status)
                    lessonLiveData.postValue(
                        lessonLiveData.value?.apply {
                            this.readingStatus = status
                        }
                    )
                }
                3 -> {
                    appDatabase.lessonDao().updateSpeakingSectionStatus(lessonId, status)
                    lessonLiveData.postValue(
                        lessonLiveData.value?.apply {
                            this.speakingStatus = status
                        }
                    )
                }
            }
        }
    }

    // TODO() - Isko theek krna h baad me..
    fun updateQuestionStatus(
        status: QUESTION_STATUS,
        questionId: String?,
        isVideoPercentComplete: Boolean = false,
        quizCorrectQuestionIds: ArrayList<Int> = ArrayList()
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                //val isFirstAttempt = appDatabase.lessonDao().getAttemptNumber(lessonId) <= 1
                if (isVideoPercentComplete) {
                    val resp = AppObjectController.chatNetworkService.updateQuestionStatus(
                        UpdateQuestionStatus(
                            status = status.name,
                            questionId = questionId,
                            video = isVideoPercentComplete,
                            //show_leaderboard_animation = isFirstAttempt
                        )
                    )
                    if (resp.isSuccessful && resp.body() != null) {
                        appDatabase.lessonQuestionDao().updateQuestionStatus("$questionId", status)
                        val lessonQuestions = lessonQuestionsLiveData.value
                        lessonQuestionsLiveData.postValue(
                            lessonQuestions?.apply {
                                this.filter { it.id == questionId }
                                    .getOrNull(0)?.status = status
                            })
                        //updatedLessonResponseLiveData.postValue(resp.body())
                        return@launch
                    }
                } else {
                    if (status == QUESTION_STATUS.IP) {
                        appDatabase.lessonQuestionDao().updateQuestionStatus("$questionId", status)
                        val lessonQuestions = lessonQuestionsLiveData.value
                        lessonQuestionsLiveData.postValue(
                            lessonQuestions?.apply {
                                this.filter { it.id == questionId }
                                    .getOrNull(0)?.status = status
                            })
                    } else {
                        val resp = AppObjectController.chatNetworkService.updateQuestionStatus(
                            UpdateQuestionStatus(
                                status = status.name,
                                questionId = questionId,
                                video = isVideoPercentComplete,
                                correctQuestions = quizCorrectQuestionIds,
                                //show_leaderboard_animation = isFirstAttempt
                            )
                        )
                        if (resp.isSuccessful && resp.body() != null) {
                            appDatabase.lessonQuestionDao()
                                .updateQuestionStatus("$questionId", status)
                            val lessonQuestions = lessonQuestionsLiveData.value
                            lessonQuestionsLiveData.postValue(
                                lessonQuestions?.apply {
                                    this.filter { it.id == questionId }
                                        .getOrNull(0)?.status = status
                                })
                            //updatedLessonResponseLiveData.postValue(resp.body())
                            return@launch
                        }
                    }
                }

            } catch (e: Exception) {
                e.printStackTrace()
                return@launch
            }
        }
    }

    fun getAssessmentData(lessonQuestions: List<LessonQuestion>) {
        viewModelScope.launch(Dispatchers.IO) {
            val assessmentList: ArrayList<AssessmentWithRelations> = arrayListOf()
            lessonQuestions.filter { it.type == LessonQuestionType.QUIZ }.forEach {
                it.assessmentId?.let { assessment ->
                    getAssessmentById(assessment)?.let { assessmentWithRelations ->
                        assessmentList.add(assessmentWithRelations)
                        appDatabase.assessmentDao().insertAssessment(assessmentWithRelations)
                    }
                }
            }
            vocabAssessmentData.postValue(assessmentList)
        }
    }

    fun fetchAssessmentDetails(assessmentId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            getAssessmentById(assessmentId)?.let {
                grammarAssessmentLiveData.postValue(it)
                if (it.assessment.status == AssessmentStatus.COMPLETED) {
                    grammarAssessmentStatus.postValue(it.assessment.status)
                }
            }
        }
    }

    private suspend fun getAssessmentById(assessmentId: Int): AssessmentWithRelations? {
        return withContext(viewModelScope.coroutineContext + Dispatchers.IO) {
            try {
                var assessmentRelations = getAssessmentFromDB(assessmentId)
                if (assessmentRelations != null &&
                    assessmentRelations.questionList.isNullOrEmpty().not()
                ) {
                    return@withContext assessmentRelations
                } else {
                    val response = getAssessmentFromServer(assessmentId)
                    if (response.isSuccessful) {
                        response.body()?.let {
                            insertAssessmentToDB(it)
                            assessmentRelations = getAssessmentFromDB(assessmentId)
                            return@withContext assessmentRelations
                        }
                    }
                }
            } catch (ex: Throwable) {
                ex.showAppropriateMsg()
                return@withContext null
            }
            return@withContext null
        }
    }

    private suspend fun getAssessmentFromDB(assessmentId: Int): AssessmentWithRelations? =
        withContext(viewModelScope.coroutineContext + Dispatchers.IO) {
            appDatabase.assessmentDao().getAssessmentById(assessmentId)
        }

    private suspend fun getAssessmentFromServer(assessmentId: Int) =
        AppObjectController.chatNetworkService.getAssessmentId(assessmentId)

    private suspend fun insertAssessmentToDB(assessmentResponse: AssessmentResponse) =
        appDatabase.assessmentDao()
            .insertAssessmentFromResponse(assessmentResponse)

    fun saveAssessmentQuestion(assessmentQuestion: AssessmentQuestionWithRelations) {
        CoroutineScope(Dispatchers.IO).launch {
            appDatabase.assessmentDao()
                .insertAssessmentQuestion(assessmentQuestion)
        }
    }

    fun getMaxIntervalForVideo(videoId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            appDatabase.videoEngageDao()
                .getWatchTimeForVideo(videoId)?.let {
                    if (it.graph.isNullOrEmpty().not()) {
                        grammarVideoInterval.postValue(it.graph.last())
                    }
                }
        }
    }

    fun updateQuestionInLocal(question: LessonQuestion) {
        viewModelScope.launch(Dispatchers.IO) {
            appDatabase.lessonQuestionDao().updateQuestionObject(question)
        }
    }

    fun getPointsForVocabAndReading(questionId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val response = AppObjectController.chatNetworkService.getSnackBarText(questionId)
            pointsSnackBarText.postValue(response)
        }
    }

    fun addTaskToService(requestEngage: RequestEngage) {
        viewModelScope.launch(Dispatchers.IO) {
            val insertedId =
                appDatabase.pendingTaskDao().insertPendingTask(
                    PendingTaskModel(requestEngage, PendingTask.READING_PRACTICE_OLD)
                )
            FileUploadService.uploadSinglePendingTasks(
                AppObjectController.joshApplication,
                insertedId
            )

        }
    }

    @Synchronized
    fun startRecordAudio(recordListener: OnAudioRecordListener?) {
        val onRecordListener: OnAudioRecordListener = object :
            OnAudioRecordListener {
            override fun onRecordFinished(recordingItem: RecordingItem) {
                isRecordingStarted = false
                recordListener?.onRecordFinished(recordingItem)
                AppObjectController.isRecordingOngoing = false
            }

            override fun onError(e: Int) {
                recordListener?.onError(e)
                isRecordingStarted = false
                AppObjectController.isRecordingOngoing = false
            }

            override fun onRecordingStarted() {
                isRecordingStarted = true
                recordListener?.onRecordingStarted()
                AppObjectController.isRecordingOngoing = true
            }
        }
        AppDirectory.tempRecordingFileM4A().let {
            mAudioRecording.setOnAudioRecordListener(onRecordListener)
            mAudioRecording.setFile(it.absolutePath)
            recordFile = it
            mAudioRecording.startRecording()
            return@let true
        }
    }

    @Synchronized
    fun stopRecordingAudio(cancel: Boolean) {
        mAudioRecording.stopRecording(cancel)
        isRecordingStarted = false
    }

    fun startRecord(): Boolean {
        AppDirectory.tempRecordingFile().let {
            recordFile = it
            AudioRecording.audioRecording.startPlayer(recordFile)
            return@let true
        }
        return false
    }

    fun stopRecording() {
        AudioRecording.audioRecording.stopPlaying()
    }

    fun isRecordingStarted(): Boolean {
        return isRecordingStarted
    }

    fun getPracticeAfterUploaded(id: String, callback: (LessonQuestion?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            callback.invoke(appDatabase.lessonQuestionDao().getUpdatedLessonQuestion(id))
        }
    }

    fun getCourseIdByLessonId(lessonId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            courseId.postValue(
                appDatabase.lessonDao()
                    .getLesson(lessonId)?.courseId?.toString() ?: EMPTY
            )
        }
    }

    fun getTopicDetail(topicId: String) {
        viewModelScope.launch {
            val topicDetails = if (Utils.isInternetAvailable()) {
                getTopicFromAPI(topicId)
            } else {
                getTopicFromDB(topicId)
            }

            speakingTopicLiveData.postValue(topicDetails)
        }
    }

    private suspend fun getTopicFromDB(topicId: String): SpeakingTopic? {
        return withContext(viewModelScope.coroutineContext + Dispatchers.IO) {
            try {
                appDatabase.speakingTopicDao().getTopicById(topicId)
            } catch (e: Exception) {
                null
            }
        }
    }

    private suspend fun getTopicFromAPI(topicId: String): SpeakingTopic? {
        return withContext(viewModelScope.coroutineContext + Dispatchers.IO) {
            try {
                val response = AppObjectController.commonNetworkService.getTopicDetail(topicId)
                appDatabase.speakingTopicDao().updateTopic(response)
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
            getTopicFromDB(topicId)
        }
    }

}
