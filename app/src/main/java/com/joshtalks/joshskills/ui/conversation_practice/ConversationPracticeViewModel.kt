package com.joshtalks.joshskills.ui.conversation_practice

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.joshtalks.joshskills.core.ApiCallStatus
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.JoshApplication
import com.joshtalks.joshskills.core.PractiseUser
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.joshskills.core.custom_ui.recorder.AudioRecorder
import com.joshtalks.joshskills.core.custom_ui.recorder.AudioRecorderBuilder
import com.joshtalks.joshskills.core.io.AppDirectory
import com.joshtalks.joshskills.repository.server.AmazonPolicyResponse
import com.joshtalks.joshskills.repository.server.conversation_practice.Answer
import com.joshtalks.joshskills.repository.server.conversation_practice.ConversationPractiseModel
import com.joshtalks.joshskills.repository.server.conversation_practice.Quiz
import com.joshtalks.joshskills.repository.server.conversation_practice.SubmitConversationPractiseRequest
import com.joshtalks.joshskills.util.showAppropriateMsg
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

class ConversationPracticeViewModel(application: Application) : AndroidViewModel(application) {

    var isPractise = false
    var isPlayerInit = false

    var practiseWho: PractiseUser? = null
    var recordFile: File? = null
    var isRecordingRunning = false
    private var mAudioRecorder: AudioRecorder? = null
    var context: JoshApplication = getApplication()


    private val jobs = arrayListOf<Job>()
    private val _conversationPracticeLiveData: MutableLiveData<ConversationPractiseModel> =
        MutableLiveData()
    val conversationPracticeLiveData: LiveData<ConversationPractiseModel> =
        _conversationPracticeLiveData
    val apiCallStatusLiveData: MutableLiveData<ApiCallStatus> = MutableLiveData()
    val successApiLiveData: MutableLiveData<Boolean> = MutableLiveData()


    fun initRecorder(): Boolean {
        AppDirectory.tempRecordingFile().let {
            mAudioRecorder = AudioRecorderBuilder.with(context)
                .fileName(it.absolutePath)
                .config(AudioRecorder.MediaRecorderConfig.DEFAULT)
                .loggable()
                .build()
            recordFile = it
            isPlayerInit = true
            return@let true
        }
        return false
    }

    fun startRecording(listener: AudioRecorder.OnStartListener?) {
        mAudioRecorder?.start(listener)
    }

    fun stopRecording(listener: AudioRecorder.OnPauseListener?) {
        mAudioRecorder?.pause(listener)
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


    fun submitPractise(practiseId: String) {
        jobs += viewModelScope.launch(Dispatchers.IO) {
            val answerUrl: String
            try {
                apiCallStatusLiveData.postValue(ApiCallStatus.START)
                val obj = mapOf("media_path" to recordFile?.name!!)
                val responseObj =
                    AppObjectController.chatNetworkService.requestUploadMediaAsync(obj).await()
                val statusCode: Int = uploadOnS3Server(responseObj, recordFile!!.absolutePath)
                if (statusCode in 200..210) {
                    val url = responseObj.url.plus(File.separator).plus(responseObj.fields["key"])
                    answerUrl = url
                } else {
                    apiCallStatusLiveData.postValue(ApiCallStatus.FAILED)
                    return@launch
                }
                val list = conversationPracticeLiveData.value?.run {
                    quizModel.map {
                        Quiz(id = it.id,
                            isAttempted = it.isAttempted,
                            answers = it.answersModel.filter { answersModel -> answersModel.isSelectedByUser }
                                .map { answerModel -> Answer(answerModel.id) })
                    }
                }

                val title = "abcde"
                val text = "hello worlds"

                val conversationPractiseRequest =
                    SubmitConversationPractiseRequest(practiseId, answerUrl, title, text, list)

                val response =
                    AppObjectController.commonNetworkService.submitConversationPractice(
                        conversationPractiseRequest
                    )
                if (response.isSuccessful) {
                    apiCallStatusLiveData.postValue(ApiCallStatus.SUCCESS)
                    successApiLiveData.postValue(true)
                    return@launch
                }

            } catch (ex: Throwable) {
                ex.showAppropriateMsg()
            }
            apiCallStatusLiveData.postValue(ApiCallStatus.FAILED)
        }
    }


    private suspend fun uploadOnS3Server(
        responseObj: AmazonPolicyResponse,
        mediaPath: String
    ): Int {
        return viewModelScope.async(Dispatchers.IO) {
            val parameters = emptyMap<String, RequestBody>().toMutableMap()
            for (entry in responseObj.fields) {
                parameters[entry.key] = Utils.createPartFromString(entry.value)
            }

            val requestFile = File(mediaPath).asRequestBody("*".toMediaTypeOrNull())
            val body = MultipartBody.Part.createFormData(
                "file",
                responseObj.fields["key"],
                requestFile
            )
            val responseUpload = AppObjectController.mediaDUNetworkService.uploadMediaAsync(
                responseObj.url,
                parameters,
                body
            ).execute()
            return@async responseUpload.code()
        }.await()
    }


    override fun onCleared() {
        super.onCleared()
        jobs.forEach { it.cancel() }
        mAudioRecorder?.isRecording?.run {
            mAudioRecorder?.cancel()
        }
    }
}
