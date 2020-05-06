package com.joshtalks.joshskills.ui.feedback

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.crashlytics.android.Crashlytics
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.core.JoshApplication
import com.joshtalks.joshskills.core.showToast
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.repository.server.feedback.UserFeedbackRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.net.SocketTimeoutException
import java.net.UnknownHostException


class FeedbackViewModel(application: Application) : AndroidViewModel(application) {
    var context: JoshApplication = getApplication()

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

                } else {
                    showToast(context.getString(R.string.generic_message_for_error))
                }
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
        }
    }


}