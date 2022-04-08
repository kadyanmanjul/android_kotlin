package com.joshtalks.joshskills.ui.voip.new_arch.ui.feedback

import androidx.lifecycle.MutableLiveData
import com.joshtalks.joshskills.base.BaseViewModel
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.service.WorkManagerAdmin
import com.joshtalks.joshskills.repository.local.model.KFactor
import com.joshtalks.joshskills.ui.voip.new_arch.ui.callbar.VoipPref
import kotlinx.coroutines.*
import retrofit2.Response
import java.util.HashMap

class FeedbackViewModel : BaseViewModel() {

    var responseLiveData = MutableLiveData<Response<KFactor>>()

    fun getProfileImage():String {
       return VoipPref.getProfileImage()
    }
    fun getCallerName():String {
        return VoipPref.getLastCallerName()
    }
    fun getCallDuration() : String{
        val mTime = StringBuilder()
        val callTime = VoipPref.getLastCallStartTime()
        val second: Int = (callTime / 1000 % 60).toInt()
        val minute = (callTime / (1000 * 60) % 60).toInt()
        if (minute > 0) {
            mTime.append(minute).append(getMinuteString(minute))
        }
        if (second > 0) {
            mTime.append(second).append(getSecondString(second))
        }
        return mTime.toString()
    }

    private fun getMinuteString(min: Int): String {
        if (min > 1) {
            return " minutes "
        }
        return " minute "
    }

    private fun getSecondString(sec: Int): String {
        if (sec > 1) {
            return " seconds "
        }
        return " second "
    }

    fun closeDialog(function: (() -> Unit)?){
        function?.invoke()
    }

    fun submitFeedback(response: String) {
        CoroutineScope(Dispatchers.IO).launch {
            withTimeout(550) {
                try {
                    val requestParams: HashMap<String, String> = HashMap()
                    requestParams["channel_name"] = VoipPref.getLastCallerName()
                    requestParams["agora_mentor_id"] = VoipPref.getCurrentUserAgoraId().toString()
                    requestParams["response"] = response
                    val apiResponse = AppObjectController.p2pNetworkService.p2pCallFeedbackV2(requestParams)
                    responseLiveData.postValue(apiResponse)
                    WorkManagerAdmin.syncFavoriteCaller()
                    delay(250)
                } catch (ex: Throwable) {
                    ex.printStackTrace()
                }
            }
        }
    }
}




