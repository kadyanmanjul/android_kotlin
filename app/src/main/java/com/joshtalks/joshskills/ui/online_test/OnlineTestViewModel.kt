package com.joshtalks.joshskills.ui.online_test

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.core.abTest.repository.ABTestRepository
import com.joshtalks.joshskills.core.analytics.MixPanelEvent
import com.joshtalks.joshskills.core.analytics.MixPanelTracker
import com.joshtalks.joshskills.core.analytics.ParamKeys
import com.joshtalks.joshskills.core.io.AppDirectory
import com.joshtalks.joshskills.core.service.DownloadUtils
import com.joshtalks.joshskills.repository.local.entity.DOWNLOAD_STATUS
import com.joshtalks.joshskills.repository.local.entity.LessonQuestion
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.repository.local.model.assessment.Assessment
import com.joshtalks.joshskills.repository.local.model.assessment.AssessmentQuestionWithRelations
import com.joshtalks.joshskills.repository.local.model.assessment.Choice
import com.joshtalks.joshskills.repository.server.assessment.AssessmentStatus
import com.joshtalks.joshskills.repository.server.assessment.AssessmentType
import com.joshtalks.joshskills.repository.server.assessment.OnlineTestRequest
import com.joshtalks.joshskills.repository.server.assessment.OnlineTestResponse
import com.joshtalks.joshskills.util.showAppropriateMsg
import com.tonyodev.fetch2.NetworkType
import com.tonyodev.fetch2.Priority
import com.tonyodev.fetch2.Request
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

class OnlineTestViewModel(application: Application) : AndroidViewModel(application) {

    val lessonQuestionsLiveData: MutableLiveData<List<LessonQuestion>> = MutableLiveData()
    val grammarAssessmentLiveData: MutableLiveData<OnlineTestResponse> = MutableLiveData()
    val message: MutableLiveData<String> = MutableLiveData()
    val apiStatus: MutableLiveData<ApiCallStatus> = MutableLiveData()
    val repository: ABTestRepository by lazy { ABTestRepository() }


