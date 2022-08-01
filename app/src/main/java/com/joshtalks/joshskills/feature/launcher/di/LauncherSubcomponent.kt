package com.joshtalks.joshskills.feature.launcher.di

import androidx.fragment.app.FragmentActivity
import com.joshtalks.joshskills.core.JoshApplication
import com.joshtalks.joshskills.di.annotation.ActivityScope
import com.joshtalks.joshskills.di.components.ApplicationComponent
import com.joshtalks.joshskills.feature.launcher.LauncherActivity
import dagger.Component
import dagger.Subcomponent

@Subcomponent(modules = [LauncherViewModelModule::class, LauncherNetworkModule::class])
interface LauncherSubcomponent {
    fun inject(activity : LauncherActivity)

    @Subcomponent.Builder
    interface Builder {
        fun build() : LauncherSubcomponent
    }
}


fun FragmentActivity.component() : ApplicationComponent {
    return (application as JoshApplication)
        .applicationGraph
}