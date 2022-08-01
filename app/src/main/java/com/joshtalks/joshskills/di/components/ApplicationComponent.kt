package com.joshtalks.joshskills.di.components

import android.app.Application
import com.joshtalks.joshskills.core.JoshApplication
import com.joshtalks.joshskills.di.annotation.AppScope
import com.joshtalks.joshskills.di.module.ApplicationModule
import com.joshtalks.joshskills.di.module.BaseViewModelModule
import com.joshtalks.joshskills.di.module.NetworkModule
import com.joshtalks.joshskills.di.module.SubcomponentModule
import com.joshtalks.joshskills.di.viewmodel.ViewModelFactoryModule
import com.joshtalks.joshskills.feature.launcher.di.LauncherSubcomponent
import dagger.BindsInstance
import dagger.Component

@AppScope
@Component(modules = [
    ApplicationModule::class,
    NetworkModule::class,
    ViewModelFactoryModule::class,
    SubcomponentModule::class
])
interface ApplicationComponent {
    fun getLauncherComponentBuilder() : LauncherSubcomponent.Builder
    fun inject(joshApplication: JoshApplication)

    @Component.Builder
    interface Builder {
        @BindsInstance
        fun application(application: Application) : Builder

        fun build() : ApplicationComponent
    }
}