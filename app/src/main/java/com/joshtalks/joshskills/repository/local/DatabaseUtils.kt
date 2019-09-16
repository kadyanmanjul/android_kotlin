package com.joshtalks.joshskills.repository.local

import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.repository.local.entity.ChatModel
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
            chatModel.isSync = false
            chatModel.created=Date()
            chatModel.chatLocalId?.let {
                chatModel.chatId=it
            }
            AppObjectController.appDatabase.chatDao().insertAMessage(chatModel)
        }
    }



}