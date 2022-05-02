package com.joshtalks.joshskills.ui.voip.new_arch.ui.feedback

import androidx.lifecycle.MutableLiveData
import com.joshtalks.joshskills.base.BaseViewModel
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.service.WorkManagerAdmin
import com.joshtalks.joshskills.repository.local.model.KFactor
import com.joshtalks.joshskills.ui.call.data.local.VoipPref
import kotlinx.coroutines.*
import retrofit2.Response
import java.util.HashMap

class FeedbackViewModel : BaseViewModel() {

    var responseLiveData = MutableLiveData<Response<KFactor>?>()

    fun getProfileImage():String {
       return VoipPref.getLastProfileImage()
    }
    fun getCallerName():String {
        return VoipPref.getLastRemoteUserName()
    }
    fun getCallDurationString() : String{
        val mTime = StringBuilder()
        var second = VoipPref.getLastCallDurationInSec()
        val minute = getDurationInMin()
        if (minute > 0) {
            mTime.append(minute).append(getMinuteString(minute))
        }
        if (second > 0) {
            if(second<60){
                mTime.append(second).append(getSecondString(second))
            }else{
                second %= 60
                mTime.append(second).append(getSecondString(second))
            }
        }
        return mTime.toString()
    }

    fun getDurationInMin(): Int {
        val second = VoipPref.getLastCallDurationInSec()
        val min = (second % 3600) / 60
        return min.toInt()
    }

    private fun getMinuteString(min: Int): String {
        if (min > 1) {
            return " minutes "
        }
        return " minute "
    }

    private fun getSecondString(sec: Long): String {
        if (sec > 1L) {
            return " seconds "
        }
        return " second "
    }

    fun closeDialog(){
        responseLiveData.postValue(null)
    }

    fun submitFeedback(response: String) {
        CoroutineScope(Dispatchers.IO).launch {
            withTimeout(550) {
                try {
                    val requestParams: HashMap<String, String> = HashMap()
                    requestParams["channel_name"] = VoipPref.getLastCallChannelName()
                    requestParams["agora_mentor_id"] = VoipPref.getLocalUserAgoraId().toString()
                    requestParams["response"] = response
                    val apiResponse = AppObjectController.p2pNetworkService.p2pCallFeedbackV2(requestParams)
                    responseLiveData.postValue(apiResponse)
                    WorkManagerAdmin.syncFavoriteCaller()
                    delay(250)
                } catch (ex: Throwable) {
                    ex.printStackTrace()
                    responseLiveData.postValue(null)
                }
            }
        }
    }
}




