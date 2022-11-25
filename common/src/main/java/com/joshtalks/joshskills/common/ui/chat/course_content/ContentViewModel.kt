package com.joshtalks.joshskills.common.ui.chat.course_content

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.joshtalks.joshskills.common.core.AppObjectController
import com.joshtalks.joshskills.common.core.EMPTY
import com.joshtalks.joshskills.common.repository.local.minimalentity.CourseContentEntity
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ContentViewModel(application: Application) :
    AndroidViewModel(application) {

    var conversationId: String = EMPTY
    private var compositeDisposable = CompositeDisposable()
    private var appDatabase = AppObjectController.appDatabase

    private val _userContentViewModel: MutableLiveData<List<CourseContentEntity>> =
        MutableLiveData()
    val userContentViewModel: LiveData<List<CourseContentEntity>> = _userContentViewModel

    fun getReceivedCourseContent() {
        viewModelScope.launch(Dispatchers.IO) {
            val obj = appDatabase.chatDao()
                .getRegisterCourseMinimal22(conversationId)
            _userContentViewModel.postValue(obj)
        }
    }
}
