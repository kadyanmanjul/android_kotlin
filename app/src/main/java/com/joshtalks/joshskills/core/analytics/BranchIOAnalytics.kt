package com.joshtalks.joshskills.core.analytics

import com.crashlytics.android.Crashlytics
import com.joshtalks.joshskills.core.AppObjectController
import io.branch.referral.util.BRANCH_STANDARD_EVENT
import io.branch.referral.util.BranchEvent
import io.branch.referral.util.CurrencyType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object BranchIOAnalytics {
    fun pushToBranch(event: BRANCH_STANDARD_EVENT, extras: HashMap<String, String>? = null) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val branchEvent = BranchEvent(event)
                extras?.let {
                    for ((k, v) in it) {
                        branchEvent.addCustomDataProperty(k, v)
                        println("$k = $v")
                    }
                    branchEvent.setTransactionID(extras["payment_id"])
                    branchEvent.setCurrency(CurrencyType.INR)
                    extras["amount"]?.toDouble()?.run {
                        branchEvent.setRevenue(this)
                    }
                }
                branchEvent.logEvent(AppObjectController.joshApplication)

            } catch (ex: Exception) {
                Crashlytics.logException(ex)
                ex.printStackTrace()
            }
        }

    }

}