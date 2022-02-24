package com.joshtalks.joshskills.core.abTest

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.joshtalks.joshskills.ui.group.repository.ABTestRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

open class ABTestViewModel(application: Application) : AndroidViewModel(application) {
    protected val jobs = arrayListOf<Job>()
    val liveData = MutableLiveData<ABTestCampaignData?>()
    val repository: ABTestRepository by lazy { ABTestRepository() }
    fun getCampaignData(campaign: String) {
        jobs += viewModelScope.launch(Dispatchers.IO) {
            repository.getCampaignData(campaign)?.let { campaign ->
                liveData.postValue(campaign)
            }
        }
    }

    fun updateAllCampaigns(list: List<String>) {
        jobs += viewModelScope.launch(Dispatchers.IO) {
            repository.updateAllCampaigns(list)
        }
    }

    fun getAllCampaigns() {
        jobs += viewModelScope.launch(Dispatchers.IO) {
            repository.getAllCampaigns()?.let {
                //todo post here all data
            }
        }
    }

    fun postGoal(goal: String) {
        jobs += viewModelScope.launch(Dispatchers.IO) {
            repository.postGoal(goal)
        }
    }

    override fun onCleared() {
        super.onCleared()
        jobs.forEach {
            try {
                it.cancel()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
