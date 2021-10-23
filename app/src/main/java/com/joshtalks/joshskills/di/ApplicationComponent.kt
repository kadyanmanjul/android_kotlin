package com.joshtalks.joshskills.di

import com.joshtalks.joshskills.ui.group.JoshGroupActivity
import com.joshtalks.joshskills.ui.voip.SearchingUserActivity
import dagger.Component

@AppScope
@Component(modules = [
    NetworkModule::class
])
interface ApplicationComponent {
    fun inject(activity: JoshGroupActivity)
}