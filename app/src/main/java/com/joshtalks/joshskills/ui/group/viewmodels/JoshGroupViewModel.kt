package com.joshtalks.joshskills.ui.group.viewmodels

import android.app.AlertDialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView

import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn

import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.base.BaseViewModel
import com.joshtalks.joshskills.constants.*
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.core.ONE_GROUP_REQUEST_SENT
import com.joshtalks.joshskills.core.PrefManager
import com.joshtalks.joshskills.core.analytics.MixPanelEvent
import com.joshtalks.joshskills.core.analytics.MixPanelTracker
import com.joshtalks.joshskills.core.analytics.ParamKeys
import com.joshtalks.joshskills.core.showToast
import com.joshtalks.joshskills.ui.group.constants.SHOW_NEW_INFO
import com.joshtalks.joshskills.ui.group.adapters.GroupAdapter
import com.joshtalks.joshskills.ui.group.adapters.GroupStateAdapter
import com.joshtalks.joshskills.ui.group.analytics.GroupAnalytics
import com.joshtalks.joshskills.ui.group.constants.CLOSED_GROUP
import com.joshtalks.joshskills.ui.group.constants.OPENED_GROUP
import com.joshtalks.joshskills.ui.group.model.*
import com.joshtalks.joshskills.ui.group.utils.GroupItemComparator
import com.joshtalks.joshskills.ui.group.repository.GroupRepository
import com.joshtalks.joshskills.ui.leaderboard.constants.HAS_SEEN_GROUP_LIST_CBC_TOOLTIP

