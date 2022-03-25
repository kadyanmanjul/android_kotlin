package com.joshtalks.joshskills.ui.group.repository

import android.text.format.DateUtils
import android.util.Log
import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.joshskills.core.io.AppDirectory
import com.joshtalks.joshskills.core.showToast
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.repository.server.AmazonPolicyResponse
import com.joshtalks.joshskills.ui.group.analytics.data.local.GroupChatAnalyticsEntity
import com.joshtalks.joshskills.ui.group.constants.UNREAD_MESSAGE
import com.joshtalks.joshskills.ui.group.data.GroupChatPagingSource
import com.joshtalks.joshskills.ui.group.data.GroupPagingNetworkSource
import com.joshtalks.joshskills.ui.group.lib.*
import com.joshtalks.joshskills.ui.group.model.*
import com.joshtalks.joshskills.ui.group.utils.pushMetaMessage
import com.pubnub.api.callbacks.SubscribeCallback
import id.zelory.compressor.Compressor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONArray
import java.io.File

private const val TAG = "GroupRepository"

class GroupRepository(val onDataLoaded: ((Boolean) -> Unit)? = null) {
    // TODO: Will use dagger2 for injecting apiService
    private val apiService by lazy { AppObjectController.groupsNetworkService }
    private val analyticsService by lazy { AppObjectController.groupsAnalyticsNetworkService }
    private val mentorId = Mentor.getInstance().getId()
    private val chatService: ChatService = PubNubService
    private val database = AppObjectController.appDatabase

    fun getGroupSearchResult(query: String) =
        Pager(PagingConfig(10, enablePlaceholders = false, maxSize = 150)) {
            GroupPagingNetworkSource(
                query,
                apiService = apiService,
                onDataLoaded = onDataLoaded
            )
        }

    fun getGroupListResult(onGroupsLoaded: ((Int) -> Unit)? = null): Pager<Int, GroupsItem> {
        CoroutineScope(Dispatchers.IO).launch {
            database.groupListDao().deleteAllGroupItems()
            database.groupMemberDao().clearMemberTable()
            fetchGroupListFromNetwork()
            withContext(Dispatchers.Main) {
                onGroupsLoaded?.invoke(database.groupListDao().getGroupsCount())
            }
        }
        return Pager(PagingConfig(10, enablePlaceholders = false, maxSize = 150)) {
            database.groupListDao().getPagedGroupList()
        }
    }

    fun getGroupListLocal() = database.groupListDao().getGroupListLocal()

    @ExperimentalPagingApi
    fun getGroupChatListResult(id: String): Pager<Int, ChatItem> {
        return Pager(PagingConfig(20, enablePlaceholders = false), remoteMediator = GroupChatPagingSource(apiService, id, database)) {
            database.groupChatDao().getPagedGroupChat(id)
        }
    }

    fun subscribeNotifications() {
        CoroutineScope(Dispatchers.IO).launch {
            val groups = database.groupListDao().getGroupIds()
            chatService.dispatchNotifications(groups)
        }
    }

    fun startChatEventListener() {
        CoroutineScope(Dispatchers.IO).launch {
            val groups = database.groupListDao().getGroupIds()
            chatService.unsubscribeToChatEvents(object : ChatEventObserver<SubscribeCallback> {
                override fun getObserver(): SubscribeCallback {
                    return ChatSubscriber
                }
            })
            chatService.subscribeToChatEvents(
                groups,
                object : ChatEventObserver<SubscribeCallback> {
                    override fun getObserver(): SubscribeCallback {
                        return ChatSubscriber
                    }
                })
        }
    }

