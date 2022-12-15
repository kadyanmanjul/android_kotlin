package com.joshtalks.joshskills.common.di

import dagger.Component

@AppScope
@Component(modules = [
    NetworkModule::class
])
interface ApplicationComponent {

}