package com.joshtalks.joshskills.ui.feedback

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.crashlytics.android.Crashlytics
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.repository.local.entity.FeedbackEngageModel
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.repository.server.feedback.UserFeedbackRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.net.SocketTimeoutException
import java.net.UnknownHostException


class FeedbackViewModel(application: Application) : AndroidViewModel(application) {
    var context: JoshApplication = getApplication()
    val apiCallStatusLiveData: MutableLiveData<ApiCallStatus> = MutableLiveData()

    fun submitFeedback(questionId: String, rating: Float, keyword: String, eText: String = EMPTY) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val userFeedbackRequest = UserFeedbackRequest(
                    Mentor.getInstance().getId(),
                    questionId,
                    rating,
                    keyword,
                    eText
                )
                val resp =
                    AppObjectController.commonNetworkService.postUserFeedback(userFeedbackRequest)
                if (resp.isSuccessful) {
                    AppObjectController.appDatabase.chatDao()
                        .userSubmitFeedbackStatusUpdate(questionId)
                }
                AppObjectController.appDatabase.feedbackEngageModelDao()
                    .insertFeedbackEngage(FeedbackEngageModel(questionId))
                apiCallStatusLiveData.postValue(ApiCallStatus.SUCCESS)
                return@launch
            } catch (ex: Exception) {
                when (ex) {
                    is HttpException -> {
                    }
                    is SocketTimeoutException, is UnknownHostException -> {
                        showToast(context.getString(R.string.internet_not_available_msz))
                    }
                    else -> {
                        Crashlytics.logException(ex)
                    }
                }
            }
            apiCallStatusLiveData.postValue(ApiCallStatus.FAILED)

        }
    }

    fun updateQuestionFeedbackStatus(questionId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            AppObjectController.appDatabase.chatDao().userSubmitFeedbackStatusUpdate(questionId)
        }
    }

    override fun onCleared() {
        super.onCleared()
        apiCallStatusLiveData.value = null
    }
}