    private suspend fun fetchGroupListFromNetwork(pageInfo: PageInfo? = null) {
        Log.d(TAG, "fetchGroupList: $pageInfo")
        val pubNubResponse = chatService.fetchGroupList(pageInfo)
        val groupList = pubNubResponse?.getData()?.groups ?: listOf()
        if (groupList.isEmpty())
            return
        groupList.forEach { group ->
            group?.let {
                database.groupListDao().insertGroupItem(it)
                database.groupChatDao().insertMessage(ChatItem(
                    messageId = "unread_${it.groupId}",
                    message = "0 Unread Messages",
                    groupId = it.groupId,
                    msgType = UNREAD_MESSAGE,
                    msgTime = 0,
                    sender = null
                ))
            }
        }
        val nextPage = pubNubResponse?.getPageInfo()?.pubNubNext
        fetchGroupListFromNetwork(PageInfo(pubNubNext = nextPage))
    }

    suspend fun fetchMembersFromNetwork(groupId: String, adminId: String, pageInfo: PageInfo? = null) {
        val pubNubResponse = chatService.getGroupMemberList(groupId, pageInfo)
        val memberList = pubNubResponse?.getMemberData(groupId, adminId) ?: listOf()
        if (memberList.isEmpty())
            return

        database.groupMemberDao().insertMembers(memberList)

        val nextPage = pubNubResponse?.getPageInfo()?.pubNubNext
        fetchGroupListFromNetwork(PageInfo(pubNubNext = nextPage))
    }

    suspend fun getGroupMemberList(groupId: String, adminId: String): List<GroupMember> {
        val memberList = database.groupMemberDao().getMembersFromGroup(groupId)
        if (memberList.isNotEmpty())
            return memberList
        else
            fetchMembersFromNetwork(groupId, adminId)
        return database.groupMemberDao().getMembersFromGroup(groupId)
    }

    suspend fun getGroupOnlineCount(groupId: String): Int? {
        return try {
            val response = apiService.getGroupOnlineCount(groupId)
            return (response["online_count"] as Double).toInt()
        } catch (e: Exception) {
            showToast("An error has occurred")
            0
        }
    }

    suspend fun joinGroup(groupId: String): Boolean {
        Log.e(TAG, "Joining group : $groupId")
        val response = apiService.joinGroup(GroupRequest(mentorId = mentorId, groupId = groupId))
        if (response["success"] == true) {
            try {
                database.groupListDao().insertGroupItem(
                    GroupsItem(
                        groupIcon = response["group_icon"] as String?,
                        groupId = response["group_id"] as String,
                        createdAt = (response["created_at"] as String?)?.toLongOrNull(),
                        lastMessage = "You have joined this group",
                        lastMsgTime = System.currentTimeMillis().times(10000),
                        unreadCount = null,
                        name = response["group_name"] as String?,
                        createdBy = response["created_by"] as String?,
                        totalCalls = null,
                        adminId = response["admin_id"] as String?,
                        groupType = response["group_type"] as String?
                    )
                )
                database.groupChatDao().insertMessage(ChatItem(
                    messageId = "unread_${groupId}",
                    message = "0 Unread Messages",
                    groupId = groupId,
                    msgType = UNREAD_MESSAGE,
                    msgTime = 0,
                    sender = null
                ))
                startChatEventListener()
                val recentMessageTime = database.groupChatDao().getRecentMessageTime(groupId = response["group_id"] as String)
                recentMessageTime?.let {
                    fetchUnreadMessage(it, response["group_id"] as String)
                }
                return true
            } catch (exp: Exception) {
                Log.e(TAG, "Error: ${exp.message}")
                exp.printStackTrace()
            }
        }
        return false
    }

    suspend fun fetchUnreadMessage(startTime : Long, groupId: String) {
        val messages = chatService.getUnreadMessages(groupId, startTime = startTime)
        database.groupChatDao().insertMessages(messages)
        if(messages.isEmpty())
            return
        else {
            val recentMessageTime = database.groupChatDao().getRecentMessageTime(groupId = groupId)
            recentMessageTime?.let { fetchUnreadMessage(recentMessageTime, groupId) }
        }
    }

