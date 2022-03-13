package com.joshtalks.joshskills.ui.fpp.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.facebook.internal.Mutable
import com.joshtalks.joshskills.core.ApiCallStatus
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.ui.fpp.model.PendingRequestResponse
import com.joshtalks.joshskills.ui.fpp.repository.RequestsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SeeAllRequestsViewModel(application: Application) : AndroidViewModel(application) {
    private val requestsRepository by lazy { RequestsRepository() }
    val pendingRequestsList = MutableLiveData<PendingRequestResponse>()
    val apiCallStatus = MutableLiveData<ApiCallStatus>()

    fun getPendingRequestsList() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                apiCallStatus.postValue(ApiCallStatus.START)
                val response = requestsRepository.getPendingRequestsList()
                if (response?.isSuccessful == true && response?.body() != null) {
                    pendingRequestsList.postValue(response.body())
                    apiCallStatus.postValue(ApiCallStatus.SUCCESS)
                }
            } catch (ex: Throwable) {
                apiCallStatus.postValue(ApiCallStatus.SUCCESS)
                ex.printStackTrace()
            }
        }
    }

    fun confirmOrRejectFppRequest(senderMentorId: String, userStatus: String, pageType: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val map: HashMap<String, String> = HashMap()
                map[userStatus] = "true"
                map["page_type"] = pageType
                requestsRepository.confirmOrRejectFppRequest(senderMentorId, map)
            } catch (ex: Throwable) {
                ex.printStackTrace()
            }
        }
    }
}