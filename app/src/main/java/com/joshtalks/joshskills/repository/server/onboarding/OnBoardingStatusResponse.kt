package com.joshtalks.joshskills.repository.server.onboarding

import com.google.gson.annotations.SerializedName
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.PrefManager
import java.util.*

data class OnBoardingStatusResponse(
    @SerializedName("version")
    var version: Version,
    @SerializedName("explore_type")
    var exploreType: String,
    @SerializedName("show_tooltip1")
    var showTooltip1: Boolean,
    @SerializedName("show_tooltip2")
    var showTooltip2: Boolean,
    @SerializedName("show_tooltip3")
    var showTooltip3: Boolean,
    @SerializedName("show_tooltip4")
    var showTooltip4: Boolean,
    @SerializedName("subscription_data")
    var subscriptionData: SubscriptionData,
    @SerializedName("free_trial_data")
    var freeTrialData: FreeTrialData
)

data class FreeTrialData(
    @SerializedName("is_7DFT_bought")
    var is7DFTBought: Boolean? = null,

    @SerializedName("start_date")
    var startDate: Date? = null,

    @SerializedName("end_date")
    var endDate: Date? = null,
    @SerializedName("remaining_days")
    var remainingDays: Int,
    @SerializedName("today")
    var today: Date
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
    var startDate: Date? = null,
    @SerializedName("end_date")
    var endDate: Date? = null,
    @SerializedName("remaining_days")
    var remainingDays: Int,
    @SerializedName("today")
    var today: Date
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