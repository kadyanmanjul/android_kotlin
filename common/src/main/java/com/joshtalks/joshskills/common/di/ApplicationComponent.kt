package com.joshtalks.joshskills.common.di

import com.joshtalks.joshskills.common.ui.group.JoshGroupActivity
import dagger.Component

@AppScope
@Component(modules = [
    NetworkModule::class
])
interface ApplicationComponent {
    fun inject(activity: JoshGroupActivity)
}