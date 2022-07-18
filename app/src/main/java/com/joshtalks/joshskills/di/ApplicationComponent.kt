package com.joshtalks.joshskills.di

import android.app.Application
import com.joshtalks.joshskills.core.JoshApplication
import com.joshtalks.joshskills.feature.authentication.di.AuthComponent
import dagger.BindsInstance
import dagger.Component

@AppScope
@Component(modules = [
    ApplicationModule::class,
    NetworkModule::class
])
interface ApplicationComponent {
    fun getAuthComponent() : AuthComponent
    fun inject(joshApplication: JoshApplication)

    @Component.Builder
    interface Builder {
        @BindsInstance
        fun application(application: Application) : Builder

        fun build() : ApplicationComponent
    }
}