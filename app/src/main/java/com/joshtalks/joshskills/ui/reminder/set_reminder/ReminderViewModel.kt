package com.joshtalks.joshskills.ui.reminder.set_reminder

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
import com.joshtalks.joshskills.repository.server.reminder.RequestSetReminderRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class ReminderViewModel(application: Application) : AndroidViewModel(application) {

    val context: JoshApplication = getApplication()
    val submitApiCallStatusLiveData: MutableLiveData<ApiCallStatus> = MutableLiveData()

    fun submitReminder(
        time: String,
        frequency: String,
        status: String,
        mentorId: String,
        previousTime: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val request: RequestSetReminderRequest =
                    RequestSetReminderRequest(mentorId, time, frequency, status, previousTime)
                val response = AppObjectController.commonNetworkService.setReminder(
                    request
                )
                if (response.isSuccessful) {
                    response.body()?.let {
                        if (it.success)
                            submitApiCallStatusLiveData.postValue(ApiCallStatus.SUCCESS)
                        else showToast(it.message)
                    }
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
            submitApiCallStatusLiveData.postValue(ApiCallStatus.FAILED)
        }
    }

}