package com.joshtalks.joshskills.ui.chat

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.repository.local.entity.BASE_MESSAGE_TYPE
import com.joshtalks.joshskills.repository.local.entity.ChatModel
import com.joshtalks.joshskills.repository.local.minimalentity.InboxEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import java.util.*


class UnlockClassViewModel(
    application: Application,
    private var inboxEntity: InboxEntity,
) :
    AndroidViewModel(application) {
    private val chatNetworkService = AppObjectController.chatNetworkService
    private val chatDao = AppObjectController.appDatabase.chatDao()
    val batchChange = MutableSharedFlow<Boolean>(replay = 0)
    val unlockNextClass = MutableSharedFlow<Boolean>(replay = 0)

    fun canWeAddUnlockNextClass(chatId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val chatInterval = chatDao.getUpdatedChatObjectViaId(chatId).question?.interval ?: 0
            val courseDuration = inboxEntity.duration
            if (chatInterval < courseDuration) {
                val nextClassAvailable =
                    chatDao.nextQuestionIntervalExist(inboxEntity.courseId, chatInterval)
                if (nextClassAvailable == 0L) {
                    unlockNextClass.emit(true)
                    return@launch
                }
            }
            unlockNextClass.emit(false)
        }
    }

    fun insertUnlockClassToDatabase(unlockChatModel: ChatModel) {
        viewModelScope.launch(Dispatchers.IO) {
            deleteUnlockClass()
            unlockChatModel.conversationId = inboxEntity.conversation_id
            chatDao.insertAMessage(unlockChatModel)
        }
    }

    fun updateBatchChangeRequest() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = chatNetworkService.changeBatchRequest(inboxEntity.conversation_id)
                if (response.isSuccessful) {
                    deleteUnlockClass()
                    batchChange.emit(true)
                    return@launch
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
            batchChange.emit(false)
        }

    }


    private fun deleteUnlockClass() {
        viewModelScope.launch(Dispatchers.IO) {
            chatDao.deleteSpecificTypeChatModel(
                inboxEntity.conversation_id,
                BASE_MESSAGE_TYPE.UNLOCK
            )
        }
    }

}
