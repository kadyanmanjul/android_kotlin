package com.joshtalks.joshskills.ui.voip

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.joshtalks.joshskills.core.ApiCallStatus
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.repository.server.voip.VoipCallDetailModel
import com.joshtalks.joshskills.util.showAppropriateMsg
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.ProtocolException

const val RETRY_MIN_COUNT = 5

class VoipCallingViewModel(application: Application) : AndroidViewModel(application) {
    val voipDetailsLiveData: MutableLiveData<VoipCallDetailModel> = MutableLiveData()
    val apiCallStatusLiveData: MutableLiveData<ApiCallStatus> = MutableLiveData()
    var attemptCount = 0


    fun getUserForTalk(courseId: String, topicId: Int?) {
        var supportUser = "false"
        if (attemptCount > RETRY_MIN_COUNT) {
            apiCallStatusLiveData.postValue(ApiCallStatus.FAILED_PERMANENT)
            return
        }
        attemptCount += 1
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (attemptCount == RETRY_MIN_COUNT) {
                    supportUser = "true"
                }
                val response =
                    AppObjectController.commonNetworkService.getP2PUser(
                        courseId,
                        topicId,
                        supportUser
                    )
                voipDetailsLiveData.postValue(response)
            } catch (ex: ProtocolException) {
                apiCallStatusLiveData.postValue(ApiCallStatus.RETRY)
            } catch (ex: Throwable) {
                apiCallStatusLiveData.postValue(ApiCallStatus.FAILED)
                ex.showAppropriateMsg()
                voipDetailsLiveData.postValue(null)
            }
        }
    }
}
