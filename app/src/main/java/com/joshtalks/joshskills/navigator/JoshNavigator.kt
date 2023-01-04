package com.joshtalks.joshskills.navigator

import android.content.Context
import android.content.Intent
import com.joshtalks.joshskills.LauncherActivity
import com.joshtalks.joshskills.auth.freetrail.FreeTrialOnBoardActivity
import com.joshtalks.joshskills.auth.freetrail.SignUpActivity
import com.joshtalks.joshskills.buypage.new_buy_page_layout.BuyPageActivity
import com.joshtalks.joshskills.common.core.*
import com.joshtalks.joshskills.expertcall.CallWithExpertActivity
import com.joshtalks.joshskills.explore.CourseExploreActivity
import com.joshtalks.joshskills.explore.course_details.CourseDetailsActivity
import com.joshtalks.joshskills.groups.JoshGroupActivity
import com.joshtalks.joshskills.leaderboard.LeaderBoardViewPagerActivity
import com.joshtalks.joshskills.notification.StickyNotificationService
import com.joshtalks.joshskills.settings.SettingsActivity

object JoshNavigator : Navigator {
    override fun with(context: Context): Navigator.Navigate {
        return object : Navigator.Navigate {
            override fun navigate(contract: Contract) {
                when(contract) {
                    is SplashContract -> LauncherActivity.openLauncherActivity(contract, context)
                    is GroupsContract -> JoshGroupActivity.openGroupsActivity(contract, context)
                    is SettingsContract -> SettingsActivity.openSettingsActivity(contract, context)
                    is ExpertCallContract -> CallWithExpertActivity.openExpertActivity(contract, context)
                    is LeaderboardContract -> LeaderBoardViewPagerActivity.openLeaderboardActivity(contract, context)
                    is OnBoardingContract -> FreeTrialOnBoardActivity.openFreeTrialOnBoardActivity(contract, context)
                    is SignUpContract -> SignUpActivity.openSignUpActivity(contract, context)
                    is CourseExploreContract -> CourseExploreActivity.openCourseExploreActivity(contract, context)
                    is CourseDetailContract -> CourseDetailsActivity.openCourseDetailsActivity(contract, context)
                    is BuyPageContract -> BuyPageActivity.openBuyPageActivity(contract, context)
                }
            }

             override fun serviceProvider(connection: Connection): Intent {
                return when (connection) {
                    is StickyServiceConnection -> Intent(context, StickyNotificationService::class.java)
                    else -> throw IllegalStateException("Invalid Connection")
                }
            }
        }
    }
}