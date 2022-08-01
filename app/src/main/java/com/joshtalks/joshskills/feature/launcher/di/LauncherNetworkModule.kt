package com.joshtalks.joshskills.feature.launcher.di

import com.joshtalks.joshskills.di.annotation.ActivityScope
import com.joshtalks.joshskills.feature.launcher.network.LauncherNetworkService
import dagger.Module
import dagger.Provides
import dagger.Reusable
import retrofit2.Retrofit

@Module
class LauncherNetworkModule {
    @Reusable
    @Provides
    fun provideLauncherNetworkService(retrofit: Retrofit) : LauncherNetworkService {
        return retrofit.create(LauncherNetworkService::class.java)
    }
}