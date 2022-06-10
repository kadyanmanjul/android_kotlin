package com.joshtalks.joshskills.ui.certification_exam

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.joshtalks.joshskills.core.ApiCallStatus
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.core.JoshApplication
import com.joshtalks.joshskills.core.analytics.MixPanelEvent
import com.joshtalks.joshskills.core.analytics.MixPanelTracker
import com.joshtalks.joshskills.core.analytics.ParamKeys
import com.joshtalks.joshskills.repository.local.DatabaseUtils
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.repository.server.certification_exam.Answer
import com.joshtalks.joshskills.repository.server.certification_exam.CertificateExamReportModel
import com.joshtalks.joshskills.repository.server.certification_exam.CertificationQuestionModel
import com.joshtalks.joshskills.repository.server.certification_exam.CertificationUserDetail
import com.joshtalks.joshskills.repository.server.certification_exam.RequestSubmitAnswer
import com.joshtalks.joshskills.repository.server.certification_exam.RequestSubmitCertificateExam
import com.joshtalks.joshskills.ui.certification_exam.constants.FINISH_EXAM
import com.joshtalks.joshskills.ui.certification_exam.constants.PREV_RESULT
import com.joshtalks.joshskills.ui.certification_exam.constants.START_EXAM
import com.joshtalks.joshskills.util.showAppropriateMsg
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import timber.log.Timber

class CertificationExamViewModel(application: Application) : AndroidViewModel(application) {
    private var context: JoshApplication = getApplication()

    var conversationId: String = EMPTY
    private val _certificationQuestionLiveData: MutableLiveData<CertificationQuestionModel> =
        MutableLiveData()
    val certificationQuestionLiveData: LiveData<CertificationQuestionModel> =
        _certificationQuestionLiveData
    val startExamLiveData: MutableLiveData<Unit> = MutableLiveData()
    val previousExamsResultLiveData: MutableLiveData<Unit> = MutableLiveData()
    val resumeExamLiveData: MutableLiveData<Boolean> = MutableLiveData()
    val apiStatus: MutableLiveData<ApiCallStatus> = MutableLiveData()
    val examReportLiveData: MutableLiveData<List<CertificateExamReportModel>> =
        MutableLiveData()
    val isUserSubmitExam: MutableLiveData<Boolean> = MutableLiveData()
    var isSAnswerUiShow: Boolean = false

    fun startExam() {
        saveImpression(START_EXAM)
        startExamLiveData.postValue(Unit)
    }

    fun showPreviousResult() {
        saveImpression(PREV_RESULT)
        previousExamsResultLiveData.postValue(Unit)
    }

