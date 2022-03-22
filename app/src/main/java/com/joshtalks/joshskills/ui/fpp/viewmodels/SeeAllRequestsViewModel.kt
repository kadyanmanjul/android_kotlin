package com.joshtalks.joshskills.ui.fpp.viewmodels

import android.view.View
import androidx.databinding.ObservableBoolean
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.joshtalks.joshskills.base.BaseViewModel
import com.joshtalks.joshskills.core.ApiCallStatus
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.ui.fpp.adapters.SeeAllRequestsAdapter
import com.joshtalks.joshskills.ui.fpp.constants.FPP_SEE_ALL_BACK_PRESSED
import com.joshtalks.joshskills.ui.fpp.constants.IS_REJECTED
import com.joshtalks.joshskills.ui.fpp.constants.REQUESTS_SCREEN
import com.joshtalks.joshskills.ui.fpp.constants.CONFIRM_REQUEST_TYPE
import com.joshtalks.joshskills.ui.fpp.constants.IS_ACCEPTED
import com.joshtalks.joshskills.ui.fpp.constants.NOT_NOW_REQUEST_TYPE
import com.joshtalks.joshskills.ui.fpp.constants.FPP_OPEN_USER_PROFILE
import com.joshtalks.joshskills.ui.fpp.model.PendingRequestDetail
import com.joshtalks.joshskills.ui.fpp.model.PendingRequestResponse
import com.joshtalks.joshskills.ui.fpp.repository.RequestsRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SeeAllRequestsViewModel : BaseViewModel() {
    private val requestsRepository by lazy { RequestsRepository() }
    val pendingRequestsList = MutableLiveData<PendingRequestResponse>()
    val apiCallStatus = MutableLiveData<ApiCallStatus>()
    val adapter = SeeAllRequestsAdapter()
    val hasSeeAllRequest = ObservableBoolean(true)
    val fetchingAllPendingRequestInfo = ObservableBoolean(false)
    val dispatcher: CoroutineDispatcher by lazy { Dispatchers.Main }
    val isButtonClick = ObservableBoolean(false)


    fun getPendingRequestsList() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                fetchingAllPendingRequestInfo.set(true)
                val response = requestsRepository.getPendingRequestsList()
                if (response?.isSuccessful == true && response.body() != null) {
                    withContext(dispatcher) {
                        adapter.addSeeAllRequestToList(response.body()!!.pendingRequestsList)
                        fetchingAllPendingRequestInfo.set(false)
                    }
                }
                if (response?.body()?.pendingRequestsList?.isEmpty() == true) {
                    withContext(dispatcher) {
                        hasSeeAllRequest.set(false)
                    }
                }
            } catch (ex: Throwable) {
                fetchingAllPendingRequestInfo.set(false)
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

    fun onBackPress(view: View) {
        message.what = FPP_SEE_ALL_BACK_PRESSED
        singleLiveEvent.value = message
    }

    val onItemClick: (PendingRequestDetail, Int) -> Unit = { it, type ->
        when (type) {
            CONFIRM_REQUEST_TYPE -> {
                fppRequestApiCall(it.senderMentorId ?: EMPTY, IS_ACCEPTED)
            }
            NOT_NOW_REQUEST_TYPE -> {
                fppRequestApiCall(it.senderMentorId ?: EMPTY, IS_REJECTED)
            }
            FPP_OPEN_USER_PROFILE -> {
                message.what = FPP_OPEN_USER_PROFILE
                message.obj = it.senderMentorId
                singleLiveEvent.value = message
            }
        }
    }

    fun fppRequestApiCall(senderMentorId: String, type: String) {
        confirmOrRejectFppRequest(
            senderMentorId,
            type,
            REQUESTS_SCREEN
        )
    }
}