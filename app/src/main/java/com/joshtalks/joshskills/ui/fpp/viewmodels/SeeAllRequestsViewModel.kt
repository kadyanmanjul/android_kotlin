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
                val response = p2pNetworkService.getPendingRequestsList()
                if (response.isSuccessful) {
                    pendingRequestsList.postValue(response.body())
                    return@launch
                }
                apiCallStatus.postValue(ApiCallStatus.SUCCESS)
            } catch (ex: Throwable) {
                apiCallStatus.postValue(ApiCallStatus.SUCCESS)
                ex.printStackTrace()
            }
        }
    }
    fun confirmOrRejectFppRequest(senderMentorId:String,userStatus:String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                p2pNetworkService.confirmOrRejectFppRequest(senderMentorId, mapOf(userStatus to "true"))
            } catch (ex: Throwable) {
                ex.printStackTrace()
            }
        }
    }
}