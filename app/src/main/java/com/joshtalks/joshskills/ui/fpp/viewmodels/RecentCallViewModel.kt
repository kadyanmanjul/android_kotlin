package com.joshtalks.joshskills.ui.fpp.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.joshtalks.joshskills.core.ApiCallStatus
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.ui.fpp.model.RecentCallResponse
import com.joshtalks.joshskills.ui.fpp.repository.RecentCallsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class RecentCallViewModel(application: Application) : AndroidViewModel(application) {
    private val recentCallsRepository by lazy { RecentCallsRepository() }
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
                val response = recentCallsRepository.fetchRecentCallsFromApi()
                if (response?.isSuccessful == true) {
                    recentCallList.postValue(response.body())
                    apiCallStatus.postValue(ApiCallStatus.SUCCESS)
                }
            } catch (ex: Throwable) {
                apiCallStatus.postValue(ApiCallStatus.FAILED)
                ex.printStackTrace()
            }
        }
    }

    fun sendFppRequest(receiverMentorId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                recentCallsRepository.sendFppRequest(receiverMentorId)
                getFavorites()
            } catch (ex: Throwable) {
                ex.printStackTrace()
            }
        }
    }

    fun deleteFppRequest(receiverMentorId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                recentCallsRepository.deleteFppRequest(receiverMentorId)
                getFavorites()
            } catch (ex: Throwable) {
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
                recentCallsRepository.confirmOrRejectFppRequest(senderMentorId, map)
                getFavorites()
            } catch (ex: Throwable) {
                ex.printStackTrace()
            }
        }
    }

    fun blockUser(toMentorId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val map: HashMap<String, String> = HashMap()
                map["to_mentor_id"] = toMentorId
                recentCallsRepository.blockUser(map)
            } catch (ex: Throwable) {
                ex.printStackTrace()
            }
        }
    }
}