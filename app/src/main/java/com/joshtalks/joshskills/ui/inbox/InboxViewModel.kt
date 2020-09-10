package com.joshtalks.joshskills.ui.inbox

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.joshtalks.joshcamerax.utils.SharedPrefsManager
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.JoshApplication
import com.joshtalks.joshskills.core.showToast
import com.joshtalks.joshskills.repository.local.minimalentity.InboxEntity
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.util.ReminderUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class InboxViewModel(application: Application) : AndroidViewModel(application) {

    var context: JoshApplication = getApplication()
    var appDatabase = AppObjectController.appDatabase
    val registerCourseMinimalLiveData: MutableLiveData<List<InboxEntity>> = MutableLiveData()
    val registerCourseNetworkLiveData: MutableLiveData<List<InboxEntity>> = MutableLiveData()

    fun getRegisterCourses() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                getAllRegisterCourseMinimalFromDB()
                delay(800)
                getCourseFromServer()
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    private fun getCourseFromServer() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val courseListResponse = AppObjectController.chatNetworkService.getRegisterCourses()
                if (courseListResponse.isSuccessful) {
                    if (courseListResponse.body().isNullOrEmpty()) {
                        registerCourseNetworkLiveData.postValue(null)
                    } else {
                        appDatabase.courseDao().insertRegisterCourses(courseListResponse.body()!!)
                            .let {
                                registerCourseNetworkLiveData.postValue(
                                    appDatabase.courseDao().getRegisterCourseMinimal()
                                )
                            }
                    }
                }
            } catch (ex: Exception) {
                ex.printStackTrace()

                when (ex) {
                    is HttpException -> {
                    }
                    is SocketTimeoutException, is UnknownHostException -> {
                    }
                    else -> {
                        FirebaseCrashlytics.getInstance().recordException(ex)
                    }
                }
            }
        }
    }

    private fun getAllRegisterCourseMinimalFromDB() = viewModelScope.launch {
        try {
            registerCourseMinimalLiveData.postValue(
                appDatabase.courseDao().getRegisterCourseMinimal()
            )
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    fun openReminderScreen(openReminderCallback: ((responseLiseSize: Int?) -> Unit)? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (SharedPrefsManager.newInstance(context)
                        .getBoolean(SharedPrefsManager.Companion.IS_FIRST_REMINDER, false)
                ) {
                    val response = AppObjectController.commonNetworkService.getReminders(
                        Mentor.getInstance().getId()
                    )
                    if (response.isSuccessful) {
                        response.body()?.let {
                            if (it.success) {
                                response.body()?.responseData?.let { it1 ->
                                    appDatabase.reminderDao().insertAllReminders(
                                        it1
                                    )
                                    SharedPrefsManager.newInstance(context)
                                        .putBoolean(
                                            SharedPrefsManager.Companion.IS_FIRST_REMINDER,
                                            false
                                        )
                                    val reminderUtil = ReminderUtil(getApplication())
                                    it1.forEach { reminderItem ->
                                        val timeParts = reminderItem.reminderTime.split(":")
                                        val hours = timeParts[0]
                                        val mins = timeParts[1]
                                        reminderUtil.setAlarm(
                                            when (reminderItem.reminderFrequency) {
                                                ReminderUtil.Companion.ReminderFrequency.EVERYDAY.name -> ReminderUtil.Companion.ReminderFrequency.EVERYDAY
                                                ReminderUtil.Companion.ReminderFrequency.WEEKDAYS.name -> ReminderUtil.Companion.ReminderFrequency.WEEKDAYS
                                                else -> ReminderUtil.Companion.ReminderFrequency.WEEKENDS
                                            },
                                            reminderUtil.getAlarmPendingIntent(reminderItem.id),
                                            hours.toIntOrNull(),
                                            mins.toIntOrNull()
                                        )
                                    }
                                    openReminderCallback?.invoke(it.message.toIntOrNull())
                                }
                            } else
                                showToast(it.message)
                            return@launch
                        }
                    }
                } else {
                    openReminderCallback?.invoke(appDatabase.reminderDao().getRemindersList().size)
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
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