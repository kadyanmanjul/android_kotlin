package com.joshtalks.joshskills.core.analytics

import android.os.Bundle
import com.facebook.appevents.AppEventsConstants
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.JoshSkillExecutors
import com.joshtalks.joshskills.core.RegistrationMethods
import io.branch.referral.util.BRANCH_STANDARD_EVENT
import io.branch.referral.util.CurrencyType

object MarketingAnalytics {

    fun completeRegistrationAnalytics(
        userExist: Boolean,
        registrationMethod: RegistrationMethods
    ) {
        JoshSkillExecutors.BOUNDED.submit {
            if (userExist) {
                BranchIOAnalytics.pushToBranch(BRANCH_STANDARD_EVENT.LOGIN)
            } else {
                BranchIOAnalytics.pushToBranch(BRANCH_STANDARD_EVENT.COMPLETE_REGISTRATION)

                val params = Bundle().apply {
                    putString(
                        AppEventsConstants.EVENT_PARAM_REGISTRATION_METHOD,
                        registrationMethod.type
                    )
                    putString(
                        AppEventsConstants.EVENT_PARAM_SUCCESS,
                        AppEventsConstants.EVENT_PARAM_VALUE_YES
                    )
                    putString(AppEventsConstants.EVENT_PARAM_CURRENCY, CurrencyType.INR.name)
                }


                AppObjectController.facebookEventLogger.logEvent(
                    AppEventsConstants.EVENT_NAME_COMPLETED_REGISTRATION,
                    params
                )
            }
        }
    }

}
