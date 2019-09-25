package com.joshtalks.joshskills.repository.local

import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.entity.ChatModel
import com.joshtalks.joshskills.repository.local.entity.DOWNLOAD_STATUS
import com.joshtalks.joshskills.repository.local.eventbus.VideoDownloadedBus
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
            chatModel.chatLocalId?.let {
                chatModel.chatId = it
            }
            AppObjectController.appDatabase.chatDao().insertAMessage(chatModel)
        }
    }

    fun updateVideoDownload(id: String, downloadStatus: DOWNLOAD_STATUS) {
        CoroutineScope(Dispatchers.IO).launch {
            //AppObjectController.appDatabase.chatDao().updateDownloadVideoStatus(id, downloadStatus)
            RxBus2.publish(VideoDownloadedBus(id))
        }

    }

    fun updateAllVideoStatusWhichIsDownloading() {
        CoroutineScope(Dispatchers.IO).launch {
            AppObjectController.appDatabase.chatDao().updateDownloadVideoStatusFailed()
        }

    }


}