package com.joshtalks.joshskills.premium.di

import com.joshtalks.joshskills.premium.ui.group.JoshGroupActivity
import dagger.Component

@AppScope
@Component(modules = [
    NetworkModule::class
])
interface ApplicationComponent {
    fun inject(activity: JoshGroupActivity)
}