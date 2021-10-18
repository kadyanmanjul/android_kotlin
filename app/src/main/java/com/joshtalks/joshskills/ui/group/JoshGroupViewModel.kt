package com.joshtalks.joshskills.ui.group

import android.util.Log
import androidx.databinding.ObservableBoolean
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.joshtalks.joshskills.base.BaseViewModel
import com.joshtalks.joshskills.constants.ON_BACK_PRESSED
import com.joshtalks.joshskills.constants.OPEN_GROUP
import com.joshtalks.joshskills.ui.group.model.GroupItemData
import com.joshtalks.joshskills.ui.group.repository.GroupPagingNetworkSource

private const val TAG = "JoshGroupViewModel"
class JoshGroupViewModel : BaseViewModel() {
    val hasGroups = ObservableBoolean(false)
    val adapter = GroupAdapter(GroupItemComparator)
    val onItemClick : (GroupItemData) -> Unit = {
        message.what = OPEN_GROUP
        message.obj = it
        singleLiveEvent.value = message
    }

    val flow = Pager(PagingConfig(10, enablePlaceholders = false)) {
        GroupPagingNetworkSource()
    }.flow.cachedIn(viewModelScope)

    fun onBackPress() {
        message.what = ON_BACK_PRESSED
        singleLiveEvent.value = message
    }

}