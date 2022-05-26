package com.joshtalks.joshskills.core.abTest.repository

import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.IS_A2_C1_RETENTION_ENABLED
import com.joshtalks.joshskills.core.PrefManager
import com.joshtalks.joshskills.core.USER_UNIQUE_ID
import com.joshtalks.joshskills.core.abTest.ABTestCampaignData
import com.joshtalks.joshskills.core.abTest.VariantKeys
import com.joshtalks.joshskills.core.analytics.MixPanelTracker
import com.joshtalks.joshskills.util.showAppropriateMsg
import org.json.JSONObject

private const val TAG = "ABTestRepository"

class ABTestRepository {

    private val apiService by lazy { AppObjectController.abTestNetworkService }
    private val database = AppObjectController.appDatabase.abCampaignDao()

    suspend fun getCampaignData(campaign: String): ABTestCampaignData? {

        return database.getABTestCampaign(campaign)
    }

    suspend fun updateAllCampaigns(list: List<String>) {
        try {
            database.deleteAllCampaigns()
            val prop = JSONObject()
            val apiResponse = apiService.getAllCampaigns(list.joinToString(","))
            if (apiResponse.isSuccessful && apiResponse.body() != null) {
                database.insertCampaigns(apiResponse.body()!!)

                for (i in apiResponse.body()!!) {
                    when (i.campaignKey) {
                        "SPEAKING_INTRODUCTION_VIDEO" -> {
                            if (i.isCampaignActive) {
                                prop.put("SPEAKING_INTRODUCTION_VIDEO", i.variantKey)
                            }
                        }
                        "ENGLISH_SYLLABUS_DOWNLOAD" -> {
                            if (i.isCampaignActive) {
                                prop.put("ENGLISH_SYLLABUS_DOWNLOAD", i.variantKey)
                            }
                        }
                        "ACTIVITY_FEED" -> {
                            if (i.isCampaignActive) {
                                prop.put("ACTIVITY_FEED", i.variantKey)
                            }
                        }
                        "P2P_IMAGE_SHARING" -> {
                            if (i.isCampaignActive) {
                                prop.put("P2P_IMAGE_SHARING", i.variantKey)
                            }
                        }
                        "100_POINTS" -> {
                            if (i.isCampaignActive) {
                                prop.put("100_POINTS", i.variantKey)
                            }
                        }
                        "BUY_LAYOUT_CHANGED" -> {
                            if (i.isCampaignActive) {
                                prop.put("BUY_LAYOUT_CHANGED", i.variantKey)
                            }
                        }
                        "WHATSAPP_REMARKETING" -> {
                            if (i.isCampaignActive) {
                                prop.put("WHATSAPP_REMARKETING", i.variantKey)
                            }
                        }
                        "PEOPLE_HELP_COUNT" -> {
                            if (i.isCampaignActive) {
                                prop.put("PEOPLE_HELP_COUNT", i.variantKey)
                            }
                        }
                        "EXTEND_FREE_TRIAL" -> {
                            if (i.isCampaignActive) {
                                prop.put("EXTEND_FREE_TRIAL", i.variantKey)
                            }
                        }
                        "ACTIVITY_FEED_V2" -> {
                            if (i.isCampaignActive) {
                                prop.put("ACTIVITY_FEED_V2", i.variantKey)
                            }
                        }
                        "20_MIN_TARGET" -> {
                            if (i.isCampaignActive) {
                                prop.put("20_MIN_TARGET", i.variantKey)
                            }
                        }
                        "A2_C1" -> {
                            if (i.isCampaignActive) {
                                prop.put("A2_C1", i.variantKey)
                                PrefManager.put(
                                    IS_A2_C1_RETENTION_ENABLED,
                                    (i.variantKey == VariantKeys.A2_C1_RETENTION.name) && i.variableMap?.isEnabled == true
                                )
                            }
                        }
                    }
                }

                val exp = "experiment_started"
                val obj = JSONObject()
                MixPanelTracker.mixPanel.track("$$exp", obj)
                MixPanelTracker.mixPanel.registerSuperProperties(prop)
                MixPanelTracker.mixPanel.identify(PrefManager.getStringValue(USER_UNIQUE_ID))
                MixPanelTracker.mixPanel.people.identify(PrefManager.getStringValue(USER_UNIQUE_ID))
                MixPanelTracker.mixPanel.people.set(prop)
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
