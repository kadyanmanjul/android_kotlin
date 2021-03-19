package com.joshtalks.joshskills.ui.voip

import android.app.Application
import android.location.Location
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.joshtalks.joshskills.core.ApiCallStatus
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.PrefManager
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.repository.server.voip.AgoraTokenRequest
import com.joshtalks.joshskills.repository.server.voip.RequestUserLocation
import java.net.ProtocolException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.HttpException

class VoipCallingViewModel(application: Application) : AndroidViewModel(application) {
    val apiCallStatusLiveData: MutableLiveData<ApiCallStatus> = MutableLiveData()

    fun getUserForTalk(
        courseId: String?,
        topicId: Int?,
        location: Location?,
        aFunction: (String, String, Int) -> Unit,
        is_demo: Boolean = PrefManager.getBoolValue(IS_DEMO_P2P,defValue = false)
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val request= AgoraTokenRequest( Mentor.getInstance().getId(),courseId,is_demo,topicId.toString())
                val response =
                    AppObjectController.p2pNetworkService.getAgoraClientToken(request)
                if (response.isSuccessful && response.code() in 200..203) {
                    response.body()?.let {
                        aFunction.invoke(
                            it["token"]!!,
                            it["channel_name"]!!,
                            it["uid"]!!.toInt()
                        )
                        location?.let { location ->
                            uploadUserCurrentLocation(it["channel_name"]!!, location)
                        }
                    }
                } else if (response.code() == 204) {
                    apiCallStatusLiveData.postValue(ApiCallStatus.FAILED)
                } else {
                    apiCallStatusLiveData.postValue(ApiCallStatus.FAILED)
                }
            } catch (ex: Exception) {
                Log.e("Manjul", "getUserForTalk() called $ex")
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
}
