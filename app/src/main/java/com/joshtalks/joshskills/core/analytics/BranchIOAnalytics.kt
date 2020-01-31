package com.joshtalks.joshskills.core.analytics

import com.joshtalks.joshskills.core.AppObjectController
import io.branch.referral.util.BRANCH_STANDARD_EVENT
import io.branch.referral.util.BranchEvent
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
                }
                branchEvent.logEvent(AppObjectController.joshApplication)

                /*if (extras == null) {
                    BranchEvent(event).addContentItems()
                        .logEvent(AppObjectController.joshApplication)
                } else {


                    BranchUniversalObject().userCompletedAction(event, extras)
                }
*/

            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

}