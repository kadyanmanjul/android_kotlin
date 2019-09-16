package com.joshtalks.joshskills.repository.service

import androidx.lifecycle.viewModelScope
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.joshskills.core.io.AppDirectory
import com.joshtalks.joshskills.repository.local.DatabaseUtils
import com.joshtalks.joshskills.repository.server.AmazonPolicyResponse
import com.joshtalks.joshskills.repository.server.chat_message.BaseMediaMessage
import id.zelory.compressor.Compressor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

object SyncChatService {


    fun syncChatWithServer() {
        CoroutineScope(Dispatchers.IO).launch {
            var chatModelList = AppObjectController.appDatabase.chatDao().getUnSyncMessage()
            for (chatObject in chatModelList) {
                val compressImagePath =
                    Compressor(AppObjectController.joshApplication).setQuality(75)
                        .setMaxWidth(720).setMaxHeight(1280)
                        .compressToFile(
                            File(chatObject.downloadedLocalPath),
                            chatObject.downloadedLocalPath
                        ).absolutePath

                chatObject.downloadedLocalPath = compressImagePath

                val obj = mapOf("media_path" to File(compressImagePath).name)
                val responseObj =
                    AppObjectController.chatNetworkService.requestUploadMediaAsync(obj).await()
                val statusCode: Int = uploadOnS3Server(responseObj, compressImagePath)
                if (statusCode in 200..210) {
                    val url = responseObj.url.plus(File.separator).plus(responseObj.fields["key"])
                }
            }
        }
    }

    private suspend fun uploadOnS3Server(
        responseObj: AmazonPolicyResponse,
        mediaPath: String
    ): Int {
        return CoroutineScope(Dispatchers.IO).async {
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
            return@async responseUpload.code()
        }.await()
    }
}