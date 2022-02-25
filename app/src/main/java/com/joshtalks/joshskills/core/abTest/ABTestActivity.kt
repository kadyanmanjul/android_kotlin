package com.joshtalks.joshskills.core.abTest

import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import com.joshtalks.joshskills.base.EventLiveData
import com.joshtalks.joshskills.core.CoreJoshActivity

abstract class ABTestActivity : CoreJoshActivity() {
    protected var liveData = EventLiveData

    protected val abTestViewModel by lazy { ViewModelProvider(this).get(
        ABTestViewModel::class.java) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initCampaigns()
        addABTestObserver()
    }

    protected fun addABTestObserver() {
        abTestViewModel.liveData.observe(this){
            onReceiveABTestData(it)
        }
    }

    protected fun getCampaigns(campaign:String){
        abTestViewModel.getCampaignData(campaign)
    }

    protected fun getAllCampaigns(){
        abTestViewModel.getAllCampaigns()
    }

    protected fun updateAllCampaigns(list: List<String>){
        abTestViewModel.updateAllCampaigns(list)
    }

    protected fun postGoalData(goal:String){
        abTestViewModel.postGoal(goal)
    }

    protected abstract fun onReceiveABTestData(abTestCampaignData: ABTestCampaignData?)
    protected abstract fun initCampaigns()

}