    suspend fun addGroupToServer(request: AddGroupRequest) {
        val url = if (request.groupIcon.isNotBlank()) {
            val compressedImagePath = getCompressImage(request.groupIcon)
            uploadCompressedMedia(compressedImagePath)
        } else
            ""
        request.groupIcon = url ?: ""
        val response = apiService.createGroup(request)
        if (response["success"] == true)
            try {
                database.groupListDao().insertGroupItem(
                    GroupsItem(
                        groupIcon = response["group_icon"] as String?,
                        groupId = response["group_id"] as String,
                        createdAt = (response["created_at"] as String?)?.toLongOrNull(),
                        lastMessage = "${response["created_by"] as String?} created this group",
                        lastMsgTime = System.currentTimeMillis().times(10000),
                        unreadCount = null,
                        name = request.groupName,
                        createdBy = response["created_by"] as String?,
                        totalCalls = null,
                        adminId = Mentor.getInstance().getId(),
                        groupType = response["group_type"] as String?
                    )
                )
                database.groupChatDao().insertMessage(ChatItem(
                    messageId = "unread_${response["group_id"] as String}",
                    message = "0 Unread Messages",
                    groupId = response["group_id"] as String,
                    msgType = UNREAD_MESSAGE,
                    msgTime = System.currentTimeMillis().times(10000),
                    sender = null
                ))
                startChatEventListener()
                pushMetaMessage("${Mentor.getInstance().getUser()?.firstName} has created this group", response["group_id"] as String)
            } catch (exp: Exception) {
                Log.e(TAG, "Error: ${exp.message}")
                exp.printStackTrace()
            }
        else
            showToast("An error has occurred")
    }

    suspend fun editGroupInServer(request: EditGroupRequest, isNameChanged: Boolean): Boolean {
        if (request.isImageChanged) {
            val url =
                if (request.groupIcon.isNotBlank()) {
                    val compressedImagePath = getCompressImage(request.groupIcon)
                    uploadCompressedMedia(compressedImagePath)
                } else ""
            request.groupIcon = url ?: ""
        }

        val response = apiService.editGroup(request)
        if (response.isSuccessful) {
            database.groupListDao().updateEditedGroup(request.groupId, request.groupName, request.groupIcon)
            if (request.isImageChanged)
                pushMetaMessage("${Mentor.getInstance().getUser()?.firstName} has changed the group icon", request.groupId)
            if (isNameChanged)
                pushMetaMessage("${Mentor.getInstance().getUser()?.firstName} has changed the group name to ${request.groupName}", request.groupId)
        }
        return response.isSuccessful
    }

    suspend fun leaveGroupFromServer(request: LeaveGroupRequest): Int? {
        val response = apiService.leaveGroup(request)
        if (response.isSuccessful) {
            leaveGroupFromLocal(request.groupId)
            return database.groupListDao().getGroupsCount()
        }
        return null
    }

    suspend fun getGroupName(groupId: String) = database.groupListDao().getGroupName(groupId)

    fun leaveGroupFromLocal(groupId: String) {
        CoroutineScope(Dispatchers.IO).launch {
            database.groupListDao().deleteGroupItem(groupId)
            database.timeTokenDao().deleteTimeToken(groupId)
            database.groupChatDao().deleteGroupMessages(groupId)
        }
    }

    suspend fun removeMemberFromGroup(request: LeaveGroupRequest): Boolean {
        val response = apiService.leaveGroup(request)
        if (response.isSuccessful)
            return true
        return false
    }

    suspend fun pushAnalyticsToServer(request: Map<String, Any?>) =
        analyticsService.groupImpressionDetails(request)

    private fun getCompressImage(path: String): String {
        return try {
            AppDirectory.copy(
                Compressor(AppObjectController.joshApplication).setQuality(75).setMaxWidth(720)
                    .setMaxHeight(
                        1280
                    ).compressToFile(File(path)).absolutePath, path
            )
            path
        } catch (ex: Exception) {
            ex.printStackTrace()
            path
        }
    }

