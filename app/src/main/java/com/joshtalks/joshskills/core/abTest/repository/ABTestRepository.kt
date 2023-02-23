package com.joshtalks.joshskills.core.abTest.repository

import com.google.gson.reflect.TypeToken
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.core.abTest.ABTestCampaignData
import com.joshtalks.joshskills.core.abTest.CampaignKeys
import com.joshtalks.joshskills.core.abTest.VariantKeys
import com.joshtalks.joshskills.util.showAppropriateMsg
import org.json.JSONObject

class ABTestRepository {

    private val apiService by lazy { AppObjectController.abTestNetworkService }
    private val database = AppObjectController.appDatabase.abCampaignDao()
    var data: Map<String, Boolean> = getABTestData()

    private val setOfPostedGoals: MutableSet<String> by lazy {
        PrefManager.getStringValue(AB_TEST_GOALS_POSTED).split(",").toMutableSet()
    }

    suspend fun getCampaignData(campaign: String): ABTestCampaignData? {
        return database.getABTestCampaign(campaign)
    }

    suspend fun updateAllCampaigns() {
        try {
            database.deleteAllCampaigns()
            val prop = JSONObject()
            val apiResponse = apiService.getAllCampaigns()
            if (apiResponse.isSuccessful && apiResponse.body() != null) {
                putABTestData(apiResponse.body()!!)
                database.insertCampaigns(apiResponse.body()!!)
                for (i in apiResponse.body()!!) {
                    if (i.isCampaignActive) {
                        prop.put(i.campaignKey, i.variantKey)
                        if (i.campaignKey == "A2_C1") {
                            prop.put("A2_C1", i.variantKey)
                            PrefManager.put(
                                IS_A2_C1_RETENTION_ENABLED,
                                (i.variantKey == VariantKeys.A2_C1_RETENTION.name) && i.variableMap?.isEnabled == true
                            )
                        }
                    }
                }
            }

//            val exp = "experiment_started"
//            val obj = JSONObject()
//            MixPanelTracker.mixPanel.track("$$exp", obj)
//            MixPanelTracker.mixPanel.registerSuperProperties(prop)
//            MixPanelTracker.mixPanel.identify(PrefManager.getStringValue(USER_UNIQUE_ID))
//            MixPanelTracker.mixPanel.people.identify(PrefManager.getStringValue(USER_UNIQUE_ID))
//            MixPanelTracker.mixPanel.people.set(prop)
        } catch (ex: Throwable) {
            ex.printStackTrace()
        }
    }

    suspend fun postGoal(goal: String) {
        try {
            if (setOfPostedGoals.contains(goal)) {
                return
            }
            apiService.postGoalData(mapOf("goal_key" to goal))
            setOfPostedGoals.add(goal)
            PrefManager.put(AB_TEST_GOALS_POSTED, setOfPostedGoals.joinToString(","))
        } catch (ex: Throwable) {
            ex.showAppropriateMsg()
        }
    }

    private fun getABTestData(): Map<String, Boolean> {
        return try {
            AppObjectController.gsonMapperForLocal.fromJson(
                PrefManager.getStringValue(AB_TEST_DATA),
                object : TypeToken<Map<String, Boolean>>() {}.type
            )
        } catch (e: Exception) {
            mapOf<String, Boolean>()
        }
    }

    private fun putABTestData(data: Map<String, Boolean>) {
        PrefManager.put(AB_TEST_DATA, AppObjectController.gsonMapperForLocal.toJson(data))
    }

    fun putABTestData(campaigns: List<ABTestCampaignData>) {
        val data = mutableMapOf<String, Boolean>()
        for (campaign in campaigns) {
            if (campaign.variantKey != null) {
                data[campaign.variantKey] = campaign.variableMap?.isEnabled == true
            }
        }
        putABTestData(data)
    }

    fun isVariantActive(variantName: VariantKeys): Boolean =
        data.containsKey(variantName.name) && (data[variantName.name] == true)

    fun putCampaignData(campaignData: ABTestCampaignData) {
        if (campaignData.variantKey != null) {
            data.toMutableMap().apply {
                this[campaignData.variantKey] = campaignData.variableMap?.isEnabled == true
                putABTestData(this)
            }
        }
    }
}
