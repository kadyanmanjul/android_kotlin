package com.joshtalks.joshskills.repository.local

import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.repository.local.entity.ChatModel
import com.joshtalks.joshskills.repository.local.entity.DOWNLOAD_STATUS
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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

    fun addChat(chatModel: ChatModel) {
        CoroutineScope(Dispatchers.IO).launch {
            val cal = Calendar.getInstance()
            cal.time = Date(cal.time.time)
            chatModel.isSync = false
            chatModel.created = cal.time
            chatModel.isSeen = true
            chatModel.chatLocalId?.let {
                chatModel.chatId = it
            }
            AppObjectController.appDatabase.chatDao().insertAMessage(chatModel)
        }
    }

    @JvmStatic
    fun updateVideoDownload(objs: String, downloadStatus: DOWNLOAD_STATUS) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val chatModel =
                    AppObjectController.gsonMapperForLocal.fromJson(objs, ChatModel::class.java)
                AppObjectController.appDatabase.chatDao()
                    .updateDownloadVideoStatus(chatModel, downloadStatus)
            }catch (ex:Exception ){

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