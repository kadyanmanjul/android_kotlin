package com.joshtalks.joshskills.ui.lesson

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.os.Message
import android.util.Log
import android.view.View
import androidx.databinding.ObservableField
import androidx.databinding.ObservableInt
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.base.EventLiveData
import com.joshtalks.joshskills.constants.*
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.core.AppObjectController.Companion.appDatabase
import com.joshtalks.joshskills.core.abTest.repository.ABTestRepository
import com.joshtalks.joshskills.core.analytics.MarketingAnalytics
import com.joshtalks.joshskills.core.analytics.MixPanelEvent
import com.joshtalks.joshskills.core.analytics.MixPanelTracker
import com.joshtalks.joshskills.core.analytics.ParamKeys
import com.joshtalks.joshskills.core.custom_ui.m4aRecorder.M4ABaseAudioRecording
import com.joshtalks.joshskills.core.custom_ui.recorder.OnAudioRecordListener
import com.joshtalks.joshskills.core.custom_ui.recorder.RecordingItem
import com.joshtalks.joshskills.core.io.AppDirectory
import com.joshtalks.joshskills.core.io.LastSyncPrefManager
import com.joshtalks.joshskills.repository.local.entity.*
import com.joshtalks.joshskills.repository.local.entity.practise.PointsListResponse
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.repository.local.model.assessment.AssessmentQuestionWithRelations
import com.joshtalks.joshskills.repository.local.model.assessment.AssessmentWithRelations
import com.joshtalks.joshskills.repository.server.PurchaseDataResponse
import com.joshtalks.joshskills.repository.server.PurchasePopupType
import com.joshtalks.joshskills.repository.server.RequestEngage
import com.joshtalks.joshskills.repository.server.UpdateLessonResponse
import com.joshtalks.joshskills.repository.server.assessment.AssessmentRequest
import com.joshtalks.joshskills.repository.server.assessment.AssessmentResponse
import com.joshtalks.joshskills.repository.server.assessment.RuleIdsList
import com.joshtalks.joshskills.repository.server.chat_message.UpdateQuestionStatus
import com.joshtalks.joshskills.repository.server.engage.Graph
import com.joshtalks.joshskills.repository.server.introduction.DemoOnboardingData
import com.joshtalks.joshskills.repository.server.voip.SpeakingTopic
import com.joshtalks.joshskills.repository.service.NetworkRequestHelper
import com.joshtalks.joshskills.ui.lesson.speaking.spf_models.BlockStatusModel
import com.joshtalks.joshskills.ui.lesson.speaking.spf_models.UserRating
import com.joshtalks.joshskills.ui.lesson.speaking.spf_models.VideoPopupItem
import com.joshtalks.joshskills.ui.payment.new_buy_page_layout.model.Coupon
import com.joshtalks.joshskills.ui.referral.WHATSAPP_PACKAGE_STRING
import com.joshtalks.joshskills.ui.voip.new_arch.ui.callbar.CallBar
import com.joshtalks.joshskills.util.AudioRecording
import com.joshtalks.joshskills.util.DeepLinkUtil
import com.joshtalks.joshskills.util.FileUploadService
import com.joshtalks.joshskills.util.showAppropriateMsg
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.time.Duration
import java.util.*


class LessonViewModel(application: Application) : AndroidViewModel(application) {
    private var message = Message()
    private var singleLiveEvent = EventLiveData

    val lessonQuestionsLiveData: MutableLiveData<List<LessonQuestion>> = MutableLiveData()
    val lessonLiveData: MutableLiveData<LessonModel> = MutableLiveData()
    val pointsSnackBarText: MutableLiveData<PointsListResponse> = MutableLiveData()

    val grammarAssessmentLiveData: MutableLiveData<AssessmentWithRelations> = MutableLiveData()
    val grammarVideoInterval: MutableLiveData<Graph?> = MutableLiveData()

    val vocabAssessmentData: MutableLiveData<ArrayList<AssessmentWithRelations>> = MutableLiveData()
    private var isRecordingStarted = false
    private val mAudioRecording: M4ABaseAudioRecording = M4ABaseAudioRecording()
    var recordFile: File? = null
    private var userReferralCode: String = EMPTY

