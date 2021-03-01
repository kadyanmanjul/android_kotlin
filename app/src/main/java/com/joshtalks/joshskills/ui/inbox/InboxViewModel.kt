package com.joshtalks.joshskills.ui.inbox

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.cometchat.pro.core.AppSettings
import com.cometchat.pro.core.CometChat
import com.cometchat.pro.exceptions.CometChatException
import com.cometchat.pro.models.User
import com.joshtalks.joshskills.BuildConfig
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.core.analytics.LogException
import com.joshtalks.joshskills.core.notification.FCM_TOKEN
import com.joshtalks.joshskills.repository.local.minimalentity.InboxEntity
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.repository.local.repository.inbox.DbInboxRepository
import com.joshtalks.joshskills.repository.local.repository.inbox.InboxModelRepository
import com.joshtalks.joshskills.repository.server.UserProfileResponse
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import timber.log.Timber

class InboxViewModel(application: Application) : AndroidViewModel(application) {

    var context: JoshApplication = getApplication()
    var appDatabase = AppObjectController.appDatabase
    val apiCallStatusLiveData: MutableLiveData<ApiCallStatus> = MutableLiveData()
    val userData: MutableLiveData<UserProfileResponse> = MutableLiveData()
    val groupIdLiveData: MutableLiveData<String> = MutableLiveData()

    private val _overAllWatchTime = MutableSharedFlow<Long>(replay = 0)
    val overAllWatchTime: SharedFlow<Long>
        get() = _overAllWatchTime


    private val repository: InboxModelRepository = DbInboxRepository(
        AppObjectController.appDatabase,
        AppObjectController.chatNetworkService
    )


    private val _registerCourseNetworkData = MutableSharedFlow<List<InboxEntity>>(replay = 0)
    val registerCourseNetworkData: SharedFlow<List<InboxEntity>>
        get() = _registerCourseNetworkData

    private val _registerCourseLocalData = MutableSharedFlow<List<InboxEntity>>(replay = 0)
    val registerCourseLocalData: SharedFlow<List<InboxEntity>>
        get() = _registerCourseLocalData


    fun getRegisterCourses() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                getAllRegisterCourseMinimalFromDB()
                delay(500)
                getCourseFromServer()
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    private fun getAllRegisterCourseMinimalFromDB() = viewModelScope.launch {
        _registerCourseLocalData.emit(appDatabase.courseDao().getRegisterCourseMinimal())
    }

    private fun getCourseFromServer() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val courseListResponse =
                    AppObjectController.chatNetworkService.getRegisteredCourses()
                if (courseListResponse.isNotEmpty()) {
                    appDatabase.courseDao().insertRegisterCourses(courseListResponse).let {
                        _registerCourseNetworkData.emit(
                            appDatabase.courseDao().getRegisterCourseMinimal()
                        )
                    }
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }


    fun getProfileData(mentorId: String) {
        apiCallStatusLiveData.postValue(ApiCallStatus.START)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = AppObjectController.commonNetworkService.getUserProfileData(
                    mentorId, null,
                    USER_PROFILE_FLOW_FROM.INBOX_SCREEN.value
                )
                if (response.isSuccessful && response.body() != null) {
                    apiCallStatusLiveData.postValue(ApiCallStatus.SUCCESS)
                    response.body()?.awardCategory?.sortedBy { it.sortOrder }?.map {
                        it.awards?.sortedBy { it.sortOrder }
                    }
                    if (mentorId == Mentor.getInstance().getId()) {
                        response.body()?.colorCode?.let {
                            PrefManager.put(MY_COLOR_CODE, it, false)
                        }
                    }
                    userData.postValue(response.body()!!)
                    PrefManager.put(
                        IS_PROFILE_FEATURE_ACTIVE,
                        response.body()?.isPointsActive ?: false
                    )
                    return@launch
                } else if (response.errorBody() != null
                    && response.errorBody()!!.string().contains("mentor_id is not valid")
                ) {
                    apiCallStatusLiveData.postValue(ApiCallStatus.FAILED)
                } else {
                    apiCallStatusLiveData.postValue(ApiCallStatus.FAILED)
                }

            } catch (ex: Throwable) {
                ex.printStackTrace()
            }
            apiCallStatusLiveData.postValue(ApiCallStatus.FAILED)
        }
    }


    fun logInboxEngageEvent() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                AppObjectController.signUpNetworkService.logInboxEngageEvent(
                    mapOf(
                        "mentor" to Mentor.getInstance().getId()
                    )
                )
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    fun getTotalWatchTime() {
        viewModelScope.launch {
            _overAllWatchTime.emit(appDatabase.videoEngageDao().getOverallWatchTime() ?: 0L)
        }
    }

    fun initCometChat(groupId: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (CometChat.isInitialized().not()) {
                    // CometChat not initialized
                    val appSettings = AppSettings.AppSettingsBuilder()
                        .subscribePresenceForAllUsers()
                        .setRegion(BuildConfig.COMETCHAT_REGION)
                        .build()

                    CometChat.init(
                        context,
                        BuildConfig.COMETCHAT_APP_ID,
                        appSettings,
                        object : CometChat.CallbackListener<String>() {
                            override fun onSuccess(p0: String?) {
                                Timber.d("Initialization completed successfully")
                                loginUser(groupId)
                            }

                            override fun onError(p0: CometChatException?) {
                                Timber.d("Initialization failed with exception: %s", p0?.message)
                            }

                        })
                } else {
                    // CometChat already initialized
                    loginUser(groupId)
                }
            } catch (ex: Exception) {
                LogException.catchException(ex)
            }

        }
    }

    private fun loginUser(groupId: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            when {
                CometChat.getLoggedInUser() == null -> {
                    // User not logged in
                    try {
                        CometChat.login(
                            Mentor.getInstance().getId(),
                            BuildConfig.COMETCHAT_API_KEY,
                            object : CometChat.CallbackListener<User>() {
                                override fun onSuccess(p0: User?) {
                                    Timber.d("Login Successful : %s", p0?.toString())
                                    groupId?.let {
                                        groupIdLiveData.postValue(it)
                                    }
                                    registerFCMTokenWithCometChat()
                                }

                                override fun onError(p0: CometChatException?) {
                                    Timber.d("loginUser failed with exception: %s", p0?.message)
                                }

                            })
                    } catch (ex: Exception) {
                        ex.printStackTrace()
                    }
                }
                CometChat.getLoggedInUser().uid != Mentor.getInstance().getId() -> {
                    // Any other user is logged in. So we have to logout first
                    try {
                        CometChat.logout(object : CometChat.CallbackListener<String>() {
                            override fun onSuccess(p0: String?) {
                                loginUser(groupId)
                            }

                            override fun onError(p0: CometChatException?) {
                                Timber.d("loginUser failed with exception: %s", p0?.message)
                            }

                        })
                    } catch (ex: Exception) {
                        ex.printStackTrace()
                    }
                }
                else -> {
                    // User already logged in
                    groupId?.let {
                        groupIdLiveData.postValue(it)
                    }
                    registerFCMTokenWithCometChat()
                }
            }
        }
    }

    fun registerFCMTokenWithCometChat() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
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
            } catch (ex: Throwable) {
                Timber.d(ex)
            }
        }
    }

}