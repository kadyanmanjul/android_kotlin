package com.joshtalks.joshskills.repository.server.onboarding

import com.google.gson.annotations.SerializedName
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.PrefManager

data class OnBoardingStatusResponse(
    @SerializedName("version")
    var version: Version,
    @SerializedName("explore_type")
    var exploreType: String,

    @SerializedName("subscription_data")
    var subscriptionData: SubscriptionData,

    @SerializedName("free_trial_data")
    var freeTrialData: FreeTrialData
)

data class FreeTrialData(
    @SerializedName("is_7DFT_bought")
    var is7DFTBought: Boolean? = null,

    @SerializedName("start_date")
    var startDate: String? = null,

    @SerializedName("end_date")
    var endDate: String? = null
) {

    override fun toString(): String {
        return AppObjectController.gsonMapper.toJson(this)
    }

    companion object {
        private const val FREE_TRIEAL_DATA_MAP_OBJECT = "free_trial_map_object"

        @JvmStatic
        fun getMapObject(): FreeTrialData? {
            return try {
                AppObjectController.gsonMapper.fromJson(
                    PrefManager.getStringValue(FREE_TRIEAL_DATA_MAP_OBJECT),
                    FreeTrialData::class.java
                )
            } catch (ex: Exception) {
                ex.printStackTrace()
                null
            }
        }

        fun update(obj: FreeTrialData) {
            PrefManager.put(FREE_TRIEAL_DATA_MAP_OBJECT, AppObjectController.gsonMapper.toJson(obj))
        }
    }
}

data class SubscriptionData(
    @SerializedName("is_subscription_bought")
    var isSubscriptionBought: Boolean? = null,
    @SerializedName("start_date")
    var startDate: String? = null,
    @SerializedName("end_date")
    var endDate: String? = null
) {

    companion object {
        private const val SUBSCRIPTION_DATA_MAP_OBJECT = "subscription_map_object"

        @JvmStatic
        fun getMapObject(): SubscriptionData? {
            return try {
                AppObjectController.gsonMapper.fromJson(
                    PrefManager.getStringValue(SUBSCRIPTION_DATA_MAP_OBJECT),
                    SubscriptionData::class.java
                )
            } catch (ex: Exception) {
                ex.printStackTrace()
                null
            }
        }

        fun update(obj: SubscriptionData) {
            PrefManager.put(
                SUBSCRIPTION_DATA_MAP_OBJECT,
                AppObjectController.gsonMapper.toJson(obj)
            )
        }
    }
}