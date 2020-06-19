package com.joshtalks.joshskills.ui.help

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.crashlytics.android.Crashlytics
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.ApiCallStatus
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.JoshApplication
import com.joshtalks.joshskills.core.showToast
import com.joshtalks.joshskills.repository.server.FAQ
import com.joshtalks.joshskills.repository.server.FAQCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import retrofit2.HttpException
import timber.log.Timber
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class HelpViewModel(application: Application) : AndroidViewModel(application) {

    var context: JoshApplication = getApplication()
    val faqCategoryLiveData: MutableLiveData<List<FAQCategory>> = MutableLiveData()
    val apiCallStatusLiveData: MutableLiveData<ApiCallStatus> = MutableLiveData()
    val faqListLiveData: MutableLiveData<List<FAQ>> = MutableLiveData()
    private val jobs = arrayListOf<Job>()

    fun getAllHelpCategory() {
        jobs += viewModelScope.launch(Dispatchers.IO) {
            try {
                val response =
                    AppObjectController.commonNetworkService.getHelpCategoryV2()
                if (response.isSuccessful) {
                    apiCallStatusLiveData.postValue(ApiCallStatus.SUCCESS)
                    faqCategoryLiveData.postValue(response.body())
                    return@launch
                }

            } catch (ex: Exception) {
                when (ex) {
                    is HttpException -> {
                        showToast(context.getString(R.string.generic_message_for_error))
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

    fun getFaq() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response: List<FAQ> =
                    AppObjectController.commonNetworkService.getFaqList()
                faqListLiveData.postValue(response)
            } catch (ex: Exception) {
                when (ex) {
                    is HttpException -> {
                        showToast(context.getString(R.string.generic_message_for_error))
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

    fun postFaqFeedback(id: String, boolean: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val requestMap = mutableMapOf<String, String?>()
                if (boolean)
                    requestMap["yes_count"] = "1"
                else
                    requestMap["no_count"] = "1"
                AppObjectController.commonNetworkService.patchFaqFeedback(id, requestMap)
            } catch (ex: Exception) {
                Timber.tag("FAQ Feedback").e(ex)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        jobs.forEach { it.cancel() } // cancels the job and waits for its completion
    }
}
