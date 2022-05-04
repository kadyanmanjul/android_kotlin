package com.joshtalks.joshskills.ui.voip.voip_rating

import androidx.lifecycle.viewModelScope
import com.joshtalks.joshskills.base.BaseViewModel
import com.joshtalks.joshskills.ui.fpp.constants.TO_MENTOR_ID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CallRatingsViewModel: BaseViewModel() {
    private val callRatingsRepository by lazy { CallRatingsRepository() }
    fun blockUser(toMentorId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val map: HashMap<String, String> = HashMap()
                map[TO_MENTOR_ID] = toMentorId
                callRatingsRepository.blockUser(map)
            } catch (ex: Throwable) {
                ex.printStackTrace()
            }
        }
    }

    fun submitCallRatings(agoraCallId : Int, rating : Int, agoraMentorId : String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val map: HashMap<String, String> = HashMap()
                map["agora_mentor"] = agoraMentorId
                map["agora_call"] = agoraCallId.toString()
                map["rating"] = rating.toString()
                callRatingsRepository.submitCallRating(map)
            } catch (ex: Throwable) {
                ex.printStackTrace()
            }
        }
    }

}