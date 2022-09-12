package com.joshtalks.joshskills.ui.chat

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.repository.local.entity.BASE_MESSAGE_TYPE
import com.joshtalks.joshskills.repository.local.minimalentity.InboxEntity
import com.joshtalks.joshskills.repository.server.PurchaseDataResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

const val BATCH_CHANGE_FAILURE = -1
const val BATCH_CHANGE_BUY_FIRST = 0
const val BATCH_CHANGE_SUCCESS = 1

class UnlockClassViewModel(
    application: Application,
    private var inboxEntity: InboxEntity,
) :
    AndroidViewModel(application) {
    private val chatNetworkService = AppObjectController.chatNetworkService
    private val chatDao = AppObjectController.appDatabase.chatDao()
    private val lessonDao = AppObjectController.appDatabase.lessonDao()

    val batchChange = MutableSharedFlow<Int>(replay = 0)
    val unlockNextClass = MutableSharedFlow<Boolean>(replay = 0)
    val purchaseDialogData = MutableSharedFlow<PurchaseDataResponse?>()

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

    fun updateBatchChangeRequest() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = chatNetworkService.changeBatchRequest(inboxEntity.conversation_id)
                if (response.code() == 405) {
                    batchChange.emit(BATCH_CHANGE_BUY_FIRST)
                    return@launch
                } else if (response.isSuccessful) {
                    batchChange.emit(BATCH_CHANGE_SUCCESS)
                    return@launch
                }
            } catch (ex: Throwable) {
                ex.printStackTrace()
            }
            batchChange.emit(BATCH_CHANGE_FAILURE)
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
