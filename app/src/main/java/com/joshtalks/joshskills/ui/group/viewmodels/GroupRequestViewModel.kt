package com.joshtalks.joshskills.ui.group.viewmodels

import androidx.databinding.ObservableBoolean
import androidx.lifecycle.viewModelScope
import com.joshtalks.joshskills.base.BaseViewModel
import com.joshtalks.joshskills.constants.ON_BACK_PRESSED
import com.joshtalks.joshskills.ui.group.adapters.GroupRequestAdapter
import com.joshtalks.joshskills.ui.group.repository.GroupRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GroupRequestViewModel : BaseViewModel() {

    val repository = GroupRepository()
    val requestAdapter = GroupRequestAdapter()
    val fetchingGrpInfo = ObservableBoolean(false)

    var conversationId: String = ""
    var groupId: String = ""

    fun getRequestList() {
        viewModelScope.launch(Dispatchers.IO) {
            val response = repository.fetchRequestList(groupId) ?: listOf()
            withContext(Dispatchers.Main) {
                requestAdapter.addRequestsToList(response)
            }
        }
    }

    fun onBackPress() {
        message.what = ON_BACK_PRESSED
        singleLiveEvent.value = message
    }

    val requestBtnResponse: (Boolean) -> Unit = {
        //TODO : Complete this function
    }
}