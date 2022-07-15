package com.joshtalks.joshskills.ui.voip

import android.app.Application
import android.location.Location
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.joshtalks.joshskills.core.ApiCallStatus
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.PrefManager
import com.joshtalks.joshskills.base.local.model.Mentor
import com.joshtalks.joshskills.repository.server.voip.AgoraTokenRequest
import com.joshtalks.joshskills.repository.server.voip.RequestUserLocation
import com.joshtalks.joshskills.ui.voip.analytics.CurrentCallDetails
import com.joshtalks.joshskills.util.showAppropriateMsg
import java.net.ProtocolException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import retrofit2.HttpException
import timber.log.Timber

private const val TAG = "VoipCallingViewModel"

class VoipCallingViewModel(application: Application) : AndroidViewModel(application) {
    val apiCallStatusLiveData: MutableLiveData<ApiCallStatus> = MutableLiveData()

    fun getUserForTalk(
            courseId: String?,
            topicId: Int?,
            location: Location?,
            aFunction: (String, String, Int) -> Unit,
            is_demo: Boolean = PrefManager.getBoolValue(IS_DEMO_P2P, defValue = false),
            groupId: String? = null
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val request = AgoraTokenRequest(
                        Mentor.getInstance().getId(),
                        courseId,
                        is_demo,
                        topicId.toString(),
                        groupId = groupId
                )
                val response =
                        AppObjectController.p2pNetworkService.getAgoraClientToken(request)
                if (response.isSuccessful && response.code() in 200..203) {
                    response.body()?.let {
                        CurrentCallDetails.set(
                                it["channel_name"] ?: "",
                                callId = it["agora_call_id"] ?: "",
                                callieUid = it["uid"] ?: "",
                                callerUid = ""
                        )

                        try {
                            AppObjectController.p2pNetworkService.sendAgoraTokenConformation(mapOf("agora_call_id" to it["agora_call_id"]))
                        } catch (e: Exception) {
                            e.printStackTrace()
                            WebRtcService.tokenConformationApiFailed()
                            apiCallStatusLiveData.postValue(ApiCallStatus.FAILED)
                            return@launch
                        }
                        location?.let { location ->
                            uploadUserCurrentLocation(it["channel_name"]!!, location)
                        }
                        aFunction.invoke(
                                it["token"]!!,
                                it["channel_name"]!!,
                                it["uid"]!!.toInt()
                        )
                    }
                } else if (response.code() == 204) {
                    apiCallStatusLiveData.postValue(ApiCallStatus.FAILED)
                } else {
                    apiCallStatusLiveData.postValue(ApiCallStatus.FAILED)
                }
            } catch (ex: Exception) {
                when (ex) {
                    is ProtocolException, is HttpException -> {
                        apiCallStatusLiveData.postValue(ApiCallStatus.FAILED)
                    }
                    else -> {
                        apiCallStatusLiveData.postValue(ApiCallStatus.FAILED_PERMANENT)
                    }
                }
            }
        }
    }

    fun initCallForFavoriteCaller(
            courseId: String,
            topicId: Int?,
            location: Location,
            aFunction: (String, String, Int) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val requestParams: HashMap<String, String> = HashMap()
                requestParams["mentor_id"] = Mentor.getInstance().getId()
                requestParams["course_id"] = courseId
                requestParams["topic_id"] = topicId?.toString() ?: ""
                val response =
                        AppObjectController.p2pNetworkService.getFavoriteUserAgoraToken(requestParams)
                if (response.isSuccessful && response.code() in 200..203) {
                    apiCallStatusLiveData.postValue(ApiCallStatus.SUCCESS)
                    response.body()?.let {
                        uploadUserCurrentLocation(it["channel_name"]!!, location)
                        aFunction.invoke(
                                it["token"]!!,
                                it["channel_name"]!!,
                                it["uid"]!!.toInt()
                        )
                    }
                } else if (response.code() == 204 || response.code() == 500) {
                    apiCallStatusLiveData.postValue(ApiCallStatus.FAILED_PERMANENT)
                } else {
                    apiCallStatusLiveData.postValue(ApiCallStatus.FAILED)
                }
            } catch (ex: Throwable) {
                ex.showAppropriateMsg()
            }
        }
    }

    fun initCallForNewUser(
            courseId: String,
            topicId: Int?,
            location: Location,
            aFunction: (String, String, Int) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val requestParams: HashMap<String, String> = HashMap()
                requestParams["mentor_id"] = Mentor.getInstance().getId()
                requestParams["course_id"] = courseId
                requestParams["topic_id"] = topicId?.toString() ?: ""
                val response =
                        AppObjectController.p2pNetworkService.getNewUserAgoraToken(requestParams)
                if (response.isSuccessful && response.code() in 200..203) {
                    apiCallStatusLiveData.postValue(ApiCallStatus.SUCCESS)
                    response.body()?.let {
                        uploadUserCurrentLocation(it["channel_name"]!!, location)
                        aFunction.invoke(
                                it["token"]!!,
                                it["channel_name"]!!,
                                it["uid"]!!.toInt()
                        )
                    }
                } else if (response.code() == 204 || response.code() == 500) {
                    apiCallStatusLiveData.postValue(ApiCallStatus.FAILED_PERMANENT)
                } else {
                    apiCallStatusLiveData.postValue(ApiCallStatus.FAILED)
                }
            } catch (ex: Throwable) {
                ex.showAppropriateMsg()
            }
        }
    }

    private fun uploadUserCurrentLocation(channelName: String, location: Location) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val requestObj =
                        RequestUserLocation(channelName, location.latitude, location.longitude)
                AppObjectController.p2pNetworkService.uploadUserLocationAgora(requestObj)
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    fun saveImpression(eventName: String) {
        CoroutineScope(Job() + Dispatchers.IO).launch(Dispatchers.IO) {
            try {
                val requestData = hashMapOf(
                        Pair("mentor_id", Mentor.getInstance().getId()),
                        Pair("event_name", eventName)
                )
                AppObjectController.commonNetworkService.saveImpression(requestData)
            } catch (ex: Exception) {
                Timber.e(ex)
            }
        }
    }

}
