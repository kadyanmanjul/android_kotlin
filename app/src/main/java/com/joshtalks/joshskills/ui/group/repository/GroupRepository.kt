package com.joshtalks.joshskills.ui.group.repository

import android.util.Log
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.joshskills.core.io.AppDirectory
import com.joshtalks.joshskills.core.showToast
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.repository.server.AmazonPolicyResponse
import com.joshtalks.joshskills.ui.group.analytics.data.network.GroupsAnalyticsService
import com.joshtalks.joshskills.ui.group.data.GroupApiService
import com.joshtalks.joshskills.ui.group.data.GroupPagingNetworkSource
import com.joshtalks.joshskills.ui.group.model.AddGroupRequest
import com.joshtalks.joshskills.ui.group.model.JoinGroupRequest
import id.zelory.compressor.Compressor
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody

private const val TAG = "GroupRepository"
class GroupRepository(val onDataLoaded : ((Boolean) -> Unit)? = null) {
    // TODO: Will use dagger2 for injecting apiService
    private val apiService: GroupApiService = AppObjectController.retrofit.create(GroupApiService::class.java)
    private val analyticsService: GroupsAnalyticsService = AppObjectController.retrofit.create(GroupsAnalyticsService::class.java)
    private val mentorId = Mentor.getInstance().getId()

    fun getGroupSearchResult(query : String) = Pager(PagingConfig(10, enablePlaceholders = false, maxSize = 150)) {
        GroupPagingNetworkSource(query, isSearching = true, apiService = apiService, onDataLoaded = onDataLoaded)
    }

    fun getGroupListResult() = Pager(PagingConfig(10, enablePlaceholders = false, maxSize = 150)) {
        GroupPagingNetworkSource(apiService = apiService, mentorId = mentorId, onDataLoaded = onDataLoaded)
    }

    suspend fun joinGroup(groupId : String) {
        apiService.joinGroup(JoinGroupRequest(mentorId = mentorId, groupId = groupId))
    }

    suspend fun addGroupToServer(request : AddGroupRequest) {
                val url = if(request.groupIcon.isNotBlank()) {
                    val compressedImagePath = getCompressImage(request.groupIcon)
                    uploadCompressedMedia(compressedImagePath)
                } else
                    ""
                request.groupIcon = url ?: ""
                apiService.createGroup(request)
    }

    suspend fun pushAnalyticsToServer(request : Map<String, Any?>) = analyticsService.groupImpressionDetails(request)

    private fun getCompressImage(path: String): String {
        return try {
            AppDirectory.copy(
                Compressor(AppObjectController.joshApplication).setQuality(75).setMaxWidth(720).setMaxHeight(
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
    ) : String? {
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