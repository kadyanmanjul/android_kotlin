package com.joshtalks.joshskills.common.ui.group.lib

import com.joshtalks.joshskills.common.ui.group.model.*
import com.pubnub.api.models.consumer.objects_api.uuid.PNGetUUIDMetadataResult

interface ChatService {
    fun initializeChatService()
    fun <T> subscribeToChatEvents(groups : List<String>, observer: ChatEventObserver<T>)
    fun <T> unsubscribeToChatEvents(observer: ChatEventObserver<T>)
    fun sendMessage(groupName: String, messageItem: MessageItem, isBackend: Boolean = false)
    fun sendGroupNotification(groupId: String, messageItem: Map<String, Any?>)
    fun dispatchNotifications(groups: List<String>)
    fun removeNotifications(groups: List<String>)
    fun cancelAllPubNubNotifications()
    fun fetchGroupList(pageInfo: PageInfo? = null) : NetworkData?
    fun getUnreadMessageCount(groupId: String, lastSeenTimestamp : Long) : Long
    fun getLastMessageDetail(groupId: String,groupType:String) : Pair<String, Long>
    fun getMessageHistory(groupId: String, startTime : Long? = null) : List<ChatItem>
    fun getUnreadMessages(groupId: String, startTime : Long) : List<ChatItem>
    fun getGroupMemberList(groupId: String, pageInfo: PageInfo? = null): MemberNetworkData?
    fun getPubNubOnlineMembers(groupId: String): List<String>?
    fun getUserMetadata(mentorId: String): PNGetUUIDMetadataResult?
}

interface NetworkData {
    fun getData() : GroupListResponse
    fun getPageInfo() : PageInfo
}

interface MemberNetworkData {
    fun getMemberData(groupId: String, adminId: String) : List<GroupMember>?
    fun getPageInfo() : PageInfo
}

interface ChatEventObserver<T> {
    fun getObserver() : T
}

