package com.joshtalks.joshskills.ui.group.viewmodels

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.View

import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.lifecycle.viewModelScope
import androidx.paging.ExperimentalPagingApi
import androidx.paging.cachedIn
import androidx.recyclerview.widget.RecyclerView
import com.flurry.sdk.gr

import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.base.BaseViewModel
import com.joshtalks.joshskills.constants.*
import com.joshtalks.joshskills.core.isCallOngoing
import com.joshtalks.joshskills.core.showToast
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.ui.group.*
import com.joshtalks.joshskills.ui.group.adapters.GroupChatAdapter
import com.joshtalks.joshskills.ui.group.adapters.GroupMemberAdapter
import com.joshtalks.joshskills.ui.group.constants.MESSAGE
import com.joshtalks.joshskills.ui.group.lib.ChatService
import com.joshtalks.joshskills.ui.group.lib.PubNubService
import com.joshtalks.joshskills.ui.group.model.LeaveGroupRequest
import com.joshtalks.joshskills.ui.group.model.MessageItem
import com.joshtalks.joshskills.ui.group.repository.GroupRepository
import com.joshtalks.joshskills.ui.group.utils.GroupChatComparator
import com.joshtalks.joshskills.ui.group.utils.getMemberCount
import com.joshtalks.joshskills.ui.group.utils.pushMetaMessage
import com.pubnub.api.models.consumer.push.payload.PushPayloadHelper

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "GroupChatViewModel"

class GroupChatViewModel : BaseViewModel() {
    val repository = GroupRepository()
    val hasJoinedGroup = ObservableBoolean(false)
    var groupHeader = ObservableField("")
    val groupSubHeader = ObservableField("")
    var imageUrl = ObservableField("")
    val groupCreator = ObservableField("")
    val groupCreatedAt = ObservableField("")
    var conversationId: String = ""
    val userOnlineCount = ObservableField("")
    var memberCount = ObservableField(0)
    val memberAdapter = GroupMemberAdapter()
    var showAllMembers = ObservableBoolean(false)
    val chatAdapter = GroupChatAdapter(GroupChatComparator).apply {
        registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                Log.d(TAG, "onItemRangeInserted: ")
                super.onItemRangeInserted(positionStart, itemCount)
                if(scrollToEnd) {
                    Log.d(TAG, "onItemRangeInserted: SCROLL TO END")
                    message.what = SCROLL_TO_END
                    singleLiveEvent.value = message
                }
            }
        })
    }
    val joiningNewGroup = ObservableBoolean(false)
    var chatSendText: String = ""
    var scrollToEnd = false
    private val chatService : ChatService = PubNubService

    var groupId: String = ""
    var adminId: String = ""

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
        joiningNewGroup.set(true)
        viewModelScope.launch {
            try {
                val response = repository.joinGroup(groupId)
                if (response) {
                    withContext(Dispatchers.Main) {
                        hasJoinedGroup.set(true)
                        joiningNewGroup.set(false)
                        getOnlineUserCount()
                        message.what = REFRESH_GRP_LIST_HIDE_INFO
                        message.data = Bundle().apply {
                            putBoolean(SHOW_NEW_INFO, true)
                        }
                        singleLiveEvent.value = message
                        pushMetaMessage("${Mentor.getInstance().getUser()?.firstName} has joined this group", groupId)
                    }
                } else joiningNewGroup.set(false)
            } catch (e: Exception) {
                joiningNewGroup.set(false)
                showToast("Error joining group")
                e.printStackTrace()
            }
        }
    }

    fun getOnlineUserCount() {
        viewModelScope.launch {
            try {
                val response = repository.getOnlineUserCount(groupId)
                Log.d(TAG, "getOnlineUserCount: ${response["online_count"]}")
                userOnlineCount.set("${(response["online_count"] as Double).toInt()}")
            } catch (e: Exception) {
                showToast("Unable to get online user count")
                e.printStackTrace()
            }
        }
    }

    fun refreshGroupInfo() {
        viewModelScope.launch(Dispatchers.Main) {
            val item = repository.getGroupItem(groupId)
            if (item != null) {
                groupHeader.set(item.name)
                imageUrl.set(item.groupIcon)
            }
        }
    }

    fun resetUnreadAndTimeToken() {
        if (hasJoinedGroup.get()) {
            viewModelScope.launch(Dispatchers.Main) {
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
        joiningNewGroup.set(true)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val request = LeaveGroupRequest(
                    groupId = groupId,
                    mentorId = Mentor.getInstance().getId()
                )
                val groupCount = repository.leaveGroupFromServer(request)
                pushMetaMessage("${Mentor.getInstance().getUser()?.firstName} has left the group", groupId)
                withContext(Dispatchers.Main) {
                    message.what = REFRESH_GRP_LIST_HIDE_INFO
                    message.data = Bundle().apply {
                        putBoolean(SHOW_NEW_INFO, groupCount != 0)
                    }
                    singleLiveEvent.value = message
                    repository.startChatEventListener()
                    joiningNewGroup.set(false)
                    onBackPress()
                    onBackPress()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    joiningNewGroup.set(false)
                    showToast("An error has occurred")
                }
                e.printStackTrace()
            }
        }
    }

    fun showExitDialog(view: View) {
        val builder = AlertDialog.Builder(view.context)
        builder.setMessage("Exit \"${groupHeader.get()}\" group?")
            .setPositiveButton("Exit") { dialog, id ->
                leaveGroup()
            }
            .setNegativeButton(R.string.cancel) { dialog, id ->
                dialog.cancel()
            }

        builder.show()
    }

    @ExperimentalPagingApi
    fun getChatData() = repository.getGroupChatListResult(groupId).flow.cachedIn(viewModelScope)

    fun getGroupInfo() {
        joiningNewGroup.set(true)
        viewModelScope.launch(Dispatchers.Main) {
            val memberResult = repository.getGroupMemberList(groupId, adminId)
            memberCount.set(memberResult.count)
            memberAdapter.addMembersToList(memberResult.list)

            joiningNewGroup.set(false)
        }
    }

    fun expandGroupList(view: View) {
        view.visibility = View.GONE
        memberAdapter.shouldShowAll()
    }

    fun sendMessage(view: View) {
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
            chatService.sendMessage(groupId, message)
            chatService.sendGroupNotification(groupId, getNotification(msg))
            clearText()
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
                this.setBody(msg)
            })

        }
        pushPayloadHelper.setFcmPayload(fcmPayload)

        return pushPayloadHelper.build()
    }
}