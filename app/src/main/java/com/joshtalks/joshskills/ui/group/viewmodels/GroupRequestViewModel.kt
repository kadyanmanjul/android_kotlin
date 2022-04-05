package com.joshtalks.joshskills.ui.group.viewmodels

import androidx.databinding.ObservableBoolean
import androidx.lifecycle.viewModelScope
import com.joshtalks.joshskills.base.BaseViewModel
import com.joshtalks.joshskills.constants.DISMISS_PROGRESS_BAR
import com.joshtalks.joshskills.constants.ON_BACK_PRESSED
import com.joshtalks.joshskills.constants.SHOW_PROGRESS_BAR
import com.joshtalks.joshskills.core.showToast
import com.joshtalks.joshskills.ui.group.adapters.GroupRequestAdapter
import com.joshtalks.joshskills.ui.group.constants.CLOSED_GROUP
import com.joshtalks.joshskills.ui.group.model.GroupRequest
import com.joshtalks.joshskills.ui.group.repository.GroupRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GroupRequestViewModel : BaseViewModel() {

    val repository = GroupRepository()
    val requestAdapter = GroupRequestAdapter()
    val noRequests = ObservableBoolean(false)

    var conversationId: String = ""
    var groupId: String = ""

    fun onBackPress() {
        message.what = ON_BACK_PRESSED
        singleLiveEvent.value = message
    }

    fun showProgressDialog(msg: String) {
        message.what = SHOW_PROGRESS_BAR
        message.obj = msg
        singleLiveEvent.value = message
    }

    fun dismissProgressDialog() {
        message.what = DISMISS_PROGRESS_BAR
        singleLiveEvent.value = message
    }

    val requestBtnResponse: (String, Boolean) -> Unit = { mentorId, allow ->
        showProgressDialog("Allowing to join group...")
        viewModelScope.launch {
            try {
                val request = GroupRequest(
                    mentorId = mentorId,
                    groupId = groupId,
                    allow = allow,
                    groupType = CLOSED_GROUP
                )
                val response = repository.sendRequestResponse(request)
                if (response)
                    dismissProgressDialog()
                else {
                    dismissProgressDialog()
                    showToast("Error responding to the request")
                }
            } catch (e: Exception) {
                dismissProgressDialog()
                showToast("Error responding to the request")
                e.printStackTrace()
            }
        }
    }

    fun getRequestList() {
        showProgressDialog("Loading...")
        viewModelScope.launch(Dispatchers.IO) {
            val response = repository.fetchRequestList(groupId) ?: listOf()
            withContext(Dispatchers.Main) {
                if (response.isEmpty())
                    noRequests.set(true)
                requestAdapter.addRequestsToList(response)
                dismissProgressDialog()
            }
        }
    }
}