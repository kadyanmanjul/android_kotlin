package com.joshtalks.joshskills.ui.fpp.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.joshtalks.joshskills.core.ApiCallStatus
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.ui.fpp.model.RecentCallResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

class RecentCallViewModel(application: Application) : AndroidViewModel(application) {
    private val p2pNetworkService = AppObjectController.p2pNetworkService
    val recentCallList = MutableSharedFlow<RecentCallResponse?>()
    val apiCallStatus = MutableSharedFlow<ApiCallStatus>()

    fun getFavorites() {
        viewModelScope.launch(Dispatchers.IO) {
            fetchRecentCallsFromApi()
        }
    }

    private fun fetchRecentCallsFromApi() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = p2pNetworkService.getRecentCallsList(Mentor.getInstance().getId())
                if (response.isSuccessful) {
                    recentCallList.emit(response.body())
                    return@launch
                }
                apiCallStatus.emit(ApiCallStatus.SUCCESS)
            } catch (ex: Throwable) {
                apiCallStatus.emit(ApiCallStatus.SUCCESS)
                ex.printStackTrace()
            }
        }
    }
     fun sendFppRequest(receiverMentorId:String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                p2pNetworkService.sendFppRequest(receiverMentorId)
            } catch (ex: Throwable) {
                ex.printStackTrace()
            }
        }
    }
     fun deleteFppRequest(receiverMentorId:String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                p2pNetworkService.deleteFppRequest(receiverMentorId)
            } catch (ex: Throwable) {
                ex.printStackTrace()
            }
        }
    }

}