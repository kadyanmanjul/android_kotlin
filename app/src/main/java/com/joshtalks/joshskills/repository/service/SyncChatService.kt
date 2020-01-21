package com.joshtalks.joshskills.repository.service

import android.content.pm.PackageManager
import androidx.lifecycle.MutableLiveData
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.joshskills.repository.local.entity.BASE_MESSAGE_TYPE
import com.joshtalks.joshskills.repository.local.entity.ChatModel
import com.joshtalks.joshskills.repository.server.AmazonPolicyResponse
import com.joshtalks.joshskills.repository.server.chat_message.*
import kotlinx.coroutines.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileNotFoundException


object SyncChatService {

    fun syncChatWithServer(refreshViewLiveData: MutableLiveData<ChatModel>? = null) {
        CoroutineScope(Dispatchers.IO).launch {

            if (Utils.isInternetAvailable()) {
                val chatModelList = AppObjectController.appDatabase.chatDao().getUnSyncMessage()

                for (chatObject in chatModelList) {
                    var url = ""

                    if ((chatObject.type == BASE_MESSAGE_TYPE.TX).not()) {
                        try {
                            if (checkReadExternalPermission() && checkWriteExternalPermission()) {
                                chatObject.downloadedLocalPath?.let { filePath ->
                                    url = filePath
                                    if (filePath.isEmpty()) {
                                        return@launch
                                    }
                                    val obj = mapOf("media_path" to File(filePath).name)

                                    val responseObj =
                                        AppObjectController.chatNetworkService.requestUploadMediaAsync(
                                            obj
                                        ).await()
                                    val statusCode: Int =
                                        uploadOnS3Server(responseObj, filePath).await()

                                    if (statusCode in 200..210) {
                                        url =
                                            responseObj.url.plus(File.separator)
                                                .plus(responseObj.fields["key"])
                                    }
                                }
                            }
                        } catch (exception: FileNotFoundException) {
                            AppObjectController.appDatabase.chatDao()
                                .forceFullySync(chatObject.chatId)
                            exception.printStackTrace()
                        } catch (exception: Exception) {
                            exception.printStackTrace()
                        }
                    }
                    val tChatMessage: BaseChatMessage


                    if (url.isEmpty()) {
                        chatObject.text?.let {
                            tChatMessage = TChatMessage(it)
                            sendTextMessage(
                                tChatMessage,
                                chatObject,
                                chatObject.conversationId,
                                refreshViewLiveData
                            )
                        }
                    } else {
                        if (chatObject.type == BASE_MESSAGE_TYPE.VI) {
                            chatObject.downloadedLocalPath?.let { path ->
                                tChatMessage = TVideoMessage(path, path)
                                (tChatMessage as BaseMediaMessage).url = url
                                sendTextMessage(
                                    tChatMessage,
                                    chatObject,
                                    chatObject.conversationId,
                                    refreshViewLiveData
                                )
                            }
                        } else if (chatObject.type == BASE_MESSAGE_TYPE.IM) {
                            chatObject.downloadedLocalPath?.let { path ->
                                tChatMessage = TImageMessage(path, path)
                                (tChatMessage as BaseMediaMessage).url = url
                                sendTextMessage(
                                    tChatMessage,
                                    chatObject,
                                    chatObject.conversationId,
                                    refreshViewLiveData
                                )
                            }
                        } else if (chatObject.type == BASE_MESSAGE_TYPE.AU) {
                            chatObject.downloadedLocalPath?.let { path ->
                                tChatMessage = TAudioMessage(path, path)
                                (tChatMessage as BaseMediaMessage).url = url
                                sendTextMessage(
                                    tChatMessage,
                                    chatObject,
                                    chatObject.conversationId,
                                    refreshViewLiveData
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun sendTextMessage(
        messageObject: BaseChatMessage,
        chatModel: ChatModel?,
        conversation_id: String, refreshViewLiveData: MutableLiveData<ChatModel>? = null
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                messageObject.conversation = conversation_id
                val responseChat =
                    AppObjectController.chatNetworkService.sendMessage(messageObject).await()
                NetworkRequestHelper.updateChat(
                    responseChat,
                    refreshViewLiveData,
                    messageObject,
                    chatModel
                )

            } catch (ex: Exception) {
                //registerCourseLiveData.postValue(null)
                ex.printStackTrace()
            }

        }
    }

    private fun uploadOnS3Server(
        responseObj: AmazonPolicyResponse,
        mediaPath: String
    ): Deferred<Int> {
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
        }
    }


    private fun checkReadExternalPermission(): Boolean {
        val permission = android.Manifest.permission.READ_EXTERNAL_STORAGE
        val res = AppObjectController.joshApplication.checkCallingOrSelfPermission(permission)
        return res == PackageManager.PERMISSION_GRANTED
    }

    private fun checkWriteExternalPermission(): Boolean {
        val permission = android.Manifest.permission.WRITE_EXTERNAL_STORAGE
        val res = AppObjectController.joshApplication.checkCallingOrSelfPermission(permission)
        return res == PackageManager.PERMISSION_GRANTED
    }

}