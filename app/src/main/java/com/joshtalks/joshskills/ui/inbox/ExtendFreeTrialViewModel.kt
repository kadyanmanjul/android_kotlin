package com.joshtalks.joshskills.ui.inbox

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.core.abTest.CampaignKeys
import com.joshtalks.joshskills.core.abTest.GoalKeys
import com.joshtalks.joshskills.core.analytics.MixPanelTracker
import com.joshtalks.joshskills.repository.local.minimalentity.InboxEntity
import com.joshtalks.joshskills.ui.group.repository.ABTestRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject


class ExtendFreeTrialViewModel(application: Application) : AndroidViewModel(application) {
    var extendedFreeTrialCourseNetworkData: MutableLiveData<List<InboxEntity>> = MutableLiveData()
    var isDataObtainedProcessRunninng: MutableLiveData<Boolean> = MutableLiveData()

    val repository: ExtendFreeTrialRepository by lazy { ExtendFreeTrialRepository() }
    val abTestRepository: ABTestRepository by lazy { ABTestRepository() }


    fun extendFreeTrial() {
        viewModelScope.launch(Dispatchers.IO) {
            isDataObtainedProcessRunninng.postValue(true)
            val response = repository.extendFreeTrial()
            if (response != null && response) {
                getCourseData()
            } else if (response != null && !response) {
                isDataObtainedProcessRunninng.postValue(false)
                showToast("Free Trial can't be extended")
            } else {
                isDataObtainedProcessRunninng.postValue(false)
            }
        }
    }

    private fun getCourseData() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val courseListResponse =
                    repository.getCourseData()
                if (courseListResponse != null && courseListResponse.isEmpty().not()) {
                    extendedFreeTrialCourseNetworkData.postValue(courseListResponse)
                    postGoal(GoalKeys.EFT_SUCCESS.name, CampaignKeys.EXTEND_FREE_TRIAL.name)
                    PrefManager.put(COURSE_EXPIRY_TIME_IN_MS, 0L)
                    isDataObtainedProcessRunninng.postValue(false)
                } else {
                    isDataObtainedProcessRunninng.postValue(false)
                    showToast("Something Went Wrong")
                }
            } catch (ex: Exception) {
                isDataObtainedProcessRunninng.postValue(false)
                ex.printStackTrace()
            }
        }
    }

    fun postGoal(goal: String, campaign: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            abTestRepository.postGoal(goal)
            if (campaign != null) {
                val data = ABTestRepository().getCampaignData(campaign)
                data?.let {
                    val props = JSONObject()
                    props.put("Variant", data?.variantKey ?: EMPTY)
                    props.put("Variable", AppObjectController.gsonMapper.toJson(data?.variableMap))
                    props.put("Campaign", campaign)
                    props.put("Goal", goal)
                    MixPanelTracker().publishEvent(goal, props)
                }
            }
        }
    }

}