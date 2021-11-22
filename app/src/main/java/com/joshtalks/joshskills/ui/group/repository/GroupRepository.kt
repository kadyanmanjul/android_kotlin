package com.joshtalks.joshskills.ui.group.repository

import android.util.Log
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import com.flurry.sdk.it
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.joshskills.core.io.AppDirectory
import com.joshtalks.joshskills.core.showToast
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.repository.server.AmazonPolicyResponse
import com.joshtalks.joshskills.ui.group.analytics.data.network.GroupsAnalyticsService
import com.joshtalks.joshskills.ui.group.data.GroupApiService
import com.joshtalks.joshskills.ui.group.data.GroupPagingNetworkSource
import com.joshtalks.joshskills.ui.group.lib.PubNubService
import com.joshtalks.joshskills.ui.group.model.AddGroupRequest
import com.joshtalks.joshskills.ui.group.model.GroupItemData
import com.joshtalks.joshskills.ui.group.model.GroupsItem
import com.joshtalks.joshskills.ui.group.model.JoinGroupRequest
import com.joshtalks.joshskills.ui.group.model.PageInfo
import com.pubnub.api.models.consumer.PNPage
import id.zelory.compressor.Compressor
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    private val database = AppObjectController.appDatabase
    val chatService = PubNubService.getChatService()

    fun getGroupSearchResult(query: String) =
        Pager(PagingConfig(10, enablePlaceholders = false, maxSize = 150)) {
            GroupPagingNetworkSource(
                query,
                isSearching = true,
                apiService = apiService,
                onDataLoaded = onDataLoaded
            )
        }

    fun getGroupListResult(onGroupsLoaded : (() -> Unit)? = null) : Pager<Int, GroupsItem> {
        CoroutineScope(Dispatchers.IO).launch {
            fetchGroupList()
            onGroupsLoaded?.invoke()
        }
        return Pager(PagingConfig(10, enablePlaceholders = false, maxSize = 150)) {
            database.groupListDao().getPagedGroupList()
        }
    }

    private suspend fun fetchGroupList() {
        fetchGroupListFromNetwork()
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

    suspend fun joinGroup(groupId: String) {
        apiService.joinGroup(GroupRequest(mentorId = mentorId, groupId = groupId))
    }

    suspend fun addGroupToServer(request: AddGroupRequest) {
        val url = if (request.groupIcon.isNotBlank()) {
            val compressedImagePath = getCompressImage(request.groupIcon)
            uploadCompressedMedia(compressedImagePath)
        } else
            ""
        request.groupIcon = url ?: ""
        apiService.createGroup(request)
    }

    suspend fun editGroupInServer(request: EditGroupRequest) {
        val url =
            if (request.groupIcon.isNotBlank()) {
                val compressedImagePath = getCompressImage(request.groupIcon)
                uploadCompressedMedia(compressedImagePath)
            } else
                ""
        request.groupIcon = url ?: ""
        apiService.editGroup(request)
    }

    suspend fun leaveGroupFromServer(request: LeaveGroupRequest) {
        apiService.leaveGroup(request)
    }

    suspend fun pushAnalyticsToServer(request: Map<String, Any?>) =
        analyticsService.groupImpressionDetails(request)

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

    private suspend fun uploadCompressedMedia(
        mediaPath: String
    ): String? {
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
}