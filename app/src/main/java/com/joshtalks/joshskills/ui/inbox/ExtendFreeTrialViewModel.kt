package com.joshtalks.joshskills.ui.inbox

import android.app.Application
import android.os.Message
import androidx.databinding.ObservableBoolean
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.base.EventLiveData
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.core.abTest.CampaignKeys
import com.joshtalks.joshskills.core.abTest.GoalKeys
import com.joshtalks.joshskills.core.analytics.MixPanelTracker
import com.joshtalks.joshskills.ui.group.repository.ABTestRepository
import com.joshtalks.joshskills.util.showAppropriateMsg
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

const val variant = "Variant"
const val variable = "Variable"
const val campaign_v = "Campaign"
const val goal_v = "Goal"
const val OPEN_CONVERSATION_ACTIVITY = 0
const val OPEN_EFT_CONVERSATION_ACTIVITY = 1
class ExtendFreeTrialViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ExtendFreeTrialRepository by lazy { ExtendFreeTrialRepository() }
    private val abTestRepository: ABTestRepository by lazy { ABTestRepository() }
    val singleLiveEvent : EventLiveData = EventLiveData
    private val message = Message()
    val isProgressVisible = ObservableBoolean(false)



    fun extendFreeTrial() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                isProgressVisible.set(true)
                isProgressVisible.notifyChange()
                val response = repository.extendFreeTrial()
                if (response.isSuccessful) {
                    getCourseData()
                } else{
                    isProgressVisible.set(false)
                    isProgressVisible.notifyChange()
                    showToast(AppObjectController.joshApplication.getString(R.string.unextendable_freetrial))
                }
            }catch (ex : Exception){
                isProgressVisible.set(false)
                isProgressVisible.notifyChange()
                ex.showAppropriateMsg()
            }
        }
    }

    private fun getCourseData() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val courseListResponse =
                    repository.getCourseData()
                if (courseListResponse != null && courseListResponse.isEmpty().not()) {
                    withContext(Dispatchers.Main){
                        message.what= OPEN_EFT_CONVERSATION_ACTIVITY
                        message.obj=courseListResponse[0]
                        singleLiveEvent.value=message
                    }
                    postGoal(GoalKeys.EFT_SUCCESS.name, CampaignKeys.EXTEND_FREE_TRIAL.name)
                    PrefManager.put(COURSE_EXPIRY_TIME_IN_MS, 0L)
                    isProgressVisible.set(false)
                    isProgressVisible.notifyChange()
                } else {
                    isProgressVisible.set(false)
                    isProgressVisible.notifyChange()
                    showToast(AppObjectController.joshApplication.getString(R.string.something_went_wrong))
                }
            } catch (ex: Exception) {
                isProgressVisible.set(false)
                isProgressVisible.notifyChange()
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
                    props.put(variant, data?.variantKey ?: EMPTY)
                    props.put(variable, AppObjectController.gsonMapper.toJson(data?.variableMap))
                    props.put(campaign_v, campaign)
                    props.put(goal_v, goal)
                    MixPanelTracker().publishEvent(goal, props)
                }
            }
        }
    }
    fun openConversationActivity(){
        message.what= OPEN_CONVERSATION_ACTIVITY
        singleLiveEvent.value=message
    }
}