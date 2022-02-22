package com.joshtalks.joshskills.ui.fpp.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.joshtalks.joshskills.core.ApiCallStatus
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.ui.fpp.model.PendingRequestResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SeeAllRequestsViewModel(application: Application) : AndroidViewModel(application){
    private val p2pNetworkService = AppObjectController.p2pNetworkService
    val pendingRequestsList = MutableLiveData<PendingRequestResponse>()
    val apiCallStatus = MutableLiveData<ApiCallStatus>()

    fun getPendingRequestsList() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                apiCallStatus.postValue(ApiCallStatus.START)
                val response = p2pNetworkService.getPendingRequestsList()
                if (response.isSuccessful) {
                    pendingRequestsList.postValue(response.body())
                    apiCallStatus.postValue(ApiCallStatus.SUCCESS)
                    return@launch
                }
            } catch (ex: Throwable) {
                apiCallStatus.postValue(ApiCallStatus.SUCCESS)
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
            } catch (ex: Throwable) {
                ex.printStackTrace()
            }
        }
    }
}