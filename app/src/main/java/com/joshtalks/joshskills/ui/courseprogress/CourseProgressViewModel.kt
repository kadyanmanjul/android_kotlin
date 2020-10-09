package com.joshtalks.joshskills.ui.courseprogress

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.repository.local.minimalentity.CourseContentEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CourseProgressViewModel(application: Application) :
    AndroidViewModel(application) {

    var conversationId: String = EMPTY
    private var appDatabase = AppObjectController.appDatabase

    private val _userContentViewModel: MutableLiveData<List<CourseContentEntity>> =
        MutableLiveData()
    val userContentViewModel: LiveData<List<CourseContentEntity>> = _userContentViewModel

    fun getReceivedCourseContent(conversationId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val obj = appDatabase.chatDao()
                .getRegisterCourseMinimal22(conversationId)
            _userContentViewModel.postValue(obj)
        }
    }
}
