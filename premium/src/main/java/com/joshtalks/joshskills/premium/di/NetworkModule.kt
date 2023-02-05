package com.joshtalks.joshskills.premium.di

import com.joshtalks.joshskills.premium.core.AppObjectController
import dagger.Module
import dagger.Provides
import retrofit2.Retrofit

@Module
class NetworkModule {
    @AppScope
    @Provides
    fun provideRetrofit() : Retrofit {
        return AppObjectController.retrofit
    }
}