package com.joshtalks.joshskills.core.abTest

import android.os.Bundle
import android.view.View
import androidx.lifecycle.ViewModelProvider
import com.joshtalks.joshskills.base.EventLiveData
import com.joshtalks.joshskills.core.CoreJoshFragment

abstract class ABTestFragment : CoreJoshFragment() {
    protected var liveData = EventLiveData

    protected val abTestViewModel by lazy { ViewModelProvider(requireActivity()).get(
        ABTestViewModel::class.java) }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initCampaigns()
        addABTestObserver()
    }

    protected fun addABTestObserver() {
        abTestViewModel.liveData.observe(requireActivity()){
            onReceiveABTestData(it)
        }
    }

    protected fun getCampaigns(campaign:String){
        abTestViewModel.getCampaignData(campaign)
    }

    protected fun getAllCampaigns(){
        abTestViewModel.getAllCampaigns()
    }

    protected fun postGoalData(goal:String,campaign: String?){
        abTestViewModel.postGoal(goal,campaign)
    }

    protected abstract fun onReceiveABTestData(abTestCampaignData: ABTestCampaignData?)
    protected abstract fun initCampaigns()

}
