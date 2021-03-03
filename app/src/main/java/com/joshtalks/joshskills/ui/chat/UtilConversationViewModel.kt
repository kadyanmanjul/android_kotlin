package com.joshtalks.joshskills.ui.chat

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.cometchat.pro.core.AppSettings
import com.cometchat.pro.core.CometChat
import com.cometchat.pro.exceptions.CometChatException
import com.cometchat.pro.models.User
import com.joshtalks.joshskills.BuildConfig
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.IS_PROFILE_FEATURE_ACTIVE
import com.joshtalks.joshskills.core.PrefManager
import com.joshtalks.joshskills.core.USER_PROFILE_FLOW_FROM
import com.joshtalks.joshskills.core.analytics.LogException.catchException
import com.joshtalks.joshskills.core.notification.FCM_TOKEN
import com.joshtalks.joshskills.repository.local.entity.ChatModel
import com.joshtalks.joshskills.repository.local.entity.MESSAGE_STATUS
import com.joshtalks.joshskills.repository.local.minimalentity.InboxEntity
import com.joshtalks.joshskills.repository.server.UserProfileResponse
import com.joshtalks.joshskills.repository.server.groupchat.GroupDetails
import java.util.ConcurrentModificationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import timber.log.Timber


class UtilConversationViewModel(application: Application, private var inboxEntity: InboxEntity) :
    AndroidViewModel(application) {
    private val commonNetworkService = AppObjectController.commonNetworkService
    private var appDatabase = AppObjectController.appDatabase
    private val jobs = arrayListOf<Job>()
    val userLoginLiveData: MutableLiveData<GroupDetails> = MutableLiveData()
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
                    delay(1500)
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

    fun initCometChat(groupDetails: GroupDetails? = null) {
        isLoading.postValue(true)
        jobs += viewModelScope.launch(Dispatchers.IO) {
            try {
                if (CometChat.isInitialized().not()) {
                    // CometChat not initialized
                    val appSettings = AppSettings.AppSettingsBuilder()
                        .subscribePresenceForAllUsers()
                        .setRegion(BuildConfig.COMETCHAT_REGION)
                        .build()

                    CometChat.init(
                        AppObjectController.joshApplication,
                        BuildConfig.COMETCHAT_APP_ID,
                        appSettings,
                        object : CometChat.CallbackListener<String>() {
                            override fun onSuccess(p0: String?) {
                                Timber.d("Initialization completed successfully")
                                if (groupDetails == null) {
                                    getGroupDetails(inboxEntity.conversation_id)
                                } else {
                                    loginUser(groupDetails)
                                }
                            }

                            override fun onError(p0: CometChatException?) {
                                Timber.e("Initialization failed with exception: %s", p0?.message)
                                isLoading.postValue(false)
                            }

                        })
                } else {
                    // CometChat already initialized
                    if (groupDetails == null) {
                        getGroupDetails(inboxEntity.conversation_id)
                    } else {
                        loginUser(groupDetails)
                    }
                }
            } catch (ex: Exception) {
                catchException(ex)
            }

        }
    }

    private fun getGroupDetails(conversationId: String) {
        jobs += viewModelScope.launch(Dispatchers.IO) {
            try {
                val params = mapOf(Pair("conversation_id", conversationId))
                val response = AppObjectController.chatNetworkService.getGroupDetails(params)
                loginUser(response)
            } catch (ex: Exception) {
                isLoading.postValue(false)
                ex.printStackTrace()
            }
        }
    }

    private fun loginUser(groupDetails: GroupDetails) {
        jobs += viewModelScope.launch(Dispatchers.IO) {
            when {
                CometChat.getLoggedInUser() == null -> {
                    // User not logged in
                    try {
                        CometChat.login(
                            groupDetails.userId,
                            BuildConfig.COMETCHAT_API_KEY,
                            object : CometChat.CallbackListener<User>() {
                                override fun onSuccess(p0: User?) {
                                    Timber.d("Login Successful : %s", p0?.toString())
                                    registerFCMTokenWithCometChat()
                                    userLoginLiveData.postValue(groupDetails)
                                    isLoading.postValue(false)
                                }

                                override fun onError(p0: CometChatException?) {
                                    Timber.d("Login failed with exception: %s", p0?.message)
                                    isLoading.postValue(false)
                                }

                            })
                    } catch (ex: Exception) {
                        ex.printStackTrace()
                    }
                }
                CometChat.getLoggedInUser().uid != groupDetails.userId -> {
                    // Any other user is logged in. So we have to logout first
                    try {
                        CometChat.logout(object : CometChat.CallbackListener<String>() {
                            override fun onSuccess(p0: String?) {
                                loginUser(groupDetails)
                            }

                            override fun onError(p0: CometChatException?) {
                                Timber.d(
                                    "Logout previous user failed with exception: %s",
                                    p0?.message
                                )
                                isLoading.postValue(false)
                            }

                        })
                    } catch (ex: Exception) {
                        ex.printStackTrace()
                    }
                }
                else -> {
                    registerFCMTokenWithCometChat()
                    userLoginLiveData.postValue(groupDetails)
                    isLoading.postValue(false)
                }
            }
        }
    }

    fun getCometChatUnreadMessageCount(conversationId: String) {
        jobs += viewModelScope.launch(Dispatchers.IO) {
            try {
                val response =
                    AppObjectController.chatNetworkService.getUnreadMessageCount(conversationId)
                val count = response.body()?.get("count")?.asInt ?: 0
                delay(1800)
                unreadMessageCount.emit(count)
            } catch (ex: Throwable) {
                Timber.d(ex)
            }
        }
    }

    fun registerFCMTokenWithCometChat() {
        val token = PrefManager.getStringValue(FCM_TOKEN)
        CometChat.registerTokenForPushNotification(
            token,
            object : CometChat.CallbackListener<String?>() {
                override fun onSuccess(s: String?) {
                    Timber.d("FCM Token $token Registered with CometChat")
                }

                override fun onError(e: CometChatException) {
                    Timber.d("Unable to register FCM Token with CometChat")
                }
            })
    }
}
