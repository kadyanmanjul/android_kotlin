package com.joshtalks.joshskills.common.di

import com.joshtalks.joshskills.common.core.AppObjectController
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