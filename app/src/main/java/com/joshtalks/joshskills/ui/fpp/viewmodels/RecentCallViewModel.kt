package com.joshtalks.joshskills.ui.fpp.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.joshtalks.joshskills.core.ApiCallStatus
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.ui.fpp.model.RecentCallResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class RecentCallViewModel(application: Application) : AndroidViewModel(application) {
    private val p2pNetworkService = AppObjectController.p2pNetworkService
    val recentCallList = MutableLiveData<RecentCallResponse?>()
    val apiCallStatus = MutableLiveData<ApiCallStatus>()

    fun getFavorites() {
        viewModelScope.launch(Dispatchers.IO) {
            fetchRecentCallsFromApi()
        }
    }

    private fun fetchRecentCallsFromApi() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                apiCallStatus.postValue(ApiCallStatus.START)
                val response = p2pNetworkService.getRecentCallsList(Mentor.getInstance().getId())
                if (response.isSuccessful) {
                    recentCallList.postValue(response.body())
                    apiCallStatus.postValue(ApiCallStatus.SUCCESS)
                    return@launch
                }
            } catch (ex: Throwable) {
                apiCallStatus.postValue(ApiCallStatus.FAILED)
                ex.printStackTrace()
            }
        }
    }
     fun sendFppRequest(receiverMentorId:String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                p2pNetworkService.sendFppRequest(receiverMentorId)
                getFavorites()
            } catch (ex: Throwable) {
                ex.printStackTrace()
            }
        }
    }
     fun deleteFppRequest(receiverMentorId:String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                p2pNetworkService.deleteFppRequest(receiverMentorId)
                getFavorites()
            } catch (ex: Throwable) {
                ex.printStackTrace()
            }
        }
    }
    fun confirmOrRejectFppRequest(senderMentorId:String,userStatus:String,pageType:String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val map: HashMap<String, String> = HashMap<String, String>()
                map[userStatus] = "true"
                map["page_type"] = pageType
                p2pNetworkService.confirmOrRejectFppRequest(senderMentorId, map)
                getFavorites()
            } catch (ex: Throwable) {
                ex.printStackTrace()
            }
        }
    }
}