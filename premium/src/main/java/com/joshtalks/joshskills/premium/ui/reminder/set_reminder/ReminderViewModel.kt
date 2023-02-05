package com.joshtalks.joshskills.premium.ui.reminder.set_reminder

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.joshtalks.joshskills.premium.R
import com.joshtalks.joshskills.premium.core.ApiCallStatus
import com.joshtalks.joshskills.premium.core.AppObjectController
import com.joshtalks.joshskills.premium.core.JoshApplication
import com.joshtalks.joshskills.premium.core.showToast
import com.joshtalks.joshskills.premium.repository.server.reminder.ReminderRequest
import com.joshtalks.joshskills.premium.repository.server.reminder.ReminderResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class ReminderViewModel(application: Application) : AndroidViewModel(application) {

    val context: JoshApplication = getApplication()
    var appDatabase = AppObjectController.appDatabase
    val submitApiCallStatusLiveData: MutableLiveData<ApiCallStatus> = MutableLiveData()

    fun submitReminder(
        reminderId: Int,
        time: String,
        frequency: String,
        status: String,
        mentorId: String,
        previousTime: String,
        onAlarmSetSuccess: ((reminderId: Int) -> Unit)? = null
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val request = ReminderRequest(mentorId, time, frequency, status, previousTime)
                val response = AppObjectController.commonNetworkService.setReminder(
                    request
                )
                if (response.isSuccessful) {
                    response.body()?.let {
                        if (it.success) {
                            val id: Int
                            if (previousTime.isBlank())
                                id = it.responseData
                            else
                                id = reminderId
                            onAlarmSetSuccess?.invoke(id)
                            submitApiCallStatusLiveData.postValue(ApiCallStatus.SUCCESS)
                            appDatabase.reminderDao().insertReminder(
                                ReminderResponse(
                                    id, mentorId, frequency, status, time
                                )
                            )
                        } else {
                            showToast(it.message)
                            submitApiCallStatusLiveData.postValue(ApiCallStatus.FAILED)
                        }
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
                        try {
                            FirebaseCrashlytics.getInstance().recordException(ex)
                        }catch (ex:Exception){
                            ex.printStackTrace()
                        }
                    }
                }
                submitApiCallStatusLiveData.postValue(ApiCallStatus.FAILED)
            }
        }
    }

}