    val practiceFeedback2LiveData: MutableLiveData<PracticeFeedback2> = MutableLiveData()
    val practiceEngagementData: MutableLiveData<PracticeEngagement> = MutableLiveData()
    val courseId: MutableLiveData<String> = MutableLiveData()
    val lessonId: MutableLiveData<Int> = MutableLiveData()
    val speakingTopicLiveData: MutableLiveData<SpeakingTopic?> = MutableLiveData()
    val updatedLessonResponseLiveData: MutableLiveData<UpdateLessonResponse> = MutableLiveData()
    val demoLessonNoLiveData: MutableLiveData<Int> = MutableLiveData()
    val demoOnboardingData: MutableLiveData<DemoOnboardingData> = MutableLiveData()
    val apiStatus: MutableLiveData<ApiCallStatus> = MutableLiveData()
    val favoriteCaller = MutableSharedFlow<Boolean>(replay = 0)
    val ruleListIds: MutableLiveData<RuleIdsList> = MutableLiveData()
    val lessonSpotlightStateLiveData: MutableLiveData<LessonSpotlightState?> = MutableLiveData(null)
    val grammarSpotlightClickLiveData: MutableLiveData<Unit> = MutableLiveData()
    val speakingSpotlightClickLiveData: MutableLiveData<Unit> = MutableLiveData()
    val eventLiveData: MutableLiveData<Event<Boolean>> = MutableLiveData()
    var isFreeTrail = false
    val introVideoLiveDataForSpeakingSection: MutableLiveData<VideoPopupItem> = MutableLiveData()
    val callBtnHideShowLiveData: MutableLiveData<Int> = MutableLiveData()
    val howToSpeakLiveData: MutableLiveData<Boolean> = MutableLiveData()
    val introVideoCompleteLiveData: MutableLiveData<Boolean> = MutableLiveData()
    val practicePartnerCallDurationLiveData: MutableLiveData<Long> = MutableLiveData()
    var isUserBlock: ObservableField<BlockStatusModel> = ObservableField<BlockStatusModel>()
    var userRating = ObservableField<UserRating>()
    val blockLiveData: MutableLiveData<Boolean> = MutableLiveData(false)
    val voipState by lazy {
        CallBar()
    }
    val filePath: MutableLiveData<String> = MutableLiveData()
    val videoDownPath: MutableLiveData<String> = MutableLiveData()
    val outputFile: MutableLiveData<String> = MutableLiveData()
    val coursePopupData: MutableLiveData<PurchaseDataResponse?> = MutableLiveData()
    val callCountLiveData: MutableLiveData<Int?> = MutableLiveData(null)
    val isNewStudentActive = ObservableField(false)
    val completedLessonCount: MutableLiveData<Int?> = MutableLiveData(null)
    val mentorCoupon: MutableLiveData<Coupon> = MutableLiveData(null)
    val isFreeTrialUser : MutableLiveData<Boolean> = MutableLiveData(false)

    fun practicePartnerCallDurationFromNewScreen(time: Long) =
        practicePartnerCallDurationLiveData.postValue(time)

    fun isD2pIntroVideoComplete(event: Boolean) = introVideoCompleteLiveData.postValue(event)
    fun isHowToSpeakClicked(event: Boolean) = howToSpeakLiveData.postValue(event)
    fun showHideSpeakingFragmentCallButtons(event: Int) = callBtnHideShowLiveData.postValue(event)

    val abTestRepository: ABTestRepository by lazy { ABTestRepository() }
    val isVideoMuxFailed: Boolean = false

    val isExpertBtnEnabled: MutableLiveData<Boolean> = MutableLiveData()

    var lessonCompletePopUpClick : MutableLiveData<Int> = MutableLiveData()
    var lessonActivityScreen :String = "LESSON_ACTIVITY"

    init {
        getRating()
        getExpertBtnVisibility()
    }

