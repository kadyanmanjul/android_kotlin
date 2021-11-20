package com.joshtalks.joshskills.ui.group.viewmodels

import android.os.Bundle
import android.util.Log
import android.view.View

import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField
import androidx.lifecycle.viewModelScope

import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.base.BaseViewModel
import com.joshtalks.joshskills.constants.*
import com.joshtalks.joshskills.core.isCallOngoing
import com.joshtalks.joshskills.core.showToast
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.ui.group.GROUPS_ID
import com.joshtalks.joshskills.ui.group.GROUPS_IMAGE
import com.joshtalks.joshskills.ui.group.GROUPS_TITLE
import com.joshtalks.joshskills.ui.group.IS_FROM_KEYBOARD
import com.joshtalks.joshskills.ui.group.adapters.GroupChatAdapter
import com.joshtalks.joshskills.ui.group.adapters.GroupMemberAdapter
import com.joshtalks.joshskills.ui.group.lib.ChatService
import com.joshtalks.joshskills.ui.group.lib.PubNubService
import com.joshtalks.joshskills.ui.group.model.GroupMember
import com.joshtalks.joshskills.ui.group.model.LeaveGroupRequest
import com.joshtalks.joshskills.ui.group.repository.GroupRepository
import com.joshtalks.joshskills.ui.group.utils.GroupChatComparator
import com.joshtalks.joshskills.ui.group.utils.getMemberCount

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "GroupChatViewModel"

class GroupChatViewModel : BaseViewModel() {
    val repository = GroupRepository()
    val hasJoinedGroup = ObservableBoolean(false)
    val groupHeader = ObservableField("")
    val groupSubHeader = ObservableField("")
    val imageUrl = ObservableField("")
    val groupCreator = ObservableField("")
    val groupCreatedAt = ObservableField("")
    var conversationId: String = ""
    val userOnlineCount = ObservableField("")
    var showAllMembers = ObservableBoolean(false)
    lateinit var memberAdapter: GroupMemberAdapter
    var chatAdapter = GroupChatAdapter(GroupChatComparator)
    var chatSendText: String = ""
    lateinit var chatService: ChatService

    var groupId: String = ""
        set(value) {
            field = value
            if (field != "")
                chatService = PubNubService.getChatService(field)
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
        viewModelScope.launch {
            try {
                repository.joinGroup(groupId)
                withContext(Dispatchers.Main) {
                    hasJoinedGroup.set(true)
                    getOnlineUserCount()
                    message.what = SHOULD_REFRESH_GROUP_LIST
                    singleLiveEvent.value = message
                }
            } catch (e: Exception) {
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

    fun openEmojiKeyboard(isFromKeyboard: Boolean) {
        message.what = OPEN_EMOJI_KEYBOARD
        message.data = Bundle().apply {
            putBoolean(IS_FROM_KEYBOARD, isFromKeyboard)
        }
        singleLiveEvent.value = message
    }

    fun openGroupInfo() {
        //TODO("This below data is just for testing, need to be removed while implementation")
        val members = listOf(
            GroupMember("3", "Sukesh", "", false, false),
            GroupMember("4", "Aaditya", "", false, true),
            GroupMember("5", "Sagar", "", false, true),
            GroupMember("6", "Param", "", false, false),
            GroupMember("7", "Mehta", "", false, true),
            GroupMember(
                "2",
                "Hi",
                "https://www.joshtalks.com/wp-content/uploads/2020/09/joshlogo.png",
                false,
                false
            ),
            GroupMember("1", "Hello", "", true, true),
            GroupMember("8", "Bye", "", false, false)
        )
        memberAdapter = GroupMemberAdapter(this, members)
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

    fun leaveGroup(view: View) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val request = LeaveGroupRequest(
                    groupId = groupId,
                    mentorId = Mentor.getInstance().getId()
                )
                repository.leaveGroupFromServer(request)
                withContext(Dispatchers.Main) {
                    message.what = SHOULD_REFRESH_GROUP_LIST
                    singleLiveEvent.value = message
                    onBackPress()
                    onBackPress()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showToast("An error has occurred")
                }
                e.printStackTrace()
            }
        }
    }

    fun expandGroupList(view: View) {
        view.visibility = View.GONE
        showAllMembers.set(true)
        memberAdapter.notifyDataSetChanged()
    }

    fun sendMessage(view: View) {
        message.what = SEND_MSG
        singleLiveEvent.value = message
    }

    fun pushMessage(msg: String) {
        chatService.sendMessage(msg)
        clearText()
    }

    private fun clearText() {
        message.what = CLEAR_CHAT_TEXT
        singleLiveEvent.value = message
    }

}