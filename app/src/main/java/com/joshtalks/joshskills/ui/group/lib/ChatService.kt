package com.joshtalks.joshskills.ui.group.lib

import androidx.lifecycle.LiveData
import com.joshtalks.joshskills.core.Event
import com.joshtalks.joshskills.ui.group.model.GroupItemData
import com.joshtalks.joshskills.ui.group.model.GroupListResponse
import com.joshtalks.joshskills.ui.group.model.PageInfo

interface ChatService {
    fun initializeChatService()
    fun createGroup(groupName: String, imageUrl : String)
    fun sendMessage(msg : String)
    fun getOnlineCount(groupName : String) : LiveData<Event<Int>>
    fun getMembersCount(groupName: String) : LiveData<Event<Int>>
    fun fetchGroupList(pageInfo: PageInfo? = null) : NetworkData?
    fun getUnreadMessageCount(groupId: String, lastSeenTimestamp : Long) : Long
    fun getLastMessageDetail(groupId: String) : Pair<String, Long>
}

interface NetworkData {
    fun getData() : GroupListResponse
    fun getPageInfo() : PageInfo
}

