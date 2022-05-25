package com.joshtalks.joshskills.ui.voip.new_arch.ui.call_rating

import androidx.lifecycle.viewModelScope
import com.joshtalks.joshskills.base.BaseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CallRatingsViewModel: BaseViewModel() {
    private val callRatingsRepository by lazy { CallRatingsRepository() }
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