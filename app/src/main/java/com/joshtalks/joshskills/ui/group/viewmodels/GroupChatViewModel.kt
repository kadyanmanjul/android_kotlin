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
import com.joshtalks.joshskills.ui.group.GROUPS_ID
import com.joshtalks.joshskills.ui.group.GROUPS_TITLE
import com.joshtalks.joshskills.ui.group.repository.GroupRepository
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
    lateinit var groupId: String

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

    fun openGroupInfo() {
        message.what = OPEN_GROUP_INFO
        singleLiveEvent.value = message
    }

    fun editGroupInfo() {
        message.what = OPEN_NEW_GROUP
        singleLiveEvent.value = message
    }
}