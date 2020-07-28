package com.joshtalks.joshskills.ui.conversation_practice

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.joshtalks.joshskills.core.ApiCallStatus
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.PractiseUser
import com.joshtalks.joshskills.core.io.AppDirectory
import com.joshtalks.joshskills.repository.server.conversation_practice.ConversationPractiseModel
import com.joshtalks.joshskills.util.AudioRecording
import com.joshtalks.joshskills.util.showAppropriateMsg
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File

class ConversationPracticeViewModel(application: Application) : AndroidViewModel(application) {

    var isPractise = false
    var practiseWho: PractiseUser? = null
    var recordFile: File? = null
    var isRecordingRunning = false


    private val jobs = arrayListOf<Job>()
    private val _conversationPracticeLiveData: MutableLiveData<ConversationPractiseModel> =
        MutableLiveData()
    val conversationPracticeLiveData: LiveData<ConversationPractiseModel> =
        _conversationPracticeLiveData


    val apiCallStatusLiveData: MutableLiveData<ApiCallStatus> = MutableLiveData()

    fun startRecord(): Boolean {
        AppDirectory.tempRecordingFile().let {
            recordFile = it
            AudioRecording.audioRecording.startPlayer(recordFile)
            return@let true
        }
        return false
    }

    fun stopRecording() {
        AudioRecording.audioRecording.stopPlaying()
        isRecordingRunning = false
    }

    fun fetchConversationPractice(practiseId: String) {
        jobs += viewModelScope.launch(Dispatchers.IO) {
            try {
                val response =
                    AppObjectController.commonNetworkService.getConversationPractise(practiseId)
                if (response.isSuccessful) {
                    apiCallStatusLiveData.postValue(ApiCallStatus.SUCCESS)
                    _conversationPracticeLiveData.postValue(response.body())
                    return@launch
                }

            } catch (ex: Throwable) {
                ex.showAppropriateMsg()
            }
            apiCallStatusLiveData.postValue(ApiCallStatus.FAILED)
        }
    }


    override fun onCleared() {
        super.onCleared()
        jobs.forEach { it.cancel() }
    }
}