import kotlinx.android.synthetic.main.group_type_dialog.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    val adapter = GroupAdapter(GroupItemComparator,"search")
    val stateAdapter = GroupStateAdapter()
    val hasGroupData = ObservableBoolean(true)
    val addingNewGroup = ObservableBoolean(false)
    val newGroupVisible = ObservableBoolean(false)
    val groupType = ObservableField("")
    var shouldRefreshGroupList = false
    val isFromVoip = ObservableBoolean(false)
    val isFromGroupInfo = ObservableBoolean(false)
    val groupListCount = ObservableField(0)
    var isImageChanged = false
    var openedGroupId: String? = null
    var groupMemberCounts: Map<String, GroupMemberCount> = mapOf()
    var conversationId: String = ""
    var agoraId: Int = 0
    var mentorId: String = EMPTY

    val onItemClick: (GroupItemData) -> Unit = {
        message.what = OPEN_GROUP
        message.obj = it
        singleLiveEvent.value = message
        openedGroupId = it.getUniqueId()
    }

    fun getGroupData(): Flow<PagingData<GroupsItem>> {
        showProgressDialog("Loading your groups...")
        return repository.getGroupListResult(::groupDataLoaded).flow.cachedIn(viewModelScope)
    }

    fun getGroupLocalData() = repository.getGroupListLocal()

    fun onBackPress() {
        message.what = ON_BACK_PRESSED
        singleLiveEvent.value = message
    }

    fun groupDataLoaded(size: Int) {
        hasGroupData.set(size > 0)
        hasGroupData.notifyChange()
        repository.subscribeNotifications()
        repository.startChatEventListener()
        dismissProgressDialog()
    }

    fun showImageThumb(imagePath: String) {
        message.what = GROUP_IMAGE_SELECTED
        message.obj = imagePath
        singleLiveEvent.value = message
    }

    fun onSearch() {
        MixPanelTracker.publishEvent(MixPanelEvent.SEARCH_GROUPS).push()
        GroupAnalytics.push(GroupAnalytics.Event.FIND_GROUPS_TO_JOIN)
        message.what = SEARCH_GROUP
        singleLiveEvent.value = message
    }

    fun onSearch(view: View) {
        MixPanelTracker.publishEvent(MixPanelEvent.FIND_GROUPS_TO_JOIN).push()
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
            message.what = CREATE_GROUP_VALIDATION
            singleLiveEvent.value = message
        }
    }

    fun openAdminResponsibility(request: AddGroupRequest) {
        message.what = OPEN_ADMIN_RESPONSIBILITY
        message.obj = request
        singleLiveEvent.value = message
        GroupAnalytics.push(GroupAnalytics.Event.OPEN_ADMIN_RESPONSIBILITY)
        MixPanelTracker.publishEvent(MixPanelEvent.OPEN_ADMIN_RESPONSIBILITY).push()
    }

    fun createGroup(view: View) {
        message.what = ADD_GROUP_TO_SERVER
        singleLiveEvent.value = message
    }

    fun showProgressDialog(loadingMsg: String) {
        message.what = SHOW_PROGRESS_BAR
        message.obj = loadingMsg
        singleLiveEvent.value = message
    }

    fun dismissProgressDialog() {
        message.what = DISMISS_PROGRESS_BAR
        singleLiveEvent.value = message
    }

    fun onTooltipClick(view: View) {
        PrefManager.put(HAS_SEEN_GROUP_LIST_CBC_TOOLTIP, true)
        adapter.peek(0)?.let { onItemClick.invoke(it) }
    }

    fun openTypeChooser(view: View) {
        val groupTypeDialog = AlertDialog.Builder(view.context)
            .setView(R.layout.group_type_dialog)
            .setCancelable(false)
            .create()

        groupTypeDialog.show()
        groupTypeDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        groupTypeDialog.select_group_type.setOnClickListener {
            MixPanelTracker.publishEvent(MixPanelEvent.CHOOSE_GROUP_TYPE)
            if (groupTypeDialog.open_group_radio.isChecked) {
                (view as TextView).text = "Open Group"
                MixPanelTracker.addParam(ParamKeys.GROUP_TYPE, OPENED_GROUP)
                    .push()
            } else {
                (view as TextView).text = "Closed Group"
                MixPanelTracker.addParam(ParamKeys.GROUP_TYPE, CLOSED_GROUP)
                    .push()
            }
            groupTypeDialog.dismiss()
        }
    }

    fun addGroup(request: AddGroupRequest) {
        showProgressDialog("Creating a new group...")
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.addGroupToServer(request)
                MixPanelTracker.publishEvent(MixPanelEvent.NEW_GROUP_CREATED)
                    .addParam(ParamKeys.GROUP_TYPE, request.groupType)
                    .addParam(ParamKeys.ICON_ADDED, request.groupIcon != EMPTY)
                    .push()
                withContext(Dispatchers.Main) {
                    message.what = REFRESH_GRP_LIST_HIDE_INFO
                    message.data = Bundle().apply {
                        putBoolean(SHOW_NEW_INFO, true)
                    }
                    singleLiveEvent.value = message
                    dismissProgressDialog()
                    onBackPress()
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
                    dismissProgressDialog()
                }
                e.printStackTrace()
            }
        }
    }

    fun editGroup(request: EditGroupRequest, isNameChanged: Boolean) {
        showProgressDialog("Editing group information...")
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val isSuccess = repository.editGroupInServer(request, isNameChanged)
                MixPanelTracker.publishEvent(MixPanelEvent.GROUP_INFO_EDITED)
                    .addParam(ParamKeys.GROUP_ID, request.groupId)
                    .addParam(ParamKeys.IS_SUCCESS, isSuccess)
                    .push()
                withContext(Dispatchers.Main) {
                    if (isSuccess) {
                        message.what = SHOULD_REFRESH_GROUP_LIST
                        singleLiveEvent.value = message
                        onBackPress()
                        onBackPress()
                    }
                    dismissProgressDialog()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    if (e is HttpException) {
                        if (e.code() == 501)
                            showToast("Error : Same group name exist")
                        else
                            showToast("Error while editing group")
                    } else
                        showToast("Unknown Error Occurred")
                    dismissProgressDialog()
                }
                e.printStackTrace()
            }
        }
    }

    suspend fun getGroupOnlineCount() {
        addingNewGroup.set(true)
        withContext(Dispatchers.IO) {
            groupMemberCounts = repository.getGroupMembersCount() ?: mapOf()
            if (groupMemberCounts.isEmpty()) hasGroupData.set(false)
            addingNewGroup.set(false)
        }
    }

    fun setGroupsCount() = viewModelScope.launch(Dispatchers.IO) {
        groupListCount.set(repository.getGroupsCount())
        withContext(Dispatchers.Main) {
            if ((groupListCount.get() ?: 0) > 3) {
                message.what = INIT_LIST_TOOLTIP
                singleLiveEvent.value = message
            }
        }
    }

    fun subscribeToChat(groupId: String) = repository.startChatEventListener(groupId)

    fun unSubscribeToChat()  = repository.unSubscribeToChat()

    suspend fun deleteExtraMessages() = repository.removeExtraMessages()

    suspend fun getClosedGroupCount() = repository.getClosedGrpCount()

    fun getOneGrpReqStatus() = PrefManager.getBoolValue(ONE_GROUP_REQUEST_SENT)
}