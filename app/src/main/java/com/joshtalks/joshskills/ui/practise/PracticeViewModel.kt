package com.joshtalks.joshskills.ui.practise

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.gson.reflect.TypeToken
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.JoshApplication
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.core.io.AppDirectory
import com.joshtalks.joshskills.core.showToast
import com.joshtalks.joshskills.repository.local.entity.ChatModel
import com.joshtalks.joshskills.repository.local.entity.EXPECTED_ENGAGE_TYPE
import com.joshtalks.joshskills.repository.local.entity.PracticeEngagement
import com.joshtalks.joshskills.repository.local.entity.PracticeFeedback
import com.joshtalks.joshskills.repository.local.entity.PracticeFeedback2
import com.joshtalks.joshskills.repository.local.entity.QUESTION_STATUS
import com.joshtalks.joshskills.repository.server.AmazonPolicyResponse
import com.joshtalks.joshskills.repository.server.RequestEngage
import com.joshtalks.joshskills.util.AudioRecording
import com.joshtalks.joshskills.util.showAppropriateMsg
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.Response
import java.io.File
import java.lang.reflect.Type


class PracticeViewModel(application: Application) :
    AndroidViewModel(application) {
    private var compositeDisposable = CompositeDisposable()
    var context: JoshApplication = getApplication()
    var recordFile: File? = null
    val requestStatusLiveData: MutableLiveData<Boolean> = MutableLiveData()
    val practiceFeedback2LiveData: MutableLiveData<PracticeFeedback2> = MutableLiveData()


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

    }

    fun submitPractise(
        chatModel: ChatModel,
        requestEngage: RequestEngage,
        engageType: EXPECTED_ENGAGE_TYPE?,
        isAudioPractice:Boolean=false
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val localPath = requestEngage.localPath
                if (requestEngage.localPath.isNullOrEmpty().not()) {
                    val obj = mapOf("media_path" to File(requestEngage.localPath!!).name)
                    val responseObj =
                        AppObjectController.chatNetworkService.requestUploadMediaAsync(obj).await()
                    val statusCode: Int = uploadOnS3Server(responseObj, requestEngage.localPath!!)
                    if (statusCode in 200..210) {
                        val url =
                            responseObj.url.plus(File.separator).plus(responseObj.fields["key"])
                        requestEngage.answerUrl = url
                    } else {
                        requestStatusLiveData.postValue(false)
                        return@launch
                    }
                }
                if (requestEngage.answerUrl.isNullOrEmpty().not() && engageType != null) {
                    when {
                        EXPECTED_ENGAGE_TYPE.TX == engageType -> {
                            AppAnalytics.create(AnalyticsEvent.TEXT_SUBMITTED.NAME).push()
                        }
                        EXPECTED_ENGAGE_TYPE.AU == engageType -> {
                            AppAnalytics.create(AnalyticsEvent.AUDIO_SUBMITTED.NAME).push()
                        }
                        EXPECTED_ENGAGE_TYPE.VI == engageType -> {
                            AppAnalytics.create(AnalyticsEvent.VIDEO_SUBMITTED.NAME).push()
                        }
                        EXPECTED_ENGAGE_TYPE.DX == engageType -> {
                            AppAnalytics.create(AnalyticsEvent.DOCUMENT_SUBMITTED.NAME).push()
                        }
                    }
                }

                val resp = AppObjectController.chatNetworkService.submitPracticeAsync(requestEngage)
                if (resp.isSuccessful && resp.body() != null) {

                    val engangementList = List(1) { resp.body()!! }
                    chatModel.question?.practiceEngagement = engangementList
                    chatModel.question?.status = QUESTION_STATUS.AT

                    AppObjectController.appDatabase.chatDao()
                        .updateQuestionObject(chatModel.question!!)
                    requestStatusLiveData.postValue(true)
                    resp.body()?.localPath = localPath
                    if(isAudioPractice)
                        getAudioFeedback(chatModel, resp, engageType, false, mutableListOf())

                } else {
                    requestStatusLiveData.postValue(false)
                    if (resp.code() == 400) {
                        showToast(context.getString(R.string.generic_message_for_error))
                    }
                }
            } catch (ex: Exception) {
                requestStatusLiveData.postValue(false)
                ex.showAppropriateMsg()
            }
        }
    }

    fun getAudioFeedback(
        chatModel: ChatModel,
        response: Response<PracticeEngagement>,
        engageType: EXPECTED_ENGAGE_TYPE?,
        isRetry: Boolean,
        list: MutableList<PracticeEngagement>
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (isRetry.not()) {
                    chatModel.question?.let {
                        val list2 = AppObjectController.appDatabase.chatDao()
                            .getPractiseObject(it.questionId)

                        val practiceEngagement: Type =
                            object : TypeToken<List<PracticeEngagement>>() {}.type
                        val listPrac: List<PracticeEngagement> =
                            AppObjectController.gsonMapper.fromJson(list2, practiceEngagement)
                        list.addAll(listPrac)

                        //list2?.let { it1 -> list.addAll(it1) }
                    }
                    if (engageType != null) {
                        when {
                            EXPECTED_ENGAGE_TYPE.AU == engageType -> {
                            }
                            else -> return@launch
                        }
                    }
                }

                val data = mapOf(
                    "engagement_id" to response.body()?.id.toString(),
                    "transcript_id" to response.body()?.transcriptId.toString()
                )

                val resp = AppObjectController.chatNetworkService.getAudioFeedback(data)
                if (resp.status == "completed" || resp.status == "error") {
                    response.body()?.practiceFeedback =
                        PracticeFeedback(resp.score, resp.grade, resp.text, resp.status)
                    response.body()?.let {
                        list.add(it)
                        chatModel.question?.let {
                            AppObjectController.appDatabase.chatDao()
                                .updatePractiseObject(it.questionId, list)
                        }
                    }
                    practiceFeedback2LiveData.postValue(resp)

                } else {
                    delay(10_000)
                    //Thread.sleep(1_000)
                    getAudioFeedback(chatModel, response, engageType, true, list)
//                    requestStatusLiveData.postValue(true)

                }
            } catch (ex: Exception) {
                requestStatusLiveData.postValue(false)
                ex.showAppropriateMsg()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        compositeDisposable.clear()
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

}
