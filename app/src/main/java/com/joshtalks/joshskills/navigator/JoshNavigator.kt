package com.joshtalks.joshskills.navigator

import android.content.Context
import com.joshtalks.joshskills.LauncherActivity
import com.joshtalks.joshskills.auth.freetrail.FreeTrialOnBoardActivity
import com.joshtalks.joshskills.auth.freetrail.SignUpActivity
import com.joshtalks.joshskills.common.core.*
import com.joshtalks.joshskills.expertcall.CallWithExpertActivity
import com.joshtalks.joshskills.groups.JoshGroupActivity
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
                    is LeaderboardContract -> {}
                    is OnBoardingContract -> FreeTrialOnBoardActivity.openFreeTrialOnBoardActivity(contract, context)
                    is SignUpContract -> SignUpActivity.openSignUpActivity(contract, context)
                }
            }
        }
    }
}