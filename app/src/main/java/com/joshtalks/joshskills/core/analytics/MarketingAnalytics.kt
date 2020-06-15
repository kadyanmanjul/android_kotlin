package com.joshtalks.joshskills.core.analytics

import android.os.Bundle
import com.facebook.appevents.AppEventsConstants
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.RegistrationMethods
import com.joshtalks.joshskills.repository.server.CourseExploreModel
import io.branch.referral.util.BRANCH_STANDARD_EVENT
import io.branch.referral.util.BranchEvent
import io.branch.referral.util.CurrencyType

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

    //todo  setup locale according user country
    fun courseViewAnalytics(courseModel: CourseExploreModel) {
        //Facebook start
        val params = Bundle()
        params.putString(AppEventsConstants.EVENT_PARAM_CONTENT_ID, courseModel.id.toString())
        params.putString(AppEventsConstants.EVENT_PARAM_CONTENT_TYPE, "Course")
        params.putString(AppEventsConstants.EVENT_PARAM_CONTENT, courseModel.toString())
        params.putString(AppEventsConstants.EVENT_PARAM_CURRENCY, CurrencyType.INR.name)
        params.putString(AppEventsConstants.EVENT_PARAM_SEARCH_STRING, courseModel.courseName)
        params.putString(AppEventsConstants.EVENT_PARAM_VALUE_TO_SUM, courseModel.amount.toString())
        params.putString(
            AppEventsConstants.EVENT_PARAM_SUCCESS,
            AppEventsConstants.EVENT_PARAM_VALUE_YES
        )

        AppObjectController.facebookEventLogger.logEvent(
            AppEventsConstants.EVENT_NAME_VIEWED_CONTENT,
            params
        )
        //Facebook end


        //Branch start
        val extras: HashMap<String, String> = HashMap()
        extras["test_id"] = courseModel.id.toString()
        extras["course_name"] = courseModel.courseName
        extras["name"] = courseModel.courseName
        extras["amount"] = courseModel.amount.toString()
        extras["\$price"] = courseModel.amount.toString()

        val branchEvent = BranchEvent(BRANCH_STANDARD_EVENT.VIEW_ITEM)
        branchEvent.setCurrency(CurrencyType.INR)
            .setDescription(courseModel.courseDuration)
            .setSearchQuery(courseModel.courseName)

        extras.let {
            for ((k, v) in it) {
                branchEvent.addCustomDataProperty(k, v)
                println("$k = $v")
            }
        }

        branchEvent.logEvent(AppObjectController.joshApplication)
        //Branch end
    }

    fun initCheckoutCourseAnalytics(testId: String) {
        val params = Bundle().apply {
            putString(AppEventsConstants.EVENT_PARAM_CONTENT_ID, testId)
            putString(
                AppEventsConstants.EVENT_PARAM_SUCCESS,
                AppEventsConstants.EVENT_PARAM_VALUE_YES
            )
        }
        AppObjectController.facebookEventLogger.logEvent(
            AppEventsConstants.EVENT_NAME_INITIATED_CHECKOUT,
            params
        )
    }

}