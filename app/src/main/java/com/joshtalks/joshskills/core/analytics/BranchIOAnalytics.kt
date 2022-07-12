package com.joshtalks.joshskills.core.analytics

import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.joshtalks.joshskills.BuildConfig
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.JoshSkillExecutors
import io.branch.referral.util.BRANCH_STANDARD_EVENT
import io.branch.referral.util.BranchEvent
import io.branch.referral.util.CurrencyType

object BranchIOAnalytics {
    fun pushToBranch(event: BRANCH_STANDARD_EVENT, extras: HashMap<String, String>? = null) {
        JoshSkillExecutors.BOUNDED.submit {
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
                branchEvent.setCurrency(CurrencyType.INR)
                branchEvent.logEvent(AppObjectController.joshApplication)
            } catch (ex: Exception) {
                try {
                    FirebaseCrashlytics.getInstance().recordException(ex)
                }catch (ex: Exception) {
                    ex.printStackTrace()
                }
                ex.printStackTrace()
            }
        }
    }
}