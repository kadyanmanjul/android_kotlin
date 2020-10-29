package com.joshtalks.joshskills.ui.voip

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.joshtalks.joshskills.core.ApiCallStatus
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.RETRY_COUNT
import com.joshtalks.joshskills.repository.server.voip.VoipCallDetailModel
import com.joshtalks.joshskills.util.showAppropriateMsg
import java.net.ProtocolException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class VoipCallingViewModel(application: Application) : AndroidViewModel(application) {
    val voipDetailsLiveData: MutableLiveData<VoipCallDetailModel> = MutableLiveData()
    val apiCallStatusLiveData: MutableLiveData<ApiCallStatus> = MutableLiveData()
    var retryCount = 0

    fun getUserForTalk(courseId: String) {
        if (retryCount > RETRY_COUNT) {
            apiCallStatusLiveData.postValue(ApiCallStatus.FAILED_PERMANENT)
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = AppObjectController.commonNetworkService.getP2PUser(courseId)
                apiCallStatusLiveData.postValue(ApiCallStatus.SUCCESS)
                voipDetailsLiveData.postValue(response)
                retryCount = 0
            } catch (ex: ProtocolException) {
                retryCount++
                apiCallStatusLiveData.postValue(ApiCallStatus.RETRY)
            } catch (ex: Throwable) {
                apiCallStatusLiveData.postValue(ApiCallStatus.FAILED)
                ex.showAppropriateMsg()
                voipDetailsLiveData.postValue(null)
            }
        }
    }
}
