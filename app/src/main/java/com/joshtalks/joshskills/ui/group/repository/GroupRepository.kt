package com.joshtalks.joshskills.ui.group.repository

import android.util.Log
import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import com.flurry.sdk.it
import com.google.gson.Gson

import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.joshskills.core.io.AppDirectory
import com.joshtalks.joshskills.repository.local.AppDatabase
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.repository.server.AmazonPolicyResponse
import com.joshtalks.joshskills.ui.group.analytics.data.network.GroupsAnalyticsService
import com.joshtalks.joshskills.ui.group.constants.RECEIVE_META_MESSAGE_LOCAL
import com.joshtalks.joshskills.ui.group.data.GroupApiService
import com.joshtalks.joshskills.ui.group.data.GroupChatPagingSource
import com.joshtalks.joshskills.ui.group.data.GroupPagingNetworkSource
import com.joshtalks.joshskills.ui.group.lib.ChatEventObserver
import com.joshtalks.joshskills.ui.group.lib.ChatService
import com.joshtalks.joshskills.ui.group.lib.PubNubService
import com.joshtalks.joshskills.ui.group.model.*
import com.joshtalks.joshskills.ui.group.model.AddGroupRequest
import com.joshtalks.joshskills.ui.group.model.EditGroupRequest
import com.joshtalks.joshskills.ui.group.model.GroupRequest
import com.joshtalks.joshskills.ui.group.model.GroupsItem
import com.joshtalks.joshskills.ui.group.model.LeaveGroupRequest
import com.joshtalks.joshskills.ui.group.model.PageInfo
import com.joshtalks.joshskills.ui.group.model.TimeTokenRequest
import com.joshtalks.joshskills.ui.group.utils.getMessageType
import com.joshtalks.joshskills.ui.group.utils.pushMetaMessage

import com.pubnub.api.PubNub
import com.pubnub.api.callbacks.SubscribeCallback
import com.pubnub.api.models.consumer.PNStatus
import com.pubnub.api.models.consumer.objects_api.channel.PNChannelMetadataResult
import com.pubnub.api.models.consumer.objects_api.membership.PNMembershipResult
import com.pubnub.api.models.consumer.objects_api.uuid.PNUUIDMetadataResult
import com.pubnub.api.models.consumer.pubsub.PNMessageResult
import com.pubnub.api.models.consumer.pubsub.PNPresenceEventResult
import com.pubnub.api.models.consumer.pubsub.PNSignalResult
import com.pubnub.api.models.consumer.pubsub.files.PNFileEventResult
import com.pubnub.api.models.consumer.pubsub.message_actions.PNMessageActionResult
import id.zelory.compressor.Compressor
import java.io.File

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody

private const val TAG = "GroupRepository"

class GroupRepository(val onDataLoaded: ((Boolean) -> Unit)? = null) {
    // TODO: Will use dagger2 for injecting apiService
    private val apiService: GroupApiService =
        AppObjectController.retrofit.create(GroupApiService::class.java)
    private val analyticsService: GroupsAnalyticsService =
        AppObjectController.retrofit.create(GroupsAnalyticsService::class.java)
    private val mentorId = Mentor.getInstance().getId()
    private val chatService: ChatService = PubNubService
    private val database = AppObjectController.appDatabase

    object ChatSubscriber : SubscribeCallback() {
        private val database = AppObjectController.appDatabase
        private var onNewMessageAdded : (() -> Unit)? = null

        fun setNewMessageListener(onNewMessageAdded : (() -> Unit)) {
            this.onNewMessageAdded = onNewMessageAdded
        }

        override fun status(pubnub: PubNub, pnStatus: PNStatus) {
            Log.d(TAG, "status: ${pnStatus}")
        }

