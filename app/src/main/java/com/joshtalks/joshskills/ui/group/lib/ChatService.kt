package com.joshtalks.joshskills.ui.group.lib

import com.joshtalks.joshskills.ui.group.model.*

interface ChatService {
    fun initializeChatService()
    fun <T> subscribeToChatEvents(groups : List<String>, observer: ChatEventObserver<T>)
    fun <T> unsubscribeToChatEvents(observer: ChatEventObserver<T>)
    fun createGroup(groupName: String, imageUrl : String)
    fun sendMessage(groupName: String, messageItem: MessageItem)
    fun sendGroupNotification(groupId: String, messageItem: Map<String, Any?>)
    fun dispatchNotifications(groups : List<String>)
    fun getOnlineMember(groupId: String) : Int
    fun fetchGroupList(pageInfo: PageInfo? = null) : NetworkData?
    fun getUnreadMessageCount(groupId: String, lastSeenTimestamp : Long) : Long
    fun getLastMessageDetail(groupId: String) : Pair<String, Long>
    fun getMessageHistory(groupId: String, startTime : Long? = null) : List<ChatItem>
    fun getUnreadMessages(groupId: String, startTime : Long) : List<ChatItem>
    fun getChannelMembers(groupId: String, adminId: String): MemberResult?
}

interface NetworkData {
    fun getData() : GroupListResponse
    fun getPageInfo() : PageInfo
}

interface ChatEventObserver<T> {
    fun getObserver() : T
}

