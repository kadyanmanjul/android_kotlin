package com.joshtalks.joshskills.ui.group.viewmodels

import android.app.AlertDialog
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast

import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.lifecycle.viewModelScope
import androidx.paging.ExperimentalPagingApi
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.recyclerview.widget.RecyclerView

import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.base.BaseViewModel
import com.joshtalks.joshskills.constants.*
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.core.analytics.MixPanelEvent
import com.joshtalks.joshskills.core.analytics.MixPanelTracker
import com.joshtalks.joshskills.core.analytics.ParamKeys
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.track.AGORA_UID
import com.joshtalks.joshskills.ui.group.adapters.GroupChatAdapter
import com.joshtalks.joshskills.ui.group.adapters.GroupMemberAdapter
import com.joshtalks.joshskills.ui.group.analytics.GroupAnalytics
import com.joshtalks.joshskills.ui.group.constants.*
import com.joshtalks.joshskills.ui.group.lib.ChatService
import com.joshtalks.joshskills.ui.group.lib.PubNubService
import com.joshtalks.joshskills.ui.group.model.ChatItem
import com.joshtalks.joshskills.ui.group.model.GroupJoinRequest
import com.joshtalks.joshskills.ui.group.model.GroupMember
import com.joshtalks.joshskills.ui.group.model.LeaveGroupRequest
import com.joshtalks.joshskills.ui.group.model.MessageItem
import com.joshtalks.joshskills.ui.group.repository.GroupRepository
import com.joshtalks.joshskills.ui.group.utils.GroupChatComparator
import com.joshtalks.joshskills.ui.group.utils.getMemberCount
import com.joshtalks.joshskills.ui.group.utils.pushMetaMessage
import com.joshtalks.joshskills.ui.group.utils.pushTimeMetaMessage
import com.pubnub.api.models.consumer.push.payload.PushPayloadHelper
import de.hdodenhof.circleimageview.CircleImageView

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.internal.concat

private const val TAG = "GroupChatViewModel"

class GroupChatViewModel : BaseViewModel() {

    val repository = GroupRepository()
    val hasJoinedGroup = ObservableBoolean(false)
    var groupHeader = ObservableField("")
    var imageUrl = ObservableField("")
    var groupType = ObservableField("")
    val groupCreator = ObservableField("")
    val groupJoinStatus = ObservableField("")
    var memberCount = ObservableField(0)
    var requestCount = ObservableField("")
    val memberAdapter = GroupMemberAdapter()
    val groupSubHeader = ObservableField("")
    val requestQuestion = ObservableField("Why do you want to join this group?")
    val fetchingGrpInfo = ObservableBoolean(false)
    val showRequestsTab = ObservableBoolean(false)
    var scrollToEnd = false
    var unreadCount = 0
    private val chatService : ChatService = PubNubService

    var groupId: String = ""
    var adminId: String = ""
    var conversationId: String = ""
    var groupText: String = ""
    var chatSendText: String = ""
    var agoraId: Int = 0