    fun getQuestions(certificateExamId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            if (hasResumeExam(certificateExamId)) {
                resumeExamLiveData.postValue(true)
                delay(200)
            } else {
                try {
                    val responseObj =
                        AppObjectController.commonNetworkService.getCertificateExamDetails(
                            certificateExamId
                        )
                    responseObj.certificateExamId = certificateExamId
                    resumeExamLiveData.postValue(false)
                    _certificationQuestionLiveData.postValue(responseObj)
                } catch (ex: Throwable) {
                    ex.showAppropriateMsg()
                }
            }
        }
    }

    fun openResumeExam(certificateExamId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            if (hasResumeExam(certificateExamId)) {
                resumeExamLiveData.postValue(true)
                delay(200)
                resumeExamLiveData.postValue(null)
            } else {
                _certificationQuestionLiveData.postValue(null)
            }
        }
    }

    private fun hasResumeExam(certificateExamId: Int): Boolean {
        val obj = CertificationQuestionModel.getResumeExam(certificateExamId)
        return if (obj == null) {
            false
        } else {
            if (certificateExamId == obj.certificateExamId) {
                resumeExamLiveData.postValue(true)
                _certificationQuestionLiveData.postValue(obj)
                return true
            }
            false
        }
    }

    fun submitExam(certificateExamId: Int, obj: CertificationQuestionModel) {
        viewModelScope.launch(Dispatchers.IO) {
            saveImpression(FINISH_EXAM)
            val request = getExamSubmitRequestObject(certificateExamId, obj)
            try {
                val responseObj =
                    AppObjectController.commonNetworkService.submitExam(request)
                if (responseObj.isSuccessful) {
                    DatabaseUtils.getCExamDetails(
                        conversationId = conversationId,
                        certificationId = certificateExamId
                    )
                    apiStatus.postValue(ApiCallStatus.SUCCESS)
                } else {
                    apiStatus.postValue(ApiCallStatus.RETRY)
                }
            } catch (ex: Throwable) {
                ex.showAppropriateMsg()
                apiStatus.postValue(ApiCallStatus.FAILED)
            }
        }
    }

    private fun getExamSubmitRequestObject(
        certificateExamId: Int,
        obj: CertificationQuestionModel
    ): RequestSubmitCertificateExam {
        val userSelectedAnswerList: ArrayList<RequestSubmitAnswer> =
            ArrayList(obj.questions.size)
        var userSelectedAnswer: RequestSubmitAnswer

        obj.questions.forEach {
            if (it.userSelectedOption > -1) {
                userSelectedAnswer = RequestSubmitAnswer(
                    it.userSelectedOption,
                    isUserGiveCorrectAnswer(it.userSelectedOption, it.answers),
                    it.questionId
                )
                userSelectedAnswerList.add(userSelectedAnswer)
            }
        }

        val request = RequestSubmitCertificateExam()
        request.attemptNo = obj.attemptCount + 1
        request.certificateExamId = certificateExamId
        request.answers = userSelectedAnswerList
        return request
    }

    private fun isUserGiveCorrectAnswer(userSelectedOption: Int, answers: List<Answer>): Boolean {
        return answers.find { it.id == userSelectedOption }?.isCorrect ?: false
    }

    fun getUserAllExamReports(certificateExamId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val resp =
                    AppObjectController.commonNetworkService.getExamReports(certificateExamId)
                apiStatus.postValue(ApiCallStatus.SUCCESS)
                examReportLiveData.postValue(resp)
            } catch (ex: Throwable) {
                ex.showAppropriateMsg()
                apiStatus.postValue(ApiCallStatus.FAILED)
                ex.printStackTrace()
            }
        }
    }

    val cUserDetails = MutableSharedFlow<CertificationUserDetail?>(replay = 0)
    val certificateUrl = MutableSharedFlow<String>(replay = 0)

    fun getCertificateUserDetails() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val resp = AppObjectController.commonNetworkService.getCertificateUserDetails()
                cUserDetails.emit(resp)
            } catch (ex: Throwable) {
                ex.showAppropriateMsg()
                apiStatus.postValue(ApiCallStatus.FAILED)
                ex.printStackTrace()
            }
        }
    }

    fun postCertificateUserDetails(certificationUserDetail: CertificationUserDetail) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val resp =
                    AppObjectController.commonNetworkService.submitUserDetailForCertificate(certificationUserDetail)
                //certificateUrl.emit(resp.getOrDefault("pdf", ""))
                certificateUrl.emit(resp.getOrDefault("img_url", ""))
            } catch (ex: Throwable) {
                ex.showAppropriateMsg()
                apiStatus.postValue(ApiCallStatus.FAILED)
                ex.printStackTrace()
            }
        }
    }

    fun saveImpression(eventName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val requestData = hashMapOf(
                    Pair("mentor_id", Mentor.getInstance().getId()),
                    Pair("event_name", eventName),
                    Pair("exam_type", certificationQuestionLiveData.value?.type ?: EMPTY)
                )
                AppObjectController.commonNetworkService.saveCertificateImpression(requestData)
            } catch (ex: Exception) {
                Timber.e(ex)
            }
        }
    }
}
