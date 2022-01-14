package com.joshtalks.joshskills.ui.group.viewmodels

import android.app.AlertDialog
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter

import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.lifecycle.viewModelScope
import androidx.paging.ExperimentalPagingApi
import androidx.paging.cachedIn
import androidx.recyclerview.widget.RecyclerView

import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.base.BaseViewModel
import com.joshtalks.joshskills.constants.*
import com.joshtalks.joshskills.core.isCallOngoing
import com.joshtalks.joshskills.core.showToast
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.ui.group.*
import com.joshtalks.joshskills.ui.group.adapters.GroupChatAdapter
import com.joshtalks.joshskills.ui.group.adapters.GroupMemberAdapter
import com.joshtalks.joshskills.ui.group.analytics.GroupAnalytics
import com.joshtalks.joshskills.ui.group.constants.MESSAGE
import com.joshtalks.joshskills.ui.group.lib.ChatService
import com.joshtalks.joshskills.ui.group.lib.PubNubService
import com.joshtalks.joshskills.ui.group.model.GroupMember
import com.joshtalks.joshskills.ui.group.model.LeaveGroupRequest
import com.joshtalks.joshskills.ui.group.model.MessageItem
import com.joshtalks.joshskills.ui.group.repository.GroupRepository
import com.joshtalks.joshskills.ui.group.utils.GroupChatComparator
import com.joshtalks.joshskills.ui.group.utils.getMemberCount
import com.joshtalks.joshskills.ui.group.utils.pushMetaMessage
import com.pubnub.api.models.consumer.push.payload.PushPayloadHelper
import de.hdodenhof.circleimageview.CircleImageView

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "GroupChatViewModel"

class GroupChatViewModel : BaseViewModel() {

    val repository = GroupRepository()
    val hasJoinedGroup = ObservableBoolean(false)
    var groupHeader = ObservableField("")
    var imageUrl = ObservableField("")
    val groupCreator = ObservableField("")
    val groupCreatedAt = ObservableField("")
    var memberCount = ObservableField(0)
    val memberAdapter = GroupMemberAdapter()
    val groupSubHeader = ObservableField("")
    val fetchingGrpInfo = ObservableBoolean(false)
    var scrollToEnd = false
    var unreadCount = 0
    private val chatService : ChatService = PubNubService

    var groupId: String = ""
    var adminId: String = ""
    var conversationId: String = ""
    var chatSendText: String = ""

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

    val openMemberPopup: (GroupMember, View) -> Unit = { it, view ->
        if (!it.isAdmin) {
            val builder = AlertDialog.Builder(view.context)
            val memberOptions = arrayOf("Remove ${it.memberName}")
            builder.setAdapter(ArrayAdapter(view.context, android.R.layout.simple_list_item_1, memberOptions)) { _, item ->
                when(item) {
                    0 -> {
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
        val memberText = groupSubHeader.get() ?: "0"
        val memberCount = getMemberCount(memberText)
        if (memberCount == 0) {
            showToast("Unknown Error Occurred")
            return
        } else if (memberCount == 1) {
            showToast("You are the only member, Can't Initiate a Call")
            return
        }

        message.what = OPEN_CALLING_ACTIVITY
        message.data = Bundle().apply {
            putString(GROUPS_ID, groupId)
            putString(GROUPS_TITLE, groupHeader.get())
        }
        singleLiveEvent.value = message
    }

    fun joinGroup(view: View) {
        showProgressDialog(view.context, "Joining Group...")
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

    fun showExitDialog(view: View) {
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
                getGroupInfo()
                GroupAnalytics.push(GroupAnalytics.Event.MEMBER_REMOVED_FROM_GROUP, groupId, mentorId)
                pushMetaMessage("${Mentor.getInstance().getUser()?.firstName} removed $memberName", groupId)
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
        if (hasJoinedGroup.get()) {
            message.what = OPEN_GROUP_INFO
            singleLiveEvent.value = message
        }
    }

    fun editGroupInfo() {
        message.what = EDIT_GROUP_INFO
        message.data = Bundle().apply {
            putString(GROUPS_TITLE, groupHeader.get())
            putString(GROUPS_IMAGE, imageUrl.get())
            putString(GROUPS_ID, groupId)
        }
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
                if (groupCount == -1) {
                    showToast("An error has occurred")
                    onBackPress()
                    return@launch
                }
                pushMetaMessage("${Mentor.getInstance().getUser()?.firstName} has left the group", groupId)
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
                showProgressDialog(view.context, "$loadMsg...")
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
    fun getChatData() = repository.getGroupChatListResult(groupId).flow.cachedIn(viewModelScope)

    fun getGroupInfo() {
        fetchingGrpInfo.set(true)
        viewModelScope.launch(Dispatchers.IO) {
            val memberResult = repository.getGroupMemberList(groupId, adminId)
            memberCount.set(memberResult?.memberCount)
            groupSubHeader.set("${memberCount.get()} members, ${memberResult?.onlineCount} online")
            withContext(Dispatchers.Main){
                memberAdapter.addMembersToList(memberResult?.list!!)
                fetchingGrpInfo.set(false)
                dismissProgressDialog()
            }
        }
    }

    fun expandGroupList(view: View) {
        view.visibility = View.GONE
        memberAdapter.shouldShowAll(true)
    }

    fun sendMessage(view: View) {
        GroupAnalytics.checkMsgTime(GroupAnalytics.Event.MESSAGE_SENT, groupId)
        message.what = SEND_MSG
        singleLiveEvent.value = message
    }

    fun pushMessage(msg: String) {
        if (msg.isNotEmpty()) {
            val message = MessageItem(
                msg = msg,
                msgType = MESSAGE,
                mentorId = Mentor.getInstance().getId()
            )
            scrollToEnd = true
            chatService.sendGroupNotification(groupId, getNotification(msg))
            chatService.sendMessage(groupId, message)
            clearText()
            resetUnreadLabel()
        }
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
}