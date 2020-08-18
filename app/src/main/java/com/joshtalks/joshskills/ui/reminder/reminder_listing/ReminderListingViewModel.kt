package com.joshtalks.joshskills.ui.reminder.reminder_listing

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.crashlytics.android.Crashlytics
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.JoshApplication
import com.joshtalks.joshskills.core.showToast
import com.joshtalks.joshskills.repository.server.reminder.ReminderRequest
import com.joshtalks.joshskills.repository.server.reminder.ReminderResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.*

class ReminderListingViewModel(application: Application) : AndroidViewModel(application) {

    val context: JoshApplication = getApplication()
    var appDatabase = AppObjectController.appDatabase
    var reminderList: LiveData<List<ReminderResponse>>

    init {
        reminderList = appDatabase.reminderDao().getAllReminders()
    }

    fun updateReminder(
        id: Int,
        time: String,
        frequency: String,
        status: String,
        mentorId: String,
        previousTime: String,
        onReminderUpdate: ((reminderResponse: ReminderResponse) -> Unit)? = null
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val request =
                    ReminderRequest(mentorId, time, frequency, status, previousTime)
                val response = AppObjectController.commonNetworkService.setReminder(
                    request
                )
                if (response.isSuccessful) {
                    response.body()?.let {
                        if (it.success) {
                            val reminderResponse = ReminderResponse(
                                id, mentorId, frequency, status, time, "",
                                Date().toString()
                            )
                            appDatabase.reminderDao().updateReminder(reminderResponse)
                            onReminderUpdate?.invoke(reminderResponse)
                        } else
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
