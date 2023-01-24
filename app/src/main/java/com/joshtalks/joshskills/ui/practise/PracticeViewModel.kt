package com.joshtalks.joshskills.ui.practise

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.JoshApplication
import com.joshtalks.joshskills.core.abTest.ABTestCampaignData
import com.joshtalks.joshskills.repository.local.entity.practise.PointsListResponse
import com.joshtalks.joshskills.core.abTest.repository.ABTestRepository
import kotlinx.coroutines.*


class PracticeViewModel(application: Application) :
    AndroidViewModel(application) {
    var context: JoshApplication = getApplication()
    val pointsSnackBarText: MutableLiveData<PointsListResponse> = MutableLiveData()
    val abTestCampaignliveData = MutableLiveData<ABTestCampaignData?>()
    val repository: ABTestRepository by lazy { ABTestRepository() }
    fun getCampaignData(campaign: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.getCampaignData(campaign)?.let { campaign ->
                abTestCampaignliveData.postValue(campaign)
            }
        }
    }

    fun getPointsForVocabAndReading(questionId: String?, channelName: String? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response =
                    AppObjectController.chatNetworkService.getSnackBarText(questionId, channelName)
                pointsSnackBarText.postValue(response)
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    fun postGoal(s: String) {
        viewModelScope.launch(Dispatchers.IO) {
            ABTestRepository().postGoal(s)
        }
    }

}
