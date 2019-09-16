package com.joshtalks.joshskills.ui.inbox

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.viewModelScope
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.JoshApplication
import com.joshtalks.joshskills.repository.local.entity.Course
import com.joshtalks.joshskills.repository.local.minimalentity.InboxEntity
import kotlinx.coroutines.CoroutineScope
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
                delay(500)
                val courseList = AppObjectController.chatNetworkService.getRegisterCourses().await()
                if (courseList.isNullOrEmpty()) {
                    registerCourseNetworkLiveData.postValue(null)
                } else {
                    appDatabase.courseDao().insertRegisterCourses(courseList).let {
                        registerCourseNetworkLiveData.postValue(appDatabase.courseDao().getRegisterCourseMinimal())
                    }
                }
            } catch (ex: Exception) {

                //registerCourseLiveData.postValue(null)
                ex.printStackTrace()
            }

        }

    }


    fun getAllRegisterCourseMinimalFromDB() = viewModelScope.launch {
        try {
            registerCourseMinimalLiveData.postValue(appDatabase.courseDao().getRegisterCourseMinimal())
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }



}