        override fun message(pubnub: PubNub, pnMessageResult: PNMessageResult) {
            Log.d(TAG, "message: $pnMessageResult")
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val messageItem = Gson().fromJson(pnMessageResult.message, MessageItem::class.java)
                    database.groupListDao().updateGroupItem(
                        lastMessage = "${pnMessageResult.userMetadata.asString}: ${messageItem.msg}",
                        lastMsgTime = pnMessageResult.timetoken,
                        id = pnMessageResult.channel
                    )
                    // Meta + Sender
                    database.groupChatDao().insertMessage(
                        ChatItem(
                            sender = pnMessageResult.userMetadata.asString,
                            message = messageItem.msg,
                            msgTime = pnMessageResult.timetoken,
                            groupId = pnMessageResult.channel,
                            msgType = messageItem.getMessageType(),
                            messageId = "${pnMessageResult.timetoken}_${pnMessageResult.channel}_${messageItem.mentorId}"
                        )
                    )
                    if (messageItem.getMessageType() == RECEIVE_META_MESSAGE_LOCAL && messageItem.msg.contains("changed")){
                        val message = messageItem.msg
                        when (message.contains("changed the group icon")) {
                            true -> { TODO("UPDATE IMAGE ICON") }
                            false -> {
                                val newGroupName = messageItem.msg.substring(messageItem.msg.lastIndexOf("the group name to ") + 18)
                                database.groupListDao().updateGroupName(pnMessageResult.channel, newGroupName)
                            }
                        }
                    }
                    onNewMessageAdded?.invoke()
                } catch (e : Exception) {
                    e.printStackTrace()
                }
            }
        }

        override fun presence(pubnub: PubNub, pnPresenceEventResult: PNPresenceEventResult) {
            Log.d(TAG, "presence: $pnPresenceEventResult")
        }

        override fun signal(pubnub: PubNub, pnSignalResult: PNSignalResult) {
            Log.d(TAG, "signal: $pnSignalResult")
        }

        override fun uuid(pubnub: PubNub, pnUUIDMetadataResult: PNUUIDMetadataResult) {
            Log.d(TAG, "uuid: $pnUUIDMetadataResult")
        }

        override fun channel(pubnub: PubNub, pnChannelMetadataResult: PNChannelMetadataResult) {
            Log.d(TAG, "channel: $pnChannelMetadataResult")
        }

        override fun membership(pubnub: PubNub, pnMembershipResult: PNMembershipResult) {
            Log.d(TAG, "membership: $pnMembershipResult")
        }

        override fun messageAction(
            pubnub: PubNub,
            pnMessageActionResult: PNMessageActionResult
        ) {
            Log.d(TAG, "messageAction: $pnMessageActionResult")
        }

        override fun file(pubnub: PubNub, pnFileEventResult: PNFileEventResult) {}
    }

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
            fetchGroupListFromNetwork()
            onGroupsLoaded?.invoke(database.groupListDao().getGroupsCount())
        }
        return Pager(PagingConfig(10, enablePlaceholders = false, maxSize = 150)) {
            database.groupListDao().getPagedGroupList()
        }
    }

    @ExperimentalPagingApi
    fun getGroupChatListResult(id: String): Pager<Int, ChatItem> {
        return Pager(PagingConfig(20, enablePlaceholders = false,), remoteMediator = GroupChatPagingSource(apiService, id, database)) {
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
            group?.let { database.groupListDao().insertGroupItem(it) }
        }
        val nextPage = pubNubResponse?.getPageInfo()?.pubNubNext
        Log.d(TAG, "fetchGroupList: Next Page H : ${nextPage?.hash}")
        Log.d(TAG, "fetchGroupList: Next Page ${nextPage}")
        fetchGroupListFromNetwork(PageInfo(pubNubNext = nextPage))
    }

    suspend fun joinGroup(groupId: String): Boolean {
        Log.e(TAG, "Joining group : ${groupId}")
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
                        totalCalls = null
                    )
                )
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
        val messages = chatService.getUnreadMessages(
            groupId,
            startTime = startTime
        )
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
                        totalCalls = null
                    )
                )
                startChatEventListener()
                pushMetaMessage("${Mentor.getInstance().getUser()?.firstName} has created this group", response["group_id"] as String)
            } catch (exp: Exception) {
                Log.e(TAG, "Error: ${exp.message}")
                exp.printStackTrace()
            }
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

    suspend fun leaveGroupFromServer(request: LeaveGroupRequest): Int {
        val response = apiService.leaveGroup(request)
        if (response.isSuccessful) {
            database.groupListDao().deleteGroupItem(request.groupId)
            database.timeTokenDao().deleteTimeToken(request.groupId)
            database.groupChatDao().deleteGroupMessages(request.groupId)
        }
        return database.groupListDao().getGroupsCount()
    }

    suspend fun pushAnalyticsToServer(request: Map<String, Any?>) =
        analyticsService.groupImpressionDetails(request)

    fun getGroupMemberList(groupId: String, admin: String): MemberResult =
        chatService.getChannelMembers(groupId = groupId, adminId = admin)

    fun setUserPresence(isOnline: Boolean) {
        CoroutineScope(Dispatchers.IO).launch {
            val groups = database.groupListDao().getGroupIds()
            if (groups.isNotEmpty()) chatService.setMemberPresence(groups, isOnline)
        }
    }

    suspend fun getOnlineUserCount(groupId: String) = apiService.getOnlineUserCount(groupId)

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
                Log.e(TAG, "An error has occurred")
                e.printStackTrace()
            }
        }
    }

    fun getRecentTimeToken(id: String) = database.timeTokenDao().getOpenedTime(id)?.times(10000)

    suspend fun getLastSentMsgTime(id: String): Boolean {
        if (System.currentTimeMillis() - database.groupListDao().getLastSentMsgTime(id) > 86400000) {
            database.groupListDao().setLastSentMsgTime(System.currentTimeMillis(), id)
            return true
        }
        return false
    }

    suspend fun removeUnreadMsg() = database.groupChatDao().deleteUnreadLabel()
}