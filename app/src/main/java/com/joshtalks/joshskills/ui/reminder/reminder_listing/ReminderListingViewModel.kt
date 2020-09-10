package com.joshtalks.joshskills.ui.reminder.reminder_listing

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.JoshApplication
import com.joshtalks.joshskills.core.showToast
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.repository.server.reminder.DeleteReminderRequest
import com.joshtalks.joshskills.repository.server.reminder.ReminderRequest
import com.joshtalks.joshskills.repository.server.reminder.ReminderResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

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
                                id, mentorId, frequency, status, time
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
                        FirebaseCrashlytics.getInstance().recordException(ex)
                    }
                }
            }
            return@launch
        }
    }

    fun deleteReminders(
        reminderIds: ArrayList<Int>,
        onRemindersDeleted: ((reminderIds: ArrayList<Int>) -> Unit)? = null
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val request =
                    DeleteReminderRequest(Mentor.getInstance().getId(), reminderIds)
                val response = AppObjectController.commonNetworkService.deleteReminders(
                    request
                )
                if (response.isSuccessful) {
                    response.body()?.let {
                        if (it.success) {
                            appDatabase.reminderDao().deleteReminders(reminderIds)
                            onRemindersDeleted?.invoke(reminderIds)
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
                        FirebaseCrashlytics.getInstance().recordException(ex)
                    }
                }
            }
            return@launch
        }

    }
}