    private suspend fun uploadCompressedMedia(mediaPath: String): String? {
        try {
            val obj = mapOf("media_path" to File(mediaPath).name)
            val responseObj =
                AppObjectController.chatNetworkService.requestUploadMediaAsync(obj).await()
            val statusCode: Int = uploadOnS3Server(responseObj, mediaPath)
            if (statusCode in 200..210) {
                val url = responseObj.url.plus(File.separator).plus(responseObj.fields["key"])
                return url
            }

        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        return null
    }

    private fun uploadOnS3Server(
        responseObj: AmazonPolicyResponse,
        mediaPath: String
    ): Int {
        val parameters = emptyMap<String, RequestBody>().toMutableMap()
        for (entry in responseObj.fields) {
            parameters[entry.key] = Utils.createPartFromString(entry.value)
        }

        val requestFile = File(mediaPath).asRequestBody("*".toMediaTypeOrNull())
        val body = MultipartBody.Part.createFormData(
            "file",
            responseObj.fields["key"],
            requestFile
        )
        val responseUpload = AppObjectController.mediaDUNetworkService.uploadMediaAsync(
            responseObj.url,
            parameters,
            body
        ).execute()
        return responseUpload.code()
    }

    suspend fun getGroupItem(groupId: String) = database.groupListDao().getGroupItem(groupId)

    suspend fun resetUnreadAndTimeToken(groupId: String) {
        database.groupListDao().resetUnreadCount(groupId)
        database.timeTokenDao().insertNewTimeToken(
            TimeTokenRequest(
                mentorId = Mentor.getInstance().getId(),
                groupId = groupId,
                timeToken = System.currentTimeMillis()
            )
        )
    }

    suspend fun removeExtraMessages() {
        val ids = database.groupListDao().getGroupIds()
        for (id in ids) database.groupChatDao().deleteOldGroupMessages(id)
    }

    suspend fun fireTimeTokenAPI() {
        val timeTokenList = database.timeTokenDao().getAllTimeTokens()
        for (token in timeTokenList) {
            val response = apiService.updateTimeToken(
                TimeTokenRequest(token.mentorId, token.groupId, token.timeToken * 10000L)
            )
            try {
                if (response.isSuccessful)
                    database.timeTokenDao().deleteTimeEntry(token.groupId, token.timeToken)
            } catch (e: Exception) {
                e.printStackTrace()
                if (response.code() == 501)
                    database.timeTokenDao().deleteTimeToken(token.groupId)
            }
        }
    }

    fun getRecentTimeToken(id: String) = database.timeTokenDao().getOpenedTime(id)?.times(10000)

    suspend fun getLastSentMsgTime(id: String): Boolean {
        val timeFromDb = database.groupsAnalyticsDao().getLastSentMsgTime(id)
        if (timeFromDb == null || !DateUtils.isToday(timeFromDb)) {
            database.groupsAnalyticsDao().setLastSentMsgTime(GroupChatAnalyticsEntity(id, System.currentTimeMillis()))
            return true
        }
        return false
    }

    suspend fun resetUnreadLabel(id: String) = database.groupChatDao().resetUnreadLabel(id)

    suspend fun setUnreadChatLabel(count: Int, id: String) {
        val time = database.groupChatDao().getUnreadLabelTime(count, id)
        if (time != null) {
            database.groupChatDao().setUnreadLabelTime("$count Unread Messages", time - 1, id)
        }
    }

    suspend fun getGroupsCount() = database.groupListDao().getGroupsCount()

    suspend fun getGroupMembersCount(): Map<String, GroupMemberCount>? {
        return try {
            val groupIds = database.groupListDao().getGroupIds()
            if (groupIds.isNotEmpty())
                apiService.getOnlineUserCount(JSONArray(groupIds))
            else
                null
        } catch (e: Exception){
            showToast("An error has occurred")
            null
        }
    }
}