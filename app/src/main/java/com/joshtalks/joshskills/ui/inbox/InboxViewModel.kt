package com.joshtalks.joshskills.ui.inbox

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.crashlytics.android.Crashlytics
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.JoshApplication
import com.joshtalks.joshskills.core.showToast
import com.joshtalks.joshskills.repository.local.minimalentity.InboxEntity
import com.joshtalks.joshskills.repository.local.model.Mentor
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
                        Crashlytics.logException(ex)
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

    fun openReminderScreen(openReminderCallback: ((responseLiseSize: Int) -> Unit)? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = AppObjectController.commonNetworkService.getReminders(
                    Mentor.getInstance().getId()
                )
                if (response.isSuccessful) {
                    response.body()?.let {
                        if (it.success)
                            response.body()?.responseData?.let { it1 ->
                                appDatabase.reminderDao().insertAllReminders(
                                    it1
                                )
                                openReminderCallback?.invoke(it1.size)
                            }
                        else
                            showToast(it.message)
                        return@launch
                    }
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
                        Crashlytics.logException(ex)
                    }
                }
            }
            return@launch
        }
    }

}