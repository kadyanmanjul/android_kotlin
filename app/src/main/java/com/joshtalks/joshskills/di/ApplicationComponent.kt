package com.joshtalks.joshskills.di

import com.joshtalks.joshskills.ui.voip.SearchingUserActivity
import dagger.Component

@Component
interface ApplicationComponent {
    fun inject(activity: SearchingUserActivity)
}