package com.joshtalks.joshskills.ui.group.repository

import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.abTest.ABTestCampaignData
import com.joshtalks.joshskills.util.showAppropriateMsg

private const val TAG = "ABTestRepository"

class ABTestRepository {

    private val apiService by lazy { AppObjectController.abTestNetworkService }
    private val database = AppObjectController.appDatabase.abCampaignDao()

    suspend fun getCampaignData(campaign: String): ABTestCampaignData? {

        if (database.getABTestCampaign(campaign) != null) {
            return database.getABTestCampaign(campaign)!!
        } else {
            try {
                val apiResponse = apiService.getCampaignData(campaign)
                if (apiResponse.isSuccessful && apiResponse.body() != null) {
                    database.insertCampaign(apiResponse.body()!!)
                    return database.getABTestCampaign(campaign)!!
                }
            } catch (ex: Throwable) {
                ex.showAppropriateMsg()
            }
            return null
        }
    }

    suspend fun updateAllCampaigns(list: List<String>) {
        try {
            database.deleteAllCampaigns()
            val apiResponse = apiService.getAllCampaigns(list.joinToString(","))
            if (apiResponse.isSuccessful && apiResponse.body() != null) {
                database.insertCampaigns(apiResponse.body()!!)
            }
        } catch (ex: Throwable) {
            ex.showAppropriateMsg()
        }
    }

    suspend fun getAllCampaigns(): List<ABTestCampaignData>? {
        try {
            return database.getAllABTestCampaigns()
        } catch (ex: Throwable) {
            ex.showAppropriateMsg()
        }
        return null
    }

    suspend fun postGoal(goal: String) {
        try {
            apiService.postGoalData(mapOf("goal_key" to goal))
        } catch (ex: Throwable) {
            ex.showAppropriateMsg()
        }
    }

}
