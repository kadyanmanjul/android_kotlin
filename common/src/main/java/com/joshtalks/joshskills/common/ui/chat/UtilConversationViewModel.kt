package com.joshtalks.joshskills.common.ui.chat

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.joshtalks.joshskills.common.core.AppObjectController
import com.joshtalks.joshskills.common.core.IS_PROFILE_FEATURE_ACTIVE
import com.joshtalks.joshskills.common.core.PrefManager
import com.joshtalks.joshskills.common.core.USER_PROFILE_FLOW_FROM
import com.joshtalks.joshskills.common.repository.local.entity.ChatModel
import com.joshtalks.joshskills.common.repository.local.entity.MESSAGE_STATUS
import com.joshtalks.joshskills.common.repository.local.minimalentity.InboxEntity
import com.joshtalks.joshskills.common.ui.userprofile.models.UserProfileResponse
import java.util.ConcurrentModificationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

class UtilConversationViewModel(application: Application, private var inboxEntity: InboxEntity) :
    AndroidViewModel(application) {
    private val commonNetworkService by lazy { AppObjectController.commonNetworkService }
    private val appDatabase by lazy { AppObjectController.appDatabase }
    private val jobs = arrayListOf<Job>()
    val isLoading: MutableLiveData<Boolean> = MutableLiveData()
    val unreadMessageCount = MutableSharedFlow<Int>(replay = 0)
    val userData = MutableSharedFlow<UserProfileResponse>(replay = 0)

    fun getProfileData(mentorId: String) {
        jobs += viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = commonNetworkService.getUserProfileData(
                    mentorId, null,
                    USER_PROFILE_FLOW_FROM.CONVERSATION.value
                )
                response.body()?.let { ur ->
                    ur.awardCategory?.sortedBy { it.sortOrder }?.map {
                        it.awards?.sortedBy {
                            it.sortOrder
                        }
                    }
                    PrefManager.put(
                        IS_PROFILE_FEATURE_ACTIVE,
                        response.body()?.isPointsActive ?: false
                    )
                    //delay(850)
                    userData.emit(ur)
                }
            } catch (ex: Throwable) {
                ex.printStackTrace()
            }
        }
    }

    fun updateInDatabaseReadMessage(readChatList: MutableSet<ChatModel>) {
        jobs += viewModelScope.launch(Dispatchers.IO) {
            try {
                if (readChatList.isNullOrEmpty().not()) {
                    val idList = readChatList.map { it.chatId }.toMutableList()
                    appDatabase.chatDao().updateMessageStatus(MESSAGE_STATUS.SEEN_BY_USER, idList)
                }
            } catch (ex: ConcurrentModificationException) {
                ex.printStackTrace()
            }
        }
    }
}
