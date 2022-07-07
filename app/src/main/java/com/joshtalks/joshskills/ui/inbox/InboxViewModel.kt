package com.joshtalks.joshskills.ui.inbox

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.core.abTest.ABTestCampaignData
import com.joshtalks.joshskills.core.abTest.VariantKeys
import com.joshtalks.joshskills.core.abTest.repository.ABTestRepository
import com.joshtalks.joshskills.core.analytics.LogException
import com.joshtalks.joshskills.core.analytics.MixPanelTracker
import com.joshtalks.joshskills.repository.local.minimalentity.InboxEntity
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.ui.group.repository.GroupRepository
import com.joshtalks.joshskills.ui.userprofile.models.UserProfileResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import org.json.JSONObject

class InboxViewModel(application: Application) : AndroidViewModel(application) {

    var context: JoshApplication = getApplication()
    var appDatabase = AppObjectController.appDatabase
    val apiCallStatusLiveData: MutableLiveData<ApiCallStatus> = MutableLiveData()
    val userData: MutableLiveData<UserProfileResponse> = MutableLiveData()
    val groupIdLiveData: MutableLiveData<String> = MutableLiveData()
    var increaseCoursePriceABtestLiveData = MutableLiveData<ABTestCampaignData?>()

    private val _overAllWatchTime = MutableSharedFlow<Long>(replay = 0)
    val overAllWatchTime: SharedFlow<Long>
        get() = _overAllWatchTime

    private val _registerCourseNetworkData = MutableSharedFlow<List<InboxEntity>>(replay = 0)
    val registerCourseNetworkData: SharedFlow<List<InboxEntity>>
        get() = _registerCourseNetworkData

    private val _registerCourseLocalData = MutableSharedFlow<List<InboxEntity>>(replay = 0)
    val registerCourseLocalData: SharedFlow<List<InboxEntity>>
        get() = _registerCourseLocalData

    val extendFreeTrialAbTestLiveData = MutableLiveData<ABTestCampaignData?>()

    val repository: ABTestRepository by lazy { ABTestRepository() }
    fun getEFTCampaignData(campaign: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.getCampaignData(campaign)?.let { campaign ->
                extendFreeTrialAbTestLiveData.postValue(campaign)
            }
            getRegisterCourses()
        }
    }

    fun getA2C1CampaignData(campaign: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.getCampaignData(campaign)?.let { campaign ->
                PrefManager.put(
                    IS_A2_C1_RETENTION_ENABLED,
                    (campaign.variantKey == VariantKeys.A2_C1_RETENTION.name) && campaign.variableMap?.isEnabled == true
                )
            } ?: run {
                AppObjectController.abTestNetworkService.getCampaignData(campaign).let { response ->
                    response.body()?.let { campaign ->
                        PrefManager.put(
                            IS_A2_C1_RETENTION_ENABLED,
                            (campaign.variantKey == VariantKeys.A2_C1_RETENTION.name) && campaign.variableMap?.isEnabled == true
                        )
                    }
                }
            }
        }
    }

    fun getICPABTest(campaign: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.getCampaignData(campaign)?.let { campaign ->
                    increaseCoursePriceABtestLiveData.postValue(campaign)
                } ?: run {
                    AppObjectController.abTestNetworkService.getCampaignData(campaign)
                        .let { response ->
                            increaseCoursePriceABtestLiveData.postValue(response.body())

                        }
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
                increaseCoursePriceABtestLiveData.postValue(null)
            }
        }
    }

    fun getRegisterCourses() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                getAllRegisterCourseMinimalFromDB()
                getCourseFromServer()
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    private fun getAllRegisterCourseMinimalFromDB() = viewModelScope.launch(Dispatchers.IO) {
        _registerCourseLocalData.emit(appDatabase.courseDao().getRegisterCourseMinimal())
    }

    private fun getCourseFromServer() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val courseListResponse =
                    AppObjectController.chatNetworkService.getRegisteredCourses()
                if (courseListResponse.isEmpty()) {
                    _registerCourseNetworkData.emit(emptyList())
                    return@launch
                }
                appDatabase.courseDao().insertRegisterCourses(courseListResponse).let {
                    delay(1000)
                    _registerCourseNetworkData.emit(
                        appDatabase.courseDao().getRegisterCourseMinimal()
                    )
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
                    MixPanelTracker.mixPanel.identify(PrefManager.getStringValue(USER_UNIQUE_ID))
                    MixPanelTracker.mixPanel.people.identify(
                        PrefManager.getStringValue(
                            USER_UNIQUE_ID
                        )
                    )
                    val prop = JSONObject()
                    prop.put("total points", response.body()?.points)
                    prop.put("total min spoken", response.body()?.minutesSpoken)
                    MixPanelTracker.mixPanel.people.set(prop)
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
        viewModelScope.launch(Dispatchers.IO) {
            _overAllWatchTime.emit(appDatabase.videoEngageDao().getOverallWatchTime() ?: 0L)
        }
    }

    fun handleGroupTimeTokens() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                GroupRepository().fireTimeTokenAPI()
                GroupRepository().subscribeNotifications()
            } catch (ex: Exception) {
                LogException.catchException(ex)
            }
        }
    }

    fun handleBroadCastEvents() {
        CoroutineScope(Dispatchers.IO).launch {
            for (event in appDatabase.broadcastDao().getAllEvents()) {
                try {
                    val res = AppObjectController.commonNetworkService.saveBroadcastEvent(event)
                    if (res.isSuccessful)
                        appDatabase.broadcastDao().deleteEvent(event.id)
                } catch (ex: Exception) {
                    LogException.catchException(ex)
                }
            }
        }
    }

    fun initializeMoEngageUser() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                AppObjectController.groupsNetworkService.createMoEngageUser(
                    Mentor.getInstance().getId()
                )
            } catch (ex: Exception) {
                LogException.catchException(ex)
            }
        }
    }
}