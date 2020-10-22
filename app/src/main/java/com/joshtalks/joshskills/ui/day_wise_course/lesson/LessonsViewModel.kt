package com.joshtalks.joshskills.ui.day_wise_course.lesson

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import com.joshtalks.joshskills.core.JoshApplication
import com.joshtalks.joshskills.repository.local.entity.LessonModel
import kotlinx.coroutines.launch

class LessonsViewModel(application: Application) :
    AndroidViewModel(application) {

    private val mRepository: LessonRepository = LessonRepository()
    var context: JoshApplication = getApplication()

    var allLessons: LiveData<PagedList<LessonModel>>? = null

    var config: PagedList.Config = PagedList.Config.Builder()
        .setPageSize(10)
        .setEnablePlaceholders(true)
        .setInitialLoadSizeHint(10)
        .build()

    fun getLessons(): LiveData<PagedList<LessonModel>>? {
        if (allLessons == null) allLessons = LivePagedListBuilder(
            mRepository.fetchLessonsForLocal(), config
        ).build()
        viewModelScope.launch { }
        return allLessons
    }

    fun syncLessonsWithServer(courseId: String) {
        viewModelScope.launch {
            mRepository.syncLessonsWithServer(courseId)
        }
    }
}