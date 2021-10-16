package com.joshtalks.joshskills.ui.voip

import android.app.Application
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.joshtalks.joshskills.core.ApiCallStatus
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.repository.local.model.Mentor
import java.net.ProtocolException
import java.util.HashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.HttpException

class WebrtcViewModel(application: Application) : AndroidViewModel(application) {
    val apiCallStatusLiveData: MutableLiveData<ApiCallStatus> = MutableLiveData()
    val audioState = ObservableField(CallAudioState.HANDSET)
    val isWiredHeadphoneConnected = ObservableBoolean(false)
    val isBluetoothHeadsetConnected = ObservableBoolean(false)

    fun initMissedCall(partnerId: String, aFunction: (String, String, Int) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val requestParams: HashMap<String, String> = HashMap()
                requestParams["mentor_id"] = Mentor.getInstance().getId()
                requestParams["partner_id"] = partnerId
                val response =
                    AppObjectController.p2pNetworkService.getFavoriteUserAgoraToken(requestParams)
                if (response.isSuccessful && response.code() in 200..203) {
                    response.body()?.let {
                        aFunction.invoke(
                            it["token"]!!,
                            it["channel_name"]!!,
                            it["uid"]!!.toInt()
                        )
                    }
                } else if (response.code() == 204) {
                    apiCallStatusLiveData.postValue(ApiCallStatus.INVALIDED)
                } else {
                    apiCallStatusLiveData.postValue(ApiCallStatus.FAILED)
                }
            } catch (ex: Exception) {
                when (ex) {
                    is ProtocolException, is HttpException -> {
                        apiCallStatusLiveData.postValue(ApiCallStatus.INVALIDED)
                    }
                    else -> {
                        apiCallStatusLiveData.postValue(ApiCallStatus.FAILED_PERMANENT)
                    }
                }
            }
        }
    }
}

enum class CallAudioState(val value: String) {
    HANDSET("Handset"), SPEAKER("Speaker"), BLUETOOTH("Bluetooth"), HEADPHONE("Headphone")
}