package com.joshtalks.joshskills.ui.group.viewmodels

import android.os.Bundle
import android.util.Log
import android.view.View

import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn

import com.joshtalks.joshskills.base.BaseViewModel
import com.joshtalks.joshskills.constants.*
import com.joshtalks.joshskills.core.showToast
import com.joshtalks.joshskills.ui.group.SHOW_NEW_INFO
import com.joshtalks.joshskills.ui.group.adapters.GroupAdapter
import com.joshtalks.joshskills.ui.group.adapters.GroupStateAdapter
import com.joshtalks.joshskills.ui.group.analytics.GroupAnalytics
import com.joshtalks.joshskills.ui.group.model.AddGroupRequest
import com.joshtalks.joshskills.ui.group.model.EditGroupRequest
import com.joshtalks.joshskills.ui.group.utils.GroupItemComparator
import com.joshtalks.joshskills.ui.group.model.GroupItemData
import com.joshtalks.joshskills.ui.group.repository.GroupRepository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.internal.notifyAll

import retrofit2.HttpException

private const val TAG = "JoshGroupViewModel"

class JoshGroupViewModel : BaseViewModel() {
    val onDataLoaded: (Boolean) -> Unit = {
        Log.d(TAG, ": $it")
        hasGroupData.set(it)
        hasGroupData.notifyChange()
    }

    val repository = GroupRepository(onDataLoaded)
    val groupTitle = ObservableField("Groups")
    val groupImageUrl = ObservableField("")
    val adapter = GroupAdapter(GroupItemComparator)
    val stateAdapter = GroupStateAdapter()
    val hasGroupData = ObservableBoolean(true)
    val addingNewGroup = ObservableBoolean(false)
    var shouldRefreshGroupList = false
    val isFromVoip = ObservableBoolean(false)
    val isFromGroupInfo = ObservableBoolean(false)
    var isImageChanged = false
    var conversationId: String = ""
    var currentlyOpenedGroup = ""

    val onItemClick: (GroupItemData) -> Unit = {
        // TODO : Check if has data
        message.what = OPEN_GROUP
        message.obj = it
        singleLiveEvent.value = message
    }

    fun getGroupData() =
        repository.getGroupListResult(::groupDataLoaded).flow.cachedIn(viewModelScope)

    fun onBackPress() {
        message.what = ON_BACK_PRESSED
        singleLiveEvent.value = message
    }

    fun groupDataLoaded(size : Int) {
        hasGroupData.set(size > 0)
        hasGroupData.notifyChange()
        repository.subscribeNotifications()
        repository.startChatEventListener()
    }

    fun showImageThumb(imagePath: String) {
        message.what = GROUP_IMAGE_SELECTED
        message.obj = imagePath
        singleLiveEvent.value = message
    }

    fun onSearch() {
        message.what = SEARCH_GROUP
        singleLiveEvent.value = message
    }

    fun onSearch(view: View) {
        GroupAnalytics.push(GroupAnalytics.Event.FIND_GROUPS_TO_JOIN)
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
        if (isFromGroupInfo.get()) {
            message.what = SAVE_GROUP_INFO
            singleLiveEvent.value = message
        } else {
            message.what = ADD_GROUP_TO_SERVER
            singleLiveEvent.value = message
        }
    }

    fun addGroup(request: AddGroupRequest) {
        addingNewGroup.set(true)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.addGroupToServer(request)
                withContext(Dispatchers.Main) {
                    message.what = REFRESH_GRP_LIST_HIDE_INFO
                    message.data = Bundle().apply {
                        putBoolean(SHOW_NEW_INFO, true)
                    }
                    singleLiveEvent.value = message
                    addingNewGroup.set(false)
                    onBackPress()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    if (e is HttpException) {
                        if (e.code() == 501)
                            showToast("Error : Same Group exist")
                        else
                            showToast("Error while adding group")
                    } else
                        showToast("Unknown Error Occurred")
                    addingNewGroup.set(false)
                }
                e.printStackTrace()
            }
        }
    }

    fun editGroup(request: EditGroupRequest, isNameChanged: Boolean) {
        addingNewGroup.set(true)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val isSuccess = repository.editGroupInServer(request, isNameChanged)
                withContext(Dispatchers.Main) {
                    if (isSuccess) {
                        message.what = SHOULD_REFRESH_GROUP_LIST
                        singleLiveEvent.value = message
                        onBackPress()
                        onBackPress()
                    }
                    addingNewGroup.set(false)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    if (e is HttpException) {
                        if (e.code() == 501)
                            showToast("Error : Same group name exist")
                        else
                            showToast("Error while adding group")
                    } else
                        showToast("Unknown Error Occurred")
                    addingNewGroup.set(false)
                }
                e.printStackTrace()
            }
        }
    }
}