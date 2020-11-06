package com.joshtalks.joshskills.ui.certification_exam

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.joshtalks.joshskills.core.ApiCallStatus
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.JoshApplication
import com.joshtalks.joshskills.repository.server.certification_exam.Answer
import com.joshtalks.joshskills.repository.server.certification_exam.CertificationQuestionModel
import com.joshtalks.joshskills.repository.server.certification_exam.RequestSubmitCertificateExam
import com.joshtalks.joshskills.repository.server.certification_exam.UserSelectedAnswer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class CertificationExamViewModel(application: Application) : AndroidViewModel(application) {
    private var context: JoshApplication = getApplication()

    private val _certificationQuestionLiveData: MutableLiveData<CertificationQuestionModel> =
        MutableLiveData()
    val certificationQuestionLiveData: LiveData<CertificationQuestionModel> =
        _certificationQuestionLiveData
    val startExamLiveData: MutableLiveData<Unit> = MutableLiveData()
    val previousExamsResultLiveData: MutableLiveData<Unit> = MutableLiveData()
    val resumeExamLiveData: MutableLiveData<Boolean> = MutableLiveData()
    val apiStatus: MutableLiveData<ApiCallStatus> = MutableLiveData()


    fun startExam() {
        startExamLiveData.postValue(Unit)
    }

    fun previousResult() {
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
                } catch (ex: Exception) {
                    ex.printStackTrace()
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
            val request = getExamSubmitRequestObject(certificateExamId, obj)
            try {
                val responseObj =
                    AppObjectController.commonNetworkService.submitExam(request)
                if (responseObj.isSuccessful) {
                    apiStatus.postValue(ApiCallStatus.SUCCESS)
                } else {
                    apiStatus.postValue(ApiCallStatus.RETRY)
                }
            } catch (ex: Exception) {
                apiStatus.postValue(ApiCallStatus.FAILED)
                ex.printStackTrace()
            }

        }
    }

    private fun getExamSubmitRequestObject(
        certificateExamId: Int,
        obj: CertificationQuestionModel
    ): RequestSubmitCertificateExam {
        val userSelectedAnswerList: ArrayList<UserSelectedAnswer> =
            ArrayList(obj.questions.size)
        var userSelectedAnswer: UserSelectedAnswer
        obj.questions.forEach {
            if (it.userSelectedOption != null) {
                userSelectedAnswer = UserSelectedAnswer(
                    it.userSelectedOption,
                    isUserGiveCorrectAnswer(it.userSelectedOption!!, it.answers),
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

}