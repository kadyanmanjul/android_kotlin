package com.joshtalks.joshskills.ui.reminder.reminder_listing

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.crashlytics.android.Crashlytics
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.JoshApplication
import com.joshtalks.joshskills.core.showToast
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.repository.server.reminder.ReminderResponse
import com.joshtalks.joshskills.repository.server.reminder.RequestSetReminderRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class ReminderListingViewModel(application: Application) : AndroidViewModel(application) {

    val context: JoshApplication = getApplication()
    val reminderList: MutableLiveData<List<ReminderResponse>> = MutableLiveData()

    fun getReminders(mentorId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = AppObjectController.commonNetworkService.getReminders(
                    mentorId
                )
                if (response.isSuccessful) {
                    response.body()?.let {
                        if (it.success)
                            reminderList.postValue(response.body()?.responseData)
                        else
                            showToast(it.message)
                        return@launch
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
            return@launch
        }
    }

    fun updateReminder(
        time: String,
        frequency: String,
        status: String,
        mentorId: String,
        previousTime: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val request =
                    RequestSetReminderRequest(mentorId, time, frequency, status, previousTime)
                val response = AppObjectController.commonNetworkService.setReminder(
                    request
                )
                if (response.isSuccessful) {
                    response.body()?.let {
                        if (it.success)
                            getReminders(Mentor.getInstance().getId())
                        else
                            showToast(it.message)
                        return@launch
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
            return@launch
        }
    }

}
