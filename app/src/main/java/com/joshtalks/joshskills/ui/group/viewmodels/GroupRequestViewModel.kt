package com.joshtalks.joshskills.ui.group.viewmodels

import androidx.databinding.ObservableBoolean
import com.joshtalks.joshskills.base.BaseViewModel
import com.joshtalks.joshskills.constants.ON_BACK_PRESSED
import com.joshtalks.joshskills.core.showToast
import com.joshtalks.joshskills.ui.group.adapters.GroupRequestAdapter
import com.joshtalks.joshskills.ui.group.model.GroupMemberRequest
import com.joshtalks.joshskills.ui.group.repository.GroupRepository

class GroupRequestViewModel : BaseViewModel() {

    val repository = GroupRepository()
    val requestAdapter = GroupRequestAdapter()
    val fetchingGrpInfo = ObservableBoolean(false)
    var conversationId: String = ""

    fun onBackPress() {
        message.what = ON_BACK_PRESSED
        singleLiveEvent.value = message
    }

    val requestBtnResponse: (Boolean) -> Unit = {
        //TODO : Complete this function
    }
}