    val chatAdapter = GroupChatAdapter(GroupChatComparator).apply {
        registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                super.onItemRangeInserted(positionStart, itemCount)
                message.what = NEW_CHAT_ADDED
                val bundle = Bundle()
                if (positionStart == 0 && unreadCount != 0) {
                    bundle.putInt(GROUP_CHAT_UNREAD, if (unreadCount > 2) unreadCount - 2 else 0)
                    unreadCount = 0
                } else bundle.putInt(GROUP_CHAT_UNREAD, 0)
                message.data = bundle
                singleLiveEvent.value = message
            }
        })
    }

    fun setChatAdapterType() {
        chatAdapter.setType(groupType.get() ?: OPENED_GROUP)
    }

    val openMemberPopup: (GroupMember, View) -> Unit = { it, view ->
        if (it.mentorID != Mentor.getInstance().getId() && !(groupType.get().equals(COHORT_GROUP) && adminId == it.mentorID)) {
            val builder = AlertDialog.Builder(view.context)
            var memberOptions = arrayOf("View Profile")

            if (adminId == Mentor.getInstance().getId() && !it.isAdmin)
                memberOptions = memberOptions.concat("Remove ${it.memberName}")

            builder.setAdapter(ArrayAdapter(view.context, android.R.layout.simple_list_item_1, memberOptions)) { _, item ->
                when (item) {
                    0 -> openProfile(it.mentorID)
                    1 -> {
                        val removeMsg = "Remove ${it.memberName} from \"${groupHeader.get()}\" group?"
                        showAlertDialog(view, removeMsg, "Ok", "Removing member") {
                            removeMemberFromGroup(it.mentorID, it.memberName)
                        }
                    }
                }
            }

            val dialog: AlertDialog = builder.create()
            dialog.show()
        }
    }

    fun onBackPress() {
        message.what = ON_BACK_PRESSED
        singleLiveEvent.value = message
    }

    fun callGroup() {
        if (isCallOngoing(R.string.call_engage_initiate_call_message))
            return

        if (groupType.get() != DM_CHAT) {
            val memberText = groupSubHeader.get() ?: "0"
            val memberCount = getMemberCount(memberText)
            if (memberCount == 0) {
                showToast("Unknown Error Occurred")
                return
            } else if (memberCount == 1) {
                showToast("You are the only member, Can't Initiate a Call")
                return
            }
        }

        message.what = OPEN_CALLING_ACTIVITY
        message.data = Bundle().apply {
            putString(GROUPS_ID, groupId)
            putString(GROUPS_TITLE, groupHeader.get())
            putString(GROUP_TYPE, groupType.get())
            if (groupId == DM_CHAT)
                putInt(AGORA_UID, agoraId)
        }
        singleLiveEvent.value = message
    }

    fun joinGroup(view: View) {
        if (groupType.get() == CLOSED_GROUP && groupJoinStatus.get() == "REQUEST TO JOIN") {
            MixPanelTracker.publishEvent(MixPanelEvent.REQUEST_TO_JOIN_GROUP)
                .addParam(ParamKeys.GROUP_ID, groupId)
                .push()
            message.what = OPEN_GROUP_REQUEST
            singleLiveEvent.value = message
        } else if (groupType.get() == OPENED_GROUP)
            joinPublicGroup()
    }

    fun validateJoinRequest(view: View) {
        message.what = REQUEST_GROUP_VALIDATION
        singleLiveEvent.value = message
    }

    fun joinPublicGroup() {
        showProgressDialog("Joining Group...")
        viewModelScope.launch {
            try {
                val response = repository.joinGroup(groupId)
                if (response) {
                    withContext(Dispatchers.Main) {
                        dismissProgressDialog()
                        message.what = REFRESH_GRP_LIST_HIDE_INFO
                        message.data = Bundle().apply {
                            putBoolean(SHOW_NEW_INFO, true)
                        }
                        singleLiveEvent.value = message
                        pushMetaMessage("${Mentor.getInstance().getUser()?.firstName} has joined this group", groupId)
                        onBackPress()
                        onBackPress()
                        MixPanelTracker.publishEvent(MixPanelEvent.JOIN_GROUP)
                            .addParam(ParamKeys.GROUP_ID,groupId)
                            .push()
                    }
                } else {
                    dismissProgressDialog()
                    showToast("Error joining group")
                }
            } catch (e: Exception) {
                dismissProgressDialog()
                showToast("Error joining group")
                e.printStackTrace()
            }
        }
    }

    fun joinPrivateGroup(request: GroupJoinRequest) {
        showProgressDialog("Sending Request to join...")
        viewModelScope.launch {
            try {
                val response = repository.sendJoinGroupRequest(request)
                if (response) {
                    onBackPress()
                    onBackPress()
                    message.what = CLEAR_SEARCH
                    singleLiveEvent.value = message
                    showToast("Request sent! Please wait for the admin to accept")
                } else
                    showToast("Error in sending request")
                dismissProgressDialog()
                PrefManager.put(ONE_GROUP_REQUEST_SENT, true)
                GroupAnalytics.push(GroupAnalytics.Event.REQUEST_TO_JOIN, groupId)
                MixPanelTracker.publishEvent(MixPanelEvent.REQUEST_TO_JOIN_SUBMIT)
                    .addParam(ParamKeys.GROUP_ID, groupId)
                    .addParam(ParamKeys.ANSWER, request.answer)
                    .push()
            } catch (e: Exception) {
                dismissProgressDialog()
                showToast("Error in sending request")
                e.printStackTrace()
            }
        }
    }

    fun showExitDialog(view: View) {
        MixPanelTracker.publishEvent(MixPanelEvent.EXIT_GROUP_CLICKED)
            .addParam(ParamKeys.GROUP_ID, groupId)
            .push()
        showAlertDialog(
            view,
            "Exit \"${groupHeader.get()}\" group?",
            "Exit",
            "Leaving group"
        ) { leaveGroup() }
    }

    fun removeMemberFromGroup(mentorId: String, memberName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val request = LeaveGroupRequest(
                    groupId = groupId,
                    mentorId = mentorId
                )
                val response = repository.removeMemberFromGroup(request)
                if (!response) {
                    showToast("An error has occurred")
                    dismissProgressDialog()
                    return@launch
                }
                pushMetaMessage("${Mentor.getInstance().getUser()?.firstName} removed $memberName", groupId, mentorId)
                getGroupInfo(false)
                GroupAnalytics.push(GroupAnalytics.Event.MEMBER_REMOVED_FROM_GROUP, groupId, mentorId)
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    dismissProgressDialog()
                    showToast("An error has occurred")
                }
                e.printStackTrace()
            }
        }
    }

    fun refreshGroupInfo() {
        viewModelScope.launch(Dispatchers.Main) {
            val item = repository.getGroupItem(groupId)
            if (item != null) {
                groupHeader.set(item.name)
                imageUrl.set(item.groupIcon ?: "")
            }
        }
    }

    fun resetUnreadAndTimeToken() {
        if (hasJoinedGroup.get()) {
            viewModelScope.launch(Dispatchers.IO) {
                repository.resetUnreadAndTimeToken(groupId)
            }
        }
    }

    fun openEmojiKeyboard(isFromKeyboard: Boolean) {
        message.what = OPEN_EMOJI_KEYBOARD
        message.data = Bundle().apply {
            putBoolean(IS_FROM_KEYBOARD, isFromKeyboard)
        }
        singleLiveEvent.value = message
    }

    fun openGroupInfo() {
        if (groupType.get() == DM_CHAT) {
            message.what = OPEN_PROFILE_DM_FPP
            singleLiveEvent.value = message
        } else {
            if (hasJoinedGroup.get()) {
                message.what = OPEN_GROUP_INFO
                singleLiveEvent.value = message
                MixPanelTracker.publishEvent(MixPanelEvent.VIEW_GROUP_INFO)
                    .addParam(ParamKeys.GROUP_ID, groupId)
                    .push()
            }
        }
    }

    fun openRequestList(view: View) {
        message.what = OPEN_GROUP_REQUESTS_LIST
        message.obj = groupId
        singleLiveEvent.value = message
        GroupAnalytics.push(GroupAnalytics.Event.OPEN_REQUESTS_LIST, groupId)
        MixPanelTracker.publishEvent(MixPanelEvent.OPEN_GROUP_REQUESTS)
            .addParam(ParamKeys.GROUP_ID, groupId)
            .push()
    }

    fun editGroupInfo() {
        MixPanelTracker.publishEvent(MixPanelEvent.EDIT_GROUP_INFO_CLICKED)
            .addParam(ParamKeys.GROUP_ID, groupId)
            .push()
        message.what = EDIT_GROUP_INFO
        message.data = Bundle().apply {
            putBoolean(IS_FROM_GROUP_INFO, true)
            putString(GROUPS_TITLE, groupHeader.get())
            putString(GROUPS_IMAGE, imageUrl.get())
            putString(GROUPS_ID, groupId)
            putString(GROUP_TYPE, groupType.get())
        }
        singleLiveEvent.value = message
    }

    fun openProfile(mentorId: String) {
        message.what = OPEN_PROFILE_PAGE
        message.obj = mentorId
        singleLiveEvent.value = message
        GroupAnalytics.push(GroupAnalytics.Event.OPENED_PROFILE)
        MixPanelTracker.publishEvent(MixPanelEvent.VIEW_PROFILE_GROUP)
            .addParam(ParamKeys.GROUP_ID, groupId)
            .addParam(ParamKeys.IS_ADMIN, adminId == Mentor.getInstance().getId())
            .push()
    }

    fun onRemoveFpp() {
        message.what = REMOVE_DM_FPP
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

    fun leaveGroup() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val request = LeaveGroupRequest(
                    groupId = groupId,
                    mentorId = Mentor.getInstance().getId()
                )
                val groupCount = repository.leaveGroupFromServer(request) ?: -1
                MixPanelTracker.publishEvent(MixPanelEvent.EXIT_GROUP)
                    .addParam(ParamKeys.GROUP_ID, groupId)
                    .addParam(ParamKeys.IS_SUCCESS, groupCount != -1)
                    .push()
                if (groupCount == -1) {
                    showToast("An error has occurred")
                    onBackPress()
                    return@launch
                }
                pushMetaMessage("${Mentor.getInstance().getUser()?.firstName} has left the group", groupId)
                GroupAnalytics.push(GroupAnalytics.Event.EXIT_GROUP, groupId)
                withContext(Dispatchers.Main) {
                    message.what = REFRESH_GRP_LIST_HIDE_INFO
                    message.data = Bundle().apply {
                        putBoolean(SHOW_NEW_INFO, groupCount != 0)
                    }
                    singleLiveEvent.value = message
                    repository.startChatEventListener()
                    dismissProgressDialog()
                    onBackPress()
                    onBackPress()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    dismissProgressDialog()
                    showToast("An error has occurred")
                }
                e.printStackTrace()
            }
        }
    }

    fun showAlertDialog(view: View, dialogMessage: String, positiveBtnText: String, loadMsg: String, function: () -> Unit) {
        val builder = AlertDialog.Builder(view.context)
        val dialog: AlertDialog = builder.setMessage(dialogMessage)
            .setPositiveButton(positiveBtnText) { dialog, id ->
                showProgressDialog("$loadMsg...")
                function.invoke()
            }
            .setNegativeButton("Cancel") { dialog, id ->
                dialog.cancel()
            }
            .create()

        dialog.show()

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).let {
            it.setTypeface(null, Typeface.BOLD)
            it.setTextColor(Color.parseColor("#107BE5"))
        }
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).let{
            it.setTypeface(null, Typeface.BOLD)
            it.setTextColor(Color.parseColor("#8D8D8D"))
        }
    }

    @ExperimentalPagingApi
    fun getChatData(): Flow<PagingData<ChatItem>> {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val startTime = getFetchStartTime(groupId).times(10000)

                if (startTime > 0 && chatService.getUnreadMessageCount(groupId, startTime) > repository.getChatCount(groupId, startTime))
                    repository.getRecentTimeToken(groupId)?.let {
                        repository.fetchUnreadMessage(startTime, groupId)
                    }
            } catch (e: Exception) {
                setFetchTimeInPref(groupId, dateStartOfDay().time)
            }
        }
        return repository.getGroupChatListResult(groupId).flow.cachedIn(viewModelScope)
    }

    private fun getFetchStartTime(groupId: String): Long {
        val timeMap = PrefManager.getPrefMap(GROUP_CHAT_CHECK_TIMES) ?: mutableMapOf()
        val lastTimeChecked = (timeMap[groupId] as Double?)?.toLong() ?: 0L

        setFetchTimeInPref(groupId)

        if (dateStartOfDay().time > lastTimeChecked) {
            return dateStartOfDay().time
        } else if (PrefManager.getLongValue(GROUP_SUBSCRIBE_TIME) > lastTimeChecked) {
            return lastTimeChecked
        }

        return -1
    }

    private fun setFetchTimeInPref(groupId: String, time: Long = System.currentTimeMillis()) {
        val timeMap = PrefManager.getPrefMap(GROUP_CHAT_CHECK_TIMES) ?: mutableMapOf()
        timeMap[groupId] = time
        PrefManager.putPrefObject(GROUP_CHAT_CHECK_TIMES, timeMap)
    }

    //TODO: Refactor loading code (if conditions)
    fun getGroupInfo(showLoading: Boolean = true) {
        if (showLoading) fetchingGrpInfo.set(true)
        viewModelScope.launch(Dispatchers.IO) {
            val memberResult = repository.getGroupMemberList(groupId, adminId)
            val onlineAndRequestCount = repository.getOnlineAndRequestCount(groupId)
            val onlineCount = (onlineAndRequestCount["online_count"] as? Double)?.toInt()
            val requestCnt = (onlineAndRequestCount["request_count"] as? Double)?.toInt()
            memberCount.set(memberResult.size)
            requestCount.set("$requestCnt")
            if (groupType.get() == DM_CHAT && onlineCount == 2)
                groupSubHeader.set("online")
            else if(groupType.get() == DM_CHAT)
                groupSubHeader.set(" ")
            else
                groupSubHeader.set("${memberCount.get()} members, $onlineCount online")
            setRequestsTab()
            withContext(Dispatchers.Main) {
                memberAdapter.addMembersToList(memberResult)
                if (showLoading) fetchingGrpInfo.set(false)
                else showToast("Removed member from the group", Toast.LENGTH_LONG)
                dismissProgressDialog()
            }
        }
    }

    fun setRequestsTab() {
        when {
            adminId != Mentor.getInstance().getId() -> showRequestsTab.set(false)
            groupType.get().equals(CLOSED_GROUP) -> showRequestsTab.set(true)
            else -> showRequestsTab.set(false)
        }
    }

    fun expandGroupList(view: View) {
        view.visibility = View.GONE
        memberAdapter.shouldShowAll(true)
        MixPanelTracker.publishEvent(MixPanelEvent.VIEW_MORE_MEMBERS)
            .addParam(ParamKeys.GROUP_ID, groupId)
            .addParam(ParamKeys.IS_ADMIN, adminId == Mentor.getInstance().getId())
            .push()
    }

    fun sendMessage(view: View) {
        if (Utils.isInternetAvailable()){
            GroupAnalytics.checkMsgTime(GroupAnalytics.Event.MESSAGE_SENT, groupId)
            MixPanelTracker.publishEvent(MixPanelEvent.GROUP_MESSAGE_SENT)
                .addParam(ParamKeys.GROUP_ID, groupId)
                .push()
            message.what = SEND_MSG
            singleLiveEvent.value = message
        }else{
            showToast("No Internet Connection")
        }
    }

    fun pushMessage(msg: String) {
        if (msg.isNotEmpty()) {
            val message = MessageItem(
                msg = msg,
                msgType = MESSAGE,
                mentorId = Mentor.getInstance().getId()
            )
            scrollToEnd = true
            viewModelScope.launch(Dispatchers.IO) {
                if (shouldSendNotification(groupId)) {
                    chatService.sendGroupNotification(groupId, getNotification(msg))
                    GroupAnalytics.push(GroupAnalytics.Event.NOTIFICATION_SENT, groupId)
                }
                if (repository.checkIfFirstMsg(groupId))
                    pushTimeMetaMessage(groupId)
                chatService.sendMessage(groupId, message)
            }
            clearText()
            resetUnreadLabel()
        }
    }

    private fun shouldSendNotification(id: String): Boolean {
        val timeMap = PrefManager.getPrefMap(GROUP_NOTIFICATION_TIMES) ?: mutableMapOf()

        return if (((timeMap[id] as Double?)?.toLong() ?: 0L) < (System.currentTimeMillis() - (1000 * 60 * 30L))) {
            timeMap[id] = System.currentTimeMillis()
            PrefManager.putPrefObject(GROUP_NOTIFICATION_TIMES, timeMap)
            true
        } else
            false
    }

    private fun clearText() {
        message.what = CLEAR_CHAT_TEXT
        singleLiveEvent.value = message
    }

    private fun getNotification(msg: String) : Map<String, Any?> {
        val pushPayloadHelper = PushPayloadHelper()

        val fcmPayload = PushPayloadHelper.FCMPayload().apply {
            setNotification(PushPayloadHelper.FCMPayload.Notification().apply {
                this.setTitle("${groupHeader.get()}")
                this.setBody("${Mentor.getInstance().getUser()?.firstName} : $msg")
            })
                .setData(
                    mapOf(
                        Pair("is_group", "true"),
                        Pair("action", "open_group_chat_client"),
                        Pair("action_data", Mentor.getInstance().getId()),
                        Pair("group_id", groupId)
                    )
                )
        }
        pushPayloadHelper.setFcmPayload(fcmPayload)

        return pushPayloadHelper.build()
    }

    fun scrollChatToEnd(view: View, unread: CircleImageView) {
        scrollToEnd = true
        view.visibility = View.GONE
        unread.visibility = View.INVISIBLE
        message.what = NEW_CHAT_ADDED
        singleLiveEvent.value = message
    }

    fun resetUnreadLabel() = viewModelScope.launch(Dispatchers.IO) {
        repository.resetUnreadLabel(groupId)
    }

    fun setUnreadLabel(count: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.setUnreadChatLabel(count, groupId)
        }
    }

    fun getGroupJoinText(status: String): String {
        return if (status == REQUESTED_GROUP)
            "REQUEST SENT"
        else if (status == NOT_JOINED_GROUP && groupType.get() == CLOSED_GROUP)
            "REQUEST TO JOIN"
        else
            "JOIN GROUP"
    }

    fun removeFpp(uId: Int) {
        try {
            viewModelScope.launch(Dispatchers.IO) {
                repository.removeUserFormFppLit(uId)
                withContext(Dispatchers.Main) {
                    message.what = REMOVE_GROUP_AND_CLOSE
                    message.obj = groupId
                    singleLiveEvent.value = message
                    repository.startChatEventListener()
                }
            }
        } catch (ex: Exception) { }
    }
}