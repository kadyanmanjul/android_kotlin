package com.joshtalks.joshskills.core.analytics

import android.os.Bundle
import com.facebook.appevents.AppEventsConstants
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.RegistrationMethods
import io.branch.referral.util.BRANCH_STANDARD_EVENT

object MarketingAnalytics {

    fun completeRegistrationAnalytics(
        userExist: Boolean,
        registrationMethod: RegistrationMethods
    ) {
        if (userExist) {
            BranchIOAnalytics.pushToBranch(BRANCH_STANDARD_EVENT.LOGIN)
        } else {
            BranchIOAnalytics.pushToBranch(BRANCH_STANDARD_EVENT.COMPLETE_REGISTRATION)

            val params = Bundle()
            params.putString(
                AppEventsConstants.EVENT_PARAM_REGISTRATION_METHOD,
                registrationMethod.type
            )
            params.putString(
                AppEventsConstants.EVENT_PARAM_SUCCESS,
                AppEventsConstants.EVENT_PARAM_VALUE_YES
            )

            AppObjectController.facebookEventLogger.logEvent(
                AppEventsConstants.EVENT_NAME_COMPLETED_REGISTRATION,
                params
            )
        }
    }

}
