package com.joshtalks.joshskills.ui.fpp.viewmodels

import android.view.View
import androidx.databinding.ObservableBoolean
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.joshtalks.joshskills.base.BaseViewModel
import com.joshtalks.joshskills.core.ApiCallStatus
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.ui.fpp.adapters.RecentCallsAdapter
import com.joshtalks.joshskills.ui.fpp.constants.*
import com.joshtalks.joshskills.ui.fpp.model.RecentCall
import com.joshtalks.joshskills.ui.fpp.model.RecentCallResponse
import com.joshtalks.joshskills.ui.fpp.repository.RecentCallsRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RecentCallViewModel : BaseViewModel() {
    private val recentCallsRepository by lazy { RecentCallsRepository() }
    val recentCallList = MutableLiveData<RecentCallResponse?>()
    val apiCallStatus = MutableLiveData<ApiCallStatus>()
    val adapter = RecentCallsAdapter()
    val isListEmpty = ObservableBoolean(false)
    val fetchingAllRecentCall = ObservableBoolean(false)
    val dispatcher: CoroutineDispatcher by lazy { Dispatchers.Main }
    var itemPosition  = 0
    var isFirstTime = true

    fun getRecentCall() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                fetchingAllRecentCall.set(true)
                val response = recentCallsRepository.fetchRecentCallsFromApi()
                if (response?.isSuccessful == true) {

                    if (response.body()?.arrayList != null) {
                        withContext(dispatcher) {
                            if (isFirstTime) {
                                adapter.addRecentCallToList(response.body()?.arrayList!!)
                                isFirstTime = false
                            } else {
                                adapter.updateList(response.body()!!.arrayList)
                                message.what = SCROLL_TO_POSITION
                                message.obj = itemPosition
                                singleLiveEvent.value = message
                            }
                            fetchingAllRecentCall.set(false)
                            isListEmpty.set(false)
                        }
                    }

                    if (adapter.itemCount <= 0) {
                        withContext(dispatcher) {
                            isListEmpty.set(true)
                        }
                    }

                    if (response.body()?.arrayList?.isEmpty() == true) {
                        withContext(dispatcher) {
                            isListEmpty.set(true)
                        }
                    }
                }
            } catch (ex: Throwable) {
                fetchingAllRecentCall.set(false)
                ex.printStackTrace()
            }
        }
    }

    fun sendFppRequest(receiverMentorId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                recentCallsRepository.sendFppRequest(receiverMentorId)
                getRecentCall()
            } catch (ex: Throwable) {
                ex.printStackTrace()
            }
        }
    }

    fun deleteFppRequest(receiverMentorId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                recentCallsRepository.deleteFppRequest(receiverMentorId)
                getRecentCall()
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
                getRecentCall()
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

    val onItemClick: (RecentCall, Int, Int) -> Unit = { it, type, position ->
        itemPosition = position
        when (type) {
            RECENT_CALL_USER_BLOCK -> {
                message.what = RECENT_CALL_USER_BLOCK
                message.obj = it
                singleLiveEvent.value = message
            }
            RECENT_OPEN_USER_PROFILE -> {
                message.what = RECENT_OPEN_USER_PROFILE
                message.obj = it.receiverMentorId
                singleLiveEvent.value = message
            }
            RECENT_CALL_SENT_REQUEST -> {
                sendFppRequest(it.receiverMentorId?: EMPTY)
            }
            RECENT_CALL_REQUESTED -> {
                deleteFppRequest(it.receiverMentorId?: EMPTY)
            }
            RECENT_CALL_HAS_RECIEVED_REQUESTED -> {
                message.what = RECENT_CALL_HAS_RECIEVED_REQUESTED
                message.obj = it
                singleLiveEvent.value = message
            }
        }
    }

    fun onBackPress(view: View) {
        message.what = FPP_RECENT_CALL_ON_BACK_PRESS
        singleLiveEvent.value = message
    }
}