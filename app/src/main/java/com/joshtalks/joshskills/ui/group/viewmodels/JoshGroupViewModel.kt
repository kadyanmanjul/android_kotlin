package com.joshtalks.joshskills.ui.group.viewmodels

import android.util.Log
import android.view.View
import androidx.databinding.ObservableBoolean
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import com.joshtalks.joshskills.base.BaseViewModel
import com.joshtalks.joshskills.constants.ADD_GROUP_TO_SERVER
import com.joshtalks.joshskills.constants.GROUP_IMAGE_SELECTED
import com.joshtalks.joshskills.constants.NO_GROUP_AVAILABLE
import com.joshtalks.joshskills.constants.ON_BACK_PRESSED
import com.joshtalks.joshskills.constants.OPEN_GROUP
import com.joshtalks.joshskills.constants.OPEN_IMAGE_CHOOSER
import com.joshtalks.joshskills.constants.OPEN_NEW_GROUP
import com.joshtalks.joshskills.constants.OPEN_POPUP_MENU
import com.joshtalks.joshskills.constants.SEARCH_GROUP
import com.joshtalks.joshskills.constants.SHOULD_REFRESH_GROUP_LIST
import com.joshtalks.joshskills.core.showToast
import com.joshtalks.joshskills.ui.group.adapters.GroupAdapter
import com.joshtalks.joshskills.ui.group.adapters.GroupStateAdapter
import com.joshtalks.joshskills.ui.group.model.AddGroupRequest
import com.joshtalks.joshskills.ui.group.utils.GroupItemComparator
import com.joshtalks.joshskills.ui.group.model.GroupItemData
import com.joshtalks.joshskills.ui.group.repository.GroupRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "JoshGroupViewModel"
class JoshGroupViewModel : BaseViewModel() {
    val onDataLoaded : (Boolean) -> Unit = {
        Log.d(TAG, ": $it")
        if(it.not() && isFromVoip.get()) {
            message.what = NO_GROUP_AVAILABLE
            singleLiveEvent.value = message
        }
        if(isFromVoip.get().not()) {
            hasGroupData.set(it)
            hasGroupData.notifyChange()
        }
    }
    val repository = GroupRepository(onDataLoaded)
    val adapter = GroupAdapter(GroupItemComparator)
    val stateAdapter = GroupStateAdapter()
    val hasGroupData = ObservableBoolean(true)
    val addingNewGroup = ObservableBoolean(false)
    var shouldRefreshGroupList = false
    val isFromVoip = ObservableBoolean(false)

    val onItemClick : (GroupItemData) -> Unit = {
        // TODO : Check if has data
        message.what = OPEN_GROUP
        message.obj = it
        singleLiveEvent.value = message
    }

    fun getGroupData() = repository.getGroupListResult().flow.cachedIn(viewModelScope)

    fun onBackPress() {
        message.what = ON_BACK_PRESSED
        singleLiveEvent.value = message
    }

    fun showImageThumb(imagePath : String) {
        message.what = GROUP_IMAGE_SELECTED
        message.obj = imagePath
        singleLiveEvent.value = message
    }

    fun onSearch() {
        message.what = SEARCH_GROUP
        singleLiveEvent.value = message
    }

    fun onMoreOption() {
        Log.d(TAG, "onMoreOption: ")
        message.what = OPEN_POPUP_MENU
        singleLiveEvent.value = message
    }

    fun openNewGroup() {
        message.what = OPEN_NEW_GROUP
        singleLiveEvent.value = message
    }

    fun openImageChooser(view: View) {
        message.what = OPEN_IMAGE_CHOOSER
        singleLiveEvent.value = message
    }

    fun saveGroupInfo(view: View) {
        message.what = ADD_GROUP_TO_SERVER
        singleLiveEvent.value = message
    }

    fun addGroup(request : AddGroupRequest) {
        showToast("Uploading")
        addingNewGroup.set(true)
        viewModelScope.launch(Dispatchers.IO) {
            try {
            repository.addGroupToServer(request)
                withContext(Dispatchers.Main) {
                    message.what = SHOULD_REFRESH_GROUP_LIST
                    singleLiveEvent.value = message
                    showToast("Group Added")
                    addingNewGroup.set(false)
                    onBackPress()
                }
            } catch (e : Exception) {
                withContext(Dispatchers.Main) {
                    showToast("Error while adding groups")
                }
                e.printStackTrace()
            }
        }
    }
}