    private fun getExpertBtnVisibility() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = AppObjectController.commonNetworkService.getButtonExpertVisibility()
                if (response.isSuccessful) {
                    val enabled = response.body()?.status == true
                    isExpertBtnEnabled.postValue(enabled)
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
                Log.e(TAG, "${ex.message}")
            }
        }
    }

    fun getVideoData() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = AppObjectController.chatNetworkService.getIntroSpeakingVideo()
                if (response.isSuccessful) {
                    introVideoLiveDataForSpeakingSection.postValue(response.body())
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
                Log.e(TAG, "${ex.message}")
            }
        }
    }

    fun permissionGranted() {
        message.what = PERMISSION_FROM_READING_GRANTED
        singleLiveEvent.value = message
    }

    fun askStoragePermission() {
        message.what = PERMISSION_FROM_READING
        singleLiveEvent.value = message
    }

    fun startBlockTimer() {
        isUserBlock.set(PrefManager.getBlockStatusObject(BLOCK_STATUS))
        blockLiveData.postValue(true)
    }

    fun getLesson(lessonId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val lesson = getLessonFromDB(lessonId)
            if (lesson != null) {
                lessonLiveData.postValue(lesson!!)
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

    fun getQuestions(lessonId: Int, isDemo: Boolean = false, retryCount: Int = 0) {
        viewModelScope.launch(Dispatchers.IO) {
            val questionsFromDB = getQuestionsFromDB(lessonId)
            //TODO remove below line and uncomment above code after getting correct data from API
            //val questionsFromDB = emptyList<LessonQuestion>()
            if (questionsFromDB.isNotEmpty()) {
                apiStatus.postValue(ApiCallStatus.SUCCESS)
                lessonQuestionsLiveData.postValue(questionsFromDB)
                return@launch
            }

            val questionsFromAPI: List<LessonQuestion> = getQuestionsFromAPI(lessonId, false)
            if (questionsFromAPI.isNotEmpty()) {
                apiStatus.postValue(ApiCallStatus.SUCCESS)
                lessonQuestionsLiveData.postValue(questionsFromAPI)
                return@launch
            }
            if (questionsFromDB.isEmpty() && questionsFromAPI.isEmpty()) {
                if (retryCount == 0)
                    getQuestions(lessonId, isDemo, retryCount + 1)
                else
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

                    val audioList =
                        appDatabase.chatDao().getAudiosOfQuestion(lessonQuestion.id)
                    if (audioList.isNullOrEmpty().not()) {
                        lessonQuestion.audioList = audioList
                    }

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

                        else -> {
                            lessonQuestion.imageList =
                                appDatabase.chatDao().getImagesOfQuestion(lessonQuestion.id)
                            lessonQuestion.videoList =
                                appDatabase.chatDao().getVideosOfQuestion(lessonQuestion.id)
                        }

                    }
                }
                lessonQuestions
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    private suspend fun getQuestionsFromAPI(
        lessonId: Int,
        isDemo: Boolean = false,
    ): List<LessonQuestion> {
        return withContext(viewModelScope.coroutineContext + Dispatchers.IO) {
            try {
                var lastSyncTime = LastSyncPrefManager.getStringValue(lessonId.toString())
                if (lastSyncTime.isBlank()) {
                    lastSyncTime = "0"
                }
                if (isDemo) {
                    lastSyncTime = "0"
                }
                val response = AppObjectController.chatNetworkService.getQuestionsForLesson(
                    lastSyncTime,
                    lessonId
                )

                if (response.data.isNullOrEmpty().not()) {
                    response.data.forEach {
                        it.questionId = it.id
                        it.lessonId = lessonId
                        saveQuestionToDB(it)
                    }
                    response.data.maxByOrNull { it.modified }?.let {
                        LastSyncPrefManager.put(
                            it.lessonId.toString(),
                            it.modified.time.div(1000).toString()
                        )
                    }

                    // Update status in case of newly added questions to an existing lesson
                    val updatedQuestions = getQuestionsFromDB(lessonId)
                    val lesson = getLessonFromDB(lessonId)
                    if (lesson?.grammarStatus == LESSON_STATUS.CO) {
                        updatedQuestions.filter { it.chatType == CHAT_TYPE.GR }.forEach {
                            if (it.status == QUESTION_STATUS.NA) {
                                lesson.grammarStatus = LESSON_STATUS.AT
                            }
                        }
                    }
                    if (lesson?.translationStatus == LESSON_STATUS.CO) {
                        updatedQuestions.filter { it.chatType == CHAT_TYPE.TR }.forEach {
                            if (it.status == QUESTION_STATUS.NA) {
                                lesson.vocabStatus = LESSON_STATUS.AT
                            }
                        }
                    }
                    if (lesson?.vocabStatus == LESSON_STATUS.CO) {
                        updatedQuestions.filter { it.chatType == CHAT_TYPE.VP }.forEach {
                            if (it.status == QUESTION_STATUS.NA) {
                                lesson.vocabStatus = LESSON_STATUS.AT
                            }
                        }
                    }
                    if (lesson?.readingStatus == LESSON_STATUS.CO) {
                        updatedQuestions.filter { it.chatType == CHAT_TYPE.RP }.forEach {
                            if (it.status == QUESTION_STATUS.NA) {
                                lesson.readingStatus = LESSON_STATUS.AT
                            }
                        }
                    }
                    if (lesson?.speakingStatus == LESSON_STATUS.CO) {
                        updatedQuestions.filter { it.chatType == CHAT_TYPE.SP }.forEach {
                            if (it.status == QUESTION_STATUS.NA) {
                                lesson.speakingStatus = LESSON_STATUS.AT
                            }
                        }
                    }
                    if (
                        lesson?.grammarStatus == LESSON_STATUS.AT ||
                        lesson?.translationStatus == LESSON_STATUS.AT ||
                        lesson?.vocabStatus == LESSON_STATUS.AT ||
                        lesson?.readingStatus == LESSON_STATUS.AT ||
                        lesson?.speakingStatus == LESSON_STATUS.AT
                    ) {
                        lesson.status = LESSON_STATUS.AT
                    }
                    lesson?.let {
                        updateLesson(it)
                    }
                    apiStatus.postValue(ApiCallStatus.SUCCESS)
                    return@withContext updatedQuestions
                } else {
                    apiStatus.postValue(ApiCallStatus.FAILED)
                }
            } catch (ex: Throwable) {
                apiStatus.postValue(ApiCallStatus.FAILED)
                ex.printStackTrace()
            }
            return@withContext emptyList<LessonQuestion>()
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
                    question.practiseEngagementV2?.forEach { practiceEngagementV2 ->
                        practiceEngagementV2.questionForId = question.id
                        appDatabase.practiceEngagementDao()
                            .insertPractise(practiceEngagementV2)
                    }
                }
            } catch (ex: Throwable) {
                ex.printStackTrace()
            }
        }
    }

    fun updateSectionStatus(lessonId: Int, status: LESSON_STATUS, tabPosition: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            when (tabPosition) {
                GRAMMAR_POSITION -> {
                    appDatabase.lessonDao().updateGrammarSectionStatus(lessonId, status)
                    lessonLiveData.postValue(
                        lessonLiveData.value?.apply {
                            this.grammarStatus = status
                        }
                    )
                }
                VOCAB_POSITION -> {
                    appDatabase.lessonDao().updateVocabularySectionStatus(lessonId, status)
                    lessonLiveData.postValue(
                        lessonLiveData.value?.apply {
                            this.vocabStatus = status
                        }
                    )
                }
                READING_POSITION -> {
                    appDatabase.lessonDao().updateReadingSectionStatus(lessonId, status)
                    lessonLiveData.postValue(
                        lessonLiveData.value?.apply {
                            this.readingStatus = status
                        }
                    )
                }
                SPEAKING_POSITION -> {
                    if (lessonLiveData.value?.speakingStatus != LESSON_STATUS.CO && status == LESSON_STATUS.CO) {
                        MarketingAnalytics.logSpeakingSectionCompleted()
                        if (PrefManager.getBoolValue(IS_FREE_TRIAL)) {
                            MarketingAnalytics.logSpeakingSectionCompletedForFreeTrial()
                        }
                    }
                    appDatabase.lessonDao().updateSpeakingSectionStatus(lessonId, status)
                    lessonLiveData.postValue(
                        lessonLiveData.value?.apply {
                            this.speakingStatus = status
                        }
                    )
                }
                TRANSLATION_POSITION -> {
                    appDatabase.lessonDao().updateTranslationSectionStatus(lessonId, status)
                    lessonLiveData.postValue(
                        lessonLiveData.value?.apply {
                            this.translationStatus = status
                        }
                    )
                }
            }
        }
    }

    // TODO() - Isko theek krna h baad me.. sahi kaha krna padega isko bhi
    fun updateQuestionStatus(
        status: QUESTION_STATUS,
        questionId: String?,
        isVideoPercentComplete: Boolean = false,
        quizCorrectQuestionIds: ArrayList<Int> = ArrayList(),
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // val isFirstAttempt = appDatabase.lessonDao().getAttemptNumber(lessonId) <= 1
                if (isVideoPercentComplete) {
                    val resp = AppObjectController.chatNetworkService.updateQuestionStatus(
                        UpdateQuestionStatus(
                            status = status.name,
                            questionId = questionId,
                            video = isVideoPercentComplete,
                            // show_leaderboard_animation = isFirstAttempt
                        )
                    )
                    if (resp.isSuccessful && resp.body() != null) {
                        updatedLessonResponseLiveData.postValue(resp.body()!!)
                        appDatabase.lessonQuestionDao().updateQuestionStatus("$questionId", status)
                        updateLessonStatus()
                        appDatabase.lessonQuestionDao().getLessonQuestionById(questionId!!)
                        return@launch
                    }
                } else {
                    if (status == QUESTION_STATUS.IP) {
                        appDatabase.lessonQuestionDao().updateQuestionStatus("$questionId", status)
                        updateLessonStatus()
                    } else {
                        val resp = AppObjectController.chatNetworkService.updateQuestionStatus(
                            UpdateQuestionStatus(
                                status = status.name,
                                questionId = questionId,
                                video = isVideoPercentComplete,
                                correctQuestions = quizCorrectQuestionIds,
                                // show_leaderboard_animation = isFirstAttempt
                            )
                        )
                        if (resp.isSuccessful && resp.body() != null) {
                            updatedLessonResponseLiveData.postValue(resp.body()!!)
                            appDatabase.lessonQuestionDao()
                                .updateQuestionStatus("$questionId", status)
                            updateLessonStatus()
                            return@launch
                        }
                    }
                }
            } catch (e: Throwable) {
                e.printStackTrace()
                return@launch
            }
        }
    }

    fun getAssessmentData(lessonQuestions: List<LessonQuestion>) {
        viewModelScope.launch(Dispatchers.IO) {
            val assessmentList: ArrayList<AssessmentWithRelations> = arrayListOf()
            lessonQuestions.forEach {
                it.assessmentId?.let { assessmentId ->
                    getAssessmentById(assessmentId)?.let { assessmentWithRelations ->
                        assessmentList.add(assessmentWithRelations)
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
                            assessmentRelations = AssessmentWithRelations(it)
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
        viewModelScope.launch(Dispatchers.IO) {
            appDatabase.assessmentDao()
                .insertAssessmentQuestion(assessmentQuestion)
        }
    }

    fun saveQuizToServer(assessmentId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            delay(1000)
            try {
                val assessmentWithRelations: AssessmentWithRelations? =
                    appDatabase.assessmentDao().getAssessmentById(assessmentId)
                assessmentWithRelations?.let {
                    val assessmentRequest = AssessmentRequest(it, true)
                    AppObjectController.chatNetworkService.submitTestAsync(assessmentRequest)
                }
            } catch (ex: Throwable) {
                Timber.e(ex)
            }
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
            try {
                val response = AppObjectController.chatNetworkService.getSnackBarText(questionId)
                pointsSnackBarText.postValue(response)
            } catch (ex: Throwable) {
                ex.printStackTrace()
            }
        }
    }

    fun addTaskToService(requestEngage: RequestEngage, pendingTaskType: PendingTask) {
        viewModelScope.launch(Dispatchers.IO) {
            val insertedId =
                appDatabase.pendingTaskDao().insertPendingTask(
                    PendingTaskModel(requestEngage, pendingTaskType)
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
        viewModelScope.launch(Dispatchers.IO) {
            val topicDetailsFromLocal = getTopicFromDB(topicId)
            if (topicDetailsFromLocal != null) {
                topicDetailsFromLocal.isFromDb = true
                speakingTopicLiveData.postValue(topicDetailsFromLocal)
            }
            if (Utils.isInternetAvailable()) {
                val topicDetails = getTopicFromAPI(topicId)
                speakingTopicLiveData.postValue(topicDetails)
            }
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
            } catch (ex: Throwable) {
                ex.printStackTrace()
            }
            getTopicFromDB(topicId)
        }
    }

    suspend fun updateLessonStatus() {
        viewModelScope.launch(Dispatchers.IO) {
            lessonLiveData.postValue(
                lessonLiveData.value?.apply {
                    var lessonCompleted = this.grammarStatus == LESSON_STATUS.CO &&
                            this.vocabStatus == LESSON_STATUS.CO &&
                            this.readingStatus == LESSON_STATUS.CO &&
                            this.speakingStatus == LESSON_STATUS.CO

                    if (this.isNewGrammar && PrefManager.getBoolValue(IS_A2_C1_RETENTION_ENABLED)) {
                        lessonCompleted = lessonCompleted &&
                                this.translationStatus == LESSON_STATUS.CO
                    }
                    val lessonStatus = if (lessonCompleted) {
                        LESSON_STATUS.CO
                    } else {
                        LESSON_STATUS.AT
                    }
                    appDatabase.lessonDao().updateLessonStatus(this.id, lessonStatus)
                }
            )
        }
    }

    suspend fun updateLesson(lesson: LessonModel) {
        viewModelScope.launch(Dispatchers.IO) {
            appDatabase.lessonDao().insertSingleItem(lesson)
            lessonLiveData.postValue(lesson)
        }
    }

    fun isFavoriteCallerExist() {
        viewModelScope.launch(Dispatchers.IO) {
            delay(250)
            val count = appDatabase.favoriteCallerDao().getCountOfFavoriteCaller()
            favoriteCaller.emit(count > 0)
        }
    }

    fun getDemoLesson() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = AppObjectController.chatNetworkService.getDemoLessonModel()
                if (response != null) {
                    response.chatId = ""
                    demoLessonNoLiveData.postValue(response.lessonNo)
                    val lesson = appDatabase.lessonDao().getLesson(response.id)
                    if (lesson == null) {
                        appDatabase.lessonDao().insertSingleItem(response)
                        // demoLessonIdLiveData.postValue(response.id)
                    }
                    getQuestions(response.id, false)
                }
            } catch (ex: Throwable) {
                Timber.e(ex)
            }
        }
    }

    fun getDemoOnBoardingData() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = AppObjectController.chatNetworkService.getDemoOnBoardingData()
                if (response.isSuccessful && response.body() != null) {
                    apiStatus.postValue(ApiCallStatus.SUCCESS)
                    demoOnboardingData.postValue(response.body())
                }
            } catch (ex: Throwable) {
                apiStatus.postValue(ApiCallStatus.FAILED)
                Timber.e(ex)
            }
        }
    }

    fun getListOfRuleIds() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = AppObjectController.chatNetworkService.getListOfRuleIds()
                if (response.isSuccessful && response.body() != null && response.body()!!.totalRulesIds.isNullOrEmpty()
                        .not()
                ) {
                    ruleListIds.postValue(response.body())
                }
            } catch (ex: Throwable) {
                Timber.e(ex)
            }
        }
    }

    fun saveImpression(eventName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val requestData = hashMapOf(
                    Pair("mentor_id", Mentor.getInstance().getId()),
                    Pair("event_name", eventName)
                )
                AppObjectController.commonNetworkService.saveImpression(requestData)
            } catch (ex: Exception) {
                Timber.e(ex)
            }
        }
    }

    fun saveMicroPaymentImpression(eventName: String, eventId: String = EMPTY, previousPage: String = EMPTY) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val requestData = hashMapOf(
                    Pair("event_name", eventName),
                    Pair("expert_id", eventId),
                    Pair("previous_page", previousPage)
                )
                AppObjectController.commonNetworkService.saveMicroPaymentImpression(requestData)
            } catch (ex: Exception) {
                Timber.e(ex)
            }
        }
    }

    fun saveIntroVideoFlowImpression(eventName: String, eventDuration: Long = 0L) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val requestData = hashMapOf(
                    Pair("mentor_id", Mentor.getInstance().getId()),
                    Pair("event_name", eventName),
                    Pair("duration", eventDuration)
                )
                AppObjectController.commonNetworkService.saveIntroVideoFlowImpression(requestData)
            } catch (ex: Exception) {
                Timber.e(ex)
            }
        }
    }

    fun saveTrueCallerImpression(eventName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val requestData = hashMapOf(
                    Pair("mentor_id", Mentor.getInstance().getId()),
                    Pair("event_name", eventName)
                )
                AppObjectController.commonNetworkService.saveTrueCallerImpression(requestData)
            } catch (ex: Exception) {
                Timber.e(ex)
            }
        }
    }

    fun saveVoiceCallImpression(eventName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val requestData = hashMapOf(
                    Pair("mentor_id", Mentor.getInstance().getId()),
                    Pair("event_name", eventName)
                )
                AppObjectController.commonNetworkService.saveVoiceCallImpression(requestData)
            } catch (ex: Exception) {
                Timber.e(ex)
            }
        }
    }

    fun postGoal(goal: String, campaign: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            abTestRepository.postGoal(goal)
            if (campaign != null) {
                val data = ABTestRepository().getCampaignData(campaign)
                data?.let {
                    MixPanelTracker.publishEvent(MixPanelEvent.GOAL)
                        .addParam(ParamKeys.VARIANT, data?.variantKey ?: EMPTY)
                        .addParam(
                            ParamKeys.VARIABLE,
                            AppObjectController.gsonMapper.toJson(data?.variableMap)
                        )
                        .addParam(ParamKeys.CAMPAIGN, campaign)
                        .addParam(ParamKeys.GOAL, goal)
                        .push()
                }
            }
        }
    }

    fun shareVideoForAudio(path: String) {
        DeepLinkUtil(AppObjectController.joshApplication)
            .setReferralCode(Mentor.getInstance().referralCode)
            .setReferralCampaign()
            .setListener(object : DeepLinkUtil.OnDeepLinkListener {
                override fun onDeepLinkCreated(deepLink: String) {
                    inviteFriends(
                        dynamicLink = deepLink,
                        path = path
                    )
                }
            })
            .build()
    }

    fun getRating() {
        val currentTime = Date().time
        if (PrefManager.getCallCount() >= 3) {
            viewModelScope.launch(Dispatchers.IO)
            {
                try {
                    val response = AppObjectController.chatNetworkService.getUserRating()
                    if (response.isSuccessful && response.body() != null) {
                        userRating.set(response.body())
                        PrefManager.putPrefObject(RATING_OBJECT, response.body() as UserRating)
                        PrefManager.put(RATING_TIMESTAMP, currentTime)
                        PrefManager.increaseCallCount()
                    }
                } catch (ex: Throwable) {
                    apiStatus.postValue(ApiCallStatus.FAILED)
                    Timber.e(ex)
                }
            }
        } else {
            userRating.set(PrefManager.getRatingObject(RATING_OBJECT))
        }
    }

    private fun ifRatingFromApi(currentTime: Long): Boolean {
        val previousTime: Long = PrefManager.getLongValue(RATING_TIMESTAMP, false)
        val differ = currentTime - previousTime
        return !(differ < 86400000 && differ > -86400000)
    }

    fun isUserCallBlock(isForceHit: Boolean = false) {
        if (checkBlockStatusInSP() && !isForceHit) {
            startBlockTimer()
        } else {
            blockStatusFromApi(isForceHit)
        }
    }

    private fun blockStatusFromApi(isForceHit: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO)
        {
            try {
                val response = AppObjectController.chatNetworkService.getUserBlockStatus()
                if (response.isSuccessful && response.body() != null && response.body()!!.duration != 0 && !isForceHit) {
                    response.body()!!.timestamp = System.currentTimeMillis()
                    PrefManager.putPrefObject(BLOCK_STATUS, response.body() as BlockStatusModel)
                    startBlockTimer()
                } else if (response.isSuccessful && response.body() != null && response.body()!!.duration == 0) {
                    PrefManager.putPrefObject(BLOCK_STATUS, BlockStatusModel(0, 0, "", 0))
                    blockLiveData.postValue(false)
                }
                if (response.isSuccessful && response.body() != null && response.body()!!.callsLeft >= 0) {
                    if (response.body()!!.callsLeft == 0)
                        PrefManager.put(IS_FREE_TRIAL_CALL_BLOCKED, value = true)
                    PrefManager.put(FT_CALLS_LEFT, response.body()!!.callsLeft)
                    callCountLiveData.postValue(response.body()!!.callsLeft)
                }
            } catch (ex: Throwable) {
                blockLiveData.postValue(false)
                apiStatus.postValue(ApiCallStatus.FAILED)
                Timber.e(ex)
            }
        }
    }

    private fun checkBlockStatusInSP(): Boolean {
        val blockStatus = PrefManager.getBlockStatusObject(BLOCK_STATUS)
        if (blockStatus?.timestamp?.toInt() == 0 && blockStatus.duration == 0)
            return false

        if (checkWithinBlockTimer(blockStatus)) {
            return true
        } else {
            PrefManager.putPrefObject(BLOCK_STATUS, BlockStatusModel(0, 0, "", 0))
            return false
        }
    }

    private fun checkWithinBlockTimer(blockStatus: BlockStatusModel?): Boolean {
        if (blockStatus != null) {
            val durationInMillis = Duration.ofMinutes(blockStatus.duration.toLong()).toMillis()
            val unblockTimestamp = blockStatus.timestamp + durationInMillis
            val currentTimestamp = System.currentTimeMillis()
            if (currentTimestamp < unblockTimestamp) {
                return true
            }
        }
        return false
    }

    fun inviteFriends(dynamicLink: String, path: String) {
        try {
            val destination = path
            val waIntent = Intent(Intent.ACTION_SEND)
            waIntent.setPackage(WHATSAPP_PACKAGE_STRING)
            waIntent.type = "*/*"
            waIntent.putExtra(Intent.EXTRA_TEXT, dynamicLink)
            waIntent.putExtra(
                Intent.EXTRA_STREAM,
                Uri.parse(destination)
            )
            waIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            message.what = SHARE_VIDEO
            message.obj = waIntent
            singleLiveEvent.value = message
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun showVideoOnFullScreen() {
        message.what = OPEN_READING_SHARING_FULLSCREEN
        singleLiveEvent.value = message
    }

    fun closeCurrentFragment() {
        message.what = CLOSE_FULL_READING_FRAGMENT
        singleLiveEvent.value = message
    }

    fun sendOutputToFullScreen(output: String) {
        message.what = SEND_OUTPUT_FILE
        message.obj = output
        singleLiveEvent.value = message
    }

    fun submitButton() {
        message.what = SUBMIT_BUTTON_CLICK
        singleLiveEvent.value = message
    }

    fun cancelButton() {
        message.what = CANCEL_BUTTON_CLICK
        singleLiveEvent.value = message
    }

    fun closeVideoView() {
        message.what = CLOSE_VIDEO_VIEW
        singleLiveEvent.value = message
    }

    fun showVideoView() {
        if (isVideoMuxFailed) {
            showAudioView()
        } else {
            message.what = SHOW_VIDEO_VIEW
            singleLiveEvent.value = message
        }
    }

    fun showAudioView() {
        message.what = SHOW_AUDIO_ONLY
        singleLiveEvent.value = message
    }

    private fun showScratchCard() {
        message.what = SHOW_SCRATCH_CARD
        singleLiveEvent.value = message
    }

    fun updatePracticeEngagement(requestEngage: RequestEngage) {
        viewModelScope.launch {
            val lessonQuestion = AppObjectController.appDatabase.lessonQuestionDao()
                .getLessonQuestionById(requestEngage.questionId)
            lessonQuestion?.let {
                val emptyList = listOf<PracticeEngagement>(PracticeEngagement())
                it.practiceEngagement = emptyList
                it.status = QUESTION_STATUS.AT
                it.practiceEngagement!!.get(0).duration = requestEngage.duration
                it.practiceEngagement!!.get(0).localPath = requestEngage.localPath
                AppObjectController.appDatabase.lessonQuestionDao()
                    .updateQuestionObject(it)
            }
        }
    }

    fun saveReadingPracticeImpression(eventName: String, lessonId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val requestData = hashMapOf(
                    Pair("lesson_id", lessonId),
                    Pair("event_name", eventName)
                )
                AppObjectController.commonNetworkService.saveReadingPracticeImpression(requestData)
            } catch (ex: Exception) {
                Timber.e(ex)
            }
        }
    }

    fun getCoursePopupData(popupType: PurchasePopupType) {
        if (PrefManager.getBoolValue(IS_COURSE_BOUGHT)) return
        viewModelScope.launch {
            try {
                val response =
                    AppObjectController.commonNetworkService.getCoursePopUpData(
                        courseId = PrefManager.getStringValue(CURRENT_COURSE_ID).ifEmpty { DEFAULT_COURSE_ID },
                        popupName = popupType.name
                    )
                if (response.isSuccessful) {
                    response.body()?.let {
                        coursePopupData.value = it.apply {
                            this.name = popupType
                        }
                    }
                }
            } catch (ex: Exception) {
                Timber.e(ex)
            }
        }
    }

    fun getCompletedLessonCount(courseId: String) {
        viewModelScope.launch {
            completedLessonCount.value = appDatabase.lessonDao().getCompletedLessonCount(courseId.toInt())
        }
    }

    suspend fun getMentorCoupon(testId: Int): Coupon? {
        try {
            val response = AppObjectController.commonNetworkService.getValidCoupon(testId, lessonActivityScreen)
            if (response.isSuccessful) {
                response.body()?.let { body ->
                    body.listOfCoupon?.firstOrNull {
                        it.isMentorSpecificCoupon == true && (it.validDuration!=null && it.validDuration.time.minus(System.currentTimeMillis()) > 0L)
                    }?.let { coupon ->
                        return coupon
                    }
                }
            }
        } catch (ex: Exception) {
            Timber.e(ex)
        }
        return null
    }

    fun checkPopupDisplay() {
        viewModelScope.launch {
            try {
                val resp = AppObjectController.commonNetworkService.getPopupType()
                if ((resp.body()?.get("show_scratch_card") == true))
                    showScratchCard()
            } catch (e: Exception) {
                e.printStackTrace()
                e.showAppropriateMsg()
            }
        }
    }
}
