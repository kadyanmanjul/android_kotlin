package com.joshtalks.joshskills.ui.inbox

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.joshtalks.joshcamerax.utils.SharedPrefsManager
import com.joshtalks.joshskills.core.ApiCallStatus
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.EXPLORE_TYPE
import com.joshtalks.joshskills.core.INSTANCE_ID
import com.joshtalks.joshskills.core.IS_SUBSCRIPTION_STARTED
import com.joshtalks.joshskills.core.IS_TRIAL_STARTED
import com.joshtalks.joshskills.core.JoshApplication
import com.joshtalks.joshskills.core.PrefManager
import com.joshtalks.joshskills.core.REMAINING_SUBSCRIPTION_DAYS
import com.joshtalks.joshskills.core.REMAINING_TRIAL_DAYS
import com.joshtalks.joshskills.core.SHOW_COURSE_DETAIL_TOOLTIP
import com.joshtalks.joshskills.core.SUBSCRIPTION_TEST_ID
import com.joshtalks.joshskills.core.USER_UNIQUE_ID
import com.joshtalks.joshskills.repository.local.minimalentity.InboxEntity
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.repository.server.UserProfileResponse
import com.joshtalks.joshskills.repository.server.onboarding.FreeTrialData
import com.joshtalks.joshskills.repository.server.onboarding.OnBoardingStatusResponse
import com.joshtalks.joshskills.repository.server.onboarding.VersionResponse
import com.joshtalks.joshskills.util.ReminderUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class InboxViewModel(application: Application) : AndroidViewModel(application) {

    var context: JoshApplication = getApplication()
    var appDatabase = AppObjectController.appDatabase
    val registerCourseMinimalLiveData: MutableLiveData<List<InboxEntity>> = MutableLiveData()
    val registerCourseNetworkLiveData: MutableLiveData<List<InboxEntity>> = MutableLiveData()
    val reminderApiCallStatusLiveData: MutableLiveData<ApiCallStatus> = MutableLiveData()
    val totalRemindersLiveData: MutableLiveData<Int> = MutableLiveData()
    val onBoardingLiveData: MutableLiveData<OnBoardingStatusResponse> = MutableLiveData()
    val overAllWatchTime: MutableLiveData<Long> = MutableLiveData()
    private val jobs = arrayListOf<Job>()
    val apiCallStatusLiveData: MutableLiveData<ApiCallStatus> = MutableLiveData()
    val userData: MutableLiveData<UserProfileResponse> = MutableLiveData()

    fun getProfileData(mentorId: String) {
        apiCallStatusLiveData.postValue(ApiCallStatus.START)
        jobs += viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = AppObjectController.commonNetworkService.getUserProfileData(mentorId)
                if (response.isSuccessful && response.body() != null) {
                    apiCallStatusLiveData.postValue(ApiCallStatus.SUCCESS)
                    response.body()?.awardCategory?.sortedBy { it.sortOrder }?.map {
                        it.awards?.sortedBy { it.sortOrder }
                    }
                    userData.postValue(response.body()!!)
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

    fun getRegisterCourses() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                getAllRegisterCourseMinimalFromDB()
                delay(800)
                getCourseFromServer()
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    private fun getCourseFromServer() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val courseListResponse =
                    AppObjectController.chatNetworkService.getRegisteredCourses()
                if (courseListResponse.isSuccessful) {
                    if (courseListResponse.body().isNullOrEmpty()) {
                        registerCourseNetworkLiveData.postValue(null)
                    } else {
                        appDatabase.courseDao().insertRegisterCourses(courseListResponse.body()!!)
                            .let {
                                registerCourseNetworkLiveData.postValue(
                                    appDatabase.courseDao().getRegisterCourseMinimal()
                                )
                            }
                    }
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
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

    private fun getAllRegisterCourseMinimalFromDB() = viewModelScope.launch {
        try {
            registerCourseMinimalLiveData.postValue(
                appDatabase.courseDao().getRegisterCourseMinimal()
            )
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    fun getRemindersFromServer() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = AppObjectController.commonNetworkService.getReminders(
                    Mentor.getInstance().getId()
                )
                if (response.success) {
                    appDatabase.reminderDao().insertAllReminders(response.responseData)
                    if (response.message.toIntOrNull() == 0) {
                        SharedPrefsManager.newInstance(context)
                            .putBoolean(
                                SharedPrefsManager.Companion.IS_FIRST_REMINDER,
                                true
                            )
                    } else {
                        SharedPrefsManager.newInstance(context)
                            .putBoolean(
                                SharedPrefsManager.Companion.IS_FIRST_REMINDER,
                                false
                            )
                    }
                    SharedPrefsManager.newInstance(context)
                        .putBoolean(
                            SharedPrefsManager.Companion.IS_REMINDER_SYNCED,
                            true
                        )
                    reminderApiCallStatusLiveData.postValue(ApiCallStatus.SUCCESS)
                    val reminderUtil = ReminderUtil(getApplication())
                    response.responseData.forEach { reminderItem ->
                        val timeParts = reminderItem.reminderTime.split(":")
                        val hours = timeParts[0]
                        val mins = timeParts[1]
                        reminderUtil.setAlarm(
                            when (reminderItem.reminderFrequency) {
                                ReminderUtil.Companion.ReminderFrequency.EVERYDAY.name -> ReminderUtil.Companion.ReminderFrequency.EVERYDAY
                                ReminderUtil.Companion.ReminderFrequency.WEEKDAYS.name -> ReminderUtil.Companion.ReminderFrequency.WEEKDAYS
                                else -> ReminderUtil.Companion.ReminderFrequency.WEEKENDS
                            },
                            reminderUtil.getAlarmPendingIntent(reminderItem.id),
                            hours.toIntOrNull(),
                            mins.toIntOrNull()
                        )
                    }
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    fun getTotalRemindersFromLocal() {
        viewModelScope.launch(Dispatchers.IO) {
            totalRemindersLiveData.postValue(appDatabase.reminderDao().getRemindersList().size)
        }
    }

    fun updateSubscriptionStatus() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response =
                    AppObjectController.signUpNetworkService.getOnBoardingStatus(
                        PrefManager.getStringValue(INSTANCE_ID, false),
                        Mentor.getInstance().getId(),
                        PrefManager.getStringValue(USER_UNIQUE_ID)
                    )
                if (response.isSuccessful) {
                    response.body()?.run {
                        onBoardingLiveData.postValue(this)
                        // Update Version Data in local
                        PrefManager.put(SUBSCRIPTION_TEST_ID, this.SubscriptionTestId)

                        val versionData = VersionResponse.getInstance()
                        versionData.version?.let {
                            it.name = this.version.name
                            it.id = this.version.id
                            VersionResponse.update(versionData)
                        }

                        // save Free trial data
                        FreeTrialData.update(this.freeTrialData)

                        PrefManager.put(EXPLORE_TYPE, this.exploreType)
                        PrefManager.put(
                            IS_SUBSCRIPTION_STARTED,
                            this.subscriptionData.isSubscriptionBought ?: false
                        )
                        PrefManager.put(IS_TRIAL_STARTED, this.freeTrialData.is7DFTBought ?: false)
                        PrefManager.put(REMAINING_TRIAL_DAYS, this.freeTrialData.remainingDays)
                        PrefManager.put(
                            REMAINING_SUBSCRIPTION_DAYS,
                            this.subscriptionData.remainingDays
                        )
                        PrefManager.put(SHOW_COURSE_DETAIL_TOOLTIP, this.showTooltip5)
                    }
                }
            } catch (ex: Throwable) {
                ex.printStackTrace()
            }
        }
    }

    fun getTotalWatchTime() {
        viewModelScope.launch {
            overAllWatchTime.postValue(
                AppObjectController.appDatabase.videoEngageDao().getOverallWatchTime() ?: 0
            )
        }
    }
}