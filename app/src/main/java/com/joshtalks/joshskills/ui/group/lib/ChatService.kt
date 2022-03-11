package com.joshtalks.joshskills.ui.group.lib

import com.joshtalks.joshskills.ui.group.model.*

interface ChatService {
    fun <T> subscribeToChatEvents(groups : List<String>, observer: ChatEventObserver<T>)
    fun <T> unsubscribeToChatEvents(observer: ChatEventObserver<T>)
    fun sendMessage(groupName: String, messageItem: MessageItem)
    fun sendGroupNotification(groupId: String, messageItem: Map<String, Any?>)
    fun dispatchNotifications(groups : List<String>)
    fun fetchGroupList(pageInfo: PageInfo? = null) : NetworkData?
    fun getUnreadMessageCount(groupId: String, lastSeenTimestamp : Long) : Long
    fun getLastMessageDetail(groupId: String) : Pair<String, Long>
    fun getMessageHistory(groupId: String, startTime : Long? = null) : List<ChatItem>
    fun getUnreadMessages(groupId: String, startTime : Long) : List<ChatItem>
    fun getGroupMemberList(groupId: String, pageInfo: PageInfo? = null): MemberNetworkData?
    fun getPubNubOnlineMembers(groupId: String): List<String>?
}

interface NetworkData {
    fun getData() : GroupListResponse
    fun getPageInfo() : PageInfo
}

interface MemberNetworkData {
    fun getMemberData(adminId: String) : MemberResult?
    fun getPageInfo() : PageInfo
}

interface ChatEventObserver<T> {
    fun getObserver() : T
}

