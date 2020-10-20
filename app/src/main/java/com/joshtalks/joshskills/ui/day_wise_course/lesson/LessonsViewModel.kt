package com.joshtalks.joshskills.ui.day_wise_course.lesson

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.JoshApplication
import com.joshtalks.joshskills.repository.local.entity.LessonModel

class LessonsViewModel(application: Application) :
    AndroidViewModel(application) {
    private val mRepository: LessonRepository
    var context: JoshApplication = getApplication()
    var appDatabase = AppObjectController.appDatabase

    var allLessons: LiveData<PagedList<LessonModel>>? = null

    var config: PagedList.Config = PagedList.Config.Builder()
        .setPageSize(10)
        .setEnablePlaceholders(true)
        .setInitialLoadSizeHint(10)
        .build()

    init {
        mRepository = LessonRepository()
    }

    fun getLessons(): LiveData<PagedList<LessonModel>>? {
        if (allLessons == null) allLessons = LivePagedListBuilder(
            mRepository.fetchLessonsForLocal(), config
        ).build()
        return allLessons
    }

}