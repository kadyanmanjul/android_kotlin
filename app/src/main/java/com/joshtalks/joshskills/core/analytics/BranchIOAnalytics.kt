package com.joshtalks.joshskills.core.analytics

import com.joshtalks.joshskills.BuildConfig
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.Utils
import io.branch.referral.util.BRANCH_STANDARD_EVENT
import io.branch.referral.util.BranchEvent
import io.branch.referral.util.CurrencyType

object BranchIOAnalytics {
    fun pushToBranch(event: BRANCH_STANDARD_EVENT, extras: HashMap<String, String>? = null): Boolean {
        try {
            val branchEvent = BranchEvent(event)
            extras?.let {
                for ((k, v) in it) {
                    branchEvent.addCustomDataProperty(k, v)
                    println("$k = $v")
                }
                branchEvent.setTransactionID(extras["payment_id"])
                extras["amount"]?.toDouble()?.run {
                    branchEvent.setRevenue(this)
                }
            }
            branchEvent.addCustomDataProperty("app_version", BuildConfig.VERSION_NAME)
            branchEvent.addCustomDataProperty("device_id", Utils.getDeviceId())
            branchEvent.setCurrency(CurrencyType.INR)
            return branchEvent.logEvent(AppObjectController.joshApplication)
        }catch (ex:java.lang.Exception){
            return false
        }
    }
}