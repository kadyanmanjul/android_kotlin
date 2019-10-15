package com.joshtalks.joshskills.repository.local

import androidx.lifecycle.viewModelScope
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.io.AppDirectory
import com.joshtalks.joshskills.repository.local.entity.ChatModel
import com.joshtalks.joshskills.repository.local.entity.DOWNLOAD_STATUS
import id.zelory.compressor.Compressor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.io.File
import java.util.*


object DatabaseUtils {


    fun updateUserMessageSeen() {
        CoroutineScope(Dispatchers.IO).launch {
            val cal = Calendar.getInstance()
            cal.time = Date()
            cal.add(Calendar.HOUR, -1)
            val oneHourBack = cal.time
            AppObjectController.appDatabase.chatDao().updateSeenMessages(compareTime = oneHourBack)
        }
    }

    suspend fun addChat(chatModel: ChatModel): Long {
        return CoroutineScope(Dispatchers.IO).async {
            val cal = Calendar.getInstance()
            cal.time = Date(cal.time.time)
            chatModel.isSync = false
            chatModel.created = cal.time
            chatModel.isSeen = true
            chatModel.chatLocalId?.let {
                chatModel.chatId = it
            }
            return@async AppObjectController.appDatabase.chatDao().insertAMessage(chatModel)
        }.await()

    }

    @JvmStatic
    fun updateVideoDownload(objs: String, downloadStatus: DOWNLOAD_STATUS) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val chatModel =
                    AppObjectController.gsonMapperForLocal.fromJson(objs, ChatModel::class.java)
                AppObjectController.appDatabase.chatDao()
                    .updateDownloadVideoStatus(chatModel, downloadStatus)
            } catch (ex: Exception) {

            }
        }

    }

    @JvmStatic
    fun updateAllVideoStatusWhichIsDownloading() {
        CoroutineScope(Dispatchers.IO).launch {
            AppObjectController.appDatabase.chatDao().updateDownloadVideoStatusFailed()
        }

    }


}