package com.joshtalks.joshskills.ui.group.views

import android.util.Log
import androidx.databinding.ObservableBoolean
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import com.joshtalks.joshskills.base.BaseViewModel
import com.joshtalks.joshskills.constants.ON_BACK_PRESSED
import com.joshtalks.joshskills.constants.OPEN_CALLING_ACTIVITY
import com.joshtalks.joshskills.ui.group.adapters.GroupAdapter
import com.joshtalks.joshskills.ui.group.adapters.GroupStateAdapter
import com.joshtalks.joshskills.ui.group.model.GroupItemData
import com.joshtalks.joshskills.ui.group.repository.GroupRepository
import com.joshtalks.joshskills.ui.group.utils.GroupItemComparator


class JoshVoipGroupViewModel : BaseViewModel() {
    val onDataLoaded : (Boolean) -> Unit = {
        hasGroupData.set(it)
        hasGroupData.notifyChange()
    }
    val hasGroupData = ObservableBoolean(true)
    val repository = GroupRepository()
    val adapter = GroupAdapter(GroupItemComparator)
    val stateAdapter = GroupStateAdapter()
    var shouldRefreshGroupList = false

    val onItemClick : (GroupItemData) -> Unit = {
        message.what = OPEN_CALLING_ACTIVITY
        message.obj = it
        singleLiveEvent.value = message
    }

    fun getGroupData() = repository.getGroupListResult().flow.cachedIn(viewModelScope)

    fun onBackPress() {
        message.what = ON_BACK_PRESSED
        singleLiveEvent.value = message
    }
}