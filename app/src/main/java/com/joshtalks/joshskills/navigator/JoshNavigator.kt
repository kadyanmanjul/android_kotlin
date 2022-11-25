package com.joshtalks.joshskills.navigator

import android.content.Context
import com.joshtalks.joshskills.common.core.Contract
import com.joshtalks.joshskills.common.core.SplashContract
import com.joshtalks.joshskills.common.core.Navigator
import com.joshtalks.joshskills.LauncherActivity

object JoshNavigator : Navigator {
    override fun with(context: Context): Navigator.Navigate {
        return object : Navigator.Navigate {
            override fun navigate(contract: Contract) {
                when(contract) {
                    is SplashContract -> LauncherActivity.openLauncherActivity(contract, context)
                }
            }
        }
    }
}