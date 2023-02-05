package com.joshtalks.joshskills.premium.ui.voip.new_arch.ui.call_rating

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.joshtalks.joshskills.premium.base.BaseViewModel
import com.joshtalks.joshskills.premium.core.AppObjectController
import com.joshtalks.joshskills.premium.repository.local.model.KFactor
import com.joshtalks.joshskills.premium.repository.local.model.Mentor
import com.joshtalks.joshskills.premium.ui.call.data.local.VoipPref
import kotlinx.coroutines.*
import retrofit2.Response
import timber.log.Timber

class CallRatingsViewModel: BaseViewModel() {

    private val callRatingsRepository by lazy { CallRatingsRepository() }
    var ifDialogShow : Int = 1
    var ifGoogleInAppReviewShow : Int = 4
    var responseLiveData = MutableLiveData<Response<KFactor>?>()


    fun blockUser(toMentorId: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val map: HashMap<String, String> = HashMap()
                map["agora_uid"] = toMentorId
                callRatingsRepository.blockUser(map)
            } catch (ex: Throwable) {
                ex.printStackTrace()
            }
        }
    }
    fun getCallDurationString() : String{
        getFppDialogFlag()
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

    private fun getFppDialogFlag() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val map: java.util.HashMap<String, Int?> = java.util.HashMap()
                map["agora_call_id"] = VoipPref.getLastCallId()
                val resp = AppObjectController.p2pNetworkService.showFppDialogNew(map).body()
                ifDialogShow = resp?.get("fpp_option") ?: 1
                ifGoogleInAppReviewShow = resp?.get("playstore_rating") ?: 4
            }catch (ex: Throwable) {
                ex.printStackTrace()
            }
        }
    }

    private fun getSecondString(sec: Long): String {
        if (sec > 1L) {
            return " seconds "
        }
        return " second "
    }

    fun submitCallRatings(agoraCallId : String, rating : Int?, callerMentorId : String,userAction:String?) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val map: HashMap<String, Any?> = HashMap()
                map["agora_mentor"] = callerMentorId.toInt()
                map["agora_call"] = agoraCallId.toInt()
                map["rating"] = rating
                map["user_action"] = userAction
                AppObjectController.p2pNetworkService.submitCallRatings(map)
            } catch (ex: Throwable) {
                ex.printStackTrace()
            }
        }
    }

    fun sendFppRequest(mentorId: String) {
        CoroutineScope(Dispatchers.IO).launch {
                try {
                   val map =  HashMap<String,String>()
                    map["page_type"] = "CALL_RATING"
                      callRatingsRepository.sendFppRequest(mentorId,map)
                } catch (ex: Throwable) {
                    ex.printStackTrace()
                }
            }
        }
    fun saveImpression(eventName: String) {
        viewModelScope.launch(Dispatchers.IO) {
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