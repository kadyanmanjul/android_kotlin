package com.joshtalks.joshskills.ui.day_wise_course

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.repository.local.entity.Question
import com.joshtalks.joshskills.repository.local.model.Mentor
import kotlinx.coroutines.launch

class CapsuleViewModel(application: Application) : AndroidViewModel(application) {

    var questions: LiveData<List<Question>> = MutableLiveData()
    val chatDao = AppObjectController.appDatabase.chatDao()

    fun getQuestions(lessonId: String) {
        viewModelScope.launch {
            questions = chatDao.getQuestionsForLesson(lessonId)
            return@launch
        }
    }

    fun syncQuestions(lessonId: String) {
        viewModelScope.launch {
            val response = AppObjectController.chatNetworkService.getQuestionsForLesson(
                Mentor.getInstance().getId(), lessonId
            )
            if (response.success) {
                chatDao.insertChatQuestions(response.responseData)
            }
            return@launch
        }
    }
}