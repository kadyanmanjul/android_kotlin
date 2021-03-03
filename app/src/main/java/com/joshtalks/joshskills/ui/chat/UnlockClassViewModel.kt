package com.joshtalks.joshskills.ui.chat

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.repository.local.entity.BASE_MESSAGE_TYPE
import com.joshtalks.joshskills.repository.local.entity.ChatModel
import com.joshtalks.joshskills.repository.local.minimalentity.InboxEntity
import java.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch


class UnlockClassViewModel(
    application: Application,
    private var inboxEntity: InboxEntity,
) :
    AndroidViewModel(application) {
    private val chatNetworkService = AppObjectController.chatNetworkService
    private val chatDao = AppObjectController.appDatabase.chatDao()
    private val lessonDao = AppObjectController.appDatabase.lessonDao()

    val batchChange = MutableSharedFlow<Boolean>(replay = 0)
    val unlockNextClass = MutableSharedFlow<Boolean>(replay = 0)

    fun canWeAddUnlockNextClass(chatId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val chatModel = chatDao.getChatObject(chatId)
            if (chatModel.type == BASE_MESSAGE_TYPE.LESSON) {
                val obj = lessonDao.getLessonFromChatId(chatModel.chatId)
                obj?.let {
                    val nextLessonAvailable =
                        lessonDao.nextLessonIntervalExist(inboxEntity.courseId, it.interval)
                    if (nextLessonAvailable == 0L) {
                        unlockNextClass.emit(true)
                        return@launch
                    }
                }
            } else if (chatModel.type == BASE_MESSAGE_TYPE.Q) {
                val obj = chatDao.getQuestion(chatModel.chatId)
                obj?.let {
                    val nextQuestionAvailable =
                        chatDao.nextQuestionIntervalExist(inboxEntity.courseId, it.interval)
                    if (nextQuestionAvailable == 0L) {
                        unlockNextClass.emit(true)
                        return@launch
                    }
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
            } catch (ex: Throwable) {
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
