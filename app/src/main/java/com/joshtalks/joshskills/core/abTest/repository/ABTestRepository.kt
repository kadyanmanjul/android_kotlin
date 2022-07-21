package com.joshtalks.joshskills.core.abTest.repository

import android.util.Log
import com.google.gson.reflect.TypeToken
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.core.abTest.ABTestCampaignData
import com.joshtalks.joshskills.core.abTest.CampaignKeys
import com.joshtalks.joshskills.core.abTest.VariantKeys
import com.joshtalks.joshskills.core.analytics.MixPanelTracker
import com.joshtalks.joshskills.util.showAppropriateMsg
import org.json.JSONObject

private const val TAG = "ABTestRepository"

class ABTestRepository {

    private val apiService by lazy { AppObjectController.abTestNetworkService }
    private val database = AppObjectController.appDatabase.abCampaignDao()
    var data: Map<String, Boolean> = getABTestData()

    private val listOfCampaigns = listOf(
        CampaignKeys.SPEAKING_INTRODUCTION_VIDEO.name,
        CampaignKeys.ACTIVITY_FEED.name,
        CampaignKeys.P2P_IMAGE_SHARING.name,
        CampaignKeys.HUNDRED_POINTS.NAME,
        CampaignKeys.ENGLISH_SYLLABUS_DOWNLOAD.name,
        CampaignKeys.BUY_LAYOUT_CHANGED.name,
        CampaignKeys.WHATSAPP_REMARKETING.name,
        CampaignKeys.PEOPLE_HELP_COUNT.name,
        CampaignKeys.EXTEND_FREE_TRIAL.name,
        CampaignKeys.ACTIVITY_FEED_V2.name,
        CampaignKeys.TWENTY_MIN_TARGET.NAME,
        CampaignKeys.NEW_LANGUAGE.name,
        CampaignKeys.A2_C1.name,
        CampaignKeys.INCREASE_COURSE_PRICE.name,
        CampaignKeys.FREEMIUM_COURSE.name,
    )

    suspend fun getCampaignData(campaign: String): ABTestCampaignData? {
        return database.getABTestCampaign(campaign)
    }

    suspend fun updateAllCampaigns(list: List<String> = listOfCampaigns) {
        try {
            database.deleteAllCampaigns()
            val prop = JSONObject()
            val apiResponse = apiService.getAllCampaigns(list.joinToString(","))
            if (apiResponse.isSuccessful && apiResponse.body() != null) {
                putABTestData(apiResponse.body()!!)
                for (i in apiResponse.body()!!) {
                    Log.d(TAG, "updateAllCampaigns:freemium Boolean " + data[VariantKeys.FREEMIUM_ENABLED.name])
                    Log.d(TAG, "updateAllCampaigns: is campaign active" + i.isCampaignActive + i.campaignKey)
                    if (i.isCampaignActive)
                        Log.d(TAG, "updateAllCampaigns: kasera madarchoz")
                        prop.put(i.campaignKey, i.variantKey)
                    if (i.campaignKey == "A2_C1") {
                        if (i.isCampaignActive) {
                            prop.put("A2_C1", i.variantKey)
                            PrefManager.put(
                                IS_A2_C1_RETENTION_ENABLED,
                                (i.variantKey == VariantKeys.A2_C1_RETENTION.name) && i.variableMap?.isEnabled == true
                            )
                        }
                    }
                }
            } else {
                Log.d(TAG, "updateAllCampaigns: else")
            }

            val exp = "experiment_started"
            val obj = JSONObject()
            MixPanelTracker.mixPanel.track("$$exp", obj)
            MixPanelTracker.mixPanel.registerSuperProperties(prop)
            MixPanelTracker.mixPanel.identify(PrefManager.getStringValue(USER_UNIQUE_ID))
            MixPanelTracker.mixPanel.people.identify(PrefManager.getStringValue(USER_UNIQUE_ID))
            MixPanelTracker.mixPanel.people.set(prop)
        } catch (ex: Throwable) {
            ex.printStackTrace()
        }
    }

    suspend fun postGoal(goal: String) {
        try {
            apiService.postGoalData(mapOf("goal_key" to goal))
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
            if (campaign.isCampaignActive && campaign.variantKey != null) {
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

    suspend fun updateVariant(removeVariant: VariantKeys? = null, newVariant: VariantKeys, campaignName: CampaignKeys) {
        try {
            val response = apiService.updateVariant(
                mapOf(
                    "variant_key" to newVariant.NAME,
                    "campaign_key" to campaignName.NAME
                )
            )
            if (response.isSuccessful) {
                data.toMutableMap().also { map ->
                    removeVariant?.let { map.remove(it.NAME) }
                    map[newVariant.NAME] = true
                    putABTestData(map)
                }

            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
