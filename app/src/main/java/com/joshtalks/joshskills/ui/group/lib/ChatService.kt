package com.joshtalks.joshskills.ui.group.lib

import androidx.lifecycle.LiveData
import com.joshtalks.joshskills.core.Event
import com.joshtalks.joshskills.ui.group.model.*

interface ChatService {
    fun initializeChatService()
    fun <T> subscribeToChatEvents(groups : List<String>, observer: ChatEventObserver<T>)
    fun <T> unsubscribeToChatEvents(observer: ChatEventObserver<T>)
    fun createGroup(groupName: String, imageUrl : String)
    fun sendMessage(groupName: String, messageItem: MessageItem)
    fun getOnlineCount(groupName : String) : LiveData<Event<Int>>
    fun getMembersCount(groupName: String) : LiveData<Event<Int>>
    fun fetchGroupList(pageInfo: PageInfo? = null) : NetworkData?
    fun getUnreadMessageCount(groupId: String, lastSeenTimestamp : Long) : Long
    fun getLastMessageDetail(groupId: String) : Pair<String, Long>
    fun getMessageHistory(groupId: String, timeToken : Long? = null) : List<ChatItem>
    fun getChannelMembers(groupId: String): MemberResult
    fun setMemberPresence(groups: List<String>, isOnline: Boolean)
}

interface NetworkData {
    fun getData() : GroupListResponse
    fun getPageInfo() : PageInfo
}

interface ChatEventObserver<T> {
    fun getObserver() : T
}

