package com.joshtalks.joshskills.ui.voip.new_arch.ui.call_rating

import androidx.lifecycle.viewModelScope
import com.joshtalks.joshskills.base.BaseViewModel
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.ui.call.data.local.VoipPref
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CallRatingsViewModel: BaseViewModel() {

    private val callRatingsRepository by lazy { CallRatingsRepository() }
    var ifDialogShow : String = "false"

    fun blockUser(toMentorId: String) {
        viewModelScope.launch(Dispatchers.IO) {
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
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val map: java.util.HashMap<String, Int?> = java.util.HashMap()
                map["agora_call_id"] = VoipPref.getLastCallId()
                val resp = AppObjectController.p2pNetworkService.showFppDialogNew(map).body()?.get("show_fpp_dialog")
                    .toString()
                ifDialogShow = resp
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

    fun submitCallRatings(agoraCallId : String, rating : Int, callerMentorId : String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val map: HashMap<String, String> = HashMap()
                map["agora_mentor"] = callerMentorId
                map["agora_call"] = agoraCallId
                map["rating"] = rating.toString()
                callRatingsRepository.submitCallRating(map)
            } catch (ex: Throwable) {
                ex.printStackTrace()
            }
        }
    }

}