    private fun fetchAssessmentDetails(lessonId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                apiStatus.postValue(ApiCallStatus.START)
                val params = mapOf("lesson_id" to lessonId)
                val response = AppObjectController.chatNetworkService.getOnlineTestQuestion(params)
                if (response.isSuccessful) {
                    apiStatus.postValue(ApiCallStatus.SUCCESS)
                    response.body()?.let {
                        if (it.totalQuestions == it.totalAnswered)
                            it.totalAnswered = 0
                        grammarAssessmentLiveData.postValue(it)
                    }
                }
            } catch (ex: Throwable) {
                apiStatus.postValue(ApiCallStatus.FAILED)
                ex.showAppropriateMsg()
            }
        }
    }

    private fun postAnswerAndGetNewQuestion(lessonId: Int = 0) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                apiStatus.postValue(ApiCallStatus.START)
                AppObjectController.appDatabase.chatDao().getOnlineTestRequest(lessonId)?.let {
                    val response =
                        AppObjectController.chatNetworkService.postAndGetNextOnlineTestQuestion(it)
                    if (response.isSuccessful) {
                        apiStatus.postValue(ApiCallStatus.SUCCESS)
                        response.body()?.let { onlineTestResponse ->
                            grammarAssessmentLiveData.postValue(onlineTestResponse)
                        }
                    }
                    AppObjectController.appDatabase.chatDao().deleteOnlineTestRequest(it)
                } ?: run {
                    apiStatus.postValue(ApiCallStatus.FAILED)
                }
            } catch (ex: Throwable) {
                apiStatus.postValue(ApiCallStatus.FAILED)
                Timber.e(ex)
                ex.showAppropriateMsg()
            }
        }
    }

    fun fetchQuestionsOrPostAnswer(lessonId: Int) {
        viewModelScope.launch {
            if (AppObjectController.appDatabase.chatDao().getOnlineTestRequest(lessonId) != null) {
                postAnswerAndGetNewQuestion(lessonId = lessonId)
            } else {
                fetchAssessmentDetails(lessonId)
            }
        }
    }

    fun sendCompletedRuleIdsToBAckend(
        ruleAssessmentQuestionId: Int
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val data = mapOf("rule_assessment_id" to ruleAssessmentQuestionId)
                val response =
                    AppObjectController.chatNetworkService.setListOfRuleIdsCompleted(data)
            } catch (ex: Throwable) {
                Timber.e(ex)
            }
        }
    }


    fun insertChoicesToDB(
        choiceList: List<Choice>,
        questionWithRelations: AssessmentQuestionWithRelations?
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            questionWithRelations?.let {

                AppObjectController.appDatabase.assessmentDao().insertAssessmentWithoutRelation(
                    Assessment(
                        -1,
                        10,
                        null,
                        null,
                        null,
                        null,
                        EMPTY,
                        null,
                        null,
                        null,
                        AssessmentType.QUIZ_V2,
                        AssessmentStatus.NOT_STARTED
                    )
                )

                AppObjectController.appDatabase.assessmentDao()
                    .insertAssessmentQuestionWithoutRelation(
                        it.question
                    )
            }
            choiceList.forEach { choice ->
                choice.questionId = questionWithRelations?.question?.remoteId ?: -1
                AppObjectController.appDatabase.assessmentDao().insertAssessmentChoice(choice)
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

    fun storeAnswerToDb(
        assessmentQuestion: AssessmentQuestionWithRelations,
        answerText: String? = null,
        ruleAssessmentQuestionId: String? = null,
        lessonId: Int = 0,
        lessonNumber: Int = 0,
        timeTaken: Long = 0,
    ) {
        val answerOrderList = arrayListOf<Int>()
        assessmentQuestion.choiceList
            .filter { it.isSelectedByUser }
            .sortedBy { it.userSelectedOrder }
            .forEach { answerOrderList.add(it.sortOrder) }
        viewModelScope.launch {
            AppObjectController.appDatabase.chatDao().insertOnlineTestAnswer(
                OnlineTestRequest(
                    question = assessmentQuestion,
                    ruleAssessmentQuestionId = ruleAssessmentQuestionId,
                    lessonId = lessonId,
                    answer = answerText.toString(),
                    answerOrder = answerOrderList,
                    timeTaken = timeTaken
                )
            )
            PrefManager.put(ONLINE_TEST_LAST_LESSON_ATTEMPTED, lessonNumber)
        }
    }

    fun downloadAudioFileForNewGrammar(choiceList: List<Choice>) {
        viewModelScope.launch {
            try {
                for (choice in choiceList) {
                    choice.downloadStatus = DOWNLOAD_STATUS.DOWNLOADING
                    AppObjectController.appDatabase.assessmentDao()
                        .updateChoiceDownloadStatusForAudio(
                            choice.remoteId,
                            DOWNLOAD_STATUS.DOWNLOADING
                        )
                    val file =
                        AppDirectory.getAudioReceivedFile(choice.audioUrl.toString()).absolutePath
                    if (choice.downloadStatus == DOWNLOAD_STATUS.DOWNLOADED) {
                        return@launch
                    }

                    val request = Request(choice.audioUrl.toString(), file)
                    request.priority = Priority.HIGH
                    request.networkType = NetworkType.ALL
                    request.tag = choice.remoteId.toString()
                    AppObjectController.getFetchObject().enqueue(request, {
                        choice.localAudioUrl = it.file
                        choice.downloadStatus = DOWNLOAD_STATUS.DOWNLOADED
                        viewModelScope.launch(this.coroutineContext) {
                            it.tag?.toInt()?.let { id ->
                                AppObjectController.appDatabase.assessmentDao()
                                    .updateChoiceDownloadStatusForAudio(
                                        id,
                                        DOWNLOAD_STATUS.DOWNLOADED
                                    )
                                AppObjectController.appDatabase.assessmentDao()
                                    .updateChoiceLocalPathForAudio(id, it.file)
                            }
                        }
                        DownloadUtils.objectFetchListener.remove(it.tag)
                    }) {
                        it.throwable?.printStackTrace()
                        choice.downloadStatus = DOWNLOAD_STATUS.FAILED
                        viewModelScope.launch(this.coroutineContext) {
                            AppObjectController.appDatabase.assessmentDao()
                                .updateAssessmentChoice(choice)
                        }
                    }
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    fun postGoal(goal: String, campaign: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.postGoal(goal)
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
}
