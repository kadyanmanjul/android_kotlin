package com.joshtalks.joshskills.ui.inbox

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.JoshApplication
import com.joshtalks.joshskills.repository.local.minimalentity.InboxEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


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
                //registerCourseLiveData.postValue(null)
                ex.printStackTrace()
            }

        }
    }

    private fun getCourseFromServer() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val courseList = AppObjectController.chatNetworkService.getRegisterCourses()
                if (courseList.isNullOrEmpty()) {
                    registerCourseNetworkLiveData.postValue(null)
                } else {
                    appDatabase.courseDao().insertRegisterCourses(courseList).let {
                        registerCourseNetworkLiveData.postValue(appDatabase.courseDao().getRegisterCourseMinimal())
                    }
                }
            } catch (ex: Exception) {
            }
        }
    }


    private fun getAllRegisterCourseMinimalFromDB() = viewModelScope.launch {
        try {
            registerCourseMinimalLiveData.postValue(appDatabase.courseDao().getRegisterCourseMinimal